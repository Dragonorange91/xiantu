"""Generate a separate written reading face; keep the approved paper-edge material."""
import json, subprocess, sys
from PIL import Image
from generate_gongfa_books import API, OUT, ROOT

def main():
    master=OUT/'masters/book_reading_paper.png'
    prompt='''One flat opaque 1024x1024 square UV texture of the FRONT READING FACE of a single ancient Chinese cultivation book page. Warm WHITE paper fills the entire canvas, almost clean white, with very subtle blocky ivory fiber shading. NO book, no perspective, no cover, no paper-stack edge stripes, no border, no binding, no background, no folded corner, no shadow, no watermark. This will map onto a tall rectangular page in Minecraft Java. Leave generous clean white margins of 12 percent on every edge. Three large vertical columns of BLACK traditional Chinese characters, read top to bottom, columns from right to left. Right column: 道法自然. Middle column: 清靜養神. Left column: 凝氣歸元. Exactly FOUR large clearly separated characters in each column, twelve characters total. Plain ancient printed square calligraphy, not elaborate swirling talisman writing. Restrained Minecraft compatible pixel texture: hard edged square clusters, dark solid strokes, large readable glyphs, limited warm white and black palette, no smooth painting, no photorealistic microtexture, no gray noise. Strong whitespace between characters. Beautiful simple white paper printed with cultivation scripture. Remain readable after reduction to 64x64.'''
    (OUT/'masters/book_reading_paper.prompt.txt').write_text(prompt,encoding='utf8')
    if not master.exists():
        subprocess.run([sys.executable,str(API),'--prompt',prompt,'--size','1024x1024','--quality','high','--response-format','omit','--timeout','240','--output',str(master)],check=True)
    with Image.open(master) as im:
        original=im.size
        im=im.convert('RGBA')
        assert im.getextrema()[3]==(255,255)
        im.resize((1024,1024),Image.Resampling.LANCZOS).save(master)
        tile=im.resize((64,64),Image.Resampling.LANCZOS)
        tile.save(OUT/'textures/book_reading_paper.png')
        tile.save(ROOT/'src/main/resources/assets/xiantu/textures/item/gongfa_reading_paper.png')
        tile.resize((512,512),Image.Resampling.NEAREST).save(OUT/'study/reading_paper_preview.png')
    (OUT/'study/reading_paper_production.json').write_text(json.dumps({'api_source_dimensions':original,'master':[1024,1024],'runtime':[64,64],'reading_face':'gongfa_reading_paper','edge_material_preserved':'gongfa_book_pages','api_response_format':'omit','visual_review':'pending'},ensure_ascii=False,indent=2),encoding='utf8')
    print('Generated separate written reading paper',flush=True)

if __name__=='__main__': main()
