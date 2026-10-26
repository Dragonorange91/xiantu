import fs from 'node:fs';
import {VoxelBuilder} from './ruin_voxel_builder.mjs';
const out='asset_sources/qunyou-figurine-20261026';
const entries=[{template:'cultivation_town',pos:[32,3,26],facing:'south'}, {template:'wanbao_pavilion',pos:[20,3,20],facing:'north'},{template:'tiles/cell_02_0_01',pos:[0,3,10],facing:'south'}];
for(const e of entries){const b=new VoxelBuilder(e.template,145,40,133);b.set(...e.pos,'xiantu:qunyou_figurine[facing='+e.facing+']');e.blocks=[...b.map.values()];}
fs.writeFileSync(out+'/town-placement.json',JSON.stringify(entries,null,2));console.log('Authored one tabletop figure in the town and its standalone/tile exports');
