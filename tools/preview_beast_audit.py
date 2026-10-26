"""Offline skinned-pose screening with original texture samples; not a game capture."""
import io,json
import numpy as np
from PIL import Image,ImageDraw
from audit_beast_models import Glb,MODELS,OUT,skin_parts

def render(row,clip,t,side):
    g=Glb(MODELS/(row['model']+'.glb'));g.clips['_rest']={'channels':[],'samplers':[]}
    rest=skin_parts(g,g.matrices('_rest',0))*row['scale']
    pts=skin_parts(g,g.matrices(clip,t))*row['scale']
    triangles=[];colors=[];offset=0
    for n in g.nodes:
        if 'mesh' not in n:continue
        for p in g.doc['meshes'][n['mesh']]['primitives']:
            ix=g.accessor(p['indices']).reshape(-1,3).astype(int);a=p['attributes']
            material=g.doc['materials'][p.get('material',0)];pbr=material.get('pbrMetallicRoughness',{})
            color=np.tile(np.array(pbr.get('baseColorFactor',[.7,.7,.7,1]))[:3]*255,(len(ix),1))
            if 'baseColorTexture' in pbr:
                tex=g.doc['textures'][pbr['baseColorTexture']['index']]['source'];v=g.doc['bufferViews'][g.doc['images'][tex]['bufferView']]
                rgb=np.array(Image.open(io.BytesIO(g.binary[v.get('byteOffset',0):v.get('byteOffset',0)+v['byteLength']])).convert('RGB'))
                uv=g.accessor(a['TEXCOORD_0'])[ix].mean(axis=1)%1
                color=rgb[np.minimum((uv[:,1]*rgb.shape[0]).astype(int),rgb.shape[0]-1),np.minimum((uv[:,0]*rgb.shape[1]).astype(int),rgb.shape[1]-1)].astype(float)
            triangles.append(ix+offset);colors.append(color);offset+=len(g.accessor(a['POSITION']))
    ix=np.concatenate(triangles);color=np.concatenate(colors)
    # Fixed framing per model across all clips and viewpoints.
    center=(rest.min(axis=0)+rest.max(axis=0))*.5
    size=np.max(rest.max(axis=0)-rest.min(axis=0))*1.45
    camera=np.array([1.,.20,.12] if side else [.12,.2,1.]);camera/=np.linalg.norm(camera)
    right=np.cross([0,1,0],camera);right/=np.linalg.norm(right);up=np.cross(camera,right)
    p=pts-center;screen=np.column_stack((p@right,-p@up))*410/size+np.array([230,235]);depth=p@camera
    tri=pts[ix];norm=np.cross(tri[:,1]-tri[:,0],tri[:,2]-tri[:,0]);norm/=np.maximum(1e-10,np.linalg.norm(norm,axis=1))[:,None]
    shade=.5+.5*np.abs(norm@np.array([.3,.8,.52]));color=np.clip(color*shade[:,None],0,255).astype(int)
    im=Image.new('RGB',(460,460),(33,39,48));draw=ImageDraw.Draw(im)
    for k in np.argsort(depth[ix].mean(axis=1)):
        draw.polygon([tuple(x) for x in screen[ix[k]]],fill=tuple(color[k]))
    draw.text((8,8),f"{row['id']} {clip.split('.')[-1]} t={t:.2f}",fill='white')
    return im

if __name__=='__main__':
    rows=json.loads((OUT/'models.json').read_text())
    import sys
    for row in rows:
        if len(sys.argv)>1 and row['id'] not in sys.argv[1:]:continue
        move=max((c for c in row['clips'] if c['name'].split('.')[-1] in ('walk','run','fly','swim')),key=lambda c:c['max_edge_growth_blocks'])
        strike=max(row['clips'],key=lambda c:c['max_edge_growth_blocks'])
        canvas=Image.new('RGB',(1380,920))
        for i,(clip,t) in enumerate([('_rest',0),(move['name'],move['worst_edge']['time']),(strike['name'],strike['worst_edge']['time'])]):
            for side in (False,True):canvas.paste(render(row,clip,t,side),(i*460,int(side)*460))
        canvas.save(OUT/(row['id']+'-offline-poses.png'))
        print(row['id'],flush=True)
