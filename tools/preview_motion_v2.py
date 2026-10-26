"""Render full motion cycles through Blender MCP; configure JOBS before exec."""
import bpy
from pathlib import Path
from mathutils import Vector
from bl_ext.blender_org.animal_animator.core import composer
from refine_motion_v2 import reset
OUT=Path(r'asset_sources/animation-readability-v2-20261017/previews')
OUT.mkdir(exist_ok=True)
for ident,clip in JOBS:
    rig=bpy.data.objects[ident+'_rig'];mesh=bpy.data.objects[ident+'_mesh']
    scene=next(s for s in bpy.data.scenes if rig.name in s.objects);bpy.context.window.scene=scene
    scene.render.engine='BLENDER_WORKBENCH';scene.render.resolution_x=640;scene.render.resolution_y=480;scene.render.resolution_percentage=100
    scene.display.shading.color_type='TEXTURE';scene.display.shading.light='STUDIO';scene.display.shading.background_type='WORLD'
    if not scene.world: scene.world=bpy.data.worlds.new('MotionV2World')
    scene.world.color=(.12,.12,.12)
    reset(rig)
    action=next(t.strips[0].action for t in rig.animation_data.nla_tracks if t.name==ident+'.'+clip)
    composer.assign_action(rig,action);start,end=map(int,action.frame_range)
    points=[]
    for f in range(start,end+1,2):
        scene.frame_set(f);ev=mesh.evaluated_get(bpy.context.evaluated_depsgraph_get())
        points.extend(ev.matrix_world @ v.co for v in ev.data.vertices)
    lo=Vector(tuple(min(p[i] for p in points) for i in range(3)));hi=Vector(tuple(max(p[i] for p in points) for i in range(3)))
    target=(lo+hi)/2;size=max(hi-lo)
    cam=scene.camera
    if not cam:
        cam=bpy.data.objects.new('v2_camera',bpy.data.cameras.new('v2_camera'));scene.collection.objects.link(cam);scene.camera=cam
    cam.data.type='ORTHO';cam.data.ortho_scale=size*1.65
    direction=Vector((.6,-3,1.0)) if clip in ('fly','glide') else Vector((3,-.7,.6))
    cam.location=target+direction*size;cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler()
    for i in range(16):
        scene.frame_set(start+(end-start)*i//16)
        scene.render.filepath=str(OUT/f'{ident}_{clip}_{i:02}.png');bpy.ops.render.render(write_still=True)
    reset(rig)
print('Rendered',JOBS)
