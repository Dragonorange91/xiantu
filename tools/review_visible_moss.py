from build_paths import review_font
from PIL import Image,ImageDraw,ImageFont
from revise_building_moss import STAGE
from generate_building_material_resources import TEXTURES,FAMILIES

sheet=Image.new('RGB',(1280,880),(30,33,36));draw=ImageDraw.Draw(sheet)
font=review_font(18)
for i,(f,info) in enumerate(FAMILIES.items()):
    x=i*256;draw.text((x+8,8),info['names'][0],font=font,fill='white')
    for row,path in enumerate([TEXTURES/f'{f}_mossy.png',STAGE/f'{f}_64.png',STAGE/f'{f}_1024.png']):
        with Image.open(path) as im:
            assert im.size==((1024,1024) if row==2 else (64,64))
            assert im.mode=='RGBA' and im.getchannel('A').getextrema()==(255,255)
            sheet.paste(im.convert('RGB').resize((240,240),Image.Resampling.LANCZOS if row==2 else Image.Resampling.NEAREST),(x+8,40+row*280))
        draw.text((x+8,283+row*280),['旧版64px','新版64px（放大）','新版1024母图缩览'][row],font=font,fill='white')
sheet.save(STAGE/'review.png');print(STAGE/'review.png')
