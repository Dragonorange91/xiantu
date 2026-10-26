import bpy
from pathlib import Path
p=Path(r"asset_sources\creature-batch-20261016\fox-rig-animated-candidate.blend")
out=p.with_suffix('.glb')
bpy.ops.wm.open_mainfile(filepath=str(p))
bpy.ops.object.select_all(action='DESELECT')
for n in ('yanwei_fox_rig','yanwei_fox_mesh'):
 o=bpy.data.objects.get(n)
 if o:o.select_set(True)
bpy.context.view_layer.objects.active=bpy.data.objects.get('yanwei_fox_rig')
bpy.ops.export_scene.gltf(filepath=str(out),export_format='GLB',use_selection=True,export_animations=True,export_animation_mode='NLA_TRACKS',export_skins=True,export_force_sampling=True,export_optimize_animation_size=False)
print('EXPORTED',out)
