"""Source-derived crop/alpha cleanup only; never synthesizes replacement artwork."""
import json,shutil
from PIL import Image
from spirit_wood_catalog import ROOT,TREES
BASE=ROOT/'asset_sources/spirit_wood'
def clean_alpha(path):
    im=Image.open(path).convert('RGBA');pixels=im.load();remaining={(x,y) for y in range(64) for x in range(64) if pixels[x,y][3]}
    groups=[]
    while remaining:
        first=remaining.pop();group={first};todo=[first]
        while todo:
            x,y=todo.pop()
            for dx in [-1,0,1]:
                for dy in [-1,0,1]:
                    neighbor=(x+dx,y+dy)
                    if neighbor in remaining:remaining.remove(neighbor);group.add(neighbor);todo.append(neighbor)
        groups.append(group)
    if not groups:return 0
    groups.sort(key=len,reverse=True);largest=groups[0];x0=min(x for x,y in largest)-2;x1=max(x for x,y in largest)+2;y0=min(y for x,y in largest)-2;y1=max(y for x,y in largest)+2
    removed=0
    for group in groups[1:]:
        # Preserve nearby meaningful details, drop atlas spill/isolated edge specks.
        if len(group)>=4 and all(x0<=x<=x1 and y0<=y<=y1 for x,y in group):continue
        for x,y in group:r,g,b,a=pixels[x,y];pixels[x,y]=(r,g,b,0);removed+=1
    im.save(path);return removed
def main():
    status=json.loads((BASE/'production-status.json').read_text(encoding='utf-8'));assert len(status)==16,'Wait for complete serial image batch'
    cleanup={}
    for tree in TREES:
        f=tree[0];folder=BASE/'candidate'/f
        if (folder/'strip.png').exists():shutil.copy2(folder/'strip.png',folder/'stick.png')
        for suffix in ['stick','long_shaft','fiber','sapling']:cleanup[f+'/'+suffix]=clean_alpha(folder/(suffix+'.png'))
        im=Image.open(BASE/'master'/f'{f}_tiles_1024.png').convert('RGBA')
        for suffix,box in [('door_top',(512,256,640,384)),('door_bottom',(512,384,640,512))]:im.crop(box).resize((64,64),Image.Resampling.LANCZOS).save(folder/(suffix+'.png'))
        for suffix in ['strip','short_hilt','reinforced_handle','processing_scroll','window_frame']:
            obsolete=folder/(suffix+'.png')
            assert obsolete.resolve().is_relative_to(BASE.resolve())
            if obsolete.exists():obsolete.unlink()
    (BASE/'candidate-cleanup.json').write_text(json.dumps({'source':'API master crops only','obsolete_materials_removed':True,'alpha_specks_removed':cleanup},ensure_ascii=False,indent=2),encoding='utf-8')
    from revise_spirit_wood_art import install_reviewed
    install_reviewed()
    print('Prepared eight candidates; reviewed targeted revisions preserved. Visual review is still required before approving status.')
if __name__=='__main__':main()
