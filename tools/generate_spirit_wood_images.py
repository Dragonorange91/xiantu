"""Serial API-only image phase. Tree-specific atlases -> audited64px resources."""
import json,subprocess,sys
from PIL import Image
from spirit_wood_catalog import ROOT,TREES,TILES,SPRITES
BASE=ROOT/'asset_sources/spirit_wood'
API=__import__('pathlib').Path.home()/'.codex/skills/image-api-generator/scripts/generate_image.py'
STYLE=('Minecraft Java64px pixel texture language, hard square clusters and crisp stepped shading, restrained coherent palette, Chinese cultivation setting, '
       'no photographic realism, no smooth gradients, no bloom, no rendered3D cube or scene, no text, no grid lines or labels. '
       '1024x1024 high quality master. Exactly aligned cells with no gutters, no borders, no overlap between cells. ')
def generate(tree,kind):
    name,cn,wood,*_,brief=tree
    master=BASE/'master'/f'{name}_{kind}_1024.png';master.parent.mkdir(parents=True,exist_ok=True)
    status_path=BASE/'production-status.json'
    existing=json.loads(status_path.read_text(encoding='utf-8')) if status_path.exists() else {}
    if master.exists() and existing.get(f'{name}/{kind}',{}).get('review')=='passed':
        print('SKIP reviewed',name,kind,flush=True);return
    if kind=='tiles':
        descriptions=['vertical bark side','circular log end rings with bark rim','stripped vertical wood grain','stripped cut end rings without bark',
          'horizontal joined wooden planks','shallow auspicious Chinese carved cloud wood panel','upper half ancient double panel wooden door',
          'lower half of matching wooden door','matching wooden trapdoor panel','slim wooden lattice frame pattern',
          'narrow wooden roof slats','carved continuous geometric trim','straight column fine grain','straight horizontal beam grain','uncarved calm wood grain','plain polished wooden plaque backing']
        prompt=STYLE+f'One square4by4 opaque texture atlas for {cn}, {wood}. Material identity: {brief}. '+\
          'Each cell256by256 pixels is a separate flat square full-bleed seamless diffuse material. Read left to right then top to bottom: '+\
          '; '.join(f'cell{i+1}: {d}' for i,d in enumerate(descriptions))+'. All cells fully opaque to their edges; no margins or scenery. Carving is wood joinery, not stone. Fine motifs must remain readable after each cell reduces to64px.'
    else:
        prompt=STYLE+f'One square4by2 grid sprite atlas on TRUE transparent background for {cn}, {wood}. Material identity: {brief}. '+\
          'Each cell256wide512high. Eight separate isolated assets, left to right then top to bottom: '+\
          '1 a dense square leaf canopy texture patch of this exact species with small true alpha gaps, nearly filling cell; '+\
          '2 small upright sapling with branched twig and species-specific foliage, rooted stem, no pot; '+\
          '3 single slender spirit-wood stick diagonally; 4 completely empty transparent cell; '+\
          '5 completely empty transparent cell;6 very long slim straight wooden spear shaft diagonally, no spear head; '+\
          '7 small bundle of fine wood fiber strands;8 completely empty transparent cell. '+\
          'Sprites remain inside each cell, centered with clear transparent margins, consistent family grain. Background must be real alpha, never a painted checkerboard. Avoid shadows outside objects.'
    if not master.exists():
        print('BEGIN',name,kind,flush=True)
        args=[sys.executable,str(API),'--prompt',prompt,'--size','1024x1024','--quality','high','--response-format','omit','--timeout','180','--output',str(master)]
        for attempt in range(2):
            result=subprocess.run(args)
            if result.returncode==0 and master.exists():break
        else:raise RuntimeError(f'Image API unavailable for {name}/{kind}; no fallback artwork')
    with Image.open(master) as im:
        original=im.size;im=im.convert('RGBA').resize((1024,1024),Image.Resampling.LANCZOS)
    im.save(master)
    names=TILES if kind=='tiles' else SPRITES
    textures=BASE/'candidate'/name;textures.mkdir(parents=True,exist_ok=True)
    for i,suffix in enumerate(names):
        if suffix.startswith('unused_'):continue
        w,h=(256,256) if kind=='tiles' else (256,512)
        tile=im.crop(((i%4)*w,(i//4)*h,(i%4+1)*w,(i//4+1)*h))
        if kind=='sprites' and suffix!='leaves':
            # Preserve the item's shape; square-pad tall cells rather than stretching.
            square=Image.new('RGBA',(512,512));square.alpha_composite(tile,(128,0));tile=square
        if suffix=='leaves':tile=tile.crop((0,128,256,384))
        tile=tile.resize((64,64),Image.Resampling.LANCZOS)
        if kind=='tiles':tile.putalpha(255)
        else:tile.putalpha(tile.getchannel('A').point(lambda a:255 if a>=128 else 0))
        tile.save(textures/f'{suffix}.png')
    print('READY',name,kind,'source',original,'alpha',im.getchannel('A').getextrema(),flush=True)
    status_path=BASE/'production-status.json'
    status=json.loads(status_path.read_text(encoding='utf-8')) if status_path.exists() else {}
    status[f'{name}/{kind}']={'master':str(master),'original_size':original,'generated':True,'review':'pending','runtime_size':[64,64]}
    status_path.write_text(json.dumps(status,ensure_ascii=False,indent=2),encoding='utf-8')
if __name__=='__main__':
    BASE.mkdir(parents=True,exist_ok=True)
    for tree in TREES:
        for kind in ('tiles','sprites'):generate(tree,kind)
    print('All image candidates generated serially; inspect before model phase.',flush=True)
