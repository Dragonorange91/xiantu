import bpy,json,math
from pathlib import Path
from mathutils import Vector
OUT=Path(r'asset_sources\creature-batch-20261016')
rows=json.loads((OUT/'source-inventory.json').read_text(encoding='utf-8'))
report=[]
for row in rows:
 for o in list(bpy.data.objects):bpy.data.objects.remove(o,do_unlink=True)
 bpy.ops.import_scene.gltf(filepath=str(OUT/'sources'/(row['name']+'.glb')))
 mesh=next(o for o in bpy.context.scene.objects if o.type=='MESH')
 points=[mesh.matrix_world@v.co for v in mesh.data.vertices];lo=Vector([min(p[i] for p in points) for i in range(3)]);hi=Vector([max(p[i] for p in points) for i in range(3)]);center=(lo+hi)/2;extent=max(hi-lo)
 camdata=bpy.data.cameras.new('Audit');cam=bpy.data.objects.new('Audit',camdata);bpy.context.scene.collection.objects.link(cam);camdata.type='ORTHO';camdata.ortho_scale=extent*1.25;scene=bpy.context.scene;scene.camera=cam
 scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='TEXTURE';scene.display.shading.light='STUDIO';scene.display.shading.background_type='WORLD';scene.world.color=(.23,.23,.23);scene.render.resolution_x=512;scene.render.resolution_y=512;scene.render.resolution_percentage=100
 for label,offset in [('front',(0,-3,.05)),('side',(3,0,.05)),('top',(0,0,3))]:
  cam.location=center+Vector(offset)*extent;cam.rotation_euler=(center-cam.location).to_track_quat('-Z','Y').to_euler();scene.render.filepath=str(OUT/'previews'/(row['name']+'_'+label+'.png'));bpy.ops.render.render(write_still=True)
 report.append({'name':row['name'],'bounds':[list(lo),list(hi)],'vertices':len(mesh.data.vertices),'faces':len(mesh.data.polygons)})
 (OUT/'geometry-audit.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print('AUDIT_RENDER_COMPLETE',len(report))
