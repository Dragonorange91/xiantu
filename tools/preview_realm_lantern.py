"""Textured orthographic preview with per-pixel depth (not a Minecraft capture)."""
from pathlib import Path
import argparse,json
import numpy as np
from PIL import Image

def render(model_path,output):
    root=Path(__file__).resolve().parents[1]
    model=json.loads(Path(model_path).read_text(encoding='utf8'))
    size=800
    rgb=np.full((size,size,3),(39,46,58),dtype=np.uint8)
    depth=np.full((size,size),-np.inf)
    def project(v):
        x,y,z=v
        return [400+(x-z)*20,570+(x+z-16)*7.2-y*24]
    for element in model['elements']:
        assert not element.get('rotation',{}).get('angle',0),'Preview requires axis-aligned lantern'
        x,y,z=element['from'];X,Y,Z=element['to']
        planes={'south':[(x,Y,Z),(X,Y,Z),(X,y,Z),(x,y,Z)],
                'east':[(X,Y,Z),(X,Y,z),(X,y,z),(X,y,Z)],
                'up':[(x,Y,z),(X,Y,z),(X,Y,Z),(x,Y,Z)]}
        for direction,verts in planes.items():
            face=element['faces'].get(direction)
            if not face or face.get('texture') is None:continue
            texture=face['texture'][1:]
            reference=model['textures'][texture].split(':')[1]
            pixels=np.array(Image.open(root/'src/main/resources/assets/xiantu/textures'/f'{reference}.png').convert('RGB'))
            u,v,U,V=face['uv']
            uv=np.array([(u,v),(U,v),(U,V),(u,V)])/16
            screen=np.array([project(p) for p in verts])
            dist=np.array([p[0]+p[2]+.6*p[1] for p in verts])
            for indices in [(0,1,2),(0,2,3)]:
                tri=screen[list(indices)];tuv=uv[list(indices)];td=dist[list(indices)]
                lo=np.maximum(np.floor(tri.min(axis=0)).astype(int),0)
                hi=np.minimum(np.ceil(tri.max(axis=0)).astype(int),size-1)
                if np.any(lo>hi):continue
                gx,gy=np.meshgrid(np.arange(lo[0],hi[0]+1)+.5,np.arange(lo[1],hi[1]+1)+.5)
                A=np.column_stack((tri[1]-tri[0],tri[2]-tri[0]))
                bary=np.stack((gx-tri[0,0],gy-tri[0,1]),axis=-1)@np.linalg.inv(A).T
                b,c=bary[...,0],bary[...,1];a=1-b-c
                current=a*td[0]+b*td[1]+c*td[2]
                target=depth[lo[1]:hi[1]+1,lo[0]:hi[0]+1]
                visible=(a>=-1e-8)&(b>=-1e-8)&(c>=-1e-8)&(current>target)
                coords=a[...,None]*tuv[0]+b[...,None]*tuv[1]+c[...,None]*tuv[2]
                tx=np.clip((coords[...,0]*pixels.shape[1]).astype(int),0,pixels.shape[1]-1)
                ty=np.clip((coords[...,1]*pixels.shape[0]).astype(int),0,pixels.shape[0]-1)
                color=pixels[ty,tx].astype(float)
                if element['name']!='light_core':color*=dict(up=1.12,south=.96,east=.75)[direction]
                region=rgb[lo[1]:hi[1]+1,lo[0]:hi[0]+1]
                region[visible]=np.clip(color[visible],0,255).astype(np.uint8)
                target[visible]=current[visible]
    Image.fromarray(rgb).save(output)

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('model');parser.add_argument('output')
    args=parser.parse_args();render(args.model,args.output)
