import bpy
from pathlib import Path
from mathutils import Vector
p=Path(r"asset_sources\creature-batch-20261016\fox-rig-animated-candidate.blend")
out=p.parent/'previews_animated_candidate'; out.mkdir(exist_ok=True)
bpy.ops.wm.open_mainfile(filepath=str(p)); s=bpy.context.scene
s.render.engine='BLENDER_WORKBENCH'; s.display.shading.light='STUDIO'; s.display.shading.studio_light='paint.sl'; s.display.shading.color_type='TEXTURE'; s.display.shading.show_shadows=True; s.display.shading.show_cavity=True
s.render.resolution_x=800;s.render.resolution_y=650;s.render.resolution_percentage=100;s.render.image_settings.file_format='PNG';s.render.film_transparent=False
world=bpy.data.worlds.new('PreviewWorld'); s.world=world
camd=bpy.data.cameras.new('PreviewCamera'); cam=bpy.data.objects.new('PreviewCamera',camd); s.collection.objects.link(cam); cam.location=(1.7,-2.7,1.1); cam.rotation_euler=(Vector((0,0,.35))-cam.location).to_track_quat('-Z','Y').to_euler(); camd.type='ORTHO';camd.ortho_scale=1.5;s.camera=cam
rig=bpy.data.objects['yanwei_fox_rig']; tracks={t.name.split('.')[-1]:t for t in rig.animation_data.nla_tracks}
for name,frame in [('idle',15),('walk',12),('run',9),('alert',15),('attack',15),('hurt',8),('death',24),('tail',15)]:
 for t in rig.animation_data.nla_tracks:t.mute=(t!=tracks[name])
 s.frame_set(frame);s.render.filepath=str(out/(name+'.png'));bpy.ops.render.render(write_still=True)
print('PREVIEWS',out)
