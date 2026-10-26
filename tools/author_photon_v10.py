"""Refine selected live Photon effects with real motion and layered silhouettes."""
import copy
import hashlib
import json
import math
import shutil
import sys
from pathlib import Path

import nbtlib as n
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
import author_photon_v9 as v9

ASSETS = ROOT / "src/main/resources/assets/xiantu"
FX_ROOT = ASSETS / "fx/beasts"
OUT = ROOT / "asset_sources/beast-vfx-v10-20261018"
BACKUP = OUT / "pre-edit"
TEXTURES = ASSETS / "textures/vfx/v10"
CHANGED = []
ARCHIVED_BOSS_IDS = {"battle_general_soul", "xuantians_remnant", "cloud_sea_lord"}


def curve(points):
    return n.Compound({
        "_type": n.String("Curve"),
        "curves": n.List[n.List[n.Float]]([n.List[n.Float](points)]),
        "defaultValue": n.Float(0), "lockControlPoint": n.Byte(1),
        "lower": n.Float(0), "upper": n.Float(2),
        "min": n.Float(-100), "max": n.Float(100),
        "xAxis": n.String("life"), "yAxis": n.String("velocity"),
    })


def set_velocity(obj, xyz):
    cfg = obj["config"]["velocityOverLifetime"]
    cfg["enable"] = n.Byte(1)
    cfg["linear"] = n.Compound({axis: v9.c(value) for axis, value in zip("xyz", xyz)})


def read_objects(stage):
    path = FX_ROOT / stage
    raw = n.load(path)
    return path, raw, raw["fx"]["mainFX"]["fxObjects"]


def save(stage, edit):
    path, raw, objects = read_objects(stage)
    if stage not in CHANGED:
        backup = BACKUP / stage
        backup.parent.mkdir(parents=True, exist_ok=True)
        if not backup.exists():
            shutil.copy2(path, backup)
        CHANGED.append(stage)
    edit(objects)
    n.File(raw, gzipped=True).save(path)


def set_texture(obj, name):
    obj["config"]["material"]["material"]["texture"] = n.String(f"xiantu:textures/vfx/v10/{name}.png")


def set_color(obj, rgb):
    value = (0xFF000000 | int(rgb, 16)) & 0xFFFFFFFF
    if value >= 0x80000000:
        value -= 0x100000000
    obj["config"]["startColor"] = n.Compound({
        "_type": n.String("Color"), "number": n.Int(value),
    })


def set_rate_lifetime(obj, rate, life, duration):
    cfg = obj["config"]
    cfg["emission"]["emissionRate"] = v9.c(rate)
    cfg["maxParticles"] = n.Int(max(4, min(64, math.ceil(rate * life) + 2)))
    cfg["startLifetime"] = v9.c(life)
    cfg["duration"] = n.Int(duration)
    cfg["looping"] = n.Byte(0)


def set_size(obj, xyz, expand=False):
    cfg = obj["config"]
    cfg["startSize"] = n.Compound({axis: v9.c(value) for axis, value in zip("xyz", xyz)})
    if expand:
        cfg["sizeOverLifetime"]["enable"] = n.Byte(1)
        points = [0, .25, .15, .8, .65, 1.15, 1, 1.55]
        cfg["sizeOverLifetime"]["size"] = n.Compound({axis: curve(points) for axis in "xyz"})


def animate_rotation(obj, degrees):
    cfg = obj["config"]["rotationOverLifetime"]
    cfg["enable"] = n.Byte(1)
    cfg["roll"] = curve([0, 0, .3, degrees * .3, .7, degrees * .7, 1, degrees])


def set_position(obj, xyz):
    obj["transform"]["localPosition"] = v9.vec(*xyz)


def make_flow_texture(name, source_box):
    master = Image.open(ROOT / "asset_sources/beast-vfx-v9-20261018/master-atlas.png").convert("RGB")
    sw, sh = master.size
    x0, y0, x1, y1 = source_box
    src = np.asarray(master.crop((x0 * sw // 4, y0 * sh // 4,
                                  (x0 + 1) * sw // 4, (y0 + 1) * sh // 4)).resize((256, 256), Image.Resampling.LANCZOS), dtype=np.float32)
    lum = src.max(2)
    alpha = np.uint8(np.clip((lum - 7) * 1.10, 0, 255))
    yy, xx = np.mgrid[:256, :256]
    atlas = Image.new("RGBA", (1024, 1024), (255, 255, 255, 0))
    for frame in range(16):
        phase = frame * math.tau / 16
        # Smooth coherent advection; each frame remains seamless at the borders.
        dx = 10 * np.sin(yy * .034 - phase) * np.sin(xx * math.pi / 255)
        dy = 7 * np.sin(xx * .029 + phase * 1.3) * np.sin(yy * math.pi / 255)
        sx = np.clip(np.rint(xx + dx), 0, 255).astype(int)
        sy = np.clip(np.rint(yy + dy), 0, 255).astype(int)
        frame_rgba = np.empty((256, 256, 4), dtype=np.uint8)
        frame_rgba[:, :, :3] = 255
        frame_rgba[:, :, 3] = alpha[sy, sx]
        atlas.paste(Image.fromarray(frame_rgba, "RGBA"), ((frame % 4) * 256, (frame // 4) * 256))
    atlas.save(TEXTURES / f"{name}.png")


def textures():
    TEXTURES.mkdir(parents=True, exist_ok=True)
    boxes = {
        "fox_core": (0, 0), "fox_flame": (1, 0), "fox_ember": (3, 3),
        "fist_vortex": (0, 3), "fist_arc": (0, 1), "fist_flash": (1, 3),
        "fist_smoke": (1, 2), "fist_wave": (3, 1), "fist_guard": (2, 3),
    }
    master = Image.open(ROOT / "asset_sources/beast-vfx-v9-20261018/master-atlas.png").convert("RGB")
    sw, sh = master.size
    for name, (cx, cy) in boxes.items():
        make_flow_texture(name, (cx, cy, cx, cy))
    (OUT / "textures.json").write_text(json.dumps({"source": "master-atlas.png", "textures": boxes}, indent=2), encoding="utf-8")


def fox_projectile():
    def edit(objects):
        assert len(objects) == 3
        shell, core, tail = objects
        set_texture(shell, "fox_flame")
        set_size(shell, (1.65, 1.65, 1.65))
        set_rate_lifetime(shell, .25, 6, 64)
        shell["config"]["simulationSpace"] = n.String("Local")
        animate_rotation(shell, 1100)
        shell["config"]["uvAnimation"]["cycle"] = n.Float(.72)
        shell["config"]["uvAnimation"]["frameOverTime"] = curve([0, 0, .33, .33, .66, .66, 1, 1])

        set_texture(core, "fox_core")
        set_size(core, (1.05, 1.05, 1.05))
        set_rate_lifetime(core, .20, 5, 64)
        core["config"]["simulationSpace"] = n.String("Local")
        animate_rotation(core, -360)

        set_texture(tail, "fox_ember")
        set_size(tail, (.9, 1.55, .9))
        set_position(tail, (0, 0, -.35))
        set_rate_lifetime(tail, .95, 8, 64)
        tail["config"]["simulationSpace"] = n.String("World")
        set_velocity(tail, (0, 0, -1.6))
        animate_rotation(tail, 520)
    save("yanwei_fox/projectile.fx", edit)


def fox_impact():
    def edit(objects):
        assert len(objects) == 3
        shell, core, ember = objects
        for obj, texture, size, life in (
            (shell, "fox_flame", (2.7, 2.7, 2.7), 10),
            (core, "fox_core", (1.3, 1.3, 1.3), 5),
            (ember, "fox_ember", (.55, .8, .55), 10),
        ):
            set_texture(obj, texture)
            set_size(obj, size, expand=True)
            set_rate_lifetime(obj, 2.4 if obj is ember else .5, life, 2)
            obj["config"]["simulationSpace"] = n.String("World")
        set_velocity(ember, (0, 2.0, 0))
        animate_rotation(shell, 740)
    save("yanwei_fox/impact.fx", edit)


def city_charge():
    def edit(objects):
        assert len(objects) == 2
        vortex, motes = objects
        set_texture(vortex, "fist_vortex")
        set_color(vortex, "EAB35A")
        set_size(vortex, (1.55, 1.55, .72))
        set_rate_lifetime(vortex, .50, 10, 24)
        animate_rotation(vortex, 680)
        set_texture(motes, "fox_ember")
        set_color(motes, "B9E9EE")
        set_size(motes, (.28, .28, .28))
        set_rate_lifetime(motes, .65, 10, 24)
        motes["config"]["simulationSpace"] = n.String("Local")
        set_velocity(motes, (0, .5, 0))
    save("xuantians_remnant/attack/charge.fx", edit)


def city_releases():
    specs = {
        "xuantians_remnant/attack/release.fx": ("fist_arc", 3, 0, 1.0, 1.8, 3.0, .82, 0),
        "xuantians_remnant/sword_array/release.fx": ("fist_arc", 3, 0, 1.2, 2.2, 3.8, .72, 1),
        "xuantians_remnant/wall_fall/release.fx": ("fist_wave", 6, 0, .55, 4.8, 1.4, .92, 2),
        "xuantians_remnant/soul_phantoms/release.fx": ("fist_smoke", 4, 1, .45, 1.4, 2.4, .20, 3),
        "xuantians_remnant/city_seal/release.fx": ("fist_guard", 4, 0, 1.2, 2.6, 2.8, .15, 4),
        "xuantians_remnant/final_city/release.fx": ("fist_vortex", 6, 0, 1.0, 4.8, 3.8, 1.18, 5),
    }
    for stage, (texture, count, _, _height, span, depth, speed, style) in specs.items():
        def edit(objects, texture=texture, count=count, height=_height, span=span, depth=depth,
                 speed=speed, style=style, stage=stage):
            # Replace generic fixed points with a compact authored attack graph.
            template = copy.deepcopy(objects[0])
            made = []
            for i in range(count):
                obj = copy.deepcopy(template)
                set_texture(obj, texture)
                set_color(obj, "FFF0CF" if i % 3 == 1 else "DCA647")
                life = 18 if style in (2, 3, 4) else 13
                set_rate_lifetime(obj, .70 if i < 2 else .35, life, 1)
                obj["config"]["simulationSpace"] = n.String("World")
                if style == 2:
                    angle = i * math.tau / count
                    radius = .22
                    set_position(obj, (math.sin(angle) * radius, .35, math.cos(angle) * radius))
                    set_size(obj, (2.9, .42, 2.9), expand=True)
                    set_velocity(obj, (math.sin(angle) * speed * 2, 0, math.cos(angle) * speed * 2))
                elif style == 3:
                    side = (i - (count - 1) / 2) * .38
                    set_position(obj, (side, height + (i % 2) * .28, .45))
                    set_size(obj, (1.3, 1.65, 1.05))
                    set_velocity(obj, (side * .7, .08 * (i % 2), .08))
                    animate_rotation(obj, 180 + i * 37)
                elif style == 4:
                    angle = i * math.tau / count
                    set_position(obj, (math.sin(angle) * .85, height, math.cos(angle) * .85))
                    set_size(obj, (1.1, 1.65, .65))
                    set_velocity(obj, (math.sin(angle) * .08, 0, math.cos(angle) * .08))
                    animate_rotation(obj, (-1 if i % 2 else 1) * 240)
                else:
                    side = (i - (count - 1) / 2) * span / max(1, count - 1)
                    y = height + (.24 if i % 2 else 0)
                    z = .72 + (i % 2) * .32
                    set_position(obj, (side, y, z))
                    set_size(obj, (2.0 if style == 5 else 1.8,
                                   .9 if style in (0, 1) else 1.15,
                                   1.1 if style in (0, 1) else 1.6), expand=True)
                    drift = (i - (count - 1) / 2) * .08 if style == 1 else 0
                    set_velocity(obj, (drift, .1 * (i % 2), speed * 20))
                    animate_rotation(obj, (i - count / 2) * 70)
                made.append(obj)
            objects[:] = n.List[n.Compound](made)
        save(stage, edit)


def boss_travel_layers():
    """Give release sprites actual local velocity instead of delayed static pops."""
    manifest = json.loads((ASSETS / "boss_combat.json").read_text(encoding="utf-8"))
    for boss, skills in manifest.items():
        for skill in skills:
            shape = skill["shape"]
            if shape not in ("line", "projectile", "lanes", "fan", "cross", "fall", "fall_sequence"):
                continue
            stage = f"{boss}/{skill['id']}/release.fx"
            try:
                source = BACKUP / stage
                if not source.exists():
                    source = FX_ROOT / stage
                original = n.load(source)
                existing = original["fx"]["mainFX"]["fxObjects"]
            except FileNotFoundError:
                continue
            def edit(objects, shape=shape, active=skill["active"], reach=skill["reach"]):
                for obj in objects:
                    name = str(obj.get("name", ""))
                    cfg = obj["config"]
                    cfg["simulationSpace"] = n.String("World")
                    cfg["duration"] = n.Int(1)
                    pos = obj["transform"]["localPosition"]
                    if shape in ("fall", "fall_sequence") and ("descending" in name):
                        cfg["startDelay"] = v9.c(0)
                        pos["y"] = n.Float(max(3.0, min(7.0, reach + 2.5)))
                        set_velocity(obj, (0, -12.0, 0))
                    elif "launch shell" in name:
                        set_velocity(obj, (0, .05, max(2.5, min(9.0, reach / max(.05, active / 20)))))
                    else:
                        x = float(pos["x"])
                        z = float(pos["z"])
                        if shape in ("line", "projectile", "lanes", "fan", "cross"):
                            # Keep the pattern's lateral lanes but advance them from
                            # the caster at the attack's authored forward speed.
                            pos["z"] = n.Float(.65 + z * .12)
                            set_velocity(obj, (x * .025, 0, max(2.0, min(9.0, reach / max(.08, active / 20)))))
                        else:
                            set_velocity(obj, (0, 0, 0))
            save(stage, edit)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    textures()
    fox_projectile()
    fox_impact()
    city_charge()
    city_releases()
    boss_travel_layers()
    records = []
    for stage in CHANGED:
        path = FX_ROOT / stage
        records.append({"fx": stage, "sha256": hashlib.sha256(path.read_bytes()).hexdigest().upper(),
                        "bytes": path.stat().st_size})
    (OUT / "manifest.json").write_text(json.dumps(records, ensure_ascii=False, indent=2), encoding="utf-8")
    old = json.loads((ASSETS / "vfx_v9_preload.json").read_text(encoding="utf-8"))
    merged = dict(old)
    active_effects = {
        effect for effect in old["effects"]
        if not any(effect.startswith(f"xiantu:beasts/{boss}/") for boss in ARCHIVED_BOSS_IDS)
    }
    merged["effects"] = sorted(active_effects | {
        "xiantu:beasts/yanwei_fox/projectile", "xiantu:beasts/yanwei_fox/impact",
    })
    merged["textures"] = sorted(set(old["textures"]) | {
        f"xiantu:textures/vfx/v10/{path.name}" for path in TEXTURES.glob("*.png")
    })
    (ASSETS / "vfx_v10_preload.json").write_text(json.dumps(merged, indent=2), encoding="utf-8")
    print(f"Authored {len(CHANGED)} native FX and {len(list(TEXTURES.glob('*.png')))} textures")


if __name__ == "__main__":
    main()
