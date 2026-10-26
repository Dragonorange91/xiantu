"""Resource contract for eight universal papers and three native unlock books."""
from pathlib import Path
import json,hashlib,re
from PIL import Image
R=Path(__file__).resolve().parents[1];A=R/'src/main/resources/assets/xiantu';T=R/'src/main/java/com/longju/xiantu/talisman'
recipes=(T/'TalismanRecipes.java').read_text(encoding='utf-8')
assert 'TalismanTomes.forTier(tier).id()' in recipes
assert 'for(int f=1;f<=8;f++)' in recipes and 'wood+"_fiber",2' in recipes
assert '"mother"' not in recipes and 'd.paper()' not in recipes
for p in T.glob('*.java'):
 s=p.read_text(encoding='utf-8');assert all(x not in s for x in ('母纸','功能纸','PROCESSING','paper_attack')),p
trade=(R/'src/main/java/com/longju/xiantu/trade/TownTradeService.java').read_text(encoding='utf-8')
assert len(re.findall(r'new Offer\("talisman_book/',trade))==3
base=json.loads((A/'models/item/xuan_yuan_yang_qi_jue.json').read_text(encoding='utf-8'))
route=json.loads((A/'models/item/talisman_blueprint.json').read_text(encoding='utf-8'))
books=['xuanmen_initial','xuanmen_true','xuanmen_secret']
manifest=json.loads((R/'asset_sources/talisman-tomes-20261025/integration.json').read_text(encoding='utf-8'))
for i,ident in enumerate(books):
 assert route['overrides'][i]=={'predicate':{'custom_model_data':i+1},'model':'xiantu:item/'+ident}
 m=json.loads((A/'models/item'/(ident+'.json')).read_text(encoding='utf-8'))
 assert m['elements']==base['elements'] and m['display']==base['display']
 for ref in m['textures'].values():
  im=Image.open(A/'textures'/(ref.removeprefix('xiantu:')+'.png')).convert('RGBA')
  assert im.size==(64,64) and im.getextrema()[3]==(255,255)
 assert hashlib.sha256((A/'textures/item'/(ident+'_cover.png')).read_bytes()).hexdigest()==manifest['books'][i]['texture_sha256']
paper=json.loads((A/'models/item/talisman_paper.json').read_text(encoding='utf-8'))
assert 'overrides' not in paper and paper['textures']['layer0']=='xiantu:item/talisman_paper'
assert 'xiantu:talisman_paper' in json.loads((R/'src/main/resources/data/xiantu/tags/items/identifiable_materials.json').read_text(encoding='utf-8'))['values']
bench=(R/'src/main/java/com/longju/xiantu/refining/FacilityCraftingRecipe.java').read_text(encoding='utf-8')
assert 'if(facility.equals("talisman_table"))return true;' in bench and 'TalismanProgress' not in bench
print('PASS:8 universal paper tiers,3 tier-band tomes,no processing blueprints,free table crafting;3 native24-face books,64px textures and model overrides')
