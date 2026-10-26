import bpy,bmesh,json,math
from pathlib import Path
from mathutils import Vector,Matrix
from bl_ext.blender_org.animal_animator.core import composer,library,profiles
OUT=Path(r'asset_sources\creature-batch-20261016')
# Always rebuild the candidate from its immutable source.
for o in list(bpy.data.objects):bpy.data.objects.remove(o,do_unlink=True)
bpy.ops.import_scene.gltf(filepath=str(OUT/'sources/焰尾狐.glb'))
mesh=next(o for o in bpy.context.scene.objects if o.type=='MESH');mesh.name='yanwei_fox_mesh'
original=len(mesh.data.polygons)
bm=bmesh.new();bm.from_mesh(mesh.data)
bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=0.000001)
bmesh.ops.dissolve_degenerate(bm,edges=list(bm.edges),dist=0.0000001)
bm.to_mesh(mesh.data);bm.free()
# Preserve UV islands and silhouette; no global decimation before pose QA.
# Body axis is angled relative to scene forward. Rotate the whole model first.
rot=Matrix.Rotation(math.radians(9),4,'Z')
mesh.data.transform(rot)
shift=Vector((.045,0,.338623));mesh.data.transform(Matrix.Translation(shift))
def pt(v):return rot@Vector(v)+shift
rigdata=bpy.data.armatures.new('yanwei_fox_rig');rig=bpy.data.objects.new('yanwei_fox_rig',rigdata);bpy.context.scene.collection.objects.link(rig)
bpy.context.view_layer.objects.active=rig;rig.select_set(True);bpy.ops.object.mode_set(mode='EDIT')
bones={}
def bone(n,h,t,parent=None):
 b=rigdata.edit_bones.new(n);b.head=pt(h);b.tail=pt(t)
 if parent:b.parent=rigdata.edit_bones[parent]
 bones[n]=(h,t)
 return b
bone('root',(-.04,.02,-.33),(-.04,.02,-.23));rigdata.edit_bones['root'].use_deform=False
bone('hips',(-.025,.10,-.04),(-.04,.015,-.015),'root')
bone('spine',(-.04,.015,-.015),(-.07,-.18,.015),'hips')
bone('chest',(-.07,-.18,.015),(-.09,-.26,.075),'spine')
bone('neck',(-.09,-.26,.075),(-.095,-.325,.15),'chest')
bone('head',(-.095,-.325,.15),(-.09,-.435,.145),'neck')
for side,x in [('L',-.015),('R',-.155)]:
 bone('front_upper.'+side,(x,-.24,-.02),(x,-.24,-.155),'chest')
 bone('front_lower.'+side,(x,-.24,-.155),(x,-.29,-.285),'front_upper.'+side)
 bone('front_foot.'+side,(x,-.29,-.285),(x,-.345,-.32),'front_lower.'+side)
for side,x in [('L',.035),('R',-.105)]:
 bone('hind_upper.'+side,(x,.105,-.025),(x,.07,-.165),'hips')
 bone('hind_lower.'+side,(x,.07,-.165),(x,.16,-.285),'hind_upper.'+side)
 bone('hind_foot.'+side,(x,.16,-.285),(x,.105,-.325),'hind_lower.'+side)
tail=[(-.02,.15,.015),(.015,.245,-.035),(.065,.33,-.025),(.12,.405,.025),(.16,.465,.13)]
for i in range(4):bone('tail_'+str(i),tail[i],tail[i+1],'hips' if i==0 else 'tail_'+str(i-1))
bpy.ops.object.mode_set(mode='OBJECT')
bpy.ops.object.select_all(action='DESELECT');mesh.select_set(True);rig.select_set(True);bpy.context.view_layer.objects.active=rig
bpy.ops.object.parent_set(type='ARMATURE_AUTO')
for v in mesh.data.vertices:
 gs=sorted([(g.group,g.weight) for g in v.groups if g.weight>0],key=lambda x:x[1],reverse=True)
 for gi,weight in gs[4:]:mesh.vertex_groups[gi].remove([v.index])
 total=sum(w for _,w in gs[:4])
 if total>0:
  for gi,weight in gs[:4]:mesh.vertex_groups[gi].add([v.index],weight/total,'REPLACE')
unweighted=[v.index for v in mesh.data.vertices if sum(g.weight for g in v.groups)<.999]
if unweighted:raise RuntimeError('Unweighted vertices '+str(len(unweighted)))
# Correct the head's own yaw around its neck, after aligning the torso axis.
pivot=rig.data.bones['head'].head_local.copy()
head_dir=rig.data.bones['head'].tail_local-pivot
correction=-math.atan2(head_dir.x,-head_dir.y)
head_index=mesh.vertex_groups['head'].index
for v in mesh.data.vertices:
 weight=next((g.weight for g in v.groups if g.group==head_index),0.0)
 if weight:v.co=pivot+Matrix.Rotation(correction*weight,3,'Z')@(v.co-pivot)
bpy.context.view_layer.objects.active=rig
bpy.ops.object.mode_set(mode='EDIT')
b=rig.data.edit_bones['head'];b.tail=pivot+Matrix.Rotation(correction,3,'Z')@(b.tail-pivot)
bpy.ops.object.mode_set(mode='OBJECT')

map={'HIPS':[['hips',1]],'SPINE':[['spine',.6],['chest',.4]],'NECK':[['neck',1]],'HEAD':[['head',1]],'TAIL':[['tail_'+str(i),1] for i in range(4)]}
for side in ['L','R']:
 for label,prefix in [('F','front'),('H','hind')]:
  for slot,suffix in [('LEG_UPPER','upper'),('LEG_LOWER','lower'),('FOOT','foot')]:map[label+'_'+slot+'.'+side]=[[prefix+'_'+suffix+'.'+side,1]]
profiles.store_mapping(rig,'WOLF',map);profiles.sync_collection(rig,map)
bpy.context.scene.animal_animator.target=rig
entries=library.anim_entries(['quad_core','wolf'])
print('AVAILABLE',[(a['id'],a.get('label')) for a in entries])
(OUT/'fox-rig-audit.json').write_text(json.dumps({'source_faces':original,'cleaned_faces':len(mesh.data.polygons),'bones':len(rigdata.bones),'unweighted':len(unweighted),'mapping':map,'available_animations':entries},ensure_ascii=False,indent=2),encoding='utf-8')
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'fox-rig-candidate.blend'))
print('RIG_CANDIDATE',len(rigdata.bones),len(mesh.data.vertices))
