"""Native Photon overlays for the remaining creatures and six Boss impact palettes.
Geometry telegraphs/projectiles are separately driven by the server skill contract.
"""
import json,time,shutil,math
from pathlib import Path
from beast_photon_helper import ROOT,call,guard
from author_beast_photon import beam,ring,motes,graph
from integrate_completion_models import SPECS
OUT=ROOT/'asset_sources/boss-combat-v4-20261018/photon'
PALETTE={'metal':'#ECCD76','water':'#82D2F1','wood':'#9BDF67','fire':'#FF8238','thunder':'#A3A5FF','earth':'#D5AF67'}
STYLES={'thunder_prison_lion':'D','electric_marten':'A','flint_armored_lizard':'A','jade_scale_crocodile':'A','azure_scale_python':'I','ancient_branch_python':'I','dark_shell_crab':'I','gold_back_beetle':'B','jade_leaf_mantis':'A','mushroom_toad':'E','copper_beak_vulture':'H','fire_beak_falcon':'H','purple_lightning_bat':'H'}
def main():
    guard();OUT.mkdir(parents=True,exist_ok=True)
    game=Path.home()/'Desktop/仙途/.minecraft/versions/1.20.1-Forge_47.4.26'
    managed=game/'resourcepacks/mcpmod-generated-vfx/assets/xiantu/fx/beasts'
    records=[]
    for rid,ident,name,behaviour,kind,width,height,element in SPECS:
        color=PALETTE[element]
        for stage in (['impact'] if kind=='BOSS' else ['flee'] if behaviour=='P' else ['warn','trail','impact']):
            if kind=='BOSS':
                e=ring(1.1,color,8,12)+[motes(color,3,.16,5,12,.15,.09,.5),motes('#FFF2CF',2,.075,3,8,.1,.13,.4)]
                doc={'emitters':e}
            else:doc=graph(STYLES.get(rid,'P'),color,stage)
            effect='xiantu:beasts/'+rid+'/'+stage
            (OUT/(rid+'-'+stage+'.json')).write_text(json.dumps(doc,indent=2),encoding='utf8')
            guard();result=call('photon_write_fx',id=effect,mode='replace' if (managed/rid/(stage+'.fx')).exists() else 'create',document=doc)
            records.append(dict(id=effect,emitters=len(doc['emitters']),result=result))
            print('AUTHORED',effect,flush=True)
    deadline=time.monotonic()+50
    while time.monotonic()<deadline:
        state=call('photon_status')
        if state.get('reload')=='complete':break
        if str(state.get('reload')).startswith('failed'):raise RuntimeError(state)
        time.sleep(.5)
    else:raise TimeoutError('Photon reload')
    call('photon_clear_fx_cache')
    for r in records:
        r['diagnostic']=call('photon_diagnose_fx',id=r['id'])
        path=r['id'].split('beasts/',1)[1]+'.fx'
        dest=ROOT/'src/main/resources/assets/xiantu/fx/beasts'/path;dest.parent.mkdir(parents=True,exist_ok=True)
        shutil.copy2(managed/path,dest)
    (OUT/'manifest.json').write_text(json.dumps(records,ensure_ascii=False,indent=2),encoding='utf8')
    print('PACKAGED',len(records),'native FX; visual QA still required')
if __name__=='__main__':main()
