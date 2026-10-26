"""API-made thread-bound book covers; serial, resumable, textures before geometry."""
from build_paths import review_font
from pathlib import Path
import argparse, json, re, subprocess, sys, shutil
from PIL import Image, ImageDraw, ImageFont, ImageEnhance

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'asset_sources/gongfa-books-20261011'
API=(Path.home() / '.codex/skills/image-api-generator/scripts/generate_image.py')
SOURCE=ROOT/'src/main/java/com/longju/xiantu/gongfa/GongfaRegistry.java'
REFERENCE=(Path.home() / 'AppData/Local/Temp/codex-clipboard-ac51d322-bffe-4a88-afd2-3a2fb2162306.png')
TIERS=['凡阶','黄阶','玄阶','地阶','天阶','仙阶','神阶','混沌阶']
THEMES={
'xuan_yuan_yang_qi_jue':'plain deep indigo cotton cloth, ivory stitching, no magic ornament; humble classic beginner manual',
'jin_guang_jue':'warm ochre cloth, a small muted brass ray seal in lower left, simple binding',
'qing_mu_jue':'moss green cloth, one small angular twig-and-leaf stamp in lower left, simple binding',
'xuan_shui_jue':'blue-black cloth, one quiet narrow rippling water band along the bottom, simple binding',
'chi_yan_jue':'brick-red cloth, a small angular ember seal in lower left, simple binding',
'yan_tu_jue':'earth-brown cloth, tiny layered rock ridge stamp in lower left, simple binding',
'geng_jin_ning_yuan_jue':'charcoal silk cloth with dull pale-brass thin parallel blade-like rays and modest brass corner edging',
'qing_mu_gui_yuan_jue':'dark jade cloth with intertwined roots and restrained branching vine corners, pale celadon thread',
'cang_lan_zhen_jing':'deep sea-blue woven cloth, three layered curling wave bands flowing along lower border, silver-blue thread',
'li_huo_zhen_jing':'burnt crimson silk, a angular rising vermilion flame curled around a tiny ember seal, dark copper border',
'shan_yue_zhen_jing':'ochre slate cloth, stepped mountain silhouettes and square mineral relief in lower half, bronze thread',
'jin_guang_xuan_gong':'black brocade, slender ordered gold rays radiating from an angular gold seal, exquisite gold-copper binding',
'bai_cao_zhen_jing':'forest-green brocade, distinct small ginkgo leaf and medicinal sprigs interlaced around lower corners, jade edging',
'you_shui_zhen_jing':'near-black midnight blue cloth, a winding silver river entering a deep whirlpool below label, frost-blue binding',
'fen_chi_chi_huo':'wine-red brocade, layered scarlet cloud forms with a compact phoenix-feather flame flourish, copper-gold edging',
'qing_shan_zhen_di':'brown-black brocade, three massive stepped mountain peaks etched in antique bronze, sturdy bronze corners',
'wu_xing_gui_cang_jing':'black silk, five small balanced interconnected metal leaf water fire rock seals forming an elegant five-element diagram on left half, gold binding',
'zi_fu_zhou_tian_jing':'deep imperial violet silk, sparse silver constellations around a subtle celestial palace silhouette, silver binding',
'tai_qing_hun_yuan_jing':'pale celadon white-jade silk, restrained winding pale-gold clouds and balanced interlocking light-dark curls, refined jade-white border',
'xuan_pin_yang_shen_jing':'midnight turquoise silk, a silver lotus cupping a tiny pearl beneath a crescent-shaped arc, silver-jade binding',
'tai_shang_yan_dao_jing':'ivory-gold silk, a precise ancient cosmological eight-trigram border with nested restrained gold angular Daoist geometry, noble gold binding',
'wu_ji_dong_xuan_jing':'obsidian silk, precise silver interlocking endless loops with tiny violet pinpoints, layered silver border and austere dark binding',
'tai_chu_dao_jing':'ancient warm ivory silk with black-gold corners, a primal ink circle breaking into asymmetrical gold cloud filaments, solemn origin-of-Dao character, exceptionally refined binding',
'hun_dun_gui_yi_jing':'very dark plum-black silk, interweaving bronze and violet currents converging into one gold central seal, intricate but restrained void-cloud corners, unique dark-bronze binding'
}

def catalog():
    rows=re.findall(r'method\("([^"]+)", "([^"]+)", (\d+),', SOURCE.read_text(encoding='utf-8'))
    assert len(rows)==24
    return [dict(id=i,name=n,tier=int(t),theme=THEMES[i]) for i,n,t in rows]

def brief(e):
    title='\n'.join(e['name'])
    return f'''Create a real usable front-cover UV texture for a Minecraft Java Chinese cultivation manual, a traditional Chinese thread-bound book following the uploaded reference. The reference ONLY establishes the flat cloth cover, LEFT stitched binding and RIGHT ivory vertical title slip. Do not copy its watermark.
Exactly ONE square 1024x1024 opaque artwork, entirely filled by the FRONT COVER edge-to-edge. Perfect flat orthographic view, no perspective, no book thickness, no pages, no background, no shadow outside, no surrounding frame, not a photograph or product mockup. This square UV will map to a tall 10x14 rectangular book cover, so slightly horizontally wide glyphs are appropriate. Put binding seam and 5 short beige horizontal stitch marks in the LEFTMOST 12 percent. Put the ivory title label in x=68..91 percent, y=10..83 percent, with a fine dark outline. Large black legible Chinese characters in EXACT top-to-bottom sequence:
{title}
The full exact title is {e['name']}. Include every character once, including the last character. No other letters, no rank captions, no watermark. Keep the ivory label clear and high contrast, one vertical column.
Cover identity: {e['theme']}. Tier {TIERS[e['tier']]}. Avoid making every cover merely a recolor; specified symbolic designs belong in LEFT/MIDDLE cover area and never behind the title label. All motifs remain crafted embroidery/foil stamped into the cover, no floating jewels, no effects outside the cover.
Minecraft-compatible pixel texture: hard-edged square clusters, clean stepped three-tone shading, broad coherent material clusters, limited palette, no soft gradients, no bloom, no realistic fabric noise, no detailed painterly illustration. Beautiful crafted ancient cultivation book, readable at 64 pixels. Low tier is attractive modest cloth, high tier is refined rare brocade and intricate distinct restrained decoration. Title readability takes priority over ornament.'''

def generate(selected):
    (OUT/'masters').mkdir(parents=True,exist_ok=True)
    reference=OUT/'线装书_玩家参考.png'
    if not reference.exists(): shutil.copy2(REFERENCE,reference)
    entries=catalog()
    (OUT/'catalog.json').write_text(json.dumps(entries,ensure_ascii=False,indent=2),encoding='utf-8')
    for e in entries:
        if selected and e['id'] not in selected: continue
        dest=OUT/'masters'/(e['id']+'.png')
        if dest.exists(): print('REUSE',e['id'],flush=True); continue
        prompt=brief(e)
        (OUT/'masters'/(e['id']+'.prompt.txt')).write_text(prompt,encoding='utf-8')
        print('GENERATING',e['id'],flush=True)
        args=[sys.executable,str(API),'--prompt',prompt,'--input-image',str(reference),'--size','1024x1024','--quality','high','--response-format','omit','--timeout','240','--output',str(dest)]
        subprocess.run(args,check=True)
        with Image.open(dest) as im:
            actual=im.size
            im.convert('RGB').resize((1024,1024),Image.Resampling.LANCZOS).save(dest)
        print('MASTER',e['id'],actual,flush=True)

def pages():
    path=OUT/'masters/book_pages.png'
    if path.exists(): return
    prompt='''Minecraft Java opaque pixel material UV texture, exactly1024x1024 square filled edge-to-edge. Side view of compressed ancient Chinese thread-bound ivory paper pages. Entire surface filled with thin parallel horizontal ivory and warm gray paper strata, slightly irregular broken short fibers. Crisp square pixel clusters, stepped3-tone restrained palette, no perspective, no book outline, no letters, no labels, no shadow outside, no realistic noise, no watercolor, no soft gradients. It is a plain off-white layered paper edge material, not wood and not yellow straw. Fine horizontal page stripes remain legible at64x64.'''
    subprocess.run([sys.executable,str(API),'--prompt',prompt,'--size','1024x1024','--quality','high','--response-format','omit','--timeout','240','--output',str(path)],check=True)
    with Image.open(path) as im:
        im.convert('RGBA').resize((1024,1024),Image.Resampling.LANCZOS).save(path)

def prepare():
    (OUT/'textures').mkdir(parents=True,exist_ok=True)
    rows=[]
    for e in catalog():
        path=OUT/'masters'/(e['id']+'.png')
        if not path.exists(): continue
        with Image.open(path) as im: source=im.convert('RGBA')
        tile=source.resize((64,64),Image.Resampling.LANCZOS)
        ImageEnhance.Sharpness(tile).enhance(1.3).save(OUT/'textures'/(e['id']+'_cover.png'))
        # Reuse approved/generated cloth away from the label. No new painted motif.
        plain=source.crop((150,20,490,260)).resize((64,64),Image.Resampling.LANCZOS)
        plain.save(OUT/'textures'/(e['id']+'_cloth.png'))
        rows.append(e|dict(state='generated_pending_visual_inspection',master=str(path),runtime=e['id']+'_cover.png'))
    (OUT/'texture-manifest.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf-8')
    font=review_font(18)
    sheet=Image.new('RGB',(6*210,((len(rows)+5)//6)*225),(31,33,37));draw=ImageDraw.Draw(sheet)
    for i,e in enumerate(rows):
        x=(i%6)*210; y=(i//6)*225
        with Image.open(OUT/'textures'/e['runtime']) as im:
            # Display the actual runtime64px texture, in its intended10:14 cover proportion.
            display=im.convert('RGB').resize((128,179),Image.Resampling.NEAREST)
        sheet.paste(display,(x+40,y+4))
        draw.text((x+5,y+187),e['name'],font=font,fill='white')
        draw.text((x+5,y+208),TIERS[e['tier']],font=font,fill=(196,188,164))
    sheet.save(OUT/'封面纹饰_64像素检查.png')
    paper=OUT/'masters/book_pages.png'
    if paper.exists():
        with Image.open(paper) as im: im.convert('RGBA').resize((64,64),Image.Resampling.LANCZOS).save(OUT/'textures/book_pages.png')
    print('PREPARED',len(rows),'covers',flush=True)

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--ids',nargs='*');p.add_argument('--prepare-only',action='store_true');a=p.parse_args()
    if not a.prepare_only:
        generate(a.ids)
        if not a.ids: pages()
    prepare()
