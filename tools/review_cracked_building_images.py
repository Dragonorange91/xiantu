"""Contact sheet of API artwork and actual runtime texture; never draws replacement art."""
from build_paths import review_font
from PIL import Image,ImageDraw,ImageFont
from generate_building_material_resources import FAMILIES,MASTER,TEXTURES,ROOT

def main():
    sheet=Image.new('RGB',(1280,850),(32,35,40))
    draw=ImageDraw.Draw(sheet)
    font=review_font(17)
    for i,(family,info) in enumerate(FAMILIES.items()):
        x=i*256
        draw.text((x+8,8),'裂损'+info['names'][0],font=font,fill='white')
        for row,path in enumerate([MASTER/f'{family}_cracked_1024.png',TEXTURES/f'{family}_cracked.png',TEXTURES/f"{info['texture']}.png"]):
            with Image.open(path) as im:
                assert im.mode=='RGBA' and im.size==((1024,1024) if row==0 else (64,64))
                assert im.getchannel('A').getextrema()==(255,255)
                sheet.paste(im.convert('RGB').resize((240,240),Image.Resampling.LANCZOS if row==0 else Image.Resampling.NEAREST),(x+8,38+row*270))
            draw.text((x+8,281+row*270),['API母图缩览','64px裂损材质（放大）','原完整主材对照'][row],font=font,fill='white')
    target=ROOT/'asset_sources/building_materials/cracked-textures-review.png'
    sheet.save(target)
    print(target)
if __name__=='__main__':main()
