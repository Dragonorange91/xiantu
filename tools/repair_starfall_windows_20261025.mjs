import fs from 'node:fs';
import {VoxelBuilder} from './ruin_voxel_builder.mjs';
const p='asset_sources/ruin-interiors-20261025/structures/starfall_sanctuary_grand.json',out='asset_sources/starfall-windows-20261025';
fs.mkdirSync(out,{recursive:true});const d=JSON.parse(fs.readFileSync(p,'utf8'));
if(d.windowRepair)throw Error('Already repaired; inspect existing report');
const b=new VoxelBuilder(d.id,...d.size);for(const c of d.blocks)b.set(...c);
const windows=d.blocks.filter(c=>c[3].startsWith('xiantu:town_walnut_lattice'));const changes=[];
for(const [x,y,z,state]of windows)for(let dx=-1;dx<=1;dx++)for(let dy=0;dy<3;dy++){
 const pos=[x+dx,y+dy,z],old=b.get(...pos);if(old===state)continue;
 if(!['minecraft:air','xiantu:cloud_floating_stone','xiantu:cloud_floating_stone_mossy','xiantu:cloud_floating_stone_cracked'].includes(old))throw Error('Unexpected window cell '+pos+': '+old);
 b.set(...pos,state);changes.push({position:pos,before:old,after:state});
}
for(const [x,y,z,state]of windows)for(let dx=-1;dx<=1;dx++)for(let dy=0;dy<3;dy++)if(b.get(x+dx,y+dy,z)!==state)throw Error('Incomplete window');
d.blocks=[...b.map.values()];d.windowRepair={date:'2026-10-25',windows:windows.map(c=>c.slice(0,3)),size:[3,3],preservedFacing:true};
fs.writeFileSync(p,JSON.stringify(d));fs.writeFileSync(out+'/changes.json',JSON.stringify({windows:windows.length,changes},null,2));console.log('Completed '+windows.length+' nine-block windows; '+changes.length+' cells changed');
