"""Complete smooth/carved step forms using accepted textures and vanilla templates."""
import json
from generate_building_material_resources import ROOT,ASSETS,DATA,FAMILIES,write_json,textured_model,variant_blockstate,slab_blockstate,block_loot,merge_tag,recipe

def main():
    langs={l:json.loads((ASSETS/f'lang/{l}.json').read_text(encoding='utf-8')) for l in ('zh_cn','en_us')}
    blocks=[];items=[];tags={k:[] for k in ('mineable/pickaxe','stairs','slabs')}
    tab=ROOT/'src/main/java/com/longju/xiantu/registry/ModCreativeTabs.java';tabs=tab.read_text(encoding='utf-8')
    for f,info in FAMILIES.items():
        for finish,zh,en in [('smooth','去纹','Smooth '),('carved','雕花','Carved ')]:
            base=f+'_'+finish
            for shape,count in [('stairs',4),('slab',6)]:
                n=base+'_'+shape;k=n.upper();mat='Blocks.BASALT' if f=='black_rock' else 'Blocks.STONE_BRICKS'
                prop=f"buildingProperties({mat}, {info['strength']}F)"
                ctor=f'StairBlock(() -> {base.upper()}.get().defaultBlockState(), {prop})' if shape=='stairs' else f'SlabBlock({prop})'
                blocks.append(f'    public static final RegistryObject<Block> {k} = BLOCKS.register("{n}", () -> new {ctor});')
                items.append(f'    public static final RegistryObject<Item> {k} = building("{n}", ModBlocks.{k});')
                write_json(ASSETS/f'blockstates/{n}.json',variant_blockstate(n) if shape=='stairs' else slab_blockstate(n))
                parents=[('', 'stairs'),('_inner','inner_stairs'),('_outer','outer_stairs')] if shape=='stairs' else [('', 'slab'),('_top','slab_top')]
                for suffix,parent in parents:write_json(ASSETS/f'models/block/{n}{suffix}.json',textured_model('minecraft:block/'+parent,bottom=base,top=base,side=base))
                if shape=='slab':write_json(ASSETS/f'models/block/{n}_double.json',{'parent':'xiantu:block/'+base})
                write_json(ASSETS/f'models/item/{n}.json',{'parent':'xiantu:block/'+n})
                write_json(DATA/f'loot_tables/blocks/{n}.json',block_loot(n,slab=shape=='slab'))
                write_json(DATA/f'recipes/{n}.json',recipe(n,['B  ','BB ','BBB'] if shape=='stairs' else ['BBB'],{'B':{'item':'xiantu:'+base}},count))
                for locale,label in [('zh_cn',zh+info['names'][2 if shape=='stairs' else 3]),('en_us',en+info['english'][2 if shape=='stairs' else 3])]:
                    for kind in ('block','item'):langs[locale][kind+'.xiantu.'+n]=label
                tags['mineable/pickaxe'].append('xiantu:'+n);tags[shape if shape=='stairs' else 'slabs'].append('xiantu:'+n)
            anchor=f'                output.accept(ModBlockItems.{base.upper()}.get());'
            if f'output.accept(ModBlockItems.{base.upper()}_STAIRS.get());' not in tabs:
                assert anchor in tabs;tabs=tabs.replace(anchor,anchor+f'\n                output.accept(ModBlockItems.{base.upper()}_STAIRS.get());\n                output.accept(ModBlockItems.{base.upper()}_SLAB.get());',1)
    tab.write_text(tabs,encoding='utf-8')
    folder=ROOT/'src/main/java/com/longju/xiantu/registry'
    for filename,lines,anchor in [('ModBlocks.java',blocks,'    private static RegistryObject<Block> registerEaves('),('ModBlockItems.java',items,'    private ModBlockItems()')]:
        p=folder/filename;s=p.read_text(encoding='utf-8')
        if 'AZURE_BRICK_SMOOTH_STAIRS =' not in s:
            assert anchor in s;s=s.replace(anchor,'\n'.join(lines)+'\n\n'+anchor,1);p.write_text(s,encoding='utf-8')
    for name,values in tags.items():merge_tag(name,values)
    for l,value in langs.items():write_json(ASSETS/f'lang/{l}.json',value)
    print('Integrated20 smooth/carved stairs and slabs; reused ten accepted64px textures.')

if __name__=='__main__':main()
