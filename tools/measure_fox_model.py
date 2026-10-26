import bpy,json
from mathutils import Vector
p=r'src\main\resources\assets\xiantu\models\entity\yanwei_fox.glb'
bpy.ops.import_scene.gltf(filepath=p)
bpy.context.view_layer.update()
for o in bpy.context.scene.objects:
 if o.type=='MESH' and o.name!='Cube':
  points=[o.matrix_world@v.co for v in o.data.vertices]
  lo=[min(p[i] for p in points) for i in range(3)];hi=[max(p[i] for p in points) for i in range(3)]
  print('FOX_MEASURE',json.dumps({'object':o.name,'min':lo,'max':hi,'size':[hi[i]-lo[i] for i in range(3)]}))
