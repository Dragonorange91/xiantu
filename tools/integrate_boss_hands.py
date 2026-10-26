"""Targeted import of original-detail Boss hands; leaves the other 31 entities untouched."""
import json,hashlib,shutil
from verify_boss_hands import ROOT,BASE,READY,IDS,main as verify
from integrate_completion_models import read,images

def main():
    verify()
    author=ROOT/'asset_sources/creature-completion-20261017'
    res=ROOT/'src/main/resources/assets/xiantu/models/entity'
    backup=BASE/'pre-integration-backup';backup.mkdir(exist_ok=True)
    manifest=author/'integration-manifest.json'
    if not (backup/manifest.name).exists():shutil.copy2(manifest,backup/manifest.name)
    rows=json.loads(manifest.read_text(encoding='utf8'))
    for row in rows:
        ident=row['model']
        if ident not in IDS:continue
        meta=json.loads((READY/(ident+'-manifest.json')).read_text(encoding='utf8'))
        d,b=read(READY/(ident+'.glb'))
        for suffix in ('.glb','_lod.glb','-manifest.json'):
            filename=ident+suffix
            if (author/filename).exists() and not (backup/filename).exists():shutil.copy2(author/filename,backup/filename)
            shutil.copy2(READY/filename,author/filename)
            if suffix.endswith('.glb'):shutil.copy2(READY/filename,res/filename)
        row.update(sha256=meta['animated_sha256'],lod_sha256=meta['lod_sha256'],joints=meta['joints'],clips=[a['name'] for a in d['animations']],images=images(d,b),status='original_hand_surface_and_holding_integrated_pending_runtime',authoring_manifest=str(READY/(ident+'-manifest.json')))
    manifest.write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf8')
    print('Integrated 5 Boss hand/holding revisions; backed up previous assets and manifest.')
if __name__=='__main__':main()
