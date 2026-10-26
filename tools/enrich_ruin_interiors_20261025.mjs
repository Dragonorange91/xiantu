/** Theme-specific VoxelBuilder patches over the accepted runtime, never old generators. */
import fs from 'node:fs';
import path from 'node:path';
import {VoxelBuilder} from './ruin_voxel_builder.mjs';
const root=path.resolve(import.meta.dirname,'..'),out=path.join(root,'asset_sources/ruin-interiors-20261025');
fs.mkdirSync(path.join(out,'structures'),{recursive:true});
const air='minecraft:air',S='minecraft:smooth_stone',wood='minecraft:spruce_planks';
const stair=(m,f)=>`minecraft:${m}_stairs[facing=${f},half=bottom,shape=straight,waterlogged=false]`;
const slab=m=>`minecraft:${m}_slab[type=bottom,waterlogged=false]`;
const modelDims={xian_wood_table:[2,1,1],xian_writing_desk:[2,1,1],xian_wood_chair:[1,2,1],xian_bookshelf:[2,2,1],xian_wood_wardrobe:[2,2,1],xian_round_stone_table:[2,1,2],ancient_burial_coffin:[2,2,5],equipment_repair_table:[2,2,1]};
function piece(){const v=[];return {v,set(x,y,z,s){v.push([x,y,z,s])},fill(x,y,z,X,Y,Z,s){for(let a=x;a<=X;a++)for(let b=y;b<=Y;b++)for(let c=z;c<=Z;c++)this.set(a,b,c,s)},furniture(id,x,y,z){const ds=modelDims[id]??[1,1,1];for(let a=0;a<ds[0];a++)for(let b=0;b<ds[1];b++)for(let c=0;c<ds[2];c++)this.set(x+a,y+b,z+c,`xiantu:${id}[facing=north${modelDims[id]?`,part=${a}_${b}_${c}`:''}]`)}};}
// Each vignette has its own silhouette and purpose. Material assemblies supplement,
// rather than repeat, the model furniture. Coordinates are hand-authored clusters.
function motif(type,variant=0){const p=piece(),put=(...a)=>p.set(...a),fill=(...a)=>p.fill(...a),fur=(...a)=>p.furniture(...a);
 switch(type){
 case 'burial':
  fur('ancient_burial_coffin',1,0,0);fill(0,0,5,3,0,5,'minecraft:polished_deepslate');
  put(0,1,5,'minecraft:white_candle[candles=2,lit=true,waterlogged=false]');put(3,1,5,'minecraft:flower_pot');
  fill(0,0,0,0,1,1,'minecraft:chiseled_stone_bricks');put(0,2,0,slab('stone_brick'));break;
 case 'ossuary':
  for(let x of [0,3]){fill(x,0,0,x+1,1,1,'minecraft:bone_block[axis=y]');put(x,2,0,'minecraft:flower_pot');put(x+1,2,1,'minecraft:white_candle[candles=1,lit=true,waterlogged=false]');}
  put(2,0,3,'minecraft:cracked_deepslate_tiles');put(3,0,4,slab('stone_brick'));break;
 case 'ancestral':
  fill(0,0,0,4,0,1,'minecraft:polished_deepslate');for(let x of [0,2,4]){put(x,1,0,'minecraft:chiseled_stone_bricks');put(x,2,0,slab('stone_brick'));}
  put(1,1,1,'minecraft:white_candle[candles=3,lit=true,waterlogged=false]');put(3,1,1,'minecraft:potted_dead_bush');break;
 case 'study':
  fur('xian_writing_desk',1,0,1);fur('xian_wood_chair',2,0,3);fur('xian_bookshelf',0,0,0);
  put(3,0,0,'minecraft:bookshelf');put(4,0,0,'minecraft:lectern[facing=south,has_book=false,powered=false]');break;
 case 'archive':
  fill(0,0,0,4,1,0,'minecraft:bookshelf');put(0,2,0,slab('spruce'));put(1,2,0,slab('spruce'));put(4,2,0,slab('spruce'));
  fur('xian_display_shelf',0,0,2);put(4,0,2,'minecraft:barrel[facing=up,open=false]');put(4,1,2,'minecraft:flower_pot');
  put(2,0,3,'minecraft:lectern[facing=south,has_book=false,powered=false]');break;
 case 'bed':
  for(let x of [0,1,2])put(x,0,0,stair('spruce','south'));
  fill(0,0,1,2,0,3,slab('spruce'));for(let x of [0,2])put(x,0,4,'minecraft:spruce_trapdoor[facing=north,half=bottom,open=true,powered=false,waterlogged=false]');
  put(4,0,1,'minecraft:barrel[facing=up,open=false]');put(4,1,1,'minecraft:lantern[hanging=false,waterlogged=false]');fur('xian_wood_wardrobe',3,0,3);break;
 case 'banquet':
  fur('xian_wood_table',1,0,1);fur('xian_wood_bench',0,0,1);fur('xian_wood_bench',1,0,3);
  put(4,0,0,'minecraft:barrel[facing=up,open=false]');put(4,1,0,'minecraft:potted_azalea_bush');put(4,0,3,'minecraft:composter[level=6]');break;
 case 'forge':
  put(0,0,0,'minecraft:blast_furnace[facing=south,lit=false]');put(1,0,0,'minecraft:blast_furnace[facing=south,lit=false]');fill(0,1,0,1,2,0,'minecraft:stone_bricks');
  put(3,0,1,'minecraft:anvil[facing=north]');fur('equipment_repair_table',2,0,3);put(4,0,0,'minecraft:cauldron');break;
 case 'alchemy':
  fur('xian_writing_desk',0,0,0);put(3,0,0,'minecraft:brewing_stand[has_bottle_0=true,has_bottle_1=true,has_bottle_2=false]');
  put(3,0,2,'minecraft:cauldron');put(0,0,3,'minecraft:barrel[facing=up,open=false]');put(0,1,3,'minecraft:potted_fern');
  fur('ruin_weathered_herb_rack',4,0,3);put(4,0,0,'minecraft:campfire[facing=north,lit=false,signal_fire=false,waterlogged=false]');break;
 case 'herb':
  fill(0,0,0,4,0,1,'minecraft:spruce_planks');for(let x of [0,2,4])put(x,1,0,'minecraft:'+['potted_fern','potted_azalea_bush','potted_red_mushroom'][(x/2+variant)%3]);
  put(0,0,3,'minecraft:composter[level=5]');put(2,0,3,'minecraft:cauldron');fur('ruin_weathered_herb_rack',4,0,3);break;
 case 'armory':
  fill(0,0,0,4,0,0,wood);for(let x of [0,2,4]){put(x,1,0,'minecraft:iron_bars');put(x,2,0,'minecraft:iron_bars');}
  fur('ruin_sealed_swordstone',1,0,2);put(4,0,2,'minecraft:smithing_table');put(3,0,4,'minecraft:grindstone[face=floor,facing=west]');break;
 case 'store':
  for(let [x,z,h] of [[0,0,2],[1,0,1],[4,0,1],[0,3,1],[3,3,2]])for(let y=0;y<h;y++)put(x,y,z,'minecraft:barrel[facing='+(['south','east','up'][(x+z+y+variant)%3])+',open=false]');
  put(2,0,0,'minecraft:composter[level=3]');put(4,0,4,'minecraft:spruce_trapdoor[facing=south,half=bottom,open=false,powered=false,waterlogged=false]');break;
 case 'meditation':
  fur('xian_round_stone_table',0,0,0);fur('xian_stone_stool',0,0,3);fur('xian_stone_stool',3,0,1);
  put(4,0,0,'minecraft:chiseled_stone_bricks');put(4,1,0,'minecraft:white_candle[candles=3,lit=true,waterlogged=false]');break;
 case 'seal':
  fill(1,0,1,3,0,3,'minecraft:polished_blackstone');for(let [x,z]of [[0,0],[4,0],[0,4],[4,4]]){put(x,0,z,'minecraft:chiseled_polished_blackstone');put(x,1,z,'minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]');}
  put(2,1,2,'minecraft:amethyst_cluster[facing=up,waterlogged=false]');break;
 case 'star':
  fur('ruin_star_dial',2,0,2);for(let [x,z]of [[0,0],[4,0],[0,4],[4,4]]){put(x,0,z,'minecraft:polished_andesite');put(x,1,z,'minecraft:end_rod[facing=up]');}
  put(0,0,2,'minecraft:lectern[facing=east,has_book=false,powered=false]');break;
 case 'mine':
  fill(0,0,0,0,3,0,wood);fill(4,0,0,4,3,0,wood);fill(1,3,0,3,3,0,slab('spruce'));
  for(let z=1;z<5;z++)put(2,0,z,'minecraft:rail[shape=north_south,waterlogged=false]');
  put(0,0,3,'minecraft:coal_block');put(0,1,3,'minecraft:raw_iron_block');put(4,0,3,'minecraft:barrel[facing=south,open=false]');break;
 case 'hoist':
  fill(0,0,0,0,4,0,wood);fill(0,4,1,0,4,3,wood);fill(0,1,3,0,3,3,'minecraft:chain[axis=y,waterlogged=false]');
  fill(0,0,3,2,0,4,'minecraft:raw_copper_block');put(3,0,0,'minecraft:stonecutter[facing=south]');put(4,0,2,'minecraft:barrel[facing=up,open=false]');break;
 case 'timber':
  fill(0,0,0,4,0,1,wood);fill(0,1,0,3,1,0,wood);fill(0,0,4,0,2,4,'minecraft:spruce_fence');
  put(3,0,3,'minecraft:crafting_table');put(4,0,3,'minecraft:stonecutter[facing=west]');break;
 case 'water':
  fill(0,0,0,4,0,1,'minecraft:prismarine_bricks');for(let x of [0,4]){put(x,1,0,'minecraft:dark_prismarine');put(x,2,0,'minecraft:sea_lantern');put(x,3,0,slab('prismarine_brick'));}
  put(2,1,0,'minecraft:amethyst_cluster[facing=up,waterlogged=false]');put(1,0,3,'minecraft:brain_coral_block');put(3,0,3,'minecraft:dead_tube_coral_block');break;
 case 'nest':
  for(let [x,z]of [[0,1],[1,0],[2,0],[3,1],[0,3],[1,4],[2,4],[3,3]])put(x,0,z,slab('spruce'));
  put(1,0,2,'minecraft:hay_block[axis=y]');put(2,0,2,'minecraft:hay_block[axis=y]');put(1,1,2,'minecraft:turtle_egg[eggs=2,hatch=0]');break;
 case 'banner':
  fill(0,0,0,0,3,0,'minecraft:stone_bricks');put(0,4,0,'minecraft:lightning_rod[facing=up,powered=false,waterlogged=false]');
  fill(1,3,0,3,3,0,'minecraft:red_terracotta');put(1,2,0,'minecraft:red_terracotta');put(2,2,0,'minecraft:cracked_stone_bricks');
  fur('ruin_fallen_lintel',3,0,2);put(0,0,3,slab('stone_brick'));break;
 case 'brokenhome':
  put(0,0,0,stair('spruce','south'));put(1,0,0,stair('spruce','south'));fill(0,0,1,1,0,2,slab('spruce'));
  put(3,0,0,'minecraft:bookshelf');put(3,1,0,slab('spruce'));put(4,0,2,'minecraft:barrel[facing=south,open=false]');
  fur('ruin_fallen_lintel',1,0,4);put(0,0,4,'minecraft:flower_pot');break;
 case 'throne':
  fill(0,0,0,5,0,3,'minecraft:polished_andesite');fill(1,1,0,4,1,2,'minecraft:chiseled_stone_bricks');
  fill(1,2,0,4,3,0,'minecraft:chiseled_stone_bricks');put(1,2,1,stair('stone_brick','east'));put(4,2,1,stair('stone_brick','west'));
  fill(2,2,1,3,2,2,slab('stone_brick'));put(0,1,3,'minecraft:lantern[hanging=false,waterlogged=false]');put(5,1,3,'minecraft:lantern[hanging=false,waterlogged=false]');break;
 case 'fruit':
  put(0,0,0,'minecraft:barrel[facing=up,open=false]');put(1,0,0,'minecraft:barrel[facing=up,open=false]');put(0,1,0,'minecraft:potted_azalea_bush');
  fill(3,0,0,4,0,1,'minecraft:hay_block[axis=y]');put(4,1,0,'minecraft:melon');put(3,1,1,'minecraft:pumpkin');
  put(1,0,3,'minecraft:composter[level=7]');put(4,0,4,'minecraft:brown_mushroom_block');break;
 case 'stonebed':
  fill(0,0,0,3,0,3,'minecraft:smooth_stone');fill(0,1,0,3,1,0,slab('smooth_stone'));put(0,1,1,slab('smooth_stone'));
  put(4,0,1,'minecraft:polished_andesite');put(4,1,1,'minecraft:white_candle[candles=2,lit=true,waterlogged=false]');break;
 case 'shore':
  for(let [x,z]of [[0,0],[1,0],[1,1],[3,2],[4,3]])put(x,0,z,slab('mossy_stone_brick'));
  put(0,0,3,'minecraft:potted_fern');put(2,0,0,'minecraft:potted_red_mushroom');put(4,0,4,'minecraft:sea_pickle[pickles=2,waterlogged=false]');break;
 case 'chart':
  fur('xian_writing_desk',1,0,0);put(0,0,0,'minecraft:cartography_table');put(4,0,1,'minecraft:lectern[facing=west,has_book=false,powered=false]');
  put(0,0,3,'minecraft:barrel[facing=south,open=false]');put(0,1,3,'minecraft:lantern[hanging=false,waterlogged=false]');fur('xian_wood_chair',2,0,2);break;
 case 'munition':
  put(0,0,0,'minecraft:barrel[facing=south,open=false]');put(1,0,0,'minecraft:smithing_table');put(0,1,0,'minecraft:lantern[hanging=false,waterlogged=false]');break;
 case 'brokenarms':
  fur('ruin_sealed_swordstone',0,0,0);put(1,0,0,'minecraft:grindstone[face=floor,facing=east]');put(1,0,1,slab('stone_brick'));break;
 case 'relic':
  put(0,0,0,'minecraft:chiseled_stone_bricks');put(0,1,0,'minecraft:flower_pot');put(1,0,0,'minecraft:bone_block[axis=y]');put(1,1,0,'minecraft:white_candle[candles=2,lit=true,waterlogged=false]');break;
 default:throw Error('unknown vignette '+type);
 }return p.v;
}
// Deliberately chosen room functions, not one filler set copied over every ruin.
const programs={
 ancient_battlefield:[['munition','brokenarms'],['munition','relic'],['brokenarms','relic']],
 ancient_cultivator_cave:[['bed','banquet'],['study','archive'],['alchemy','herb'],['meditation','ancestral']],
 broken_bridge_pavilion:[['meditation','chart'],['bed','store']],
 cinnabar_seal_tower:[['ancestral','banner'],['study','seal'],['archive','chart'],['meditation','bed'],['seal','ancestral'],['store','archive']],
 cloudsea_palace:[['banquet','throne','ancestral'],['archive','study'],['meditation','star']],
 earthvein_mine_palace:[['mine','hoist'],['chart','store'],['timber','armory'],['forge','store'],['hoist','mine'],['store','chart'],['timber','brokenhome'],['forge','mine'],['mine','banner'],['chart','hoist'],['timber','store'],['forge','armory']],
 fallen_cultivator_necropolis:[['banner','ancestral'],['ancestral','burial'],['meditation','archive'],['forge','armory'],['archive','study'],['ossuary','banner'],['alchemy','herb'],['burial','burial','ossuary'],['burial','ancestral','burial'],['store','armory'],['archive','chart'],['banner','ancestral'],['star','chart'],['store','archive'],['hoist','mine'],['armory','brokenhome'],['meditation','bed']],
 five_elements_prison_grand:[['banner','seal'],['seal','ancestral'],['water','seal'],['water','store'],['armory','forge'],['armory','store'],['burial','ancestral'],['burial','ossuary'],['forge','alchemy'],['forge','store'],['herb','meditation'],['herb','store'],['store','archive']],
 floating_herb_garden:[['herb','study','bed'],['herb','store']],
 guiyuan_herb_garden:[['alchemy','bed','study'],['herb','store']],
 mysterious_bird_nest:[['nest','nest','shore','relic']],
 ninefold_star_observatory:[['archive','chart'],['star','meditation']],
 skyship_wreck:[['chart','bed'],['store','timber','armory']],
 starfall_sanctuary_grand:[['banner','ancestral'],['star','chart'],['armory','brokenhome','store'],['archive','study','chart'],['forge','armory'],['seal','banner'],['mine','hoist'],['star','store'],['ancestral','archive']],
 taixu_tomb_passage:[['ancestral','banner'],['store','ancestral'],['ossuary','burial'],['burial','archive'],['ancestral','burial'],['burial','burial'],['burial','ossuary','burial'],['burial','ancestral','burial'],['ossuary','burial']],
 water_curtain_caverns_grand:[['throne','banquet','fruit','armory','ancestral'],['stonebed','meditation'],['store','archive'],['fruit','herb'],['armory','banner'],['shore','meditation'],['shore','nest','meditation'],['fruit','store','timber'],['armory','store','banner']],
 water_palace:[['water','ancestral','banquet'],['water','store'],['water','seal']],
 wilderness_sword_tomb:[['armory','burial'],['armory','banner','ancestral','ossuary']],
 xuantian_ruined_city:[['banner','armory'],['archive','brokenhome'],['study','ancestral'],['bed','brokenhome'],['forge','armory'],['alchemy','herb'],['store','chart'],['brokenhome','banquet'],['bed','store'],['brokenhome'],['store'],['bed'],['brokenhome'],['store'],['brokenhome'],['armory','banner','seal'],['throne','ancestral','archive']],
 yunling_observatory:[['ancestral','meditation','banquet'],['study','archive'],['meditation','herb']]
};
const dirs=['north','east','south','west'];
function rotated(v,turn){let[x,y,z,s]=v;for(let t=0;t<turn;t++)[x,z]=[-z,x];s=s.replace(/facing=(north|east|south|west)/g,(_,f)=>'facing='+dirs[(dirs.indexOf(f)+turn)%4]);return [x,y,z,s];}
const report=[];
for(const file of fs.readdirSync(path.join(out,'baseline')).filter(f=>f.endsWith('.json')).sort()){
 const b=JSON.parse(fs.readFileSync(path.join(out,'baseline',file))),v=new VoxelBuilder(b.id,...b.size);for(const c of b.blocks)v.set(...c);
 const counts={},groups=[];const originalGuards=b.encounters.length;
 // Reserve the existing traversal spine and functional markers. Decorations may never
 // clear walls, bridge voids, overwrite loot or occupy a trial core/arena.
 const protectedPoints=[b.entry,...b.markers.flatMap(m=>[m.position,m.approach].filter(Boolean)),...b.encounters.map(e=>e.position)];
 const boss=b.bossPlan;
 if(boss){const [x,y,z]=boss.position;protectedPoints.push([x+6,y,z],[x+6,y,z-5],[x+6,y,z+5]);}
 function reserved(x,y,z){
  const margin=b.id==='ancient_battlefield'?1:3;
  if(protectedPoints.some(p=>Math.abs(p[1]-y)<4&&Math.abs(p[0]-x)<=margin&&Math.abs(p[2]-z)<=margin))return true;
  if(boss&&Math.abs(y-boss.position[1])<4&&Math.hypot(x-boss.position[0],z-boss.position[2])<11)return true;
  return (b.routeTargets??[]).some(p=>Math.abs(p[1]-y)<3&&Math.abs(p[0]-x)<=1&&Math.abs(p[2]-z)<=1);
 }
 function fit(cells){const lowest=Math.min(...cells.map(c=>c[1]));for(const [x,y,z,s]of cells){if(x<0||y<1||z<0||x>=b.size[0]||y>=b.size[1]||z>=b.size[2]||reserved(x,y,z)||v.get(x,y,z)!==air)return false;
  if(y===lowest){const floor=v.get(x,y-1,z);if(floor===air||floor.startsWith('minecraft:water')||floor.includes('part=')||floor.includes('slab')||floor.includes('stairs'))return false;}
 }return true;}
 const nodes=b.layoutGraph.nodes;
 for(let ri=0;ri<b.rooms.length;ri++){
  const r=b.rooms[ri],bounds=r.bounds,[x,y,z,X,Y,Z]=bounds,cy=y+1;
  const prog=programs[b.id]?.[ri];if(!prog)throw Error('Missing designed room '+b.id+':'+ri);
  // Four oriented wall strips and offset bays. Avoid doors centered on each side.
  const locations=[];
  for(let turn=0;turn<4;turn++)for(let t of [.22,.72,.42,.58]){
   if(turn===0)locations.push([Math.round(x+2+(X-x-8)*t),cy,z+2,turn]);
   if(turn===1)locations.push([X-2,cy,Math.round(z+2+(Z-z-8)*t),turn]);
   if(turn===2)locations.push([Math.round(x+6+(X-x-8)*t),cy,Z-2,turn]);
   if(turn===3)locations.push([x+2,cy,Math.round(z+6+(Z-z-8)*t),turn]);
  }
  // Rotate candidate priority per room, rather than repeat the same occupied corner.
  const shift=(ri*5)%locations.length,locs=[...locations.slice(shift),...locations.slice(0,shift)];
  // Search actual free bays when the original door/loot/guardian occupies a preferred
  // wall position. Natural sites use their real surface rather than bounding-box Y.
  const natural=b.id==='mysterious_bird_nest'||b.id==='wilderness_sword_tomb'&&ri===1;
  for(let px=x+1;px<X-1;px+=2)for(let pz=z+1;pz<Z-1;pz+=2){
   let yy=cy;
   if(natural){for(let h=Math.min(Y-3,b.size[1]-3);h>y;h--){const s=v.get(px,h-1,pz);if(s!==air&&!s.startsWith('minecraft:water')&&v.get(px,h,pz)===air){yy=h;break;}}}
   for(let turn=0;turn<4;turn++)locs.push([px,yy,pz,turn]);
  }
  const themes=[];
  for(let mi=0;mi<prog.length;mi++){
   const type=prog[mi];let placed=0;
   // Large rooms receive multiple different clusters, never a carpet of cloned props.
   const repeat=(X-x)*(Z-z)>750&&type!=='throne'&&b.id!=='water_curtain_caverns_grand'?2:1;
   for(let it=0;it<repeat;it++)for(const [ox,oy,oz,turn]of locs){
    const cells=motif(type,ri+mi).map(c=>{const [a,h,k,s]=rotated(c,turn);return [ox+a,oy+h,oz+k,s]});
    if(cells.some(c=>c[0]<x+1||c[0]>X-1||c[2]<z+1||c[2]>Z-1||c[1]>=Y-1)||!fit(cells))continue;
    for(const c of cells)v.set(...c);placed++;counts[type]=(counts[type]??0)+1;themes.push(type);groups.push({room:ri,name:r.name,type,anchor:[ox,oy,oz],turn,cells:cells.length});break;
   }
  }
  r.furnishingThemes=themes;r.purpose=prog.join(' / ')+'；独立陈设组，保留探索与战斗通道';
  // Each physical room gets local combat if it lacks nearby guardians. Boss trial
  // encounter order is gameplay data and is intentionally retained exactly.
  const trialRoom=boss&&boss.position[0]>=x&&boss.position[0]<=X&&boss.position[1]>=y&&boss.position[1]<=Y&&boss.position[2]>=z&&boss.position[2]<=Z;
  if((!boss||boss.trial!=='elements'&&!trialRoom)&&!(b.id==='xuantian_ruined_city'&&ri===0)){
   const c=nodes[ri]?.center??[Math.round((x+X)/2),cy,Math.round((z+Z)/2)];
   if(!b.encounters.some(e=>Math.abs(e.position[1]-cy)<5&&Math.hypot(e.position[0]-c[0],e.position[2]-c[2])<18)){
    let at=null;
    for(let radius=0;radius<=7&&!at;radius++)for(let dx=-radius;dx<=radius&&!at;dx++)for(let dz=-radius;dz<=radius&&!at;dz++){
     const px=c[0]+dx,pz=c[2]+dz;if(px<x+3||px>X-3||pz<z+3||pz>Z-3||reserved(px,cy,pz))continue;
     let ok=true;for(let a=-2;a<=2;a++)for(let k=-2;k<=2;k++)if(v.get(px+a,cy,pz+k)!==air||v.get(px+a,cy+1,pz+k)!==air||v.get(px+a,cy-1,pz+k)===air)ok=false;
     if(ok)at=[px,cy,pz];
    }
    if(at){const old=b.encounters[Math.min(ri,b.encounters.length-1)],e={...old,id:'room_guard_'+ri,position:at,count:3,room:ri,bounds:[x+1,cy,z+1,X-1,Y-1,Z-1]};b.encounters.push(e);protectedPoints.push(at);v.set(...at,'xiantu:ruin_encounter_seal');}
   }
  }
 }
 if(b.id==='water_curtain_caverns_grand'){
  // Sculpt the inhabited grotto, not just place miniature props along a rectangular
  // hall. Irregular rock buttresses, lowered shallow pools and mineral shelves have
  // individual outlines and leave the central trial ring open.
  const grotto=[
   {room:0,x:58,z:27,w:8,d:4,profile:[2,3,4,2],kind:'rock-seat'},
   {room:0,x:94,z:27,w:8,d:3,profile:[2,4,2],kind:'mineral-shelf'},
   {room:0,x:95,z:42,w:7,d:7,profile:[1,2,3,4,4,2,1],kind:'mineral-shelf'},
   {room:0,x:59,z:53,w:8,d:7,profile:[0,1,1,2,2,1,0],kind:'spring'},
   {room:1,x:35,z:51,w:5,d:7,profile:[2,2,1,1,2,3,2],kind:'rock-seat'},
   {room:2,x:123,z:55,w:6,d:5,profile:[1,2,3,3,1],kind:'mineral-shelf'},
   {room:3,x:59,z:81,w:5,d:5,profile:[0,1,1,1,0],kind:'spring'},
   {room:6,x:52,z:40,w:9,d:6,profile:[1,1,2,3,2,1],kind:'rock-seat'},
   {room:6,x:94,z:62,w:9,d:5,profile:[0,1,2,1,0],kind:'spring'},
   {room:7,x:46,z:81,w:6,d:5,profile:[1,2,2,1,1],kind:'mineral-shelf'},
  ];
  for(const s of grotto){const r=b.rooms[s.room],y=r.bounds[1]+1,cells=[];
   for(let dz=0;dz<s.d;dz++)for(let dx=0;dx<s.w;dx++){
    const edge=Math.min(dx,s.w-1-dx,dz,s.d-1-dz),h=Math.max(0,s.profile[dz]-(dx===0||dx===s.w-1?1:0));
    if(!h)continue;
    for(let dy=0;dy<h;dy++)cells.push([s.x+dx,y+dy,s.z+dz,dy===h-1&&edge>0?'minecraft:polished_andesite':'minecraft:andesite']);
    if(s.kind==='mineral-shelf'&&dx%3===1&&dz===2)cells.push([s.x+dx,y+h,s.z+dz,'minecraft:small_amethyst_bud[facing=up,waterlogged=false]']);
    if(s.kind==='rock-seat'&&h===1&&dx%3===1)cells.push([s.x+dx,y+h,s.z+dz,'minecraft:brown_carpet']);
   }
   if(fit(cells)){for(const c of cells)v.set(...c);groups.push({room:s.room,name:r.name,type:s.kind,anchor:[s.x,y,s.z],cells:cells.length});counts[s.kind]=(counts[s.kind]??0)+1;}
  }
  // A sheltered long feast table, distinct from the small side-room furniture.
  const feast=piece();for(let x=0;x<11;x++){feast.set(x,0,0,slab('spruce'));if(x%3===0){feast.set(x,0,2,stair('stone_brick','north'));feast.set(x,1,0,'minecraft:flower_pot');}}
  const banquet=feast.v.map(([x,y,z,s])=>[64+x,23+y,62+z,s]);if(fit(banquet)){for(const c of banquet)v.set(...c);groups.push({room:0,name:'水帘大洞',type:'long-feast-table',anchor:[64,23,62],cells:banquet.length});counts['long-feast-table']=1;}
 }
 // Existing and new waves receive the actual room volume; this prevents triggering
 // upstairs/downstairs through floors. Huge open outdoor rings retain radius triggers.
 for(const e of b.encounters){
  e.minRealm=Math.max(0,e.maxRealm-2);
  const r=b.rooms.find(r=>{const a=r.bounds,p=e.position;return p[0]>a[0]&&p[0]<a[3]&&p[1]>a[1]&&p[1]<a[4]&&p[2]>a[2]&&p[2]<a[5]});
  if(r){const a=r.bounds;e.bounds=[a[0],a[1],a[2],a[3],a[4],a[5]];}
 }
 b.revision='themed-inhabited-interiors-20261025';b.interiorDesign={distinctVignettes:counts,groups,vanillaAndMod:true};b.blocks=[...v.map.values()];
 fs.writeFileSync(path.join(out,'structures',file),JSON.stringify(b));
 report.push({id:b.id,rooms:b.rooms.length,groups:groups.length,themes:counts,oldWaves:originalGuards,waves:b.encounters.length,coffins:counts.burial??0,emptyRooms:b.rooms.filter(r=>!r.furnishingThemes.length).map(r=>r.name)});
 console.log(b.id,groups.length,'vignettes',b.encounters.length,'waves');
}
fs.writeFileSync(path.join(out,'interior-report.json'),JSON.stringify(report,null,2));
