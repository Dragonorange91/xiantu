"""Serial image-API production for stone decoration variants; never paint substitute art."""
import subprocess
import sys
from pathlib import Path
from PIL import Image
from generate_building_material_resources import FAMILIES, MASTER

API = Path.home() / ".codex/skills/image-api-generator/scripts/generate_image.py"
BRIEFS = {
    "black_rock": (
        "Smooth Profound Formation Stone: dark charcoal basalt, broad dense mineral plates, compact angular smoky gray clusters, very sparse muted metallic veins. Remove all engraved formation markings and mortar joints; no ornaments.",
        "Carved Profound Formation Stone: dark basalt, one clear recessed nested square ritual formation medallion with angular geometric channels and a worn stone border. Subtle muted bronze inlay in just a few grooves. No text or runic letters; no glow."),
    "cloud_jade": (
        "Smooth Cloud-Pattern Jade Stone: 85 percent calm ivory/celadon mineral slab, broken irregular dark green inclusions and thin branching mineral fissures. Completely remove cloud-scroll shapes, curls, spirals, ornamental white ribbons, diagonal repeated motifs and brick seams. White regions must be broad solid irregular mineral planes; preserve reference palette only. No gloss.",
        "Carved Cloud-Pattern Jade Stone: ivory celadon jade with a large shallow auspicious Chinese cloud scroll relief, curled stepped ribbons crossing diagonally, subtle chipped raised edges. Keep jade material, not brick wall, no glyphs."),
    "spring_stone": (
        "Smooth Courtyard Water Stone: muted gray teal water-worn stone, broad flat worn surfaces and sparse pale water erosion channels, a few darker damp fissures. No carved patterns, no mortar grid, no flowing liquid or plants.",
        "Carved Courtyard Water Stone: gray teal stone with shallow overlapping ripple arcs surrounding a compact lotus relief. Broad recessed channels and chipped aquatic temple stone edges. No blue glowing runes or text; not actual liquid."),
    "cloud_floating_stone": (
        "Smooth Cloudglow Floating Stone: fog gray and pale lavender porous stone, sparse cold blue mineral streaks and tiny warm peach-gold inclusions. Calm broad surfaces, no cloud carvings, no brick seams, no floating fragments.",
        "Carved Cloudglow Floating Stone: fog gray porous stone with horizontal stepped cloud bands and three asymmetrical cloud curls in low relief. Restrained cold blue veins and very small peach-gold inlay accents. Ancient celestial palace material, no glow, no text."),
}


def main():
    failures = []
    for family, briefs in BRIEFS.items():
        for suffix, brief in zip(("smooth", "carved"), briefs):
            name = f"{family}_{suffix}"
            output = MASTER / f"{name}_1024.png"
            if output.exists():
                print(f"EXISTS {name}; retain for inspection", flush=True)
                continue
            prompt = (
                "One seamless square Minecraft Java building block texture, flat orthographic diffuse albedo tile, "
                "not a scene, not a cube, not a texture atlas. Chinese cultivation architecture. " + brief +
                " Preserve reference material identity and palette, but apply the requested unique surface treatment. "
                "Hard square pixel clusters, limited restrained palette, stepped highlights and shadows, no smooth gradients, "
                "no photorealism, no bloom, no vignette, no cast light or drop shadows. "
                "Full opaque texture fills all four edges, 1024x1024 master with readable 64x64 target clusters."
            )
            print(f"BEGIN {name}", flush=True)
            args = [sys.executable, str(API), "--prompt", prompt, "--size", "1024x1024", "--response-format", "omit",
                    "--input-image", str(MASTER / f"{FAMILIES[family]['texture']}_1024.png"), "--output", str(output)]
            for attempt in range(2):
                result = subprocess.run(args)
                if result.returncode == 0 and output.exists():
                    break
                print(f"API failed {name}, attempt {attempt + 1}", flush=True)
            else:
                failures.append(name)
                continue
            with Image.open(output) as source:
                print(f"SOURCE {name}: {source.size}, {source.mode}", flush=True)
                normalized = source.convert("RGBA").resize((1024, 1024), Image.Resampling.LANCZOS)
            normalized.save(output)
            print(f"READY {name}", flush=True)
    print(f"Image phase complete; pending failures: {failures}", flush=True)
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
