"""Original periodic smoke ribbon texture; no video/screenshot or third-party atlas is embedded."""
from pathlib import Path
import math
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
W,H=256,64
im=Image.new('RGBA',(W,H))
for y in range(H):
    v=(y+.5)/H
    for x in range(W):
        u=(x+.5)/W*math.tau
        feather=math.sin(math.pi*v)**2.5
        cloud=.54+.18*math.sin(u*3+v*7)+.14*math.cos(u*5-v*11)+.1*math.sin(u*9+v*19)
        thread=0
        for j in range(5):
            line=.18+j*.15+.045*math.sin(u*(j%3+1)+j*1.7)
            thread+=math.exp(-((v-line)/(.018+j*.003))**2)*(.12+.08*math.sin(u*2+j)**2)
        alpha=min(1,max(0,feather*(cloud*.54+thread)))
        im.putpixel((x,y),(255,255,255,round(alpha*255)))
path=ROOT/'src/main/resources/assets/xiantu/textures/vfx/formation/qi_flow.png'
path.parent.mkdir(parents=True,exist_ok=True);im.save(path)
assert im.getchannel('A').getextrema()[0]==0
print('Original seamless feathered qi texture:',path,im.size)
