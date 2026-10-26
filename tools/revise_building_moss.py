"""Generate the stronger moss batch into staging; install only after visual review."""
import argparse,json,shutil,subprocess,sys
from PIL import Image
from generate_building_mossy_images import BRIEFS,API
from generate_building_material_resources import ROOT,MASTER,TEXTURES,FAMILIES

STAGE=ROOT/'asset_sources/building_materials/moss-visible-v2'

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--install',action='store_true');args=parser.parse_args()
    STAGE.mkdir(parents=True,exist_ok=True)
    if args.install:
        assert (STAGE/'review.png').exists(), 'Review the complete staged batch first'
        record=json.loads((STAGE/'manifest.json').read_text(encoding='utf-8'))
        for f in BRIEFS:
            for dest,src in [(MASTER/f'{f}_mossy_1024.png',STAGE/f'{f}_1024.png'),(TEXTURES/f'{f}_mossy.png',STAGE/f'{f}_64.png')]:
                backup=STAGE/'previous'/dest.name;backup.parent.mkdir(exist_ok=True)
                if not backup.exists():shutil.copy2(dest,backup)
                shutil.copy2(src,dest)
            record[f]['master']=str(MASTER/f'{f}_mossy_1024.png')
            record[f]['status']='stronger moss visually reviewed and integrated; deployment pending'
        (ROOT/'asset_sources/building_materials/mossy-image-manifest.json').write_text(json.dumps(record,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        print('Installed five reviewed stronger moss textures.');return
    record={}
    for f,brief in BRIEFS.items():
        target=STAGE/f'{f}_1024.png'
        if not target.exists():
            prompt=('Edit the supplied original Minecraft block texture into a visibly MOSSY ancient Chinese cultivation ruin material. '+brief+
                ' Moss must read immediately at native 64 pixel size: large contiguous irregular green pixel clusters, '+
                'mostly 6 to 18 target pixels across, with smaller broken clusters at their edges. Clearly green vegetation, '+
                'not just a hue shift, subtle dirt or fine speckles. Preserve material identity and exposed original carvings. '+
                'A single seamless flat diffuse square 1024x1024 block texture edge to edge. Minecraft pixel language, '+
                'hard square clusters, limited palette and stepped shading, no fine realistic noise. '+
                'No perspective, cube render, border, text, bloom, soft gradients, grass blades, flowers or transparency.')
            print('GENERATE',f,flush=True)
            subprocess.run([sys.executable,str(API),'--prompt',prompt,'--size','1024x1024','--quality','high',
                '--response-format','omit','--timeout','240','--input-image',str(MASTER/f"{FAMILIES[f]['texture']}_1024.png"),
                '--output',str(target)],check=True)
        with Image.open(target) as image:
            actual=image.size;master=image.convert('RGBA').resize((1024,1024),Image.Resampling.LANCZOS)
        assert master.getchannel('A').getextrema()==(255,255)
        master.save(target);master.resize((64,64),Image.Resampling.LANCZOS).save(STAGE/f'{f}_64.png')
        record[f]={'actual_api_size':actual,'master':str(target),'runtime_size':[64,64],'revision':'visible-moss-v2','status':'staged; review pending'}
        (STAGE/'manifest.json').write_text(json.dumps(record,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        print('READY',f,flush=True)

if __name__=='__main__':main()
