/** Construction-skill authoring: native blueprints, not edits to a live save.
 * Each ruin has its own topology. No common appended side wing/postprocessor.
 */
import fs from 'node:fs';
import path from 'node:path';
import {VoxelBuilder} from './ruin_voxel_builder.mjs';
const P=path.resolve(import.meta.dirname,'..'), O=path.join(P,'asset_sources/ruins-rework-20261024');
fs.mkdirSync(O+'/structures',{recursive:true});
const A='minecraft:air',S='minecraft:stone',G='minecraft:grass_block',M='minecraft:stone_bricks',W='minecraft:spruce_planks',L='minecraft:stripped_dark_oak_log';
const q=(s,f='north')=>`xiantu:${s}[facing=${f}]`;
const source=id=>JSON.parse(fs.readFileSync(`${O}/baseline/${id}.json`));
const all=[];
function make(id,w,h,d){let b=new VoxelBuilder(id,w,h,d);b.edges=[];b.nodes=[];b.encounters=[];b.resources=[];b.route=[];b.vegetation=[];b.tunnels=[];b.excavations=[];return b;}
function room(b,id,name,x,y,z,X,Y,Z,purpose='探索、战斗与室内藏物'){
 b.rooms.push({name,bounds:[x,y,z,X,Y,Z],purpose});b.nodes.push({id,name,center:[Math.floor((x+X)/2),y+1,Math.floor((z+Z)/2)],bounds:[x,y,z,X,Y,Z],theme:'earth'});return b.nodes.at(-1);
}
function line(b,a,c,mat,width=0){let n=Math.max(...a.map((v,i)=>Math.abs(v-c[i])));for(let i=0;i<=n;i++){let[x,y,z]=a.map((v,j)=>Math.round(v+(c[j]-v)*i/Math.max(n,1)));b.fill(x-width,y,z-width,x+width,y,z+width,mat);}}
function disk(b,x,y,z,rx,rz,mat){for(let X=x-rx;X<=x+rx;X++)for(let Z=z-rz;Z<=z+rz;Z++)if(((X-x)/rx)**2+((Z-z)/rz)**2<=1)b.set(X,y,Z,mat);}
function path_(b,pts,width=1,mat=M){for(let i=1;i<pts.length;i++){line(b,pts[i-1],pts[i],mat,width);let a=pts[i-1],c=pts[i],n=Math.max(...a.map((v,j)=>Math.abs(v-c[j])));for(let k=0;k<=n;k++){let[x,y,z]=a.map((v,j)=>Math.round(v+(c[j]-v)*k/Math.max(1,n)));b.clear(x-width,y+1,z-width,x+width,y+3,z+width);}}b.route.push(...pts.map(p=>[p[0],p[1]+1,p[2]]));}
function chest(b,x,y,z,grade='cache',f='south'){
 b.set(x,y-1,z,M);b.set(x,y,z,`xiantu:ruin_reward_chest[facing=${f},type=single,waterlogged=false]`);b.clear(x,y+1,z,x,y+2,z);
 let dz=f==='north'?-1:f==='south'?1:0,dx=f==='west'?-1:f==='east'?1:0;
 b.clear(x+dx,y,z+dz,x+dx,y+2,z+dz);b.set(x+dx,y-1,z+dz,M);
 b.marker('reward',[x,y,z],{id:'chest_'+b.markers.length,grade,approach:[x+dx,y,z+dz],theme:'earth',implemented:true,oneShot:true});
}
function guard(b,x,y,z,pool=['crag_rock_spider'],count=4){b.set(x,y-1,z,M);b.set(x,y,z,'xiantu:ruin_encounter_seal');b.encounters.push({id:'guard_'+b.encounters.length,position:[x,y,z],species:pool,count,maxRealm:2,radius:16,triggerRadius:14,once:true,lootTheme:'earth',implemented:true});}
function lamp(b,x,y,z){b.set(x,y,z,q('xian_red_lantern'));}
function shelf(b,x,y,z){b.set(x,y,z,q('xian_bookshelf'));b.set(x+1,y,z,q('xian_display_shelf'));}
function seating(b,x,y,z){b.set(x,y,z,q('xian_writing_desk'));b.set(x,y,z+1,q('xian_wood_chair'));}
function roof(b,x,y,z,X,Z){
 // Automatic gables and native partial tiles. The ceiling, perimeter beams and
 // rafters touch the tile underside; fins are installed only on occupied ridges.
 b.drawRoofBounds(x,y,z,X,Z,Math.ceil((Z-z)/4),'gable','qingwa',{gable:W,gableOffset:2});
 b.fill(x+2,y-1,z+2,X-2,y-1,Z-2,W);
 for(const Z0 of [z+1,Z-1])b.fill(x+1,y-1,Z0,X-1,y-1,Z0,L+'[axis=x]');
 for(const X0 of [x+1,X-1])b.fill(X0,y-1,z+1,X0,y-1,Z-1,L+'[axis=z]');
 for(let xx=x+2;xx<=X-2;xx+=3){
  for(let zz=z+1;zz<Z;zz++){let ty=y+Math.floor((Math.min(zz-z,Z-zz)-1)/2);if(ty>y)b.fill(xx,y,zz,xx,ty-1,zz,W);}
 }
 // The old builder places the finial one full cell above some thin ridges.
 for(const c of [...b.map.values()])if(c[0]>=x&&c[0]<=X&&c[2]>=z&&c[2]<=Z&&c[1]>=y&&c[3].includes('qingwa_finial')){
  b.set(c[0],c[1],c[2],A);let yy=c[1]-1;while(yy>=y&&b.get(c[0],yy,c[2])===A)yy--;
  if(yy>=y)b.set(c[0],yy+1,c[2],q('town_qingwa_finial'));
 }
}
function hall(b,id,name,x,y,z,X,Z,{open=false,h=6,wall='minecraft:white_terracotta',doors='south',furniture=true,roofed=true}={}){
 b.fill(x,y,z,X,y,Z,W);b.clear(x+1,y+1,z+1,X-1,y+h-1,Z-1);if(!open)b.walls(x,y+1,z,X,y+h,z===Z?Z+1:Z,wall);
 for(let xx=x;xx<=X;xx+=Math.max(4,Math.floor((X-x)/3)))for(const zz of [z,Z]){b.fill(xx,y+1,zz,xx,y+h,zz,q('town_vermilion_column').replace('[facing=north]','[axis=y]'));b.set(xx,y+h,zz,q('town_dougong'));}
 for(const xx of [x,X])for(const zz of [z,Z])b.fill(xx,y+1,zz,xx,y+h,zz,'xiantu:town_red_wood');
 b.fill(x,y+h,z,X,y+h,z,L+'[axis=x]');b.fill(x,y+h,Z,X,y+h,Z,L+'[axis=x]');
 b.fill(x,y+h,z,x,y+h,Z,L+'[axis=z]');b.fill(X,y+h,z,X,y+h,Z,L+'[axis=z]');
 if(!open){
  for(let xx=x+3;xx<X-1;xx+=4)for(const zz of [z,Z])b.fill(xx,y+2,zz,xx+1,y+3,zz,q('town_walnut_lattice'));
  let cx=Math.floor((x+X)/2),cz=Math.floor((z+Z)/2);
  if(doors.includes('south'))b.clear(cx-1,y+1,Z,cx+1,y+4,Z);
  if(doors.includes('north'))b.clear(cx-1,y+1,z,cx+1,y+4,z);
  if(doors.includes('east'))b.clear(X,y+1,cz-1,X,y+4,cz+1);
  if(doors.includes('west'))b.clear(x,y+1,cz-1,x,y+4,cz+1);
 }
 if(roofed)roof(b,x-2,y+h+1,z-2,X+2,Z+2);else b.drawRoofBounds(x-2,y+h+1,z-2,X+2,Z+2,2,'hip','qingwa',{opening:[x+1,z+1,X-1,Z-1]});
 for(const xx of [x+1,X-1])lamp(b,xx,y+h-1,Z-1);
 if(furniture&&!open){shelf(b,x+1,y+1,z+1);seating(b,X-3,y+1,z+2);b.set(x+1,y+1,Z-2,q('xian_wood_cabinet'));}
 return room(b,id,name,x,y,z,X,y+h,Z);
}
function island(b,x,z,rx,rz,y){for(let X=x-rx;X<=x+rx;X++)for(let Z=z-rz;Z<=z+rz;Z++){
 const r=((X-x)/rx)**2+((Z-z)/rz)**2;if(r>1)continue;let bottom=Math.max(0,y-Math.floor(6+12*(1-r)));
 for(let Y=bottom;Y<=y;Y++)b.set(X,Y,Z,Y===y?G:Y>y-3?'minecraft:dirt':(X+Z+Y)%9===0?'minecraft:andesite':S);
}}
function tunnel(b,a,c,{wide=2,high=4,mine=false}={}){
 b.tunnels.push({a,c,wide,high,mine});
 let n=Math.max(...a.map((v,i)=>Math.abs(v-c[i])));
 for(let k=0;k<=n;k++){
  let[x,y,z]=a.map((v,i)=>Math.round(v+(c[i]-v)*k/Math.max(1,n)));
  b.fill(x-wide-1,y-1,z-wide-1,x+wide+1,y+high+1,z+wide+1,mine?S:'minecraft:deepslate_bricks');
 }
 // Carve after the complete envelope, so the next segment never fills the route.
 for(let k=0;k<=n;k++){let[x,y,z]=a.map((v,i)=>Math.round(v+(c[i]-v)*k/Math.max(1,n)));b.clear(x-wide,y,z-wide,x+wide,y+high,z+wide);b.fill(x-wide,y-1,z-wide,x+wide,y-1,z+wide,mine?'minecraft:gravel':'minecraft:polished_deepslate');}
 if(mine)for(let k=4;k<n-2;k+=7){
  let[x,y,z]=a.map((v,i)=>Math.round(v+(c[i]-v)*k/n));let alongX=a[0]!==c[0];
  for(let side of [-wide,wide])b.fill(x+(alongX?0:side),y,z+(alongX?side:0),x+(alongX?0:side),y+high,z+(alongX?side:0),'minecraft:oak_log[axis=y]');
  if(alongX)b.fill(x,y+high,z-wide,x,y+high,z+wide,'minecraft:oak_log[axis=z]');else b.fill(x-wide,y+high,z,x+wide,y+high,z,'minecraft:oak_log[axis=x]');
  b.set(x,y+high-1,z,'minecraft:lantern[hanging=true]');
 }
 b.route.push(a,c);return [a,c];
}
function chamber(b,id,name,x,y,z,X,Z,h=7,mine=false){
 b.excavations.push({x,y,z,X,Z,h,mine});
 b.fill(x-1,y-2,z-1,X+1,y+h+2,Z+1,mine?S:'minecraft:deepslate_bricks');b.clear(x,y,z,X,y+h,Z);b.fill(x,y-1,z,X,y-1,Z,mine?'minecraft:gravel':'minecraft:polished_deepslate');
 if(!mine){for(let xx=x+2;xx<X;xx+=6)for(const zz of [z,Z]){b.fill(xx,y,zz,xx,y+h,zz,'minecraft:chiseled_deepslate');b.set(xx,y+3,zz- (zz===Z?1:-1),'minecraft:soul_lantern');}}
 let r=room(b,id,name,x,y-1,z,X,y+h,Z);return r;
}
function finish(b,entry,design,mode){
 // The union of all excavations is carved last: crossing gallery envelopes
 // cannot reseal earlier passages. Preserve authored furnishings and supports.
 for(const {a,c,wide,high}of b.tunnels){let n=Math.max(...a.map((v,i)=>Math.abs(v-c[i])));for(let k=0;k<=n;k++){
  let[x,y,z]=a.map((v,i)=>Math.round(v+(c[i]-v)*k/Math.max(n,1)));
  for(let X=x-wide;X<=x+wide;X++)for(let Z=z-wide;Z<=z+wide;Z++)for(let Y=y;Y<=y+high;Y++)if([S,'minecraft:deepslate_bricks','minecraft:polished_deepslate','minecraft:gravel'].includes(b.get(X,Y,Z)))b.set(X,Y,Z,A);
 }}
 for(const {x,y,z,X,Z,h,mine}of b.excavations){for(let a=x;a<=X;a++)for(let c=z;c<=Z;c++){for(let Y=y;Y<=y+h;Y++)if([S,'minecraft:deepslate_bricks','minecraft:gravel'].includes(b.get(a,Y,c)))b.set(a,Y,c,A);b.set(a,y-1,c,mine?'minecraft:gravel':'minecraft:polished_deepslate');}}
 // Sloping galleries are re-cut by horizontal projection with one floor height
 // per column: overlapping sampling kernels must not erase their own stair floors.
 for(const {a,c,wide,high,mine}of b.tunnels.filter(t=>t.a[1]!==t.c[1])){
  let vx=c[0]-a[0],vz=c[2]-a[2],den=vx*vx+vz*vz;
  for(let x=Math.min(a[0],c[0])-wide;x<=Math.max(a[0],c[0])+wide;x++)for(let z=Math.min(a[2],c[2])-wide;z<=Math.max(a[2],c[2])+wide;z++){
   let t=Math.max(0,Math.min(1,((x-a[0])*vx+(z-a[2])*vz)/den));
   if(Math.hypot(x-a[0]-t*vx,z-a[2]-t*vz)>wide+.4)continue;
   let y=Math.round(a[1]+(c[1]-a[1])*t);b.set(x,y-1,z,mine?'minecraft:gravel':'minecraft:polished_deepslate');b.clear(x,y,z,x,y+high,z);
  }
 }
 for(const m of b.markers)if(m.type==='reward'){let [x,y,z]=m.position;b.set(x,y,z,`xiantu:ruin_reward_chest[facing=${(m.approach?.[0]??x)>x?'east':(m.approach?.[0]??x)<x?'west':(m.approach?.[2]??z)>z?'south':'north'},type=single,waterlogged=false]`);b.clear(x,y+1,z,x,y+2,z);b.set(x,y-1,z,M);}
 for(const e of b.encounters){let[x,y,z]=e.position;b.set(x,y,z,'xiantu:ruin_encounter_seal');b.set(x,y-1,z,M);}
 const old=source(b.id);let meta={...old,...b.export(),entry,routeTargets:b.route,design,revision:'distinct-topologies-20261024',rooms:b.rooms,encounters:b.encounters,resourceNodes:b.resources,vegetation:b.vegetation,placement:{...old.placement,mode:mode??old.placement.mode,entry,natural:true,artificialSurfaceEntrance:false},layoutGraph:{nodes:b.nodes,edges:b.edges},bossPlan:null};
 // Preserve each ruin's real tiered loot and encounter realm ceiling.
 delete meta.furnishings;delete meta.encounter;meta.exploration=design;
 const theme=old.encounters?.[0]?.lootTheme??'earth',cap=old.encounters?.[0]?.maxRealm??2;
 for(const n of meta.layoutGraph.nodes)n.theme=theme;
 for(let m of meta.markers)if(m.type==='reward'){m.theme=theme;let template=old.markers.find(x=>x.type==='reward'&&x.grade===m.grade)??old.markers.find(x=>x.type==='reward');m.loot=template.loot;m.room=b.nodes.find(n=>m.position[0]>=n.bounds[0]&&m.position[0]<=n.bounds[3]&&m.position[2]>=n.bounds[2]&&m.position[2]<=n.bounds[5])?.id;}
 for(let e of meta.encounters){e.lootTheme=theme;e.maxRealm=cap;e.triggerRadius=32;}
 if(old.bossPlan&&b.boss){meta.bossPlan={...old.bossPlan,...b.boss};}
 if(b.id==='water_curtain_caverns_grand'){meta.waterfall={pond:[80,59,80],spillway:[80,59,107],curtain:[76,17,110,84,58,110],entrance:[80,17,109],singleEntrance:true};}
 fs.writeFileSync(O+'/structures/'+b.id+'.json',JSON.stringify(meta));
 all.push({id:b.id,name:old.name,size:b.size,rooms:b.rooms.length,rewards:b.markers.length,design});console.log(b.id,b.size.join('x'),b.map.size);
}

// 1. Battlefield: interlocking earthworks, a central ravine and broken bivouacs.
{
const b=make('ancient_battlefield',97,28,79);
for(let x=3;x<94;x++)for(let z=3;z<76;z++){let y=3+Math.floor((Math.sin(x*.11)+Math.sin(z*.16))*.6);b.fill(x,0,z,x,y,z,'minecraft:dirt');b.set(x,y,z,G);}
for(let z=14;z<=63;z++){let x=43+Math.round(6*Math.sin(z*.09));b.fill(x-3,1,z,x+3,2,z,'minecraft:gravel');b.clear(x-3,3,z,x+3,7,z);}
path_(b,[[48,3,76],[48,3,65],[27,3,65],[27,3,43],[68,3,43],[68,3,17],[39,3,17]],1,'minecraft:coarse_dirt');
for(const [x,z,rx,rz]of [[17,43,10,6],[65,58,12,5],[63,12,16,4]]){
 b.walls(x,3,z,x+rx,5,z+rz,'minecraft:cracked_stone_bricks');b.fill(x+1,3,z+1,x+rx-1,3,z+rz-1,W);b.clear(x+1,4,z+1,x+rx-1,5,z+rz-1);b.clear(x+4,4,z+rz,x+6,6,z+rz);b.fill(x,6,z,x+4,6,z+rz,'minecraft:dark_oak_slab[type=top]');
 room(b,'bunker'+x,'战壕军械掩体',x,3,z,x+rx,8,z+rz);chest(b,x+1,4,z+1);b.set(x+2,4,z+2,q('xian_display_shelf'));guard(b,x+8,4,z+3,['cracked_blade_wolf','gold_back_beetle']);
}
for(const[x,z]of [[13,17],[26,21],[77,32],[82,62]]){b.fill(x,3,z,x+3,4,z+5,'minecraft:dark_oak_planks');for(let k=0;k<4;k++)b.set(x+k,3,z+6,'minecraft:grindstone[face=floor,facing=east]');}
finish(b,[48,4,76],'错落战壕、断辕战车、河沟两岸掩体；路线横贯战场而非主殿加侧院');
}
// 2. Hermit's underground home: a ring of intimate natural chambers.
{
const b=make('ancient_cultivator_cave',73,26,67),rooms=[[9,30,25,44,'寝居'],[24,12,42,25,'藏经室'],[47,26,62,41,'炼丹室'],[26,45,44,58,'静修室']];
for(let i=0;i<rooms.length;i++){let[x,z,X,Z,n]=rooms[i];chamber(b,'r'+i,n,x,6,z,X,Z,7);shelf(b,x+1,6,z+1);seating(b,X-3,6,z+2);chest(b,x+1,6,Z-1,i===3?'core':'cache');}
for(const[a,c]of [[[17,6,33],[17,6,19]],[[17,6,19],[32,6,19]],[[38,6,19],[55,6,19]],[[55,6,19],[55,6,34]],[[55,6,37],[55,6,51]],[[55,6,51],[36,6,51]],[[30,6,51],[17,6,51]],[[17,6,51],[17,6,40]]])tunnel(b,a,c,{wide:1});
b.set(55,6,28,q('xian_wood_cabinet'));guard(b,34,6,49);finish(b,[16,6,36],'环形洞府、寝居藏经丹房静室四个生活空间；仅与天然洞穴相接','underground');
}
// 3. Broken bridge: two opposed banks, a crooked bridge and water-edge pavilion.
{
const b=make('broken_bridge_pavilion',85,29,65);
for(let x=4;x<81;x++)for(let z=5;z<60;z++){let edge=35+Math.round(5*Math.sin(z*.1));if(x<edge||x>edge+12){b.fill(x,0,z,x,3,z,'minecraft:dirt');b.set(x,3,z,G);}else b.set(x,0,z,'minecraft:gravel');}
for(let x=7;x<=77;x++){let z=43-Math.round(x*.28);b.fill(x,4,z-2,x,4,z+2,W);if(x%5===0)b.fill(x,0,z-2,x,3,z-2,L);}
b.clear(39,4,29,44,5,35);path_(b,[[28,3,58],[28,3,47],[33,3,44],[40,2,44],[49,2,44],[57,3,41],[65,3,34]],1,'minecraft:gravel');
hall(b,'water_pavilion','临水石亭',60,4,15,76,27,{open:true,furniture:false});b.set(67,5,21,q('xian_round_stone_table'));b.set(65,5,21,q('xian_stone_stool'));
hall(b,'keeper','守桥人旧屋',9,3,11,28,23,{doors:'south'});chest(b,11,4,12,'core');chest(b,74,5,17,'cache','west');guard(b,67,5,26,['cold_fang_wolf','dark_shell_crab']);
finish(b,[28,4,58],'曲折断桥跨越双岸，木栈绕行至水榭；藏物在守桥人旧屋');
}
// 4. Pagoda: complete enclosed storeys; furnishings bounded strictly inside walls.
{
const b=make('cinnabar_seal_tower',37,61,37);b.fill(6,0,6,30,1,30,M);
for(let k=0;k<6;k++){
 const y=2+k*8;hall(b,'floor'+k,['门厅','制符坊','藏经层','静修层','祭符层','塔顶封藏'][k],10,y,10,26,26,{h:6,furniture:false,doors:k===0?'south':'',roofed:k===5});
 shelf(b,16,y+1,11);seating(b,23,y+1,23);b.set(11,y+1,23,q('xian_wood_cabinet'));if(k%2===1)chest(b,11,y+1,11,k===5?'core':'cache');
}
for(let k=0;k<5;k++){let y=2+k*8,x=k%2?23:12;for(let n=0;n<8;n++){let z=k%2?13+n:20-n,Y=y+1+n;b.clear(x,Y+1,z,x+1,Y+3,z);b.fill(x,Y,z,x+1,Y,z,`minecraft:spruce_stairs[facing=${k%2?'south':'north'}]`);}}
path_(b,[[18,1,33],[18,1,28],[18,2,25]],1);guard(b,19,19,22,['yanwei_fox','flint_armored_lizard']);
finish(b,[18,2,33],'六层内收宝塔，交替内梯与藏经格；家具仅在闭合室内，不投放到瓦檐');
}
// 5. Cloud palace: a crescent island with a broad split court, no appended pad.
{
const b=make('cloudsea_palace',111,53,95);island(b,55,45,48,38,22);
hall(b,'north_hall','云海正殿',36,22,15,78,33,{h:8});hall(b,'west_library','西廊藏经阁',15,22,41,38,55,{doors:'east'});hall(b,'east_gallery','东侧观云亭',73,22,47,96,61,{open:true,furniture:false});
disk(b,55,22,55,16,14,'minecraft:quartz_bricks');disk(b,55,22,55,9,7,'minecraft:light_blue_stained_glass');
path_(b,[[57,22,82],[57,22,69],[41,22,64],[27,22,56]],1);path_(b,[[57,22,69],[79,22,68],[84,22,61]],1);path_(b,[[57,22,69],[65,22,61],[65,22,34]],1);
chest(b,17,23,42,'core');chest(b,37,23,16);chest(b,91,23,49);guard(b,59,23,42,['electric_marten','purple_lightning_bat']);
finish(b,[57,23,82],'弯月云岛、偏心镜池、三面殿阁与分流回廊，建筑落在同一自然岛体');
}
// 6. Mine: more than a thousand blocks of branching galleries in three connected levels.
{
const b=make('earthvein_mine_palace',181,43,157),ys=[8,20,32],grid=[];
for(let layer=0;layer<3;layer++){
 let y=ys[layer],row=[];
 for(let j=0;j<4;j++){let z=20+j*35+(layer%2?8:0);let a=[12,y,z],c=[168,y,z];tunnel(b,a,c,{mine:true});row.push(z);
  for(let x=17;x<165;x++)if(x%13!==0)b.set(x,y,z,'minecraft:rail[shape=east_west]');
 }
 for(let i=0;i<5;i++){let x=20+i*33+(layer===1?7:0);tunnel(b,[x,y,row[0]],[x,y,row[3]],{mine:true});}
 grid.push(row);
 for(let i=0;i<4;i++){let x=28+i*39,z=row[i];chamber(b,'mine'+layer+'_'+i,['采掘面','矿车调度间','支架仓','旧冶炼窟'][i],x-8,y,z-8,x+8,z+8,6,true);b.fill(x-7,y,z-7,x-4,y+2,z-5,'minecraft:raw_iron_block');b.set(x+6,y,z-6,'minecraft:barrel[facing=up]');chest(b,x+6,y,z+6,i===3&&layer===2?'core':'cache');guard(b,x,y,z,['crag_rock_spider','rock_back_boar']);}
}
for(let l=0;l<2;l++){let y=ys[l];tunnel(b,[165,y,grid[l][3]],[165,y,139],{mine:true});tunnel(b,[165,y,139],[150,y,139],{mine:true});tunnel(b,[150,y,139],[150,y,132],{mine:true});tunnel(b,[150,y,132],[150,y+12,108],{mine:true});tunnel(b,[150,y+12,108],[150,y+12,grid[l+1][2]],{mine:true});}
// Restore the long galleries after chamber envelopes, with supporting timber frames.
for(let l=0;l<3;l++)for(const z of grid[l])tunnel(b,[12,ys[l],z],[168,ys[l],z],{mine:true});
for(let l=0;l<3;l++)for(let i=0;i<5;i++){let x=20+i*33+(l===1?7:0);tunnel(b,[x,ys[l],grid[l][0]],[x,ys[l],grid[l][3]],{mine:true});}
for(let l=0;l<3;l++)for(let i=0;i<4;i++){let x=28+i*39,z=grid[l][i];b.set(x-6,ys[l],z-6,'minecraft:furnace[facing=south]');}
finish(b,[12,8,20],'三层大型废矿井、纵横运输巷、连续木支架、采掘面和矿仓；无通地表井口','underground');
}
// 7. Herb islands: a curving chain of four unequal islands with short connecting spans.
{
const b=make('floating_herb_garden',119,49,101);
for(const p of [[29,69,22,20,20],[34,30,24,21,20],[79,27,24,19,20],[88,67,22,23,20]])island(b,...p);
path_(b,[[29,20,70],[28,20,30],[77,20,27],[88,20,65],[29,20,70]],1,W);
hall(b,'seed','种苗藏庐',20,20,19,43,31,{h:5});hall(b,'drying','晾药长廊',67,20,17,91,29,{open:true,h:5,furniture:false});
for(const [cx,cz]of [[27,68],[83,66]])for(let x=cx-10;x<=cx+10;x+=5)for(let z=cz-9;z<=cz+9;z+=4){b.fill(x,20,z,x+2,20,z+1,G);b.set(x,21,z,'xiantu:herb_0007_wild');}
for(let x=69;x<88;x+=4)b.set(x,21,19,q('ruin_weathered_herb_rack'));chest(b,21,21,20,'core');chest(b,80,21,72);chest(b,31,21,76);guard(b,78,21,29,['jade_leaf_mantis','moss_back_bear']);
finish(b,[29,21,86],'四座不等大的弧形药岛、短桥闭环、药庐与开放晾药廊；不复用宫殿轴线');
}
// 8. Herb garden: enclosed L-shaped farmhouse around irrigation beds.
{
const b=make('guiyuan_herb_garden',69,30,65);b.fill(4,0,4,64,1,60,'minecraft:dirt');b.fill(4,2,4,64,2,60,G);
hall(b,'clinic','药师旧庐',10,2,9,37,23);hall(b,'storage','种子药库',44,2,9,59,33,{doors:'west'});
b.walls(8,3,8,61,4,57,'minecraft:mossy_stone_bricks');b.clear(30,3,57,34,6,57);
for(let x=13;x<=48;x+=7)for(let z=31;z<=47;z+=8){b.fill(x,2,z,x+4,2,z+4,G);for(let X=x;X<x+4;X+=2)for(let Z=z;Z<z+4;Z+=2)b.set(X,3,Z,'xiantu:herb_0007_wild');}
for(let z=25;z<53;z++)b.set(41,2,z,'minecraft:water');
path_(b,[[32,2,60],[32,2,52],[10,2,52],[10,2,25],[24,2,25],[24,2,23]],1,'minecraft:gravel');path_(b,[[32,2,52],[57,2,52],[57,2,37],[42,2,37],[42,2,22],[44,2,22]],1,'minecraft:gravel');
chest(b,58,3,10,'core');chest(b,11,3,10);guard(b,53,3,40,['jade_leaf_mantis','moss_back_bear']);finish(b,[32,3,60],'L形药居围绕不等宽灌渠药畦，园墙与晾药区围合，无外接侧平台');
}
// 9. A rocky nest; retain natural identity rather than a building plus workshop.
{
const b=make('mysterious_bird_nest',69,47,65);island(b,34,32,29,27,20);
for(let x=13;x<56;x++)for(let z=13;z<52;z++){let r=Math.hypot((x-34)/21,(z-32)/19);if(r>.72&&r<1)for(let y=21;y<=24+((x+z)%4);y++)b.set(x,y,z,'minecraft:dark_oak_log[axis=x]');if(r<.72)b.set(x,20,z,'minecraft:hay_block');}
b.clear(31,21,49,37,25,53);path_(b,[[34,20,59],[34,20,50],[34,20,40]],1,'minecraft:coarse_dirt');
for(const[x,z]of [[20,22],[49,35],[28,43]])b.set(x,21,z,'minecraft:bone_block');chest(b,18,21,30,'core','east');b.fill(16,21,29,17,24,33,L);chest(b,44,21,24);guard(b,36,21,29,['copper_beak_vulture']);room(b,'nest','玄鸟旧巢',13,20,13,55,29,51);finish(b,[34,21,59],'天然尖岩上的环枝鸟巢、羽骨残迹与巢壁藏隙，取消方形支台');
}
// 10. Star observatory: concentric terraces with a cross-axis astronomical library.
{
const b=make('ninefold_star_observatory',101,60,99);island(b,50,48,43,42,20);
for(let k=0;k<4;k++){let r=24-k*6,y=20+k*3;for(let Y=20;Y<=y;Y++)disk(b,50,Y,46,r,r,'minecraft:quartz_bricks');for(let n=0;n<3;n++)path_(b,[[48,y+n,70-k*6-n],[52,y+n,70-k*6-n]],0,'minecraft:quartz_stairs[facing=north]');}
b.set(50,33,46,q('ruin_star_dial'));hall(b,'library','星图藏阁',11,20,35,29,49,{doors:'east'});hall(b,'loggia','观星长廊',64,20,29,88,41,{open:true,furniture:false});
path_(b,[[50,20,88],[50,20,78]],1,'minecraft:quartz_bricks');path_(b,[[29,20,42],[31,20,42]],1);chest(b,12,21,36,'core');chest(b,82,21,39);chest(b,26,21,48);guard(b,76,21,35,['purple_lightning_bat','electric_marten']);finish(b,[50,21,88],'同心观星阶台与横向藏图阁，楼阁收于岩岛之上');
}
// 11. Compact ship cabin wholly inside the hull, hold reached via an open hatch.
{
const b=make('skyship_wreck',85,49,39),half=x=>Math.max(1,Math.min(16,Math.floor((x-2)*.7),Math.floor((82-x)*.65)));
for(let x=3;x<82;x++)for(let y=1;y<=13;y++){let r=Math.max(1,half(x)-Math.ceil((13-y)*.65));for(let z=19-r;z<=19+r;z++)if(y===1||y===13||Math.abs(z-19)>=r-1)b.set(x,y,z,y%4===1?'minecraft:dark_oak_planks':W);}
b.fill(20,5,9,65,5,29,W);b.clear(20,6,10,65,12,28);
for(let x=4;x<81;x++)for(const z of [19-half(x),19+half(x)])b.set(x,14,z,q('town_vermilion_railing',z<19?'north':'south'));
hall(b,'cabin','紧凑航图舱',23,13,12,42,26,{doors:'east',h:5});b.set(25,14,14,'minecraft:cartography_table');
for(let k=0;k<9;k++){let x=57+k,y=13-k;b.fill(x,y,11,x,y,13,'minecraft:spruce_stairs[facing=west]');b.clear(x,y+1,11,x,17,13);}
for(const x of [34,48]){b.fill(x,6,10,x,11,28,W);b.clear(x,6,x===34?23:11,x,9,x===34?26:14);}
for(const[x,z]of [[25,11],[38,11],[51,25],[62,25]]){b.fill(x,6,z,x+2,7,z+2,'minecraft:barrel[facing=up]');}
chest(b,22,6,27,'core');chest(b,46,6,12);chest(b,64,6,26);guard(b,55,6,20,['gold_back_beetle','cracked_blade_wolf']);
b.fill(49,13,19,49,43,19,'minecraft:dark_oak_log');b.fill(49,41,7,49,41,31,'minecraft:dark_oak_log[axis=z]');
for(let y=24;y<41;y++)for(let z=8;z<31;z++)if(!((y+z)%13===0&&z>26))b.set(49,y,z,y%4===0?'minecraft:light_gray_wool':'minecraft:white_wool');
room(b,'hold','错舱货腹',20,5,9,65,12,29);b.route=[[42,14,24],[56,14,12],[65,6,14],[60,6,22],[43,6,25],[23,6,26]];finish(b,[42,14,33],'修长船体、收小航图舱、侧向下舱梯、错开舱门与分隔货仓；瓦檐不超船舷');
}
// 12. Tomb: orthogonal but irregular burial network, two levels and several loops.
{
const b=make('taixu_tomb_passage',157,43,147);
const rs=[[13,13,35,31],[61,9,85,29],[117,17,139,39],[13,65,39,87],[65,57,91,85],[115,71,143,97],[39,111,65,135],[89,109,119,135]];
const centers=rs.map(([x,z,X,Z])=>[Math.floor((x+X)/2),12,Math.floor((z+Z)/2)]);
for(let i=0;i<rs.length;i++){let[x,z,X,Z]=rs[i];chamber(b,'tomb'+i,['守墓前厅','祭器藏室','侧室骨冢','回廊石室','主祭大殿','静藏墓室','殉葬室','后陵寝'][i],x,12,z,X,Z,9);}
const edges=[[0,1],[1,2],[0,3],[1,4],[2,5],[3,4],[4,5],[3,6],[4,7],[5,7],[6,7]];
for(const[a,c]of edges){let u=centers[a],v=centers[c],bend=[v[0],12,u[2]];tunnel(b,u,bend);tunnel(b,bend,v);b.edges.push({from:'tomb'+a,to:'tomb'+c,kind:'burial_corridor'});}
// Lower ossuary loop, linked by long walkable descending flights.
chamber(b,'ossuary','地下骨室',50,3,48,76,70,6);tunnel(b,[62,3,64],[62,12,91]);tunnel(b,[62,12,91],[78,12,91]);tunnel(b,[60,3,50],[33,12,50]);tunnel(b,[33,12,50],[24,12,50]);tunnel(b,[24,12,50],centers[0]);
for(let i=0;i<rs.length;i++){let[x,z,X,Z]=rs[i];let y=12;b.fill(x+4,y,z+4,x+7,y+1,z+10,'minecraft:polished_deepslate');b.fill(x+4,y+2,z+4,x+7,y+2,z+10,'minecraft:deepslate_tile_slab');b.set(X-2,y,z+2,q('xian_display_shelf'));chest(b,X-2,y,Z-2,i===7?'core':i%3===0?'vault':'cache');if(i%2===0)chest(b,x+1,y,Z-2);guard(b,Math.floor((x+X)/2)+4,y,Math.floor((z+Z)/2)+2,['crag_rock_spider','purple_lightning_bat']);}
chest(b,74,3,68,'vault');guard(b,62,3,59);finish(b,centers[0],'九间大小不同的地宫、双层墓道、十一条连廊与多个回环；棺台/祭器龛分割视线','underground');
}
// 13. Water-curtain mountain: all chambers are buried, one waterfall entrance.
{
const b=make('water_curtain_caverns_grand',169,81,163),tops=new Map();
for(let x=3;x<=165;x++)for(let z=3;z<=159;z++){
 let r=((x-83+4*Math.sin(z*.065))/81)**2+((z-75+3*Math.sin(x*.081))/80)**2;if(r>=1)continue;
 let H=Math.max(3,Math.floor(3+66*Math.pow(1-r,.72)+2*Math.sin(x*.11)*Math.sin(z*.09)));
 // A summit shelf feeds a rock-backed cliff waterfall; not a suspended water wall.
 if(x>=58&&x<=108&&z>=61&&z<=110)H=Math.max(H,60);
 if(z>110){let width=9+(z-111)*.18,dist=Math.abs(x-80);if(dist<width+16)H=Math.min(H,17+Math.round(Math.max(0,dist-width)*2.4));}
 tops.set(x+','+z,H);b.fill(x,0,z,x,H,z,S);b.fill(x,H-2,z,x,H-1,z,'minecraft:dirt');b.set(x,H,z,G);
}
const rooms=[[56,23,25,104,65,17,'arena','水帘大洞'],[33,23,48,52,72,10,'quiet','石床静室'],[109,23,52,132,76,11,'vault','石壁藏宝窟'],[56,23,78,78,97,9,'herbs','花果石苑'],[91,23,82,113,100,10,'arms','旧兵洞'],[66,23,95,94,106,8,'vestibule','瀑后湿厅']];
for(const[x,y,z,X,Z,h,id,name]of rooms){b.clear(x,y,z,X,y+h,Z);b.fill(x,y-1,z,X,y-1,Z,'minecraft:mossy_cobblestone');room(b,id,name,x,y-1,z,X,y+h,Z);}
const corridors=[[[80,17,110],[80,17,103]],[[80,17,103],[80,23,91]],[[80,23,91],[80,23,62]],[[75,23,60],[43,23,60]],[[43,23,65],[43,23,89]],[[43,23,89],[66,23,89]],[[92,23,60],[120,23,60]],[[120,23,60],[120,23,91]],[[120,23,91],[101,23,91]],[[101,23,91],[80,23,91]]];
for(const[a,c]of corridors){let n=Math.max(...a.map((v,i)=>Math.abs(v-c[i])));for(let k=0;k<=n;k++){let[x,y,z]=a.map((v,i)=>Math.round(v+(c[i]-v)*k/Math.max(n,1)));b.clear(x-2,y,z-2,x+2,y+5,z+2);b.fill(x-2,y-1,z-2,x+2,y-1,z+2,'minecraft:mossy_cobblestone');}b.route.push(a,c);}
for(const[x,z]of [[62,31],[98,31],[62,59],[98,59]])b.fill(x,23,z,x+1,37,z+1,'minecraft:andesite');
for(const[x,z]of [[35,50],[109,54],[56,80],[93,84]]){shelf(b,x,23,z);lamp(b,x+2,29,z);}
chest(b,130,23,54,'core','west');chest(b,34,23,69);chest(b,57,23,95);chest(b,111,23,98,'vault');
guard(b,66,23,88,['moss_back_bear','jade_leaf_mantis']);guard(b,120,23,69,['jade_scale_crocodile']);
b.boss={position:[80,23,46],bounds:[56,22,25,104,41,65],room:'arena'};
// Broad interconnected halls in two storeys, rather than a few small cubicles.
const lower=[[48,8,37,108,74,11,'lower_lake','地下回声湖厅'],[43,8,78,75,100,10,'lower_store','灵果藏窟'],[91,8,78,121,99,10,'lower_armory','下层石兵洞']];
for(const[x,y,z,X,Z,h,id,name]of lower){b.clear(x,y,z,X,y+h,Z);b.fill(x,y-1,z,X,y-1,Z,'minecraft:mossy_cobblestone');room(b,id,name,x,y-1,z,X,y+h,Z);}
// Upper gathering hall gains two broad wings, still fully enclosed by solid mountain.
for(const[x,z,X,Z]of [[41,35,56,48],[104,35,121,48]]){b.clear(x,23,z,X,38,Z);b.fill(x,22,z,X,22,Z,'minecraft:mossy_cobblestone');}
const lowRoutes=[[[62,8,60],[43,8,60]],[[43,8,60],[43,23,90]],[[43,23,90],[80,23,90]],[[80,8,63],[80,8,88]],[[66,8,88],[107,8,88]],[[108,8,60],[125,8,60]],[[125,8,60],[125,23,90]],[[125,23,90],[80,23,90]]];
for(const[a,c]of lowRoutes){
 b.tunnels.push({a,c,wide:2,high:5,mine:false});let n=Math.max(...a.map((v,i)=>Math.abs(v-c[i])));
 for(let k=0;k<=n;k++){let[x,y,z]=a.map((v,i)=>Math.round(v+(c[i]-v)*k/n));b.clear(x-2,y,z-2,x+2,y+5,z+2);b.fill(x-2,y-1,z-2,x+2,y-1,z+2,'minecraft:polished_deepslate');}b.route.push(a,c);
}
// Re-carving sloping galleries is handled in finish; fixed chamber floors stay distinct.
for(const[x,z]of [[53,43],[102,43],[53,68],[102,68]])b.fill(x,8,z,x+1,18,z+1,'minecraft:andesite');
for(const[x,z]of [[46,82],[93,81]]){shelf(b,x,8,z);lamp(b,x+2,15,z);}
chest(b,45,8,97,'vault');chest(b,118,8,96,'cache','west');chest(b,50,8,39,'cache');guard(b,99,8,87,['moss_back_bear','jade_scale_crocodile']);
for(const [x,z]of [[126,70],[127,70],[126,71],[127,71]])b.resources.push({position:[x,23,z],resource:'spirit_gold_essence'});
// Summit pond with a single south-facing spillway, within the existing rock shelf.
for(let x=62;x<=102;x++)for(let z=63;z<=101;z++)if(((x-82)/20)**2+((z-82)/19)**2<1){b.clear(x,60,z,x,78,z);b.set(x,58,z,'minecraft:clay');b.set(x,59,z,'minecraft:water');}
for(let z=98;z<=110;z++){b.fill(76,58,z,84,58,z,S);b.clear(76,59,z,84,78,z);b.fill(76,59,z,84,59,z,'minecraft:water');}
// Flowing (not source) curtain has solid rock immediately behind most of its run.
for(let x=76;x<=84;x++)for(let y=17;y<=58;y++)b.set(x,y,111,'minecraft:water[level=8]');
for(let x=70;x<=95;x++)for(let z=112;z<=137;z++)if(((x-82)/13)**2+((z-124)/13)**2<1){b.fill(x,0,z,x,14,z,S);b.set(x,15,z,'minecraft:clay');b.set(x,16,z,'minecraft:water');b.clear(x,17,z,x,65,z);}
// A bank path reaches one naturally arched opening behind the water curtain.
path_(b,[[70,16,145],[70,16,136],[72,16,125],[74,16,115],[80,16,112]],1,'minecraft:gravel');
 for(let y=17;y<=24;y++){
  const half=({17:4,18:3,19:4,20:4,21:3,22:2,23:2,24:1})[y];
  const centerOffset=y===18||y===22?-1:y===23?1:0;
  b.clear(80-half+centerOffset,y,108,80+half+centerOffset,y,110);
}
 // Short, offset rock shelves dissolve the two doorway cheeks into the cliff.
 for(const [x,y,z,state] of [
  [70,17,108,'minecraft:mossy_cobblestone'],[73,18,109,'minecraft:stone_brick_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]'],
  [71,20,107,'minecraft:mossy_cobblestone'],[75,22,110,'minecraft:stone_brick_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]'],
  [74,25,108,'minecraft:mossy_cobblestone'],[76,28,109,'minecraft:stone_brick_stairs[facing=east,half=bottom,shape=straight,waterlogged=false]'],
  [90,17,109,'minecraft:stone_brick_stairs[facing=west,half=bottom,shape=straight,waterlogged=false]'],[87,19,108,'minecraft:mossy_cobblestone'],
  [89,21,110,'minecraft:stone_brick_stairs[facing=west,half=bottom,shape=straight,waterlogged=false]'],[85,24,108,'minecraft:mossy_cobblestone'],
  [88,26,109,'minecraft:stone_brick_stairs[facing=west,half=bottom,shape=straight,waterlogged=false]'],[84,29,108,'minecraft:mossy_cobblestone']
 ])b.set(x,y,z,state);
for(const[x,z]of [[26,42],[40,26],[120,28],[140,48],[30,100],[136,110],[46,126],[113,131],[47,76],[118,84],[57,39],[102,35],[35,83],[128,78],[62,116],[98,121],[44,105],[117,101],[72,31],[91,28],[54,113],[64,126],[96,119],[106,104],[72,136],[91,132],[49,96],[111,91]]){let y=tops.get(x+','+z);if(y)b.vegetation.push({position:[x,y+1,z],feature:'minecraft:oak'});}
for(let x=14;x<155;x+=4)for(let z=15;z<150;z+=5){let y=tops.get(x+','+z);if(y&&b.get(x,y,z)===G){const feature=(x+z)%4===0?'minecraft:patch_tall_grass':(x*3+z)%7===0?'minecraft:patch_large_fern':'minecraft:patch_grass_plain';b.vegetation.push({position:[x,y+1,z],feature});}}
finish(b,[70,17,145],'草覆山体、天池瀑布唯一入口；双层大洞、回声湖厅、藏果窟、兵洞与两条上下层回环全埋山中','mountain');
}
// 14. Underwater palace under a sealed hemispherical glass dome.
{
const b=make('water_palace',101,39,101),cx=50,cz=50,R=44,base=5,domeHeight=29;
for(let x=5;x<=95;x++)for(let z=5;z<=95;z++){
 let r=(x-cx)**2+(z-cz)**2;if(r>R*R)continue;
 b.fill(x,0,z,x,base,z,'minecraft:prismarine_bricks');
 let top=base+Math.floor(Math.sqrt(R*R-r)*domeHeight/R);b.clear(x,base+1,z,x,top,z);b.set(x,top,z,'minecraft:light_blue_stained_glass');
 // Steep hemisphere perimeter needs vertical glass, not only a sparse height skin.
 for(const[dx,dz]of [[1,0],[-1,0],[0,1],[0,-1]]){let rr=(x+dx-cx)**2+(z+dz-cz)**2;let near=rr>R*R?base:base+Math.floor(Math.sqrt(R*R-rr)*domeHeight/R);if(near<top)b.fill(x,near,z,x,top,z,'minecraft:light_blue_stained_glass');}
}
hall(b,'palace','沉水正殿',32,5,20,69,40,{h:7,wall:'minecraft:prismarine_bricks'});
hall(b,'west','西藏珠殿',17,5,47,38,59,{h:5,wall:'minecraft:dark_prismarine',doors:'east'});
hall(b,'east','东祭潮殿',63,5,51,83,63,{h:5,wall:'minecraft:prismarine_bricks',doors:'west'});
disk(b,50,5,58,10,8,'minecraft:sea_lantern');disk(b,50,5,58,8,6,'minecraft:dark_prismarine');
path_(b,[[50,5,88],[50,5,73],[42,5,69],[28,5,69],[28,5,59]],1,'minecraft:prismarine_bricks');path_(b,[[50,5,73],[73,5,73],[73,5,63]],1,'minecraft:prismarine_bricks');path_(b,[[50,5,73],[58,5,66],[58,5,41]],1,'minecraft:prismarine_bricks');
// Water-filled bent vestibule. Only the low dogleg communicates with the ocean,
// so water remains in a contained pool and the main palace floor stays dry.
b.fill(47,2,89,53,6,96,'minecraft:prismarine_bricks');b.clear(49,3,90,51,4,96);b.fill(49,3,90,51,4,96,'minecraft:water');b.clear(49,5,87,51,7,90);b.fill(49,3,97,51,8,100,'minecraft:water');
chest(b,18,6,48,'core');chest(b,80,6,52);chest(b,33,6,21,'vault');guard(b,72,6,56,['dark_shell_crab','jade_scale_crocodile']);
finish(b,[50,7,99],'海床上的半球封闭青玻璃罩、三殿水庭与曲折潜入门；仅深海生成','underwater');
}
// 15. Sword burial ground: broken radial rings cut into a natural rocky hollow.
{
const b=make('wilderness_sword_tomb',93,37,87);
for(let x=4;x<89;x++)for(let z=4;z<83;z++){
 let r=Math.hypot((x-46)/40,(z-42)/37);if(r>1)continue;let y=2+Math.floor(5*r*r+Math.sin(x*.22)*Math.sin(z*.17));b.fill(x,0,z,x,y,z,'minecraft:andesite');b.set(x,y,z,r>.82?G:'minecraft:gravel');
}
for(let i=0;i<33;i++){let t=i*2.399,r=8+Math.sqrt(i)*4.6,x=Math.round(46+Math.cos(t)*r),z=Math.round(42+Math.sin(t)*r),Y=2+Math.floor(5*(r/40)**2+Math.sin(x*.22)*Math.sin(z*.17));
 b.set(x,Y+1,z,q('ruin_sealed_swordstone',i%2?'east':'north'));if(i%4===0)b.fill(x,Y+1,z,x,Y+3,z,'minecraft:chiseled_stone_bricks');}
path_(b,[[47,6,79],[59,4,65],[66,3,47],[53,2,31],[37,2,35],[32,3,50]],1,'minecraft:gravel');
for(let x=24;x<=54;x++)for(let z=16;z<=40;z++){let r=((x-39)/16)**2+((z-28)/14)**2;if(r<1)b.fill(x,0,z,x,Math.max(12,Math.floor(15-r*2)),z,'minecraft:andesite');}
chamber(b,'crypt','断剑地窟',32,3,23,46,34,6);tunnel(b,[32,3,50],[32,3,34],{wide:1});path_(b,[[32,5,58],[32,3,52],[32,2,48]],1,'minecraft:gravel');chest(b,44,3,25,'core','west');chest(b,34,3,25,'cache');guard(b,56,3,44,['cracked_blade_wolf','gold_back_beetle']);room(b,'sword_ring','残剑环冢',10,0,9,82,14,77);
finish(b,[47,7,79],'下凹剑冢、破碎放射剑林与埋入山壁的封剑地窟；取消右侧方台');
}
// 16. Ruined city: large districts and crossed lanes, central future boss court.
{
const b=make('xuantian_ruined_city',221,55,193);b.fill(4,0,4,216,2,188,'minecraft:dirt');b.fill(4,3,4,216,3,188,G);
for(const z of [13,178]){b.fill(12,4,z-2,208,13,z+2,M);for(let x=12;x<=208;x+=6)for(const dz of [-2,2])b.fill(x,14,z+dz,x+2,15,z+dz,M);}
for(const x of [14,206]){b.fill(x-2,4,11,x+2,13,180,M);for(let z=11;z<180;z+=6)for(const dx of [-2,2])b.fill(x+dx,14,z,x+dx,15,z+2,M);}
for(const[x,z]of [[14,13],[206,13],[14,178],[206,178]]){
 b.fill(x-6,4,z-6,x+6,17,z+6,M);b.clear(x-3,5,z-3,x+3,16,z+3);b.fill(x-6,17,z-6,x+6,17,z+6,M);
 for(let k=-6;k<=6;k+=3)for(const side of [-6,6]){b.fill(x+k,18,z+side,x+k+1,20,z+side,M);b.fill(x+side,18,z+k,x+side,20,z+k+1,M);}
 // A torn upper corner, with a supported jagged edge and rubble at its foot.
 b.clear(x+2,12,z-6,x+6,22,z-2);b.fill(x+2,4,z-5,x+5,5,z-3,'minecraft:cracked_stone_bricks');
}
b.clear(104,4,175,116,13,181);hall(b,'gate','断城门楼',98,14,173,122,182,{open:true,h:6,furniture:false});b.clear(104,4,173,116,13,182);
path_(b,[[110,3,188],[110,3,32]],3,'minecraft:polished_andesite');path_(b,[[25,3,101],[194,3,101]],3,'minecraft:polished_andesite');
const plots=[[28,27,65,48,'library','残经院'],[145,25,184,47,'academy','讲经堂'],[30,68,67,85,'housing','旧民居'],[148,65,187,86,'forge','残军械坊'],[29,119,67,141,'clinic','丹房院'],[148,122,189,143,'stores','旧商铺'],[45,152,78,168,'west_house','西宅'],[141,154,175,168,'east_house','东宅']];
for(let i=0;i<plots.length;i++){let[x,z,X,Z,id,n]=plots[i];hall(b,id,n,x,3,z,X,Z,{doors:'southnortheastwest',h:i<2?8:6});let zz=Math.floor((z+Z)/2);path_(b,[[x<100?X+1:x-1,3,zz],[110,3,zz]],1,'minecraft:gravel');chest(b,x+1,4,z+1,i<2?'vault':'cache');if(i%2===0)chest(b,X-2,4,z+2);guard(b,Math.floor((x+X)/2),4,Math.floor((z+Z)/2),['cracked_blade_wolf','gold_back_beetle']);
 // Local ruin damage, no isolated hanging roof blocks.
 b.clear(X-5,4,z,X,7,z);for(let xx=X-5;xx<=X;xx++)b.set(xx,4,z-1,q('ruin_collapsed_qingwa'));
}
// Narrow residences, covered market fronts and ruined foundations fill separate districts.
for(const[x,z,X,Z]of [[36,53,53,62],[156,52,175,59],[81,122,96,135],[123,115,138,127],[81,155,96,168],[123,147,137,160]]){
 hall(b,'lane'+x+'_'+z,'废弃里巷居所',x,3,z,X,Z,{h:4,doors:'eastwest',furniture:true});
 b.clear(x,4,z,x+4,7,z+3);b.clear(x-2,8,z-2,x+4,18,z+3);
}
for(const [x,z]of [[33,105],[54,107],[155,105],[179,107]]){
 b.fill(x,3,z,x+7,3,z+4,W);for(const X of [x,x+7])b.fill(X,4,z,X,6,z,L);
 b.fill(x,7,z,x+7,7,z+4,'minecraft:dark_oak_slab[type=bottom]');
 b.set(x+2,4,z+1,q('xian_display_shelf'));b.set(x+5,4,z+2,q('xian_wood_cabinet'));
}
for(const[x,z]of [[35,113],[72,49],[140,92],[192,119],[58,145],[146,167]]){
 for(let dx=-4;dx<=4;dx++)for(let dz=-3;dz<=3;dz++)if((dx*31+dz*17+x+z)%5===0){let st=(dx+dz)%2===0?'minecraft:cracked_stone_bricks':'minecraft:stone_brick_slab';b.set(x+dx,4,z+dz,st);}
 b.set(x,4,z,q('ruin_collapsed_qingwa'));b.set(x+2,4,z+1,q('xian_wood_bench'));
}
// Four wall breaches, collapsed battlements and exposed inner masonry.
for(const[x,z,X,Z]of [[11,47,17,55],[203,94,209,106],[58,10,68,16],[164,175,174,181]])b.clear(x,4,z,X,22,Z);
for(const[x,z]of [[21,151],[192,152],[21,38],[197,38],[75,113],[137,174]])b.vegetation.push({position:[x,4,z],feature:'minecraft:oak'});
// The central court is intentionally broad for the future city-lord Boss.
disk(b,110,3,63,26,23,'minecraft:polished_andesite');room(b,'future_boss','玄天内城演武场',83,3,35,137,29,88,'未来城主首领场地；当前不解除人形Boss封存');
hall(b,'inner_palace','残城内殿',86,3,15,136,31,{h:10});chest(b,88,4,16,'core');chest(b,133,4,17,'vault');
for(const[x,z]of [[81,116],[130,138],[79,60],[136,105],[95,155]]){b.set(x,4,z,q('xian_stone_stool'));b.set(x+2,4,z,q('xian_round_stone_table'));}
// Every building visibly ruined: large roof collapses, ragged wall crowns and exposed beams.
for(let i=0;i<b.rooms.length;i++){
 const r=b.rooms[i];if(r.purpose?.includes('未来'))continue;const[x,y,z,X,Y,Z]=r.bounds;
 if(X-x<8||Z-z<6)continue;
 const cx=Math.floor((x+X)/2),cz=Math.floor((z+Z)/2),full=i%4===2;
 for(const c of [...b.map.values()]){
  const[a,h,d,s]=c;if(a<x-2||a>X+2||d<z-2||d>Z+2||h<=y+2)continue;
  let brokenRoof=h>Y-1&&(full||(a>cx-3&&d<cz+3)||(a<x+4&&d>cz));
  let brokenWall=(a===x||a===X||d===z||d===Z)&&h>y+2+Math.abs((a*7+d*11+i*3)%5)&&((a+2*d+i)%11<5);
  if(brokenRoof||brokenWall)b.set(a,h,d,A);
 }
 // No tile/finial fragment is left without a neighbour in a collapsed roof patch.
 for(const c of [...b.map.values()]){const[a,h,d,s]=c;if(a<x-2||a>X+2||d<z-2||d>Z+2||h<=Y-1||!s.includes('qingwa'))continue;
  if([[1,0],[-1,0],[0,1],[0,-1]].every(([dx,dz])=>b.get(a+dx,h,d+dz)===A)&&b.get(a,h-1,d)===A)b.set(a,h,d,A);
 }
 for(let k=0;k<10;k++){let a=x+2+(k*7)%(X-x-3),d=z+2+(k*11)%(Z-z-3);if(b.get(a,y+1,d)===A)b.set(a,y+1,d,k%2?'minecraft:stone_brick_slab':q('ruin_collapsed_qingwa'));}
}
finish(b,[110,4,188],'213×185城垣、九组院落、纵横街巷、内殿与独立大演武场；未来Boss空间保留');
}
// 17. Stone hermitage: staggered three-court buildings inside a single enclosure.
{
const b=make('yunling_observatory',81,37,77);b.fill(6,0,6,74,3,70,'minecraft:dirt');b.fill(6,4,6,74,4,70,G);
hall(b,'main','石观正殿',25,4,13,58,29,{h:7});hall(b,'study','西厢藏书房',11,4,36,31,48,{doors:'east'});hall(b,'quiet','后院静修亭',46,4,40,67,52,{open:true,h:5,furniture:false});
b.walls(8,5,8,72,7,68,M);b.clear(37,5,68,41,8,68);
path_(b,[[39,4,72],[39,4,58],[22,4,58],[22,4,49]],1);path_(b,[[39,4,58],[58,4,58],[58,4,53]],1);path_(b,[[39,4,58],[39,4,30]],1);
b.set(42,5,39,q('ruin_star_dial'));chest(b,12,5,37,'core');chest(b,26,5,14);guard(b,55,5,44,['crag_rock_spider','rock_back_boar']);finish(b,[39,5,72],'错位三院石观、前庭星盘、藏书厢房及院内静亭，取消侧路方平台');
}
fs.writeFileSync(O+'/catalog.json',JSON.stringify(all,null,2));
