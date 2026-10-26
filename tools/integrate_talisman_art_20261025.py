"""Repack original red cinnabar artwork; never reuse the text-bearing reference edit."""
from build_paths import review_font
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
import numpy as np,json,hashlib
from scipy import ndimage as nd
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/xiantu';O=ROOT/'asset_sources/talisman-20261025'
IDS=['fireball','thunder','sword_qi','explosion','diamond','ward','reflection','lightbody','speed','invisibility','concealment','healing','detox','cleanse','soul_seal','spirit_lock','demon_seal','trap','earth_escape','blink','long_return','rebirth']
def clean_magenta(im):
 a=np.array(im.convert('RGBA'));rgb=a[:,:,:3].astype(np.int32);bg=(rgb[:,:,0]>140)&(rgb[:,:,2]>110)&(rgb[:,:,1]<100)&(rgb[:,:,0]>rgb[:,:,1]*1.5)&(rgb[:,:,2]>rgb[:,:,1]*1.4)
 seed=np.zeros(bg.shape,bool);seed[0]=bg[0];seed[-1]=bg[-1];seed[:,0]=bg[:,0];seed[:,-1]=bg[:,-1];mask=nd.binary_propagation(seed,mask=bg);a[:,:,3]=np.where(mask,0,255);a[mask,:3]=0
 return Image.fromarray(a)
def fit(im,solid=False):
 assert im.getbbox(),'Empty artwork cell'
 im=im.crop(im.getbbox());im.thumbnail((58,60),Image.Resampling.LANCZOS);a=np.array(im);a[:,:,3]=np.where(a[:,:,3]>=128,255,0)
 if solid:
  visible=a[:,:,3]>0;holes=nd.binary_fill_holes(visible)&~visible
  if holes.any():
   _,idx=nd.distance_transform_edt(~visible,return_indices=True);a[holes]=a[idx[0][holes],idx[1][holes]]
 a[a[:,:,3]==0]=0;im=Image.fromarray(a);out=Image.new('RGBA',(64,64));out.alpha_composite(im,((64-im.width)//2,(64-im.height)//2));return out
def save(name,im):
 p=A/'textures/item'/f'{name}.png';p.parent.mkdir(parents=True,exist_ok=True);im.save(p)
 model={'parent':'minecraft:item/generated','textures':{'layer0':f'xiantu:item/{name}'},'display':{'firstperson_righthand':{'rotation':[0,-10,0],'translation':[0,1,0],'scale':[.55,.55,.55]},'firstperson_lefthand':{'rotation':[0,10,0],'translation':[0,1,0],'scale':[.55,.55,.55]}}}
 if name.startswith('talisman_') and name not in ['talisman_blueprint','talisman_residue']:
  for side in ['firstperson_righthand','firstperson_lefthand']:model['display'][side]['scale']=[.44,.58,.58]
 (A/'models/item'/f'{name}.json').write_text(json.dumps(model,indent=2),encoding='utf8');return im
PINNED={'original-cinnabar-atlas.png':'be8674111cf6f2f7f324086fafe602b5e6f781baad144b400c932b41442b0f49','original-paper-materials-atlas.png':'e16e6d0778b48006f5ae1ac3175edae7168a464ff1b3a45e460a5bad5d296c6b'}
def source(name):
 assert hashlib.sha256((O/name).read_bytes()).hexdigest()==PINNED[name],f'Unreviewed artwork: {name}'
 raw=Image.open(O/name).convert('RGBA');master=raw.resize((1024,1024),Image.Resampling.LANCZOS);master.save(O/(Path(name).stem+'-normalized.png'));return master
sheet=source('original-cinnabar-atlas.png');icons=[];records=[]
for i,name in enumerate(IDS):
 x,y=i%6,i//6;tile=sheet.crop((round(x*1024/6)+3,round(y*1024/4)+3,round((x+1)*1024/6)-3,round((y+1)*1024/4)-3));icon=fit(clean_magenta(tile),solid=True);icons.append(save('talisman_'+name,icon))
 a=np.array(icon).astype(np.int32);visible=a[:,:,3]>0;red=visible&(a[:,:,0]>110)&(a[:,:,0]>a[:,:,1]*1.6)&(a[:,:,0]>a[:,:,2]*1.5);bbox=icon.getbbox();bands=[int(v.sum()) for v in np.array_split(red[bbox[1]:bbox[3]],4)]
 assert red.sum()>=100 and min(bands)>=10,f'Incomplete cinnabar glyph: {name} {bands}'
 assert not (visible&(a[:,:,:3].max(axis=2)<70)).any(),f'Black inscription: {name}'
 assert not (nd.binary_fill_holes(visible)&~visible).any(),f'Paper pinholes: {name}'
 records.append({'id':name,'cinnabar_pixels':int(red.sum()),'vertical_quarters':bands,'sha256':hashlib.sha256((A/'textures/item'/f'talisman_{name}.png').read_bytes()).hexdigest()})
finished=list(icons)
sheet=source('original-paper-materials-atlas.png');paper_icons=[]
# One universal blank paper material; tier is carried by TalismanTier, not a function type.
tile=sheet.crop((3,3,round(1024/3)-3,round(1024/3)-3))
icon=save('talisman_paper',fit(clean_magenta(tile),solid=True));paper_icons.append(icon);icons.append(icon)
# Reviewed auxiliary icons retain their original silhouettes; remove only the edge-connected navy backdrop.
from scipy import ndimage as nd
atlas=Image.open(O/'icon-atlas.png').convert('RGBA');w,h=atlas.size
for i,name in [(29,'cinnabar'),(30,'talisman_brush_1'),(31,'talisman_brush_2'),(32,'talisman_brush_3'),(33,'broken_talisman_brush'),(34,'talisman_residue'),(35,'space_sand')]:
 x,y=i%6,i//6;tile=atlas.crop((round(x*w/6)+5,round(y*h/6)+5,round((x+1)*w/6)-5,round((y+1)*h/6)-5));a=np.array(tile);bg=(a[:,:,0]<28)&(a[:,:,1]<45)&(a[:,:,2]<76)&(a[:,:,2]>a[:,:,0]+8);seed=np.zeros(bg.shape,bool);seed[0,:]=bg[0,:];seed[-1,:]=bg[-1,:];seed[:,0]=bg[:,0];seed[:,-1]=bg[:,-1];mask=nd.binary_propagation(seed,mask=bg);a[mask,3]=0;icons.append(save(name,fit(Image.fromarray(a))))
hud=clean_magenta(Image.open(ROOT/'asset_sources/hud-20261025/spell-master.png').convert('RGBA'));w,h=hud.size;hp=A/'textures/gui/hud';hp.mkdir(parents=True,exist_ok=True)
for i in range(4):
 tile=hud.crop((round(i*w/4),int(h*.30),round((i+1)*w/4),int(h*.65)));tile=tile.crop(tile.getbbox()).resize((64,64),Image.Resampling.LANCZOS);ar=np.array(tile);ar[:,:,3]=np.where(ar[:,:,3]>128,255,0);Image.fromarray(ar).save(hp/f'spell_slot_{i}.png')
if (O/'cinnabar-ore-master.png').exists():Image.open(O/'cinnabar-ore-master.png').convert('RGBA').resize((64,64),Image.Resampling.LANCZOS).save(A/'textures/block/cinnabar_ore.png')
review=Image.new('RGBA',(8*128,5*160),(36,39,44,255));draw=ImageDraw.Draw(review)
for i,icon in enumerate(icons):review.alpha_composite(icon.resize((128,128),Image.Resampling.NEAREST),((i%8)*128,(i//8)*160));draw.text(((i%8)*128,(i//8)*160+130),str(i),fill='white')
review.convert('RGB').save(O/'runtime-review.png')
names=['火球符','雷击符','剑气符','爆裂符','金刚符','护身符','反伤符','轻身符','神行符','隐身符','敛息符','回春符','解毒符','净体符','镇魂符','锁灵符','封魔符','困阵符','遁地符','瞬移符','千里符','转生符']
font=review_font(17);small=review_font(15)
def labeled_review(images,labels,path,columns,title):
 rows=(len(images)+columns-1)//columns;canvas=Image.new('RGB',(columns*150,rows*172+42),(36,39,44));draw=ImageDraw.Draw(canvas);draw.text((14,9),title,font=font,fill='#f1d79f')
 for i,(icon,label) in enumerate(zip(images,labels)):
  x=i%columns*150+11;y=i//columns*172+40;large=icon.resize((128,128),Image.Resampling.NEAREST);canvas.paste(large,(x,y),large);draw.text((x+64,y+134),label,font=small,fill='#f2dfbc',anchor='mt')
 canvas.save(path)
labeled_review(finished,names,O/'finished-cinnabar-review.png',6,'成品符箓 · 黄符纸 / 朱砂红符纹')
labeled_review(paper_icons,['一至八阶通用符纸'],O/'paper-materials-review.png',1,'符纸材料 · 按灵木阶位制造')
native=Image.new('RGBA',(6*72,4*72),(36,39,44,255))
for i,icon in enumerate(finished):native.alpha_composite(icon,(i%6*72+4,i//6*72+4))
native.save(O/'finished-cinnabar-native64.png')
manifest={'version':2,'color':'cinnabar_red','generated_from_scratch':True,'reference_pixels_used':False,'finished_count':22,'materials_separate':True,'source_sha256':PINNED,'runtime_size':[64,64],'symbols':records,'status':'integrated_source_resources_not_built_or_deployed'}
(O/'art-integration-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8')
print('Integrated',len(icons),'icons; all 22 finished symbols have complete RED cinnabar glyphs; four HUD frames retained')
