"""Serial API image edits for the full moss-weathered masonry batch."""
import subprocess,sys,json
from pathlib import Path
from PIL import Image
from generate_building_material_resources import FAMILIES,MASTER,TEXTURES,ROOT
API=Path.home()/'.codex/skills/image-api-generator/scripts/generate_image.py'
BRIEFS={
 'azure_brick':'Blue-gray rectangular spirit bricks with worn silver cloud engravings. Clearly visible olive and fresh moss-green patches cover about 40 percent of the face, spreading asymmetrically from mortar joints across several brick faces. Broad dense patches with stepped dark green edges and light green inner clusters. Keep readable staggered brick courses.',
 'black_rock':'Charcoal basalt formation slabs with aged bronze formation channels. Bright olive and emerald green moss patches cover about 35 percent of the face. Use clearly lighter green than the charcoal rock, broad uneven patches on plates and in fissures, not near-black stains. Bronze channels remain readable in the exposed stone.',
 'cloud_jade':'Pale celadon jade masonry with carved ivory cloud scrolls. Distinct deep forest green and moss-green patches cover about 35 percent of the face, strongly contrasting with pale jade. Broad asymmetric connected patches extend out of the carved recesses across parts of the jade planes. Preserve recognizable jade and exposed broken cloud ornament.',
 'spring_stone':'Blue-teal rounded water-worn courtyard stones with ripple motifs. Vivid natural olive-green and dark moss-green colonies cover about 45 percent of the face. Broad uneven damp patches spread across stone faces from the joints, not teal discoloration. Preserve recognizable stones and exposed pale ripple lines.',
 'cloud_floating_stone':'Pale lavender-gray celestial ruin stone with peach-gold cloud channels. Clearly visible dark olive and natural moss-green colonies cover about 35 percent of the face, with strong contrast to pale lavender. Broad asymmetric patches with stepped edges, not sparse gray lichen or a green tint. Preserve exposed golden cloud ornament; no floating islands or sky.',
}
def main():
    manifest={}
    for family,brief in BRIEFS.items():
        output=MASTER/f'{family}_mossy_1024.png'
        if not output.exists():
            prompt=('Edit this accepted Minecraft Java cultivation-world block texture into its moss-weathered ancient ruin variant. '
                'A single seamless flat diffuse square 1024x1024 BLOCK TEXTURE, edge to edge; same original material, '
                'palette, ornament and scale, not a new generic stone. '+brief+' '
                'Moss forms irregular asymmetric flat pixel clusters, not a uniform green overlay, not recolored whole stone. '
                'Keep readable stone gaps between moss patches. Subtle age chips but no giant holes or black spiderweb. '
                'Minecraft style: hard square clusters, restrained palette, stepped shading readable after reduction to64x64. '
                'No perspective, cube render, frames, text, sky, glow, light bloom, soft gradients, photorealism, grass blades, '
                'hanging plants, flowers, raised plant geometry or transparency. Ancient Chinese cultivation architecture.')
            print('GENERATE',family,flush=True)
            subprocess.run([sys.executable,str(API),'--prompt',prompt,'--size','1024x1024','--quality','high',
                '--response-format','omit','--timeout','240','--input-image',str(MASTER/f"{FAMILIES[family]['texture']}_1024.png"),
                '--output',str(output)],check=True)
        with Image.open(output) as im:
            actual=im.size;master=im.convert('RGBA').resize((1024,1024),Image.Resampling.LANCZOS)
        assert master.getchannel('A').getextrema()==(255,255)
        master.save(output);master.resize((64,64),Image.Resampling.LANCZOS).save(TEXTURES/f'{family}_mossy.png')
        manifest[family]={'actual_api_size':actual,'master':str(output),'runtime_size':[64,64],'status':'generated; visual review pending'}
        (ROOT/'asset_sources/building_materials/mossy-image-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        print('READY',family,'source',actual,flush=True)
if __name__=='__main__':main()
