"""API-only targeted wood-art revisions; crop reviewed masters into64px tiles.

Never synthesize replacement art. Existing models and UVs are preserved.
"""
import argparse
import json
import shutil
import subprocess
import sys
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter
from spirit_wood_catalog import ROOT, TREES, TILES

BASE = ROOT / 'asset_sources/spirit_wood'
REV = BASE / 'revision-20261011'
API = Path.home() / '.codex/skills/image-api-generator/scripts/generate_image.py'
STATUS = REV / 'status.json'
STYLE = (
    'Minecraft JAVA vanilla resource-pack pixel art, flat unlit diffuse texture, '
    'hard-edged SQUARE pixel clusters on a visible logical32x32 grid per cell. '
    'Each logical pixel is one flat color square, only4-6 stepped material colors per asset. '
    'Follow the supplied Minecraft reference pixel scale and visual simplicity. '
    'Absolutely NO tiny realistic botanical details, photographic shading, soft gradients, '
    'painterly brushwork, outlines around entire leaf sprites, ground, cast shadows, text or labels. '
    'Produce a1024x1024 master; all cells align exactly without gutters or dividers. '
)


def read_status():
    return json.loads(STATUS.read_text(encoding='utf-8')) if STATUS.exists() else {}


def write_status(data):
    REV.mkdir(parents=True, exist_ok=True)
    STATUS.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding='utf-8')


def reference(kind, group):
    ref = REV / 'references' / f'vanilla_{kind}_{group}.png'
    canvas = Image.new('RGBA', (1024, 1024), (0, 0, 0, 0))
    names = (['oak_leaves','birch_leaves','spruce_leaves','oak_leaves'] if kind == 'leaves'
             else ['oak_sapling','birch_sapling','spruce_sapling','oak_sapling'])
    for i, name in enumerate(names):
        with Image.open(REV / 'references' / f'{name}.png') as source:
            source = source.convert('RGBA')
            if kind == 'leaves':
                # Vanilla leaf sprites are grayscale for tinting; the reference
                # is colored only for display. Generated sprites use fixed colors.
                px = list(source.getdata())
                source.putdata([(int(r*.48), int(g*.65), int(b*.27), a) for r,g,b,a in px])
            canvas.alpha_composite(source.resize((512,512), Image.Resampling.NEAREST), ((i%2)*512,(i//2)*512))
    canvas.save(ref)
    return ref


def generate(job):
    kind, _, group = job.partition('_')
    if kind in ('leaves','saplings'):
        group = int(group)
        species = TREES[group*4:group*4+4]
        reference_path = reference('sapling' if kind=='saplings' else 'leaves',group)
        descriptions = [
            'round small elm leaf clusters, fresh mid green and restrained dark teal',
            'small oval birch leaf clusters, warm green with sparse muted copper patches',
            'maple lobed leaf clusters, burgundy and muted purple-red',
            'short angular dense pine needle clusters, deep green with lighter gray-green',
            'broad oval nanmu leaf clusters, natural mid green and muted olive, no yellow flowers',
            'dense cedar sprigs, dark blue-green with restrained slate-violet tips',
            'palmate wutong leaf clusters, muted green with subtle blue-green accents',
            'oval sandalwood leaf clusters, deep teal green with a few blue-green highlights, no glitter',
        ][group*4:group*4+4]
        prompt = STYLE + 'One EXACT2by2 grid of four SQUARE512by512 cells. '
        prompt += 'Read left to right, then next row: ' + '; '.join(
            f'cell{i+1} {t[1]}: {d}' for i,(t,d) in enumerate(zip(species,descriptions))) + '. '
        if kind=='leaves':
            prompt += (
                'Each cell is a seamless CUBE LEAF BLOCK face, NOT an isolated branch or a bouquet. '
                'Broad connected flat leaf-color clusters cover about80-88percent of each entire cell '
                'including all four edges, with8-18percent SMALL irregular TRUE ALPHA holes. '
                'Use chunky broken square patterns comparable to vanilla oak_leaves, only2-4logical '
                'pixels per leaf cluster. No single huge holes, no visible branch or stem, no directional '
                'hanging vine, no orderly rows of individual realistic leaves, no flower rosettes. '
                'No atmospheric dark border; all four edges tile. Transparent holes are actual alpha, '
                'not black paint or checkerboard. All leaves have fixed material colors, not grayscale.'
            )
        else:
            prompt += (
                'Each cell is ONE Minecraft crossed-plane SAPLING sprite on actual transparent alpha. '
                'Keep the SAME simple twig-and-foliage pixel vocabulary as the supplied vanilla sapling. '
                'One short angular brown central stem2logical pixels wide, about3small branching twigs, '
                '3-5 chunky connected leaf masses. Broad young seedling, NOT a miniature mature tall tree. '
                'Foliage occupies the upper two thirds; stem is clearly visible at the bottom. '
                'Subject fills75percent of cell width and85percent of height, bottom centered, '
                'no fine roots, no dirt clod, no pot, no detached leaf confetti. '
                'Leaves represented by solid stepped square clusters, no painted leaf veins. '
                'Do not shrink the sprite into a small thin twig with huge empty margins.'
            )
        outputs = [(t[0], 'sapling' if kind=='saplings' else 'leaves',i) for i,t in enumerate(species)]
        dimensions = (2,2)
    elif job=='nanmu_tiles':
        reference_path = (Path.home() / 'AppData/Local/Temp/codex-clipboard-1455bdf3-7990-45cc-8396-9c8192b945cd.png')
        descriptions = [
            'gray-brown bark vertical side, rough bark not gold-colored wood',
            'cut log end with restrained bark rim and asymmetric amber golden rings',
            'stripped wood side: natural ripple tiger-silk grain, amber honey-gold heartwood',
            'stripped log end: golden honey amber asymmetric organic growth rings',
            'horizontal wooden planks with narrow dark joins, waving golden silk grain inside each plank',
            'carved nanmu panel with shallow angular auspicious motif carved from the same amber wood',
            'upper half of ONE single wooden ancient door panel, golden wood fine lattice at top',
            'lower half of the SAME single door, plain golden wooden panel with wood rails',
            'square wood trapdoor with broad wooden rails, natural honey-gold grain',
            'square slim geometric lattice window wood material',
            'parallel narrow wood roof slats in the same golden amber material',
            'horizontal shallow angular Chinese trim carved from honey amber wood',
            'vertical pillar material with undulating golden silk fibers',
            'horizontal beam material with undulating golden silk fibers',
            'plain smooth uncarved golden nanmu panel with broad waves of natural silk grain',
            'plain polished nanmu plaque backing: honey amber wood, no letters, no border',
        ]
        prompt = STYLE + (
            'Interpret the provided REAL golden-silk nanmu photo as WOOD material, then translate '
            'its defining tiger-silk wavy shimmering grain into restrained Minecraft SQUARE pixel clusters. '
            'NOT gold metal, NOT orange flat oak, NOT repetitive straight generic grain. '
            'Warm honey amber base, irregular undulating champagne-gold fiber ribbons, deep brown '
            'broken grain lines. Use4-6clear wood tones, not realistic specular reflection. '
            'One exact4by4 opaque grid, each cell256by256, seamless square full-bleed flat material. '
            'Every cell must be opaque edge to edge, no transparency, no perspective, no borders. '
            'Door top and bottom are explicit separate UV halves, do not draw two complete doors. '
            'Read left to right then next row: '
        ) + '; '.join(f'cell{i+1}: {d}' for i,d in enumerate(descriptions)) + '.'
        outputs = [('jin_si_nanmu',s,i) for i,s in enumerate(TILES) if s!='window_frame']
        dimensions = (4,4)
    elif job=='nanmu_materials':
        reference_path = (Path.home() / 'AppData/Local/Temp/codex-clipboard-1455bdf3-7990-45cc-8396-9c8192b945cd.png')
        prompt = STYLE + (
            'One exact2by2 square cell sprite atlas with TRUE transparent background. '
            'The reference is real golden-silk nanmu. Objects are WOOD, honey amber, subtle wavy '
            'golden-silk grain, stepped dark brown shadows, no metal, no glitter. '
            'Top left: one narrow Minecraft wooden stick, diagonal lower-left to upper-right, '
            '6logical pixels thick and27pixels long. Top right: one long slender straight spear '
            'shaft, no tip, same diagonal angle,3pixels thick and29pixels long. '
            'Bottom left: a compact bundle of golden nanmu wooden fiber strands tied at center, '
            'flat chunky pixel clusters. Bottom right: fully empty transparent cell. '
            'Center each item inside its cell with clear margins; no shadows outside item silhouettes.'
        )
        outputs = [('jin_si_nanmu',s,i) for i,s in enumerate(['stick','long_shaft','fiber'])]
        dimensions = (2,2)
    else:
        raise ValueError(job)
    master = REV / 'master' / f'{job}_1024.png'
    master.parent.mkdir(parents=True,exist_ok=True)
    state = read_status()
    if master.exists() and state.get(job,{}).get('processing_version')==2:
        print('SKIP generated',job,flush=True)
        return
    (master.parent/f'{job}_prompt.txt').write_text(prompt,encoding='utf-8')
    print('BEGIN',job,flush=True)
    command = [sys.executable,str(API),'--prompt',prompt,'--input-image',str(reference_path),
               '--size','1024x1024','--quality','high','--response-format','omit',
               '--timeout','240','--output',str(master)]
    if not master.exists():
        for attempt in range(2):
            result = subprocess.run(command)
            if result.returncode==0 and master.exists():
                break
        else:
            raise RuntimeError(f'Image API failed for {job}; no alternate artwork substituted')
    with Image.open(master) as image:
        original = image.size
        image = image.convert('RGBA').resize((1024,1024),Image.Resampling.LANCZOS)
    image.save(master)
    w,h = 1024//dimensions[0],1024//dimensions[1]
    files=[]
    alpha_notes = []
    for species,suffix,index in outputs:
        tile = image.crop(((index%dimensions[0])*w,(index//dimensions[0])*h,
                           (index%dimensions[0]+1)*w,(index//dimensions[0]+1)*h))
        if job!='nanmu_tiles' and tile.getchannel('A').getextrema()[0]==255:
            # This batch has strictly colored leaves and neutral light gray/white
            # checker holes. Retain a separate inspectable mask; never use this
            # cleanup for pale bark, silver items or gray-colored foliage.
            mask=Image.new('L',tile.size,255)
            source=tile.load(); pixels=mask.load()
            for y in range(tile.height):
                for x in range(tile.width):
                    r,g,b,a=source[x,y]
                    if min(r,g,b)>=155 and max(r,g,b)-min(r,g,b)<=14:
                        pixels[x,y]=0
            removed=sum(1 for a in mask.getdata() if a==0)/mask.width/mask.height
            limits=(.01,.22) if suffix=='leaves' else (.35,.94)
            assert limits[0]<removed<limits[1], f'Uncertain neutral-background mask {job}: {removed}'
            mask_path=REV/'masks'/f'{species}_{suffix}.png';mask_path.parent.mkdir(parents=True,exist_ok=True)
            mask.save(mask_path);tile.putalpha(mask)
            alpha_notes.append({'species':species,'mask':str(mask_path.relative_to(REV)),
                                'method':'color-isolated neutral checker holes; inspect against source',
                                'source_hole_fraction':removed})
        tile = tile.resize((64,64),Image.Resampling.BOX)
        alpha = tile.getchannel('A')
        if job=='nanmu_tiles':
            tile.putalpha(255)
        else:
            assert alpha.getextrema()[0]<64, f'Fake transparency {job}/{suffix}'
            tile.putalpha(alpha.point(lambda a:255 if a>=128 else 0))
            if suffix=='leaves':
                # Enlarge only the source's existing tiny holes by one runtime
                # pixel, instead of shipping subpixel checker remnants. Repeat
                # the mask so cleanup agrees across tiled block-face edges.
                alpha=tile.getchannel('A')
                repeated=Image.new('L',(192,192))
                for yy in range(3):
                    for xx in range(3):repeated.paste(alpha,(xx*64,yy*64))
                widened=repeated.filter(ImageFilter.MinFilter(3)).crop((64,64,128,128))
                hole_fraction=sum(a==0 for a in widened.getdata())/4096
                if hole_fraction<=.38:tile.putalpha(widened)
        path=REV / 'candidate' / species / f'{suffix}.png'
        path.parent.mkdir(parents=True,exist_ok=True)
        tile.save(path)
        files.append(str(path.relative_to(REV)))
    state=read_status()
    state[job]={'master':str(master.relative_to(REV)), 'original_size':original,
                'source':'configured image API image-edit', 'files':files,
                'alpha_cleanup':alpha_notes,'processing_version':2,
                'review':'pending', 'user_acceptance':'pending'}
    write_status(state)
    print('READY',job,len(files),'tiles; original',original,flush=True)
    preview()


def preview():
    # Composite existing candidates only; labels are outside the artwork.
    rows=[]
    for t in TREES:
        entries=[(s,REV/'candidate'/t[0]/f'{s}.png') for s in ['sapling','leaves','planks','smooth','carved','stick']]
        if any(p.exists() for _,p in entries):rows.append((t[0],[(s,p) for s,p in entries if p.exists()]))
    canvas=Image.new('RGB',(1200,max(1,len(rows))*180),(62,65,69))
    draw=ImageDraw.Draw(canvas)
    for y,(name,entries) in enumerate(rows):
        draw.text((4,y*180+4),name,fill='white')
        for x,(suffix,path) in enumerate(entries):
            tile=Image.open(path).convert('RGBA').resize((128,128),Image.Resampling.NEAREST)
            canvas.paste(tile,(x*195+5,y*180+23),tile)
            draw.text((x*195+5,y*180+155),suffix,fill='white')
    canvas.save(REV/'preview.png')


def install_reviewed():
    state=read_status()
    for job,record in state.items():
        if record['review']!='passed':
            continue
        for relative in record['files']:
            source=REV/relative
            destination=BASE/'candidate'/source.parent.name/source.name
            shutil.copy2(source,destination)
    # Latest user choice supersedes the newer sapling art only.
    from restore_spirit_saplings import install
    install()


if __name__=='__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('jobs',nargs='*')
    parser.add_argument('--install-reviewed',action='store_true')
    args=parser.parse_args()
    if args.install_reviewed:
        install_reviewed()
    else:
        for job in args.jobs:generate(job)
