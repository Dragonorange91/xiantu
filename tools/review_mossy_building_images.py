"""Review API artwork beside 64px runtime textures and intact originals."""
from build_paths import review_font
from PIL import Image,ImageDraw,ImageFont
from generate_building_material_resources import ROOT,ASSETS,MASTER,FAMILIES
def main():
    sheet=Image.new('RGB',(1280,850),(32,35,40));draw=ImageDraw.Draw(sheet)
    font=review_font(17)
    for i,(f,info) in enumerate(FAMILIES.items()):
        x=i*256;draw.text((x+8,8),'苔蚀'+info['names'][0],font=font,fill='white')
        for row,p in enumerate([MASTER/f'{f}_mossy_1024.png',ASSETS/f'textures/block/{f}_mossy.png',ASSETS/f"textures/block/{info['texture']}.png"]):
            with Image.open(p) as im:
                assert im.size==((1024,1024) if row==0 else (64,64)) and im.mode=='RGBA'
                assert im.getchannel('A').getextrema()==(255,255)
                sheet.paste(im.convert('RGB').resize((240,240),Image.Resampling.LANCZOS if row==0 else Image.Resampling.NEAREST),(x+8,38+row*270))
            draw.text((x+8,281+row*270),['API母图缩览','64px苔蚀材质（放大）','原完整主材对照'][row],font=font,fill='white')
    target=ROOT/'asset_sources/building_materials/mossy-textures-review.png';sheet.save(target);print(target)
if __name__=='__main__':main()
