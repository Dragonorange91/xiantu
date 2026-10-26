"""Native Blockbench MCP pillar revision; retain floor traces and cushion bytes."""
from pathlib import Path
import base64, json, subprocess, sys, re, hashlib
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'work/visual-revision-20261020/formation'
RUN=ROOT/'asset_sources/workstations-large-20261020'
ASSET=ROOT/'src/main/resources/assets/xiantu'
SECTIONS=('single','bottom','middle','top')
def write(p,v):
 p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def parts(tier,section):
 p=[]
 def box(n,a,b,m):p.append(dict(name=n,**{'from':a,'to':b},material=m))
 def octagon(n,y0,y1,lo,hi,m):
  cut=(hi-lo)/5
  box(n+'_center',[lo+cut,y0,lo],[hi-cut,y1,hi],m)
  box(n+'_west',[lo,y0,lo+cut],[lo+cut,y1,hi-cut],m)
  box(n+'_east',[hi-cut,y0,lo+cut],[hi,y1,hi-cut],m)
 lo=5 if tier==1 else 4;hi=16-lo
 start=0
 if section in ('single','bottom'):
  octagon('carved_plinth',0,1.5,1,15,'stone')
  octagon('bronze_plinth',1.5,2.25,1.75,14.25,'metal')
  octagon('recessed_wood_socket',2.25,3,3,13,'wood');start=3
 end=10 if section in ('single','top') else 16
 # Tapered collars segment the shaft rather than overlaying coplanar faces.
 if section=='middle':
  box('lower_inscribed_shaft',[lo,0,lo],[hi,6.5,hi],'pillar_side')
  octagon('gilded_joint',6.5,7.25,lo-1,hi+1,'metal')
  octagon('jade_joint',7.25,8.5,lo-.5,hi+.5,'crystal')
  octagon('gilded_joint_upper',8.5,9.25,lo-1,hi+1,'metal')
  box('upper_inscribed_shaft',[lo,9.25,lo],[hi,16,hi],'pillar_side')
 else:
  box('inscribed_shaft',[lo,start,lo],[hi,end,hi],'pillar_side')
 if section in ('single','top'):
  octagon('neck_collar',10,10.7,lo-.5,hi+.5,'metal')
  octagon('lotus_crown_base',10.7,11.5,2.25,13.75,'stone')
  octagon('crown_rim',11.5,12,2,14,'metal')
  octagon('crystal_seat',12,12.5,4,12,'wood')
  octagon('jade_heart_lower',12.5,14.25,5,11,'crystal')
  octagon('jade_heart_tip',14.25,16,6.25,9.75,'crystal')
  if tier>=2:
   for i,(x,z) in enumerate(((2,6),(12,6),(6,2),(6,12))):
    box('lotus_guard_'+str(i),[x,12,z],[x+(2 if x in (2,12) else 4),14 if tier==2 else 14.5,z+(4 if x in (2,12) else 2)],'metal')
  if tier==3:
   for i,(x,z) in enumerate(((2,2),(12,2),(2,12),(12,12))):
    box('cloud_corner_'+str(i),[x,12,z],[x+2,13.25,z+2],'stone')
 return p
def main():
 OUT.mkdir(parents=True,exist_ok=True);calls=[{'name':'get_guide','arguments':{'topic':'modeling'}}];manifest=[]
 def call(n,a=None,s=None):
  c={'name':n,'arguments':a or {}}
  if s:c['save']='../visual-revision-20261020/formation/'+s
  calls.append(c)
 for t in (1,2,3):
  for section in SECTIONS:
   name=f'array_{t}_pillar_{section}';p=parts(t,section);mats=list(dict.fromkeys(c['material'] for c in p))
   call('new_project',{'format':'java_block','name':name,'texture_width':64,'texture_height':64})
   for mat in mats:
    path=ASSET/f'textures/block/array_{t}_{mat}.png'
    call('create_texture',{'name':mat,'width':64,'height':64,'data_url':'data:image/png;base64,'+base64.b64encode(path.read_bytes()).decode()})
   cubes=[]
   for c in p:
    d=[c['to'][i]-c['from'][i] for i in range(3)];faces={}
    for f in ('north','south','east','west','up','down'):
     u,v=(d[0],d[2]) if f in ('up','down') else (d[2],d[1]) if f in ('east','west') else (d[0],d[1])
     faces[f]={'texture':c['material'],'uv':[0,0,u*4,v*4]}
    cubes.append({k:v for k,v in c.items() if k!='material'}|{'faces':faces})
   call('add_cubes',{'cubes':cubes})
   call('execute_script',{'code':"Undo.initEdit({textures:Texture.all});Texture.all.forEach((t,i)=>{t.namespace='xiantu';t.folder='block';t.name='array_'+params.tier+'_'+params.materials[i]+'.png';t.id=String(i);});Project.parent='block/block';Undo.finishEdit('Set formation resource IDs');Canvas.updateAll();return {textures:Texture.all.length};",'params':{'tier':t,'materials':mats}})
   call('check_model',{},name+'-check')
   call('execute_script',{'code':"const p=Preview.selected;Canvas.updateAll();const b=new THREE.Box3();Cube.all.forEach(c=>b.expandByObject(c.mesh));const center=b.getCenter(new THREE.Vector3());p.setProjectionMode(false);p.camera.position.copy(center).add(new THREE.Vector3(27,20,-32));p.controls.target.copy(center);p.controls.update();p.camera.lookAt(center);p.render();return {center:center.toArray()};"})
   call('screenshot',{'width':640,'height':640,'annotate':False},name+'-preview')
   call('save_project',{'path':str(OUT/f'{name}.bbmodel')})
   call('export_model',{'codec':'java_block','path':str(OUT/f'{name}.json')})
   manifest.append(dict(name=name,tier=t,section=section,boxes=[[c['from'],c['to']] for c in p]))
 write(OUT/'calls.json',calls);write(OUT/'manifest.json',manifest)
 if '--prepare' in sys.argv:return
 subprocess.run(['C:/Program Files/nodejs/node.exe',str(RUN/'mcp-runner.mjs'),str(OUT/'calls.json')],check=True,stdout=(OUT/'mcp.log').open('w',encoding='utf-8'))
 for row in manifest:
  name=row['name'];model=json.loads((OUT/f'{name}.json').read_text(encoding='utf-8'))
  result=json.loads((OUT/f'{name}-check.json').read_text())
  text=next(c['text'] for c in result['content'] if c['type']=='text');assert json.loads(text)['issue_count']==0,(name,text)
  assert len(model['elements'])==len(row['boxes'])
  for e,b in zip(model['elements'],row['boxes']):assert [e['from'],e['to']]==b
  model['parent']='minecraft:block/block';model['textures']['particle']=model['textures']['0']
  old=ASSET/f'models/block/{name}.json';backup=OUT/'before'/old.name
  if not backup.exists():backup.parent.mkdir(exist_ok=True);backup.write_bytes(old.read_bytes())
  previous=json.loads(old.read_text());model['display']=previous.get('display',{})
  write(old,model)
 java=ROOT/'src/main/java/com/longju/xiantu/formation/FormationVisualShapes.java'
 old=java.read_text(encoding='utf-8');backup=OUT/'before/FormationVisualShapes.java'
 if not backup.exists():backup.write_text(old,encoding='utf-8')
 rows=['    private static final VoxelShape[][] PILLARS = {']
 for t in (1,2,3):
  rows.append('        {')
  for s in SECTIONS:
   boxes=next(r['boxes'] for r in manifest if r['tier']==t and r['section']==s)
   rows.append('            shape(new double[][] {')
   rows.extend('                {'+', '.join(map(str,a+b))+'},' for a,b in boxes)
   rows.append('            }),')
  rows.append('        },')
 rows.append('    };')
 old=re.sub(r'    private static final VoxelShape\[\]\[\] PILLARS = \{.*?    \};','\n'.join(rows),old,flags=re.S)
 java.write_text(old,encoding='utf-8')
 print('Integrated 12 native pillar models and matching collision; traces and cushion unchanged.')
if __name__=='__main__':main()

