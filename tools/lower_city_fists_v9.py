"""Run through Blender MCP. Reauthor only city combat clips from the saved v8 generator."""
from pathlib import Path
import bpy, inspect, json, shutil
import refine_boss_anatomy_v8 as v8
import export_replacement_bosses_v8 as exporter
import completion_authoring as ca
import refine_posture_grip as rp

ROOT=ca.ROOT
OUT=ROOT/'asset_sources/beast-vfx-v9-20261018/city'
OUT.mkdir(parents=True,exist_ok=True)

def run():
    rig,mesh=rp.select_rig('xuan_tian_cheng_zhu')
    if mesh.get('fists_v9'):print('V9 already authored; export remains repeatable');return
    bpy.data.libraries.write(str(OUT/'before-v9.blend'),{bpy.context.scene},fake_user=True,compress=True)
    source=inspect.getsource(v8.city_attacks)
    source=source.replace('-.140,.670','-.140,.640').replace('.018*pre+.045*hit','.018*pre+.038*hit')
    source=source.replace('.63+.13*pre-.13*hit','.60+.13*pre-.13*hit').replace('-.132,.655','-.132,.625').replace('-.12,.625','-.12,.600')
    exec(compile(source,'city_fists_v9','exec'),v8.__dict__)
    v8.city_attacks();v8.smooth_combat();mesh['fists_v9']=True
    exporter.BASE=OUT;exporter.OUT=OUT/'ready';exporter.OUT.mkdir(exist_ok=True)
    meta=ROOT/'asset_sources/boss-anatomy-v8-20261018/ready/xuan_tian_cheng_zhu-manifest.json'
    shutil.copy2(meta,OUT/meta.name)
    exporter.export('xuantians_remnant','xuan_tian_cheng_zhu')
    print('V9_CITY_COMPLETE original mesh/UV/textures and finger binding preserved')

if __name__=='__main__':run()
