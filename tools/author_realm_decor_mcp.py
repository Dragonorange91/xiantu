from pathlib import Path
import json
p=Path('.').resolve();w=p/'work/qa/realm-renewal-20261023';asset=p/'asset_sources/realm-renewal-20261023';asset.mkdir(parents=True,exist_ok=True)
# Native Java-block geometry and proportionate per-face UVs (4 source pixels per model unit).
def cube(name,a,b,tex='stone'):
 dx,dy,dz=[b[i]-a[i] for i in range(3)]
 uv={f:{'uv':[0,0,min(16,width),min(16,height)],'texture':{'stone':'black_rock_smooth.png','carved':'black_rock_carved.png','jade':'azure_cloud_spirit_brick.png','glow':'stone_lamp_core.png'}[tex]} for f,width,height in [('north',dx,dy),('south',dx,dy),('west',dz,dy),('east',dz,dy),('up',dx,dz),('down',dx,dz)]}
 return {'name':name,'from':a,'to':b,'box_uv':False,'autouv':0,'faces':uv}
lantern=[cube('stepped_foot',[1,0,1],[15,2,15]),cube('bevel_plinth',[2,1.8,2],[14,3.5,14]),cube('body',[4,3.4,4],[12,6.4,12],'carved'),cube('lower_rim',[2.5,3,2.5],[13.5,4,13.5]),cube('light_core',[4.5,6.5,4.5],[11.5,11.95,11.5],'glow'),cube('upper_rim',[2.5,11.9,2.5],[13.5,13,13.5]),cube('pagoda_cap',[1.5,13,1.5],[14.5,14.2,14.5]),cube('crown',[4,14.1,4],[12,15,12]),cube('finial',[6.5,14.9,6.5],[9.5,16,9.5],'jade')]
for x in [3,11.5]:
 for z in [3,11.5]:lantern.append(cube(f'frame_{x}_{z}',[x,4,z],[x+1.5,12.2,z+1.5]))
# Omit fully buried faces and the shared roof seam; keep exposed ledges intact.
lantern_hidden={'light_core':['up'],'upper_rim':['up'],'bevel_plinth':['down'],'body':['down'],'lower_rim':['down'],'crown':['down'],'finial':['down']}
for part in lantern:
 for side in lantern_hidden.get(part['name'],['up','down'] if part['name'].startswith('frame_') else []):
  part['faces'].pop(side,None)
reliq=[cube('foot',[1,0,1],[15,1.8,15]),cube('bottom',[2,1.7,2],[14,3,14]),cube('censer',[3,2.9,3],[13,7,13],'carved'),cube('lip',[2,6.8,2],[14,8,14]),cube('ash',[4,7.9,4],[12,8.3,12]),cube('backstone',[2,2,12],[14,14.5,14],'carved'),cube('shelf',[0.8,13.9,10.5],[15.2,15.4,15.2]),cube('jade_tablet',[5.5,8.5,11.6],[10.5,13.5,12.2],'jade')]
for x in [3,11.5]:
 for z in [3,11.5]:reliq.append(cube(f'leg_{x}_{z}',[x,0,z],[x+1.5,4,z+1.5]))
for x in [6,8,10]:reliq.append(cube(f'incense_{x}',[x,8,x%2+7],[x+.35,11.5,x%2+7.35]))
pillar=[cube('foot',[0,0,0],[16,2,16]),cube('plinth',[1,1.9,1],[15,4,15]),cube('shaft',[3,3.9,3],[13,12.3,13],'carved'),cube('top_mould',[1.5,12.2,1.5],[14.5,14,14.5]),cube('cap',[0,13.9,0],[16,16,16])]
for x in [2.3,12.7]:
 for z in [2.3,12.7]:pillar.append(cube(f'edge_{x}_{z}',[x,4,z],[x+1,12.2,z+1]))
# Silhouette first, then mapped stone and jade details. All modeling operations execute in Blockbench.
all=[]
for id,geo in [('realm_spirit_lantern',lantern),('realm_incense_reliquary',reliq),('realm_carved_pillar',pillar)]:
 all.extend([{'name':'new_project','arguments':{'format':'java_block','name':id,'texture_width':16,'texture_height':16},'save':id+'-new'}, {'name':'add_cubes','arguments':{'cubes':[dict(c,faces={f:dict(d,texture=None) for f,d in c['faces'].items()}) for c in geo]},'save':id+'-blockout'}, {'name':'screenshot','arguments':{'view':'front_right','width':768,'height':768,'annotate':False},'save':id+'-blockout-preview'}])
 # import accepted assets; no replacement of established textures.
 for t,f in [('stone','black_rock_smooth.png'),('carved','black_rock_carved.png'),('jade','azure_cloud_spirit_brick.png'),('glow','stone_lamp_core.png')]:
  all.append({'name':'import_texture','arguments':{'path':str(p/'src/main/resources/assets/xiantu/textures/block'/f),'name':t},'save':id+'-tex-'+t})
 for c in geo:all.append({'name':'set_cube_uv','arguments':{'cube':c['name'],'faces':c['faces']},'save':id+'-uv-'+c['name']})
 all.extend([{'name':'check_model','save':id+'-audit'},{'name':'screenshot','arguments':{'view':'front_right','width':900,'height':900,'annotate':False},'save':id+'-preview'},{'name':'save_project','arguments':{'path':str(asset/(id+'.bbmodel'))},'save':id+'-source'},{'name':'export_project','arguments':{'path':str(asset/(id+'.json'))},'save':id+'-export'}])
(w/'author-models.json').write_text(json.dumps(all),encoding='utf8')
