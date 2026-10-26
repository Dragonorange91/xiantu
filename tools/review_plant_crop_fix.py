"""Compare packaged sprites to the pre-fix backup at nearest-neighbour scale."""
from build_paths import review_font
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
R=Path(__file__).resolve().parents[1]
O=R/'work/visual-revision-20261020'
A=Path('src/main/resources/assets/xiantu/textures/block')
ids=['herb_0005','herb_0007','herb_0008','herb_0010','herb_0021','herb_0044']
canvas=Image.new('RGB',(6*208,470),'#24302b');d=ImageDraw.Draw(canvas)
font=review_font(19)
for i,ident in enumerate(ids):
 x=i*208+8
 d.text((x,6),ident,font=font,fill='white')
 for row,root,label in [(0,O/'before','修复前'),(1,R,'完整轮廓')]:
  im=Image.open(root/A/f'{ident}_stage4.png').convert('RGBA').resize((192,192),Image.Resampling.NEAREST)
  canvas.paste(im,(x,32+row*218),im)
  d.text((x,224+row*218),label,font=font,fill='white')
canvas.save(O/'plant-crop-before-after.png')
print(O/'plant-crop-before-after.png')
