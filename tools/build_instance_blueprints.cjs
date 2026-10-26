// Six independently authored layouts through construction-skill's VoxelBuilder API.
const fs=require('fs'),path=require('path');
class VoxelBuilder{
 constructor(){this.blocks=new Map();}
 get(x,y,z){return this.blocks.get(`${x},${y},${z}`)?.[3];}
 set(x,y,z,s){this.blocks.set(`${x},${y},${z}`,[x,y,z,s.includes(':')?s:'minecraft:'+s]);}
 fill(x,y,z,X,Y,Z,s){for(let a=x;a<=X;a++)for(let b=y;b<=Y;b++)for(let c=z;c<=Z;c++)this.set(a,b,c,s);}
 walls(x,y,z,X,Y,Z,s){this.fill(x,y,z,X,Y,z,s);this.fill(x,y,Z,X,Y,Z,s);this.fill(x,y,z,x,Y,Z,s);this.fill(X,y,z,X,Y,Z,s);}
 line(x,y,z,X,Y,Z,s){let n=Math.max(Math.abs(X-x),Math.abs(Y-y),Math.abs(Z-z));for(let i=0;i<=n;i++){let t=n?i/n:0;this.set(Math.round(x+(X-x)*t),Math.round(y+(Y-y)*t),Math.round(z+(Z-z)*t),s);}}
}
const specs={
 wood:{title:'苍木书院',concept:'偏心环院、药圃支线与林中藏书楼',style:'cloister',sites:[[0,65,0],[-84,65,64],[12,65,72],[-42,65,30],[54,65,96],[-46,65,108]],edges:[[0,3],[3,1],[1,5],[5,2],[2,4],[4,0],[3,2],[5,4]]},
 fire:{title:'赤岩炼火宫',concept:'放射状铸炉场、环炉廊与双入口窑殿',style:'foundry',sites:[[0,65,0],[-52,65,34],[0,65,52],[50,65,24],[50,65,96],[-60,65,94]],edges:[[0,1],[0,3],[0,2],[1,5],[5,2],[2,4],[4,3],[5,4]]},
 water:{title:'沧澜水榭',concept:'分岔水街、三岛回环桥与临水藏经楼',style:'islands',sites:[[0,65,0],[-78,65,78],[38,65,48],[-40,65,32],[20,65,108],[80,65,96]],edges:[[0,3],[0,2],[3,1],[1,4],[4,5],[5,2],[2,4]]},
 metal:{title:'金阙兵城',concept:'双侧兵营、矩形巡城道与中轴错门兵库',style:'fortress',sites:[[0,65,0],[68,65,36],[0,65,42],[-68,65,36],[0,65,102],[-66,65,102]],edges:[[0,3],[3,5],[5,4],[4,1],[1,0],[0,2],[2,4],[3,2],[2,1]]},
 earth:{title:'玄岩地宫',concept:'下沉墓道、交叉暗室与地上药园的立体环路',style:'crypt',sites:[[0,65,0],[-36,61,34],[36,61,36],[-72,65,84],[0,55,96],[72,57,98]],edges:[[0,1],[0,2],[1,3],[3,4],[4,5],[5,2],[1,4]]},
 thunder:{title:'雷泽观星台',concept:'逐级攀升栈道、岔路雷塔与高处观星殿',style:'terraces',sites:[[0,65,0],[78,69,66],[26,73,42],[-48,69,18],[-8,81,104],[-78,77,100]],edges:[[0,3],[0,2],[3,5],[5,4],[2,1],[1,4],[2,5]]}
};
const ids=['entrance','quarry','battle','herb','core','hidden'];
const colors={wood:['mossy_stone_bricks','oak_planks','stripped_oak_log','mossy_stone_bricks'],fire:['red_sandstone','polished_basalt','polished_blackstone_bricks','cut_red_sandstone'],water:['prismarine_bricks','dark_prismarine','quartz_pillar','prismarine_bricks'],metal:['stone_bricks','polished_andesite','chiseled_stone_bricks','stone_bricks'],earth:['deepslate_bricks','tuff','polished_basalt','deepslate_bricks'],thunder:['deepslate_tiles','polished_deepslate','waxed_cut_copper','chiseled_deepslate']};
const authorDistrict=require('./realm_architecture.cjs');
const layouts={};
for(const [theme,spec] of Object.entries(specs)){
 const b=new VoxelBuilder(),road=colors[theme][0];
 const sites=spec.sites.map(([x,y,z],i)=>({id:ids[i],x:x*2,y,z:z*2,rx:i===4?22:18,rz:i===4?20:17,risk:i===0?0:i===1||i===3?2:3}));
 const footprints=sites.map(s=>[s.x,s.y-1,s.z,s.rx+3,s.rz+3]);
 const routes=[];
 function roadSegment(a,c){
  if(a.x===c.x&&a.z===c.z)return;
  const n=Math.max(Math.abs(a.x-c.x),Math.abs(a.z-c.z));routes.push([a.x,a.y-1,a.z,c.x,c.y-1,c.z]);
  for(let i=0;i<=n;i++){
   const t=i/n,x=Math.round(a.x+(c.x-a.x)*t),z=Math.round(a.z+(c.z-a.z)*t),y=Math.round(a.y+(c.y-a.y)*t)-1;
   b.fill(x-2,y-2,z-2,x+2,y-1,z+2,'stone_bricks');
   b.fill(x-2,y,z-2,x+2,y,z+2,theme==='water'?'spruce_planks':road);
   b.fill(x-2,y+1,z-2,x+2,y+4,z+2,'air');
  }
 }
 // Approach each court on its cardinal doorway. Outside courts, routes may bend diagonally.
 function socket(a,c){
  const dx=c.x-a.x,dz=c.z-a.z;
  return Math.abs(dx)>Math.abs(dz)?{x:a.x+Math.sign(dx)*(a.rx+6),y:a.y,z:a.z}:{x:a.x,y:a.y,z:a.z+Math.sign(dz)*(a.rz+6)};
 }
 for(const [from,to] of spec.edges){const a=sites[from],c=sites[to],A=socket(a,c),C=socket(c,a);roadSegment(a,A);roadSegment(A,C);roadSegment(C,c);}
 // Architecture is after roads: no late diagonal corridor may slice roofs or facades.
 const rewards={},oreCells=[],herbCells=[];
 for(const s of sites){
  b.fill(s.x-s.rx,s.y-3,s.z-s.rz,s.x+s.rx,s.y-2,s.z+s.rz,'stone_bricks');
  b.fill(s.x-s.rx-2,s.y,s.z-s.rz-2,s.x+s.rx+2,s.y+12,s.z+s.rz+2,'air');
  const authored=authorDistrict(b,s,theme,s.id);oreCells.push(...authored.ores);herbCells.push(...authored.herbs);
  if(s.id!=='entrance')rewards[s.id]=authored.reward;
 }
 b.set(0,65,-6,'xiantu:instance_core');
 layouts[theme]={title:spec.title,concept:spec.concept,style:spec.style,architecture_revision:2,blocks:[...b.blocks.values()],zones:sites,routes,footprints,ore_cells:oreCells.filter(p=>b.get(...p)==='minecraft:stone'),herb_cells:herbCells.filter(([x,y,z])=>b.get(x,y-1,z)==='minecraft:grass_block'&&b.get(x,y,z)==='minecraft:air'),rewards};
}
const out=path.resolve(__dirname,'../src/main/resources/data/xiantu/instance');fs.mkdirSync(out,{recursive:true});fs.writeFileSync(path.join(out,'realm_courts.json'),JSON.stringify({schema_version:3,id:'xiantu:realm_courts',enabled:true,source:'construction-skill/VoxelBuilder',layouts}));
console.log(Object.entries(layouts).map(([k,v])=>`${k}: ${v.blocks.length} blocks; ${v.routes.length} approach segments; ${v.style}`).join('\n'));
