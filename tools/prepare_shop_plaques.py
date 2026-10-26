"""Keep the existing gilded plaque, add readable gold Kai script for four shops.

Build textures and an explicit MCP job list; geometry and UV exports run in Blockbench.
"""
import json
import base64
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
ASSET = ROOT/'src/main/resources/assets/xiantu'
OUT = ROOT/'asset_sources/town/shop-plaques-20261019'
BB = ROOT/'asset_sources/building_materials/models'
LABELS = {'wanbaoge':'万宝阁','forging_shop':'炼器铺','alchemy_pavilion':'丹阁','talisman_shop':'符箓坊'}

def dump(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')

def main():
    OUT.mkdir(parents=True,exist_ok=True)
    # Use the clean original master; previous town sections already contain writing.
    source=Image.open(ROOT/'asset_sources/building_materials/master/wooden_plaque_1024.png').convert('RGBA')
    base=Image.new('RGBA',(1536,576))
    src=[0,80,944,1024];xs=[0,48,1488,1536];ys=[0,48,528,576]
    for yy in range(3):
        for xx in range(3):
            piece=source.crop((src[xx],src[yy],src[xx+1],src[yy+1]))
            base.paste(piece.resize((xs[xx+1]-xs[xx],ys[yy+1]-ys[yy]),Image.Resampling.LANCZOS),(xs[xx],ys[yy]))
    font=ImageFont.truetype(r'C:\Windows\Fonts\simkai.ttf',310)
    calls=[]
    for stem,label in LABELS.items():
        id=stem+'_plaque'
        master=base.copy()
        mask=Image.new('L',master.size,0);d=ImageDraw.Draw(mask)
        box=d.textbbox((0,0),label,font=font)
        x=(1536-(box[2]-box[0]))//2-box[0]; y=(576-(box[3]-box[1]))//2-box[1]-5
        d.text((x,y),label,font=font,fill=255,stroke_width=1)
        # Subtle engraved shadow + lit gold, never blur the actual glyph strokes.
        shadow=Image.new('RGBA',master.size,(44,22,7,0));shadow.putalpha(mask.filter(ImageFilter.MaxFilter(7)))
        master.alpha_composite(shadow,(3,4))
        gold=Image.new('RGBA',master.size)
        gd=ImageDraw.Draw(gold)
        for yy in range(576):
            t=yy/575
            gd.line((0,yy,1535,yy),fill=(int(255-45*t),int(224-67*t),int(137-79*t),255))
        gold.putalpha(mask);master.alpha_composite(gold)
        master.save(OUT/f'{id}-master.png')
        front=master.resize((512,192),Image.Resampling.LANCZOS)
        front.save(OUT/f'{id}-front.png')
        # Item uses the existing compact 8:3 board; square packed map is intentional.
        front.resize((512,512),Image.Resampling.LANCZOS).save(ASSET/f'textures/block/{id}.png')
        variants={}
        for p in range(24):
            # part0 is lower-left as seen from the front; part7 is lower-right.
            tile=front.crop((p%8*64,(2-p//8)*64,(p%8+1)*64,(3-p//8)*64))
            tile.save(ASSET/f'textures/block/{id}_part_{p}.png')
            for facing,rot in [('north',0),('east',90),('south',180),('west',270)]:
                variants[f'facing={facing},part={p}']={'model':f'xiantu:block/{id}_part_{p}','y':rot,'uvlock':False}
        dump(ASSET/f'blockstates/{id}.json',{'variants':variants})
        dump(ASSET/f'models/item/{id}.json',{'parent':f'xiantu:block/{id}'})
        data=ROOT/'src/main/resources/data'
        loot=json.loads((data/'xiantu/loot_tables/blocks/wooden_plaque_large.json').read_text())
        # Anchor-only drop conditions must use the new block as well.
        loot=json.loads(json.dumps(loot).replace('xiantu:wooden_plaque_large',f'xiantu:{id}'))
        dump(data/f'xiantu/loot_tables/blocks/{id}.json',loot)
        emblem={'wanbaoge':'gold_ingot','forging_shop':'iron_ingot','alchemy_pavilion':'glass_bottle','talisman_shop':'paper'}[stem]
        dump(data/f'xiantu/recipes/{id}.json',{'type':'minecraft:crafting_shapeless','ingredients':[{'item':'xiantu:wooden_plaque_large'},{'item':'minecraft:gold_nugget'},{'item':f'minecraft:{emblem}'}],'result':{'item':f'xiantu:{id}'}})
        # One item source and 24 actual world sections, authored/exported via MCP.
        for p in [None,*range(24)]:
            name=id if p is None else f'{id}_part_{p}'
            template='wooden_plaque_large' if p is None else 'wooden_plaque_large_part_0'
            cube='wooden_board' if p is None else 'wooden_board_section'
            calls.extend([
                {'name':'load_project','arguments':{'path':str(BB/f'{template}.bbmodel')}},
                {'name':'set_project_meta','arguments':{'name':name}},
                {'name':'create_texture','arguments':{'name':name,'width':512 if p is None else 64,'height':512 if p is None else 64,'data_url':'data:image/png;base64,'+base64.b64encode((ASSET/f'textures/block/{name}.png').read_bytes()).decode()}},
                {'name':'set_cube_uv','arguments':{'cube':cube,'faces':{side:{'texture':name,'uv':([0,0,16,16] if side in ['north','south'] else [0.05,0.05,0.25,0.25])} for side in ['north','south','east','west','up','down']}}},
                {'name':'save_project','arguments':{'path':str(OUT/f'{name}.bbmodel')}},
                {'name':'export_model','arguments':{'codec':'java_block','path':str(OUT/f'{name}.json')}},
            ])
            if p is None:
                calls.extend([
                    {'name':'set_camera_angle','arguments':{'position':[8,8,-30],'target':[8,8,14],'angle':'ortho'}},
                    {'name':'screenshot','arguments':{'width':1024,'height':512,'annotate':False},'image':str(OUT/f'{id}-blockbench.png')}
                ])
            calls.append({'name':'close_project','arguments':{}})
        for locale in ['zh_cn','en_us']:
            path=ASSET/f'lang/{locale}.json';lang=json.loads(path.read_text(encoding='utf8'))
            text=label+'牌匾' if locale=='zh_cn' else {'wanbaoge':'Wanbao Pavilion','forging_shop':'Forge','alchemy_pavilion':'Elixir Pavilion','talisman_shop':'Talisman Workshop'}[stem]+' Plaque'
            for kind in ['block','item']:lang[f'{kind}.xiantu.{id}']=text
            dump(path,lang)
        tag=data/'minecraft/tags/blocks/mineable/axe.json';value=json.loads(tag.read_text())
        if f'xiantu:{id}' not in value['values']:value['values'].append(f'xiantu:{id}')
        dump(tag,value)
    dump(OUT/'mcp-jobs.json',calls)
    dump(OUT/'production.json',{'target':'Minecraft Java 1.20.1 Forge static blocks','source':'existing wooden_plaque_large; native Kai lettering','budget':'one cube per section, 24 sections per 8x3 plaque, no entity or tick','texture':'64px per block section, 512x192 continuous front','labels':LABELS,'mcp_jobs':len(calls)})
    print(f'Prepared {len(LABELS)} plaques and {len(calls)} MCP operations: {OUT}')

if __name__=='__main__': main()
