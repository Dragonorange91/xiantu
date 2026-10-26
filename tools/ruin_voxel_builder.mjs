/** Offline VoxelBuilder-compatible blueprint authoring. Never calls a game API.
 * Coordinates: X east, Y up, Z south. Each shop's entrance initially faces north.
 * Native NBT encoding and exact-model preview are separate consumers of this map.
 */
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const ROOT=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const AIR='minecraft:air';
const M={stone:'minecraft:stone_bricks',slab:'minecraft:stone_brick_slab[type=bottom,waterlogged=false]',
  paving:'minecraft:andesite',wood:'minecraft:spruce_planks',dark:'minecraft:dark_oak_planks',
  white:'minecraft:white_terracotta',red:'xiantu:town_red_wood',
  post:'xiantu:town_vermilion_column[axis=y]',base:'xiantu:town_vermilion_base[facing=north]',
  capital:'xiantu:town_vermilion_capital[facing=north]',bracket:'xiantu:town_dougong[facing=north]',
  beam:'xiantu:town_vermilion_beam[facing=north]',rail:'xiantu:town_vermilion_railing[facing=north]',
  window:'xiantu:town_walnut_lattice[facing=north]'};
const ns=(s,f)=>`xiantu:${s}[facing=${f}]`;
const dirs=['north','east','south','west'];
function rotateState(s,turns){
  s=s.replace(/facing=(north|east|south|west)/g,(_,v)=>'facing='+dirs[(dirs.indexOf(v)+turns)%4]);
  if(turns%2) s=s.replace(/axis=([xz])/g,(_,v)=>'axis='+(v==='x'?'z':'x'));
  return s;
}
class VoxelBuilder {
  constructor(id,w,h,d){this.id=id;this.size=[w,h,d];this.map=new Map();this.markers=[];this.rooms=[];this.roofs=[];}
  set(x,y,z,block){
    if(![x,y,z].every(Number.isInteger)||x<0||y<0||z<0||x>=this.size[0]||y>=this.size[1]||z>=this.size[2])throw Error(`${this.id}: out of bounds ${x},${y},${z}`);
    this.map.set(`${x},${y},${z}`,[x,y,z,block]);
  }
  get(x,y,z){return this.map.get(`${x},${y},${z}`)?.[3]??AIR;}
  fill(a,b,c,d,e,f,mat){for(let x=a;x<=d;x++)for(let y=b;y<=e;y++)for(let z=c;z<=f;z++)this.set(x,y,z,mat);}
  clear(...args){this.fill(...args,AIR);}
  lineX(a,b,y,z,m){this.fill(a,y,z,b,y,z,m);}
  lineZ(x,a,b,y,m){this.fill(x,y,a,x,y,b,m);}
  walls(x,y,z,X,Y,Z,m){this.fill(x,y,z,X,Y,z,m);this.fill(x,y,Z,X,Y,Z,m);this.fill(x,y,z,x,Y,Z,m);this.fill(X,y,z,X,Y,Z,m);}
  marker(type,pos,extra={}){this.markers.push({type,position:pos,...extra});}
  placeComponent(other,ox,oy,oz,turns=0){
    const [w,,d]=other.size;
    for(const [x,y,z,s]of other.map.values()){
      let a=x,c=z;if(turns===1){a=d-1-z;c=x;}if(turns===2){a=w-1-x;c=d-1-z;}if(turns===3){a=z;c=w-1-x;}
      this.set(ox+a,oy+y,oz+c,rotateState(s,turns));
    }
    for(const m of other.markers){let[x,y,z]=m.position;
      if(turns===1)[x,z]=[d-1-z,x];else if(turns===2)[x,z]=[w-1-x,d-1-z];else if(turns===3)[x,z]=[z,w-1-x];
      this.markers.push({...m,position:[ox+x,oy+y,oz+z],owner:other.id,facing:m.facing?dirs[(dirs.indexOf(m.facing)+turns)%4]:undefined});
    }
  }
  /** Hip or gable roof. Low/high native modules give a continuous half-block pitch.
   * Gable infills belong to this roof generator; no hand-built triangular patches.
   */
  drawRoofBounds(x,y,z,X,Z,height,style,material,opts={}){
    this.roofs.push({bounds:[x,y,z,X,y+height,Z],style,opening:opts.opening??null});
    const center=Math.floor((z+Z)/2);
    for(let a=x;a<=X;a++)for(let c=z;c<=Z;c++){
      if(opts.opening&&a>=opts.opening[0]&&a<=opts.opening[2]&&c>=opts.opening[1]&&c<=opts.opening[3])continue;
      const ds=[c-z,X-a,Z-c,a-x];
      let t=style==='gable'?Math.min(ds[0],ds[2]):Math.min(...ds);
      let dir=style==='gable'?(c<=center?'north':'south'):dirs[ds.indexOf(t)];
      if(t===0){
        let corner=(a===x||a===X)&&(c===z||c===Z);
        if(corner)dir=a===x?(c===z?'north':'west'):(c===z?'east':'south');
        let nearCorner=(a-x<=1||X-a<=1)&&(c-z<=1||Z-c<=1);
        this.set(a,y,c,ns(corner?'town_qingwa_corner':nearCorner?'town_qingwa_swept_eave':'town_qingwa_eave',dir));
      }else{
        let yy=y+Math.floor((t-1)/2),part=t%2?'low':'high';
        let hip=style!=='gable'&&ds.filter(v=>v===t).length===2;
        if(hip)dir=ds[0]===t?(ds[3]===t?'north':'east'):(ds[1]===t?'south':'west');
        this.set(a,yy,c,ns('town_qingwa_'+(hip?'hip_':'')+part,dir));
      }
    }
    if(!opts.opening){
      const t=Math.floor((Z-z)/2),ry=y+Math.floor(t/2);
      const lo=style==='gable'?x+1:x+t,hi=style==='gable'?X-1:X-t;
      for(let a=lo;a<=hi;a++){
        if(t%2===0)this.set(a,ry-1,center,ns('town_qingwa_crown_base','north'));
        this.set(a,ry,center,ns(t%2?'town_qingwa_ridge_high':'town_qingwa_ridge','north'));
      }
      this.set(lo,ry+(t%2),center,ns('town_qingwa_finial','south'));
      this.set(hi,ry+(t%2),center,ns('town_qingwa_finial','north'));
      if(style==='gable'&&opts.gable){
        for(const a of [x+(opts.gableOffset??2),X-(opts.gableOffset??2)]){
          for(let c=z+2;c<=Z-2;c++){
            const top=y+Math.floor((Math.min(c-z,Z-c)-1)/2);
            for(let yy=y;yy<top;yy++)this.set(a,yy,c,opts.gable);
          }
        }
      }
    }
  }
  export(){return {id:this.id,size:this.size,blocks:[...this.map.values()],markers:this.markers,rooms:this.rooms,roofs:this.roofs};}
}


export {VoxelBuilder,rotateState,M,AIR};
