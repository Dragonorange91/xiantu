"""Ensure every registered spell has its own GUI art and preserves accepted sources."""
from pathlib import Path
import hashlib,json,re
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
ART=ROOT/'asset_sources/spell-icons-20261025'
RES=ROOT/'src/main/resources/assets/xiantu'
catalog=(ROOT/'src/main/java/com/longju/xiantu/spell/SpellCatalog.java').read_text('utf8')
ids=set(re.findall(r'new Spell\("([^"]+)"',catalog))
manifest=json.loads((ART/'manifest.json').read_text('utf8'))
assert ids==set(manifest)
hashes=[]
for ident in ids:
 p=RES/'textures/gui/spells'/(ident+'.png')
 digest=hashlib.sha256(p.read_bytes()).hexdigest();hashes.append(digest)
 assert digest==manifest[ident]['runtime_sha256']
 with Image.open(p) as im:
  assert im.size==(64,64) and im.mode=='RGBA' and im.getextrema()[3]==(255,255)
 with Image.open(ART/(ident+'-master.png')) as im:assert im.size==(1024,1024)
assert len(set(hashes))==len(ids),'Spell artwork duplicated'
for cls in ('SpellClient','SpellScreen'):
 source=(ROOT/f'src/main/java/com/longju/xiantu/client/{cls}.java').read_text('utf8')
 assert 'SpellScrolls.stack' not in source and 'SpellIcons.' in source
print('Dedicated spell icons PASS:',','.join(sorted(ids)))
scroll=RES/'textures/item/spell_scroll_sword_flight.png'
scroll_art=ROOT/'asset_sources/sword-learning-scroll-20261025'
scroll_manifest=json.loads((scroll_art/'manifest.json').read_text('utf8'))
assert hashlib.sha256(scroll.read_bytes()).hexdigest()==scroll_manifest['runtime_sha256']
model=json.loads((RES/'models/item/spell_scroll_sword_flight.json').read_text('utf8'))
assert model['textures']['layer0']=='xiantu:item/spell_scroll_sword_flight','Sword learning item must not borrow fireball scroll art'
assert scroll.read_bytes()!=(RES/'textures/item/spell_scroll_fire_bolt.png').read_bytes()
with Image.open(scroll) as im:
 assert im.size==(64,64) and im.mode=='RGBA'
 assert set(im.getchannel('A').getdata())=={0,255},'Hard alpha for clean item side extrusion'
 assert im.getbbox()[0]>0 and im.getbbox()[2]<64,'Complete scroll fits inside transparent margins'
with Image.open(scroll_art/'master.png') as im:assert im.size==(1024,1024)
print('Dedicated sword learning scroll PASS: independent generated art, 64px hard alpha and model reference')
