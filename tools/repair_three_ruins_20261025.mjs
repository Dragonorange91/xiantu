/** Construction-skill VoxelBuilder repair of the currently furnished blueprints.
 * Freeze only these three inputs; never replay an older whole-world generator.
 */
import fs from 'node:fs';
import path from 'node:path';
import {VoxelBuilder} from './ruin_voxel_builder.mjs';
const P=path.resolve(import.meta.dirname,'..');
const current=path.join(P,'asset_sources/ruin-interiors-20261025/structures');
const out=path.join(P,'asset_sources/ruin-structure-fixes-20261025');
fs.mkdirSync(out+'/baseline',{recursive:true});fs.mkdirSync(out+'/structures',{recursive:true});
const AIR='minecraft:air',WOOD='minecraft:spruce_planks',QUARTZ='minecraft:quartz_bricks';
const report=[];
for(const id of ['guiyuan_herb_garden','ninefold_star_observatory','skyship_wreck']){
 const frozen=out+'/baseline/'+id+'.json';
 if(!fs.existsSync(frozen))fs.copyFileSync(current+'/'+id+'.json',frozen);
 const doc=JSON.parse(fs.readFileSync(frozen)),b=new VoxelBuilder(id,...doc.size);
 for(const c of doc.blocks)b.set(...c);
 const insideRoom=(x,z)=>doc.rooms.find(r=>x>=r.bounds[0]&&x<=r.bounds[3]&&z>=r.bounds[2]&&z<=r.bounds[5]);
 let fixes=0;
 if(id==='guiyuan_herb_garden'){
  // Later irrigation and bed writes used to overwrite the farmhouse footprint.
  for(const [x,y,z,s]of [...b.map.values()])if(s.startsWith('xiantu:herb_')){
   const room=insideRoom(x,z);
   if(room||b.get(x,y-1,z)!=='minecraft:grass_block'){
    const wall=room&&(x===room.bounds[0]||x===room.bounds[3]||z===room.bounds[2]||z===room.bounds[5]);
    b.set(x,y,z,wall?'minecraft:white_terracotta':AIR);fixes++;
   }
  }
  for(const room of doc.rooms){const [x,y,z,X,,Z]=room.bounds;
   for(let a=x;a<=X;a++)for(let c=z;c<=Z;c++)if(b.get(a,y,c)==='minecraft:grass_block')b.set(a,y,c,WOOD);
  }
  doc.herbPlacement={substrate:'minecraft:grass_block',excludeRoomFootprints:true,waterChannelsExcluded:true};
 }
 if(id==='ninefold_star_observatory'){
  // Remove the four disconnected three-step strips before making a full ascent.
  for(const [x,y,z,s]of [...b.map.values()])if(x>=48&&x<=52&&z>=48&&z<=72&&s.startsWith('minecraft:quartz_stairs'))b.set(x,y,z,AIR);
  // A three-course plinth connects the astronomical instrument to the summit.
  for(let y=30;y<=32;y++){const r=34-y;
   for(let x=50-r;x<=50+r;x++)for(let z=46-r;z<=46+r;z++)if((x-50)**2+(z-46)**2<=r*r)b.set(x,y,z,QUARTZ);
  }
  for(let tier=0;tier<4;tier++)for(let n=0;n<3;n++){
   const z=69-tier*6-n,y=21+tier*3+n;
   b.fill(48,20,z,52,y-1,z,QUARTZ);
   b.fill(48,y,z,52,y,z,'minecraft:quartz_stairs[facing=north,half=bottom,shape=straight,waterlogged=false]');
   b.clear(48,y+1,z,52,y+3,z);fixes+=5;
  }
  // Explicit landing links between the previously isolated stair strips.
  for(let tier=0;tier<3;tier++)b.fill(48,20,64-tier*6,52,23+tier*3,66-tier*6,QUARTZ);
  doc.structuralRepair={continuousSupportedStair:true,instrumentPlinth:true};
 }
 if(id==='skyship_wreck'){
  // The hold is preserved, while the outer envelope is rebuilt AFTER excavation.
  const half=x=>Math.max(1,Math.min(16,Math.floor((x-2)*.85),Math.floor((82-x)*.85)));
  const radius=(x,y)=>Math.max(1,half(x)-Math.ceil((13-y)*.28));
  for(const [x,y,z,s]of [...b.map.values()])if(y>=1&&y<=13){
   const outside=x<3||x>81||Math.abs(z-19)>radius(x,y);
   if(outside&&s!==AIR){b.set(x,y,z,AIR);fixes++;}
  }
  for(let x=3;x<=81;x++)for(let y=1;y<=13;y++){
   const r=radius(x,y);
   for(let z=19-r;z<=19+r;z++){
    const shell=y<=4||Math.abs(z-19)>=r-1||x===3||x===81;
    const hatch=x>=57&&x<=65&&z>=11&&z<=13;
    if(shell||(y===13&&!hatch))b.set(x,y,z,y%4===1?'minecraft:dark_oak_planks':WOOD);
   }
  }
  // Refit railings to the new narrowing deck, keeping the designated boarding edge.
  for(const [x,y,z,s]of [...b.map.values()])if(y===14&&s.includes('town_vermilion_railing'))b.set(x,y,z,AIR);
  for(let x=4;x<=80;x++)for(const z of [19-half(x),19+half(x)]){
   if(x>=40&&x<=44&&z>19)continue;
   b.set(x,14,z,`xiantu:town_vermilion_railing[facing=${z<19?'north':'south'}]`);
  }
  doc.structuralRepair={sealedHull:true,taperedEnds:true,retainedDeckHatch:[57,13,11,65,13,13]};
 }
 doc.blocks=[...b.map.values()];doc.revision='2026-10-25-structure-repair';
 const encoded=JSON.stringify(doc);
 fs.writeFileSync(out+'/structures/'+id+'.json',encoded);fs.writeFileSync(current+'/'+id+'.json',encoded);
 report.push({id,fixes,cells:doc.blocks.length});
}
fs.writeFileSync(out+'/report.json',JSON.stringify(report,null,2));console.log(report);
