// Construction-skill components. Every placement is made through VoxelBuilder.
// A district has four deliberate street mouths, not a road carved through its facade.
module.exports=function authorDistrict(b,s,theme,role){
 const {x,y,z,rx,rz}=s;
 const put=(a,h,c,state)=>b.set(x+a,y+h,z+c,state);
 const fill=(a,h,c,A,H,C,state)=>b.fill(x+a,y+h,z+c,x+A,y+H,z+C,state);
 const line=(a,h,c,A,H,C,state)=>b.line(x+a,y+h,z+c,x+A,y+H,z+C,state);
 const tile='xiantu:town_qingwa_', red='xiantu:town_vermilion_';
 const palettes={wood:['oak_planks','stripped_spruce_log','white_terracotta'],fire:['polished_blackstone_bricks','polished_basalt','red_nether_bricks'],water:['spruce_planks','stripped_dark_oak_log','white_terracotta'],metal:['stone_bricks','polished_andesite','stone_bricks'],earth:['deepslate_tiles','polished_basalt','deepslate_bricks'],thunder:['polished_deepslate','waxed_cut_copper','chiseled_deepslate']};
 const [floor,timber,wall]=palettes[theme];
 const core=role==='core',garden=role==='herb',mine=role==='quarry';
 function lamp(a,h,c,hanging=false){put(a,h,c,hanging?'lantern[hanging=true]':'xiantu:realm_spirit_lantern');}
 function post(a,c,h=5,style='wood',base=0){
  put(a,base,c,style==='red'?red+'base':'xiantu:realm_carved_pillar');
  fill(a,base+1,c,a,base+h-1,c,style==='red'?red+'column':timber);
  put(a,base+h,c,style==='red'?red+'capital':'xiantu:town_dougong');
 }
 function rail(a,c,A,C,h=0){line(a,h,c,A,h,C,red+'railing[facing='+(a===A?'east':'north')+']');}
 function beam(a,c,A,C,h){line(a,h,c,A,h,C,timber+(timber.includes('log')?'[axis='+(a===A?'z':'x')+']':''));}
 // Four-pitched roof assembled from the existing partial tile models.
 // No hand-authored triangular gables. Pitched rectangle gables use drawRoofBounds.
 function hip(a,h,c,R,T,roofTheme='tile'){
  const levels=Math.min(R,T);
  for(let d=0;d<=levels;d++){
   const lo=a-R+d,hi=a+R-d,front=c-T+d,back=c+T-d;
   const H=h+Math.floor(d/2);
   for(let A=lo;A<=hi;A++)for(const C of [front,back]){
    const face=C===front?'north':'south';
    put(A,H,C,roofTheme==='tile'?tile+(d%2?'high':'low')+'[facing='+face+']':roofTheme+'_stairs[facing='+(face==='north'?'south':'north')+']');
   }
   for(let C=front+1;C<back;C++)for(const A of [lo,hi]){
    const face=A===lo?'west':'east';
    put(A,H,C,roofTheme==='tile'?tile+(d%2?'high':'low')+'[facing='+face+']':roofTheme+'_stairs[facing='+(face==='west'?'east':'west')+']');
   }
   if(roofTheme==='tile')for(const A of [lo,hi])for(const C of [front,back])put(A,H,C,tile+'hip_'+(d%2?'high':'low')+'[facing='+(A===lo?(C===front?'north':'west'):(C===front?'east':'south'))+']');
  }
  const H=h+Math.ceil(levels/2);
  line(a-R+levels,H,c-T+levels,a+R-levels,H,c+T-levels,roofTheme==='tile'?tile+'ridge[facing='+(R>=T?'north':'east')+']':roofTheme+'_slab');
  if(roofTheme==='tile')for(const [A,C] of [[a-R+levels,c-T+levels],[a+R-levels,c+T-levels]])put(A,H+1,C,tile+'finial[facing=north]');
 }
 function underEaves(a,c,R,T,h){
  for(let A=a-R;A<=a+R;A+=3)for(const C of [c-T,c+T]){
   put(A,h,C,'xiantu:town_dougong[facing='+(C<c?'north':'south')+']');
   put(A,h+1,C,timber+(timber.includes('log')?'[axis=x]':''));
  }
  for(let C=c-T;C<=c+T;C+=3)for(const A of [a-R,a+R])put(A,h,C,'xiantu:town_dougong[facing='+(A<a?'west':'east')+']');
 }
 function hall(a,c,R,T,h=5,{open=false,books=false,roof='tile',redPosts=false}={}){
  fill(a-R,-1,c-T,a+R,-1,c+T,floor);
  if(!open){
   b.walls(x+a-R,y,z+c-T,x+a+R,y+h-1,z+c+T,wall);
   for(let A=a-R+2;A<a+R;A+=3)for(const C of [c-T,c+T])fill(A,2,C,A+1,3,C,'xiantu:town_walnut_lattice[facing=north]');
   for(let C=c-T+2;C<c+T;C+=3)for(const A of [a-R,a+R])fill(A,2,C,A,3,C+1,'xiantu:town_walnut_lattice[facing=east]');
  }
  for(const A of [a-R,a+R])for(const C of [c-T,c+T])post(A,C,h,redPosts?'red':'wood');
  for(const C of [c-T,c+T])beam(a-R,C,a+R,C,h);
  for(const A of [a-R,a+R])beam(A,c-T,A,c+T,h);
  underEaves(a,c,R,T,h);
  // Doorways line up with the two district streets, with framed side openings.
  for(const C of [c-T,c+T])fill(a-2,0,C,a+2,3,C,'air');
  for(const A of [a-R,a+R])fill(A,0,c-2,A,3,c+2,'air');
  hip(a,h+1,c,R+2,T+2,roof);
  if(!open){
   for(const C of [c-T,c+T])line(a-R+1,1,C,a+R-1,1,C,'spruce_trapdoor[facing='+(C<c?'north':'south')+',open=true]');
   for(const C of [c-T,c+T])fill(a-2,0,C,a+2,3,C,'air');
   for(const A of [a-R,a+R])for(const C of [c-T+2,c+T-2])put(A,4,C,'xiantu:town_carved_panel');
  }
  for(const A of [a-R+1,a+R-1])for(const C of [c-T+1,c+T-1])lamp(A,h-1,C,true);
  if(books)for(const A of [a-R+1,a+R-1])for(const C of [c-T+2,c+T-2])fill(A,0,C,A,2,C,'bookshelf');
 }
 function desk(a,c){put(a,0,c,'spruce_stairs[facing=south]');put(a,0,c+1,'spruce_slab[type=top]');put(a,1,c+1,'flower_pot');}
 function porchGate(a,c){
  for(const A of [a-4,a+4])post(A,c,5,'red');
  beam(a-4,c,a+4,c,5);hip(a,6,c,6,2);
  for(const A of [a-3,a+3])lamp(A,4,c,true);
 }
 function round(a,c,R,H,mat,base=-1){
  for(let A=-R;A<=R;A++)for(let C=-R;C<=R;C++)if(A*A+C*C<=R*R)fill(a+A,base,c+C,a+A,base+H,c+C,mat);
 }
 function cabinet(a,c){fill(a,0,c,a+2,1,c,'barrel[facing=north]');put(a,2,c,'decorated_pot');put(a+2,2,c,'lantern');}
 function bench(a,c,face='north'){
  for(let A=a-1;A<=a+1;A++)put(A,0,c,'spruce_stairs[facing='+face+']');
 }
 function planter(a,c){
  fill(a-1,0,c-1,a+1,0,c+1,'mossy_stone_brick_slab');put(a,0,c,'moss_block');put(a,1,c,'flowering_azalea');
 }
 function courtDetails(){
  // Small paving courses and a restrained border leave usable combat space.
  for(let A=-rx+1;A<rx;A++)for(let C=-rz+1;C<rz;C++){
   if(theme==='water')continue;
   if(Math.abs(A)<=2||Math.abs(C)<=2)put(A,-1,C,theme==='wood'?'stone_bricks':theme==='fire'?'polished_basalt':floor);
   else if((Math.abs(A)===rx-3||Math.abs(C)===rz-3)&&(A+C)%2===0)put(A,-1,C,'chiseled_stone_bricks');
   else if(theme==='wood'&&(A+C)%7===0)put(A,-1,C,'spruce_planks');
   else if(theme==='fire'&&(A+C)%7===0)put(A,-1,C,'cracked_polished_blackstone_bricks');
  }
 }
 function ring(a,h,c,R,mat,plane='xz'){
  for(let i=0;i<160;i++){const t=i/160*Math.PI*2,A=Math.round(Math.cos(t)*R),C=Math.round(Math.sin(t)*R);put(a+A,plane==='xy'?h+C:h,c+(plane==='xy'?0:C),mat);}
 }
 // Articulate the foundation and expose its edge rather than a single solid plate.
 for(let A=-rx;A<=rx;A++)for(let C=-rz;C<=rz;C++){
  const edge=Math.abs(A)===rx||Math.abs(C)===rz;
  put(A,-1,C,edge?'stone_bricks':floor);
  if(edge&&Math.abs(A)>3&&Math.abs(C)>3&&((A+C)%4===0))put(A,-2,C,'chiseled_stone_bricks');
 }
 if(theme==='wood'){
  // Asymmetric academy: wide rear lecture hall, side archive, open tea veranda.
  hall(0,rz-7,core?11:8,4,core?7:5,{books:true,redPosts:true});
  hall(-rx+5,-2,3,7,4,{books:!garden});
  if(!mine)hall(rx-5,-5,3,4,4,{open:true});
  porchGate(0,-rz+3);
  for(const A of [-rx+5,rx-5])for(const C of [3,8]){if(A>0&&C>4)continue;desk(A,C);}
  for(const A of [-7,7])for(const C of [-7,-4]){put(A,0,C,'moss_block');put(A,1,C,'flowering_azalea');}
  for(let A=-7;A<=7;A+=7){put(A,0,rz-7,'lectern[facing=north]');put(A,0,rz-9,'green_carpet');}
  cabinet(-rx+3,rz-9);
  // Low garden walls, linked verandas and study furniture make the court inhabited.
  for(const C of [-rz+1,rz-1])for(let A=-rx+2;A<=rx-2;A++)if(Math.abs(A)>4)put(A,0,C,'mossy_stone_brick_wall');
  for(const A of [-rx+1,rx-1])for(let C=-rz+2;C<=rz-2;C++)if(Math.abs(C)>3)put(A,0,C,'mossy_stone_brick_wall');
  for(const A of [-11,11]){bench(A,-10,'south');planter(A,-6);}
  for(const A of [-7,7]){put(A,0,rz-8,'lectern[facing=north]');put(A,0,rz-6,'bookshelf');put(A+1,0,rz-6,'bookshelf');}
  if(role==='hidden')for(const A of [-6,6])for(let C=3;C<rz-5;C+=3)fill(A,0,C,A,2,C+1,'bookshelf');
  if(role==='battle')for(const A of [-8,8])for(const C of [-5,5])put(A,0,C,'green_carpet');
 }else if(theme==='fire'){
  // Separate kiln wings and a high ventilated foundry; never an open-topped box.
  hall(0,rz-7,core?11:8,4,7,{open:true,roof:'polished_blackstone'});
  for(const A of [-rx+5,rx-5]){
   const C=-7;
   round(A,C,3,4,'red_nether_bricks',0);
   fill(A-1,0,C-3,A+1,2,C-2,'air');put(A,0,C,'campfire[lit=true]');
   hip(A,5,C,4,4,'polished_blackstone');
   fill(A-1,7,C-1,A+1,10,C+1,'polished_basalt');
   b.walls(x+A-1,y+11,z+C-1,x+A+1,y+12,z+C+1,'polished_blackstone_bricks');
   put(A,11,C,'campfire[lit=true]');
   for(const D of [-2,2]){put(A+D,0,C+4,'blast_furnace[facing=south]');put(A+D,1,C+4,'stone_pressure_plate');}
   put(A,0,C+5,'anvil');
  }
  // Grated fire channels are sealed, and never cross the main walking lanes.
  for(const A of [-7,7])for(let C=-rz+4;C<rz-4;C++){put(A,-2,C,'magma_block');put(A,-1,C,'orange_stained_glass');}
  for(const A of [-9,9])for(const C of [rz-9,rz-5]){put(A,0,C,'anvil');put(A+1,0,C,'grindstone[face=floor,facing=south]');}
  for(const A of [-5,5]){fill(A,8,rz-9,A,10,rz-9,'chain');lamp(A,7,rz-9,true);}
  porchGate(0,-rz+3);cabinet(-rx+3,2);
  for(const A of [-rx+2,rx-2])for(let C=-rz+4;C<rz-2;C++)if(Math.abs(C)>3)put(A,0,C,'polished_blackstone_brick_wall');
  for(const A of [-rx+5,rx-5]){
   for(const D of [-3,3]){put(A+D,1,-7,'polished_blackstone_brick_stairs[facing='+(D<0?'east':'west')+']');put(A+D,3,-7,'iron_bars');}
   for(const C of [-9,-5])fill(A,0,C,A,3,C,'polished_basalt');
   put(A,0,-10,'blast_furnace[facing=north,lit=true]');
  }
  for(const A of [-6,6]){fill(A,0,rz-6,A,2,rz-6,'barrel[facing=north]');put(A,3,rz-6,'iron_bars');}
  if(role==='battle')for(const A of [-7,7]){put(A,0,4,'anvil');put(A,0,7,'smithing_table');}
 }else if(theme==='water'){
  // Actual water courts, low bridges, and a permeable double-eaved pavilion.
  for(let A=-rx+1;A<rx;A++)for(let C=-rz+1;C<rz;C++)if(Math.abs(A)>3&&Math.abs(C)>3){put(A,-2,C,'prismarine_bricks');put(A,-1,C,'water');}
  fill(-8,-1,-8,8,-1,8,'spruce_planks');
  hall(0,0,7,7,6,{open:true,redPosts:true});
  if(core){for(const A of [-4,4])for(const C of [-4,4])post(A,C,3,'red',10);hip(0,14,0,6,6);}
  hall(-rx+6,rz-7,4,4,4,{open:role!=='hidden',books:role==='hidden'});
  for(const A of [-rx+6,rx-6])fill(A-2,-1,-rz+5,A+2,-1,rz-4,'spruce_planks');
  for(let A=-rx+2;A<=rx-2;A++)if(Math.abs(A)>8)for(const C of [-2,2])rail(A,C,A,C);
  for(const A of [-7,7])for(const C of [-7,7]){rail(A,C,A,C);lamp(A,5,C,true);}
  for(const A of [-rx+3,rx-3])for(const C of [-rz+6,rz-6])if(b.get(x+A,y-1,z+C)==='minecraft:water'){
   put(A,-2,C,'sea_lantern');put(A,0,C,'lily_pad');
  }
  for(const A of [-5,5]){put(A,0,-5,'spruce_stairs[facing=south]');put(A,0,-4,'spruce_slab[type=top]');put(A,1,-4,'potted_blue_orchid');}
  underEaves(0,0,7,7,6);
  // Lotus basins and two sheltered side walks, with railings beside real water.
  for(const A of [-rx+6,rx-6]){
   for(const C of [-rz+6,-2,rz-5])post(A,C,4,'red');
   hip(A,5,0,2,rz-3);
   for(let C=-rz+3;C<rz-2;C++)if(Math.abs(C)>3){rail(A-2,C,A-2,C);rail(A+2,C,A+2,C);}
  }
  for(const A of [-rx+3,rx-3])for(const C of [-10,-5,5,10])if(b.get(x+A,y-1,z+C)==='minecraft:water')put(A,0,C,'lily_pad');
  if(role==='hidden'){cabinet(-5,-5);put(4,0,5,'lectern[facing=west]');}
 }else if(theme==='metal'){
  // A low fortress enclosing a drill court, with four distinct corner bastions.
  const R=rx-2,T=rz-2;
  b.walls(x-R,y,z-T,x+R,y+4,z+T,'stone_bricks');
  for(let A=-R;A<=R;A++)for(const C of [-T,T]){put(A,5,C,'stone_brick_slab');if(A%3!==0)put(A,6,C,'stone_brick_wall');}
  for(let C=-T;C<=T;C++)for(const A of [-R,R]){put(A,5,C,'stone_brick_slab');if(C%3!==0)put(A,6,C,'stone_brick_wall');}
  for(const A of [-R+3,R-3])for(const C of [-T+3,T-3]){
   b.walls(x+A-2,y,z+C-2,x+A+2,y+8,z+C+2,'stone_bricks');
   fill(A-2,8,C-2,A+2,8,C+2,'polished_andesite');
   for(const D of [-2,2])for(let O=-2;O<=2;O+=2){put(A+D,9,C+O,'stone_brick_wall');put(A+O,9,C+D,'stone_brick_wall');}
   fill(A,2,C-2,A,4,C-2,'iron_bars');lamp(A,9,C);
  }
  for(const C of [-T,T]){fill(-3,0,C,3,4,C,'air');for(const A of [-4,4])fill(A,0,C,A,6,C,'chiseled_stone_bricks');line(-4,6,C,4,6,C,'polished_andesite');}
  for(const A of [-R,R])fill(A,0,-3,A,4,3,'air');
  hall(0,rz-8,9,4,5,{roof:'polished_blackstone'});
  for(const A of [-rx+5,rx-5])for(const C of [-8,-5,5]){
   put(A,0,C,'smooth_stone_slab');fill(A,1,C,A,3,C,'oak_fence');put(A,4,C,'hay_block');
   put(A+1,0,C,'anvil');
  }
  for(let A=-9;A<=9;A++)for(let C=-9;C<=9;C++)if(Math.abs(A)===9||Math.abs(C)===9)put(A,-1,C,'polished_andesite');
  // Vertical buttresses and loopholes break up long wall planes.
  for(let A=-R+3;A<R;A+=5)if(Math.abs(A)>5)for(const C of [-T,T]){
   fill(A,0,C,A,4,C,'chiseled_stone_bricks');put(A,5,C,'stone_brick_slab');
   fill(A+1,2,C,A+1,3,C,'iron_bars');
   put(A,0,C+(C<0?-1:1),'stone_brick_stairs[facing='+(C<0?'south':'north')+']');
  }
  for(let C=-T+3;C<T;C+=5)if(Math.abs(C)>5)for(const A of [-R,R]){
   fill(A,0,C,A,4,C,'chiseled_stone_bricks');fill(A,2,C+1,A,3,C+1,'iron_bars');
  }
  for(const A of [-R+3,R-3])for(const C of [-T+3,T-3]){
   b.walls(x+A-2,y+9,z+C-2,x+A+2,y+9,z+C+2,'stone_brick_wall');
   for(const D of [-2,2])for(const O of [-2,0,2]){put(A+D,10,C+O,'chiseled_stone_bricks');put(A+O,10,C+D,'chiseled_stone_bricks');}
  }
  if(role==='hidden')for(const A of [-7,7])cabinet(A,7);
 }else if(theme==='earth'){
  // Tall barrel vault, repeated structural ribs, burial niches and an offset sanctuary.
  const R=rx-3,T=rz-3;
  for(let A=-R;A<=R;A++){
   const H=5+Math.round(7*Math.sqrt(Math.max(0,1-(A/R)**2)));
   // The smallest vault jumps three blocks at its springing. Three-block shell
   // thickness joins adjacent courses and prevents a slit above the side wall.
   fill(A,H-1,-T,A,H+1,T,'deepslate_bricks');
   for(const C of [-T,T])fill(A,0,C,A,H,C,'deepslate_bricks');
  }
  for(const A of [-R,R])fill(A,0,-T,A,6,T,'polished_basalt');
  for(let C=-T+2;C<T;C+=6){
   for(let A=-R;A<=R;A++){
    const H=4+Math.round(7*Math.sqrt(Math.max(0,1-(A/R)**2)));
    put(A,H,C,'chiseled_deepslate');
   }
   for(const A of [-R+1,R-1]){fill(A,0,C,A,6,C,'chiseled_deepslate');lamp(A,4,C-1);}
  }
  for(const C of [-T,T]){fill(-3,0,C,3,5,C,'air');for(const A of [-4,4])fill(A,0,C-1,A,6,C+1,'chiseled_deepslate');line(-3,6,C,3,6,C,'deepslate_tile_slab');}
  for(const A of [-R,R])fill(A,0,-3,A,4,3,'air');
  for(const A of [-R+3,R-3])for(const C of [-10,-5,7,12]){
   fill(A,0,C-1,A+1,0,C+1,'polished_deepslate');
   fill(A,1,C-1,A+1,1,C+1,'deepslate_tile_slab');put(A,2,C,'skeleton_skull');
   put(A+2,0,C,'xiantu:realm_incense_reliquary');
  }
  for(const A of [-6,6])for(const C of [-7,7]){fill(A,8,C,A,10,C,'chain');lamp(A,7,C,true);}
  for(let C=-T+1;C<T;C++)put(0,-1,C,'chiseled_deepslate');
  for(let C=-T+2;C<T;C+=6)for(const A of [-R,R]){
   const D=A<0?-1:1;fill(A+D,0,C,A+D,5,C,'chiseled_deepslate');
   put(A+D,6,C,'deepslate_brick_stairs[facing='+(D<0?'east':'west')+']');
  }
  for(const C of [-T,T]){
   line(-R,6,C,R,6,C,'polished_basalt[axis=x]');
   for(const A of [-8,8]){fill(A,2,C,A,4,C,'chiseled_deepslate');put(A,5,C,'soul_lantern');}
  }
  if(core){fill(-5,0,T-3,5,1,T-1,'polished_deepslate');for(const A of [-4,4])put(A,2,T-2,'xiantu:realm_incense_reliquary');}
 }else{
  // Open observatory: copper-inlaid stellar court, armillary and lightning pylons.
  ring(0,-1,0,11,'waxed_cut_copper');ring(0,-1,0,7,'chiseled_deepslate');
  for(const A of [-rx+5,rx-5])for(const C of [-rz+5,rz-5]){
   round(A,C,2,1,'polished_deepslate',0);
   fill(A,2,C,A,8,C,'waxed_cut_copper');
   for(const D of [-1,1]){put(A+D,7,C,'waxed_cut_copper_slab');put(A,7,C+D,'waxed_cut_copper_slab');}
   put(A,9,C,'sea_lantern');put(A,10,C,'waxed_copper_block');put(A,11,C,'lightning_rod');
  }
  // Sculptural instrument is behind the central assembly area; the street remains open.
  const C=rz-9;
  for(const A of [-7,7]){fill(A,0,C,A,7,C,'polished_deepslate');put(A,8,C,'waxed_copper_block');}
  ring(0,10,C,6,'waxed_cut_copper','xy');ring(0,10,C,4,'waxed_oxidized_cut_copper','xy');
  line(-7,8,C,7,8,C,'waxed_cut_copper');line(-6,10,C,6,10,C,'iron_bars');put(0,10,C,'sea_lantern');
  hall(-rx+7,-rz+7,4,4,5,{open:true,roof:'waxed_cut_copper'});
  for(const A of [-9,9]){put(A,0,-7,'lectern[facing=south]');put(A,0,-5,'polished_deepslate_slab');}
  for(const A of [-rx+1,rx-1])for(let D=-rz+2;D<rz-1;D++)if(Math.abs(D)>3)put(A,0,D,'polished_deepslate_wall');
  for(const A of [-11,11])for(const D of [-8,8]){put(A,0,D,'chiseled_deepslate');put(A,1,D,'amethyst_cluster[facing=up]');}
  for(const A of [-rx+1,rx-1])for(const C of [-rz+1,rz-1]){fill(A,0,C,A,2,C,'polished_deepslate');put(A,3,C,'sea_lantern');put(A,4,C,'waxed_cut_copper_slab');}
  // Inlaid diagonals are broken at the four walking approaches intentionally.
  for(let D=-9;D<=9;D++)if(Math.abs(D)>2){put(D,-1,D,'waxed_oxidized_cut_copper');put(D,-1,-D,'waxed_oxidized_cut_copper');}
  if(role==='hidden')for(const A of [-6,6]){put(A,0,rz-5,'enchanting_table');put(A,0,rz-3,'bookshelf');}
 }
 // Resource districts have purpose-built workings rather than deposits overwriting rooms.
 const ores=[],herbs=[];
 if(mine){
  for(const A of [-10,-9,9,10])for(let C=-10;C<=10;C++)if(Math.abs(C)>2)for(let H=0;H<=2;H++){
   put(A,H,C,'stone');ores.push([x+A,y+H,z+C]);
  }
  for(const A of [-13,13])for(const C of [-10,10]){post(A,C,4);lamp(A,3,C,true);}
  for(const A of [-12,12])for(let C=-8;C<=8;C++)put(A,0,C,'rail[shape=north_south]');
 }
 if(garden){
  for(const A of [-10,-6,6,10])for(let C=-9;C<=9;C++)if(Math.abs(C)>2)for(let D=-1;D<=1;D++){
   put(A+D,-2,C,'dirt');put(A+D,-1,C,'grass_block');fill(A+D,0,C,A+D,2,C,'air');herbs.push([x+A+D,y,z+C]);
  }
  for(const A of [-13,13])for(const C of [-9,9]){put(A,0,C,'composter');lamp(A,1,C);}
 }
 // Both public streets stay at ground level. Openings were framed during construction;
 // only misplaced low objects are cleared here, never a facade or a roof.
 for(let A=-rx;A<=rx;A++)for(let C=-rz;C<=rz;C++)if(Math.abs(A)<=2||Math.abs(C)<=2){
  put(A,-1,C,floor);fill(A,0,C,A,3,C,'air');
 }
 courtDetails();
 const reward=require('./realm_theme_interiors.cjs')(b,s,theme,role);
 return {ores,herbs,reward};
};
