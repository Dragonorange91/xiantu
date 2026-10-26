"""Arrange real Blockbench captures, never repaint or synthesize model geometry."""
from build_paths import review_font
from PIL import Image,ImageDraw,ImageFont,ImageChops
from generate_gongfa_books import OUT,catalog,TIERS

def main():
    ids=['xuan_yuan_yang_qi_jue','jin_guang_xuan_gong','zi_fu_zhou_tian_jing','tai_qing_hun_yuan_jing','tai_shang_yan_dao_jing','tai_chu_dao_jing']
    entries={e['id']:e for e in catalog()}
    sheet=Image.new('RGB',(1200,720),(28,30,35));d=ImageDraw.Draw(sheet);font=review_font(23)
    for i,key in enumerate(ids):
        src=Image.open(OUT/'models'/(key+'_preview.png')).convert('RGBA')
        if src.getextrema()[3][0]==0:box=src.getbbox()
        else:box=ImageChops.difference(src.convert('RGB'),Image.new('RGB',src.size,(0,0,0))).getbbox()
        assert box
        tile=src.crop(box);tile.thumbnail((275,285),Image.Resampling.LANCZOS)
        x=(i%3)*400+(400-tile.width)//2;y=(i//3)*360+10
        sheet.paste(tile,(x,y),tile)
        e=entries[key];d.text(((i%3)*400+30,(i//3)*360+307),e['name']+' · '+TIERS[e['tier']],font=font,fill=(241,230,206))
    sheet.save(OUT/'线装功法_原生模型预览.png')
    print(OUT/'线装功法_原生模型预览.png')

if __name__=='__main__':main()
