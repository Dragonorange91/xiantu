// Functional themed rooms and landmarks. All placements use the supplied VoxelBuilder.
// Keep the five-block approach streets open and leave actual combat manoeuvring space.
module.exports=function(b,s,theme,role){
 const {x,y,z,rx,rz}=s,put=(a,h,c,v)=>b.set(x+a,y+h,z+c,v),
 fill=(a,h,c,A,H,C,v)=>b.fill(x+a,y+h,z+c,x+A,y+H,z+C,v);
 const wood=theme==='wood'||theme==='water',floor=wood?'spruce_planks':theme==='fire'?'polished_blackstone_bricks':theme==='thunder'?'polished_deepslate':'stone_bricks';
 const trim=wood?'stripped_dark_oak_log':theme==='fire'?'polished_basalt':'polished_andesite';
 const wall=wood?'white_terracotta':theme==='earth'?'deepslate_bricks':theme==='water'?'prismarine_bricks':'stone_bricks';
 const index=['entrance','quarry','battle','herb','core','hidden'].indexOf(role);
 const sign=(index+(theme==='fire'||theme==='water'?1:0))%2===0?-1:1;
 // Every reward belongs to a distinct usable room instead of the same right-side cubicle.
 const a=sign*(rx-7),c=rz-7,R=4,T=4;
 if(role!=='herb'&&role!=='quarry'){
  fill(a-R,-1,c-T,a+R,-1,c+T,floor);
  fill(a-R,0,c-T,a+R,5,c+T,'air');
  b.walls(x+a-R,y,z+c-T,x+a+R,y+4,z+c+T,wall);
  for(const A of [a-R,a+R])for(const C of [c-T,c+T])fill(A,0,C,A,5,C,trim);
  for(const H of [4,5])fill(a-R,H,c-T,a+R,H,c+T,H===4?trim:floor);
  for(const C of [c-T,c+T])fill(a-1,2,C,a+1,3,C,wood?'xiantu:town_walnut_lattice[facing=north]':'iron_bars');
  // Side door faces the central north/south street, always reached along a covered aisle.
  const door=a-sign*R;fill(door,0,c-1,door,2,c+1,'air');
  fill(Math.min(0,door),-1,c-1,Math.max(0,door),-1,c+1,floor);
  fill(Math.min(0,door),0,c-1,Math.max(0,door),2,c+1,'air');
  put(a,3,c,'lantern[hanging=true]');
  if(theme==='wood'){
   // Scriptorium: shelves, reading table, ink stand and a bed alcove for the curator.
   for(const A of [a-3,a+3])fill(A,0,c-2,A,2,c+2,'bookshelf');
   for(const A of [a-1,a,a+1])put(A,0,c-2,'spruce_slab[type=top]');
   put(a,1,c-2,'lectern[facing=south]');put(a-1,1,c-2,'flower_pot');
   put(a,0,c-1,'spruce_stairs[facing=north]');
   put(a+2,0,c+2,'barrel[facing=up]');put(a+2,1,c+2,'potted_fern');
  }else if(theme==='fire'){
   // Assay workshop with sealed flues and a cooling trough.
   for(const A of [a-2,a,a+2]){put(A,0,c+3,'blast_furnace[facing=north]');fill(A,1,c+3,A,3,c+3,'polished_blackstone');}
   put(a-2,0,c-2,'smithing_table');put(a-1,0,c-2,'anvil');put(a+2,0,c-2,'water_cauldron[level=3]');
   put(a-3,0,c,'barrel[facing=up]');put(a-3,1,c,'grindstone[face=floor,facing=north]');
  }else if(theme==='water'){
   // Tide archive: specimen tank is sealed on every face, scroll desk and navigational table.
   fill(a-3,0,c+1,a-1,2,c+3,'cyan_stained_glass');put(a-2,1,c+2,'water');put(a-2,0,c+2,'sea_lantern');
   for(const A of [a+2,a+3])fill(A,0,c-2,A,2,c+2,'bookshelf');
   put(a-1,0,c-2,'cartography_table');put(a+1,0,c-2,'lectern[facing=south]');put(a,0,c-1,'spruce_stairs[facing=north]');
  }else if(theme==='metal'){
   // Quartermaster: partitioned weapon racks, forging bench and banner storage.
   for(const A of [a-3,a+3])for(const C of [c-2,c,c+2]){put(A,0,C,'barrel[facing=north]');put(A,1,C,'grindstone[face=floor,facing=north]');}
   put(a-1,0,c+3,'smithing_table');put(a+1,0,c+3,'anvil');put(a,2,c+3,'gray_wall_banner[facing=north]');
   for(const A of [a-2,a+2])put(A,0,c-2,'spruce_stairs[facing=south]');
  }else if(theme==='earth'){
   // Offering chamber: raised funerary bed, urn niches and an ancestral stele.
   fill(a-2,0,c+1,a+2,0,c+3,'polished_deepslate');fill(a-1,1,c+1,a+1,1,c+3,'deepslate_tile_slab');
   put(a,2,c+2,'skeleton_skull');for(const A of [a-3,a+3]){put(A,0,c-2,'decorated_pot');put(A,1,c+2,'soul_lantern');}
   put(a,0,c-2,'xiantu:realm_incense_reliquary');
  }else{
   // Weather station: instrument console, copper cage and independent amethyst resonators.
   for(const A of [a-3,a+3]){fill(A,0,c+2,A,3,c+2,'waxed_cut_copper');put(A,4,c+2,'lightning_rod');}
   put(a-2,0,c-2,'cartography_table');put(a,0,c-2,'lectern[facing=south]');put(a+2,0,c-2,'lodestone');
   for(const A of [a-2,a+2]){put(A,0,c,'calcite');put(A,1,c,'amethyst_cluster[facing=up]');}
  }
  fill(Math.min(door,a),0,c,Math.max(door,a),2,c,'air');
 }
 // Large central buildings acquire a real upper gallery and a stair, with supported beams.
 if(role==='core'&&(theme==='wood'||theme==='metal')){
  const A=-rx+6,C=-rz+7;
  fill(A-3,-1,C-3,A+3,-1,C+3,floor);
  for(const dx of [-3,3])for(const dz of [-3,3])fill(A+dx,0,C+dz,A+dx,9,C+dz,trim);
  fill(A-3,5,C-3,A+3,5,C+3,floor);fill(A-4,10,C-4,A+4,10,C+4,'dark_oak_slab[type=bottom]');
  for(let i=0;i<6;i++){put(A-2+i, i-1, C-4,'spruce_stairs[facing=east]');fill(A-2+i,-2,C-4,A-2+i,i-2,C-4,trim);}
  for(const D of [-3,3])for(let O=-3;O<=3;O++){put(A+D,6,C+O,'dark_oak_fence');put(A+O,6,C+D,'dark_oak_fence');}
  put(A+3,6,C-3,'air');put(A,6,C,'lectern[facing=south]');
  if(theme==='wood')for(const D of [-2,2])fill(A+D,6,C+2,A+D,8,C+2,'bookshelf');
 }
 // Resource zones keep their existing protected soil/ore beds. Their rewards sit
 // inside a modest sheltered supply bay on the near perimeter, not on top of beds.
 let reward;
 if(role==='herb'||role==='quarry'){
  const C=-rz+3,A=sign*6;reward=[x+A,y,z+C];
  fill(A-2,-1,C-1,A+2,-1,C+2,floor);fill(A-2,0,C-1,A+2,3,C+2,'air');
  for(const D of [-2,2])fill(A+D,0,C+2,A+D,3,C+2,trim);
  fill(A-2,4,C-1,A+2,4,C+2,'spruce_slab[type=bottom]');
  put(A-1,0,C+2,role==='herb'?'composter':'stonecutter[facing=north]');put(A+1,0,C+2,'barrel[facing=north]');
  fill(Math.min(A,0),-1,C,Math.max(A,0),-1,C,floor);fill(Math.min(A,0),0,C,Math.max(A,0),2,C,'air');
 }else reward=[x+a,y,z+c];
 // Open chest lid and one accessible tile in front; no hidden wall deletion.
 if(role!=='entrance'){b.set(...reward,'air');b.set(reward[0],reward[1]+1,reward[2],'air');}
 return reward;
};
