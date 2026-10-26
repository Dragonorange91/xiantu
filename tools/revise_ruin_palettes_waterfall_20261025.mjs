/** Theme-specific, shape-preserving revisions of the furnished VoxelBuilder maps. */
import fs from 'node:fs';
import path from 'node:path';
import {VoxelBuilder} from './ruin_voxel_builder.mjs';
const P=path.resolve(import.meta.dirname,'..'),C=P+'/asset_sources/ruin-interiors-20261025/structures',O=P+'/asset_sources/ruin-palette-waterfall-20261025';
fs.mkdirSync(O+'/baseline',{recursive:true});fs.mkdirSync(O+'/structures',{recursive:true});
const themes={
 ancient_battlefield:['black_rock','xuan_tie_pine'],ancient_cultivator_cave:['azure_brick','zi_wen_maple'],
 broken_bridge_pavilion:['spring_stone','qing_wen_elm'],cinnabar_seal_tower:['black_rock','chi_xin_birch'],
 cloudsea_palace:['cloud_floating_stone','tian_qiong_sandalwood'],earthvein_mine_palace:['black_rock','xuan_tie_pine'],
 fallen_cultivator_necropolis:['black_rock','zi_fu_cedar'],five_elements_prison_grand:['black_rock','zi_fu_cedar'],
 floating_herb_garden:['cloud_floating_stone','qing_wen_elm'],guiyuan_herb_garden:['spring_stone','qing_wen_elm'],
 mysterious_bird_nest:['cloud_jade','jin_si_nanmu'],ninefold_star_observatory:['cloud_jade','xing_wen_wutong'],
 skyship_wreck:['azure_brick','xuan_tie_pine'],starfall_sanctuary_grand:['cloud_floating_stone','xing_wen_wutong'],
 taixu_tomb_passage:['black_rock','zi_fu_cedar'],water_curtain_caverns_grand:['spring_stone','qing_wen_elm'],
 water_palace:['cloud_jade','jin_si_nanmu'],wilderness_sword_tomb:['black_rock','xuan_tie_pine'],
 xuantian_ruined_city:['azure_brick','zi_wen_maple'],yunling_observatory:['spring_stone','qing_wen_elm']
};
const report=[];
for(const [id,[stone,wood]]of Object.entries(themes)){
 const source=O+'/baseline/'+id+'.json';if(!fs.existsSync(source))fs.copyFileSync(C+'/'+id+'.json',source);
 const d=JSON.parse(fs.readFileSync(source)),b=new VoxelBuilder(id,...d.size);for(const cell of d.blocks)b.set(...cell);
 let changed=0;
 // Exact shape counterparts only. Qingwa compound roof modules retain their accepted geometry.
 const simple={'stone_bricks':stone,'cracked_stone_bricks':stone+'_cracked','mossy_stone_bricks':stone+'_mossy','chiseled_stone_bricks':stone+'_carved',
  'stone_brick_stairs':stone+'_stairs','stone_brick_slab':stone+'_slab','stone_brick_wall':stone+'_wall',
  'spruce_planks':wood+'_planks','dark_oak_planks':wood+'_carved','spruce_stairs':wood+'_stairs','spruce_slab':wood+'_slab'};
 if(id==='ninefold_star_observatory'||id==='cloudsea_palace'){Object.assign(simple,{quartz_bricks:stone+'_smooth',quartz_block:stone+'_smooth',chiseled_quartz_block:stone+'_carved',quartz_stairs:stone+'_smooth_stairs',quartz_slab:stone+'_smooth_slab'});}
 // Underwater masonry stays marine; accents and wood are selected separately. Natural cave stone stays natural.
 if(id==='water_palace')for(const key of Object.keys(simple))if(key.includes('stone_brick'))delete simple[key];
 for(const [x,y,z,state]of [...b.map.values()]){
  const [name,properties]=state.split('['),short=name.replace('minecraft:','');
  if(!name.startsWith('minecraft:')||!simple[short])continue;
  const target=simple[short];if(!fs.existsSync(P+'/src/main/resources/assets/xiantu/blockstates/'+target+'.json'))throw Error('Missing actual material '+target);
  b.set(x,y,z,'xiantu:'+target+(properties?'['+properties:''));changed++;
 }
 if(id==='water_curtain_caverns_grand'){
  // Rebuild only the exposed front apron. Chambers and the arched gate at z110 stay intact.
  for(const[x,y,z,s]of [...b.map.values()])if(z>=111&&z<=141&&x>=57&&x<=109&&y>=17)b.set(x,y,z,'minecraft:air');
  // Remove the old artificially raised cliff cheeks above the sealed chamber roofs.
  for(let x=54;x<=111;x++)for(let z=95;z<=110;z++){
   const edge=Math.abs(x-81)/29,top=Math.max(38,Math.round(57-(z-95)*.85+edge*5+2*Math.sin(x*.37+z*.23)));
   for(let y=top+1;y<=78;y++)b.set(x,y,z,'minecraft:air');
   if(b.get(x,top,z)!=='minecraft:air'){b.set(x,top,z,'minecraft:grass_block');b.set(x,top-1,z,'minecraft:dirt');}
  }
  // The stream falls at staggered lips; banks follow a continuous valley slope, not the water steps.
  const heights=z=>z<=110?59:z<=115?52:z<=121?45:z<=126?38:z<=132?31:z<=138?24:17;
  const centers=z=>Math.round(80+3*Math.sin((z-106)*.19));
  // Blend the canyon into the entire front slope so there are no rectangular cliff seams.
  for(let x=37;x<=128;x++)for(let z=111;z<=152;z++){
   const cx=centers(z),dist=Math.abs(x-cx),naturalR=((x-83+4*Math.sin(z*.065))/81)**2+((z-75+3*Math.sin(x*.081))/80)**2;
   const mountain=Math.max(16,Math.floor(3+66*Math.pow(Math.max(0,1-naturalR),.72)));
   const valley=58-(z-111)*1.02+Math.max(0,dist-6)*.34+2*Math.sin(x*.29+z*.17);
   const top=Math.max(16,Math.min(mountain,Math.round(valley)));
   for(let y=17;y<=78;y++)b.set(x,y,z,y<=top?'minecraft:stone':'minecraft:air');
   if(top>16){b.set(x,top,z,'minecraft:grass_block');b.set(x,top-1,z,'minecraft:dirt');}
  }
  for(let z=99;z<=145;z++){
   const water=heights(z),cx=centers(z),half=z<111?4:4+Math.floor((1+Math.sin(z*.63))*1.2);
   for(let x=cx-half-2;x<=cx+half+2;x++){
    const dist=Math.abs(x-cx),edge=dist>half;
    if(edge){
     // Eroded ledge shoulders end irregularly within the softer valley bank.
     if(z>=111)for(let y=water-2;y<=water;y++)b.set(x,y,z,(x+z)%4===0?'minecraft:andesite':'minecraft:stone');
     continue;
    }
    for(let y=water+1;y<=78;y++)b.set(x,y,z,'minecraft:air');
    b.set(x,water-1,z,(x+z)%5===0?'minecraft:andesite':'minecraft:stone');
    if(z>=111)b.fill(x,23,z,x,water-2,z,'minecraft:stone');
    b.set(x,water,z,'minecraft:water');
    if(z>99&&heights(z-1)>water)for(let y=water+1;y<=heights(z-1);y++)b.set(x,y,z,'minecraft:water[level=8]');
   }
  }
  // Arch-backed covered passage behind the cascade: only its last fall opens outdoors.
  for(let z=111;z<=139;z++){b.fill(74,16,z,86,24,z,'minecraft:stone');b.clear(76,17,z,84,22,z);}
  for(let x=76;x<=84;x++)for(let y=17;y<=24;y++)b.set(x,y,140,'minecraft:water[level=8]');
  for(let x=72;x<=88;x++)for(let z=141;z<=153;z++)if(((x-80)/8)**2+((z-146)/7)**2<=1){b.set(x,15,z,'minecraft:clay');b.set(x,16,z,'minecraft:water');}
  d.waterfall={kind:'stepped',ledges:[59,52,45,38,31,24,17],entranceBehindFinalFall:[80,17,140],interiorGate:[80,17,110]};
  d.placement.entry=[80,17,145];
  // Re-anchor existing vanilla feature seeds on the new natural surface, never assemble trees.
  d.vegetation=d.vegetation.filter(v=>!(v.position[2]>=95&&v.position[2]<=150&&v.position[0]>=54&&v.position[0]<=111));
  for(let x=58;x<=108;x+=7)for(let z=113;z<=138;z+=7){let y=78;while(y>16&&b.get(x,y,z)==='minecraft:air')y--;if(b.get(x,y,z)==='minecraft:grass_block')d.vegetation.push({position:[x,y+1,z],feature:(x+z)%3===0?'minecraft:oak':'minecraft:patch_grass_plain'});}
 }
 d.materialTheme={stone,wood,shapePreserving:true,naturalRockPreserved:true};d.blocks=[...b.map.values()];
 fs.writeFileSync(C+'/'+id+'.json',JSON.stringify(d));fs.writeFileSync(O+'/structures/'+id+'.json',JSON.stringify(d));
 report.push({id,stone,wood,changed,customMaterials:new Set(d.blocks.filter(v=>v[3].startsWith('xiantu:')).map(v=>v[3].split('[')[0])).size});
}
fs.writeFileSync(O+'/report.json',JSON.stringify(report,null,2));console.log(JSON.stringify(report));
