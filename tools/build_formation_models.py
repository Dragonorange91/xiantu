"""Author formation assets through the installed desktop Blockbench MCP.

The generated shape table derives from the same verified exported cuboids.
"""
from pathlib import Path
import json, shutil
from blockbench_bridge import Bridge
from build_building_openings_blockbench import texture, cuboid, save_export, runtime_export

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'asset_sources/formation-20261011'
ASSETS=ROOT/'src/main/resources/assets/xiantu'
MODELS=OUT/'models'
FACES=('north','south','east','west','up','down')

def write(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

def part(name,a,b,material,top=None):
    return dict(name=name,start=a,end=b,material=material,top=top)

def floor(top):
    return [part('stone_foundation',[0,0,0],[16,13,16],'stone'),
            part('inlaid_wood_course',[0,13,0],[16,14,16],'wood'),
            part('etched_conductive_plate',[0,14,0],[16,16,16],'metal',top)]

def pillar(tier,section):
    width=6 if tier==1 else 8
    lo=(16-width)/2;hi=16-lo
    if section=='bottom':
        return [part('stepped_foot',[1,0,1],[15,2,15],'stone'),
                part('metal_foot_ring',[2,2,2],[14,3,14],'metal'),
                part('wood_socket',[3,3,3],[13,4,13],'wood'),
                part('engraved_shaft',[lo,4,lo],[hi,16,hi],'pillar_side')]
    if section=='middle':
        return [part('engraved_shaft',[lo,0,lo],[hi,16,hi],'pillar_side'),
                part('joining_band',[lo-1,7,lo-1],[hi+1,9,hi+1],'metal')]
    parts=[]
    if section=='single':
        parts += [part('stepped_foot',[2,0,2],[14,2,14],'stone'),
                  part('metal_foot_ring',[3,2,3],[13,4,13],'metal')]
    parts += [part('engraved_shaft',[lo,4 if section=='single' else 0,lo],[hi,10,hi],'pillar_side'),
              part('crown_metal',[3,10,3],[13,12,13],'metal'),
              part('crown_stone',[2,12,2],[14,13,14],'stone'),
              part('crystal_tip',[6,13,6],[10,16,10],'crystal')]
    if tier>=2:
        parts += [part('west_crown_prong',[2,13,6],[4,15,10],'metal'),
                  part('east_crown_prong',[12,13,6],[14,15,10],'metal')]
    if tier==3:
        parts += [part('north_crown_prong',[6,13,2],[10,15,4],'metal'),
                  part('south_crown_prong',[6,13,12],[10,15,14],'metal')]
    return parts

def cushion():
    result=[]
    bands=[(0,2,5,11),(2,4,2,14),(4,6,1,15),(6,10,0,16),(10,12,1,15),(12,14,2,14),(14,16,5,11)]
    for i,(z0,z1,x0,x1) in enumerate(bands):
        result += [part('woven_bottom_'+str(i),[x0,0,z0],[x1,1,z1],'bottom'),
                   part('rush_body_'+str(i),[x0,1,z0],[x1,4.8,z1],'side','top')]
    return result

def model(b,name,parts,tier=None):
    b.call('create_project',name=name,format='java_block')
    used=list(dict.fromkeys(m for p in parts for m in [p['material'],p['top']] if m))
    for material in used:
        filename=f'array_{tier}_{material}' if tier else f'meditation_cushion_{material}'
        texture(b,material,OUT/'textures'/f'{filename}.png')
    texids={t['name']:t['uuid'] for t in b.value(b.call('list_textures'))}
    for p in parts:
        cuboid(b,p['name'],p['start'],p['end'],p['material'])
    # This installed bridge lacks set_cube_uv. Edit native Cube faces in one
    # reversible transaction, keeping the per-face UV rectangles intact.
    edits=[{'name':p['name'], 'up':texids[p['top']] if p['top'] else None,
            'down':texids['bottom'] if not tier else None} for p in parts]
    edit_result=b.value(b.call('risky_eval',code='(()=>{const edits='+json.dumps(edits)+'; '
           'const cubes=edits.map(e=>Cube.all.find(c=>c.name===e.name)); '
           'if(cubes.some(c=>!c)) throw new Error("Missing authored cube"); '
           'Undo.initEdit({elements:cubes}); '
           'edits.forEach((e,i)=>{if(e.up)cubes[i].faces.up.texture=e.up; '
           'if(e.down)cubes[i].faces.down.texture=e.down;}); '
           'Canvas.updateAllFaces(); Undo.finishEdit("Assign formation face materials"); '
           'return {updated:cubes.length};})()'))
    assert isinstance(edit_result,dict) and edit_result.get('updated')==len(parts), edit_result
    save_export(b,'project',MODELS/f'{name}.bbmodel')
    materials=[f'xiantu:block/array_{tier}_{m}' if tier else f'xiantu:block/meditation_cushion_{m}' for m in used]
    result=runtime_export(b,MODELS/f'{name}.json',materials)
    for p,e in zip(parts,result['elements']):
        if p['top']:
            assert result['textures'][e['faces']['up']['texture'][1:]]==materials[used.index(p['top'])], (name,p['name'],'top assignment')
    result['parent']='minecraft:block/block'
    # Omit strictly internal contact faces, preserving all exposed undersides.
    # Full floor courses touch across the entire horizontal plane.
    if name.endswith(('_base','_rune','_eye','_eye_lit')):
        for i,e in enumerate(result['elements']):
            if i>0:e['faces'].pop('down',None)
            if i<len(result['elements'])-1:e['faces'].pop('up',None)
        for e in result['elements']:
            for face in e['faces']:
                e['faces'][face]['cullface']=face
    result['display']={'gui':{'rotation':[30,225,0],'translation':[0,0,0],'scale':[.625,.625,.625]},
                       'ground':{'translation':[0,3,0],'scale':[.25,.25,.25]},
                       'fixed':{'rotation':[0,0,0],'scale':[.5,.5,.5]},
                       'thirdperson_righthand':{'rotation':[75,45,0],'translation':[0,2.5,0],'scale':[.375,.375,.375]}}
    write(MODELS/f'{name}.json',result)
    write(ASSETS/'models/block'/f'{name}.json',result)
    print('EXPORTED',name,'cubes',len(result['elements']),flush=True)
    return {'name':name,'tier':tier,'cubes':len(parts),'faces':sum(len(e['faces']) for e in result['elements']),
            'bounds':[[p['start'],p['end']] for p in parts],'materials':materials,'source':str(MODELS/f'{name}.bbmodel')}

def main():
    MODELS.mkdir(parents=True,exist_ok=True)
    manifest=json.loads((OUT/'texture-manifest.json').read_text(encoding='utf-8'))
    assert all(e['state']=='inspected_for_integration' for e in manifest),'Inspect all textures first'
    for entry in manifest:shutil.copy2(OUT/'textures'/(entry['texture']+'.png'),ASSETS/'textures/block'/(entry['texture']+'.png'))
    b=Bridge();schemas={t['name']:t for t in b.request('tools/list',{})['tools']}
    write(OUT/'blockbench-tools.json',schemas)
    assert 'risky_eval' in schemas, 'Need native per-face texture editing fallback'
    if not (OUT/'preserved-project.bbmodel').exists():
        save_export(b,'project',OUT/'preserved-project.bbmodel')
    models=[]
    for tier in (1,2,3):
        for suffix,parts in [('base',floor('base_top')),('rune',floor('rune_top')),('eye',floor('eye_top')),('eye_lit',floor('eye_lit'))]:
            models.append(model(b,f'array_{tier}_{suffix}',parts,tier))
        for section in ('single','bottom','middle','top'):
            models.append(model(b,f'array_{tier}_pillar_{section}',pillar(tier,section),tier))
        for label in ('base','rune_brick'):
            filename=f'array_{label}_{tier}'
            suffix='base' if label=='base' else 'rune'
            write(ASSETS/'blockstates'/f'{filename}.json',{'variants':{
                f'facing={f}':{'model':f'xiantu:block/array_{tier}_{suffix}','y':angle,'uvlock':False}
                for f,angle in [('north',0),('east',90),('south',180),('west',270)]}})
            write(ASSETS/'models/item'/f'{filename}.json',{'parent':f'xiantu:block/array_{tier}_{suffix}'})
        name=f'array_conduit_pillar_{tier}'
        write(ASSETS/'blockstates'/f'{name}.json',{'variants':{f'section={i}':{'model':f'xiantu:block/array_{tier}_pillar_{section}'} for i,section in enumerate(('single','bottom','middle','top'))}})
        write(ASSETS/'models/item'/f'{name}.json',{'parent':f'xiantu:block/array_{tier}_pillar_single'})
        # Native tier-one registry ID remains formation_core; array_eye_1 is an existing item alias.
        core='formation_core' if tier==1 else f'array_eye_{tier}'
        write(ASSETS/'blockstates'/f'{core}.json',{'variants':{f'lit={lit}':{'model':f'xiantu:block/array_{tier}_eye'+('_lit' if lit=='true' else '')} for lit in ('false','true')}})
        for item in ([core,'array_eye_1'] if tier==1 else [core]):
            write(ASSETS/'models/item'/f'{item}.json',{'parent':f'xiantu:block/array_{tier}_eye'})
    models.append(model(b,'meditation_cushion',cushion()))
    write(ASSETS/'blockstates/meditation_cushion.json',{'variants':{'':{'model':'xiantu:block/meditation_cushion'}}})
    write(ASSETS/'models/item/meditation_cushion.json',{'parent':'xiantu:block/meditation_cushion'})
    write(OUT/'model-manifest.json',models)
    # Shape source is generated from exported model bounds, not independent guessed geometry.
    java=['package com.longju.xiantu.formation;','', 'import net.minecraft.world.level.block.Block;',
          'import net.minecraft.world.phys.shapes.Shapes;','import net.minecraft.world.phys.shapes.VoxelShape;','',
          '/** Generated by tools/build_formation_models.py from Blockbench-authored geometry. */',
          'public final class FormationVisualShapes {','    private FormationVisualShapes() {}',
          '    public static final VoxelShape CUSHION = shape(new double[][] {']
    for a,c in models[-1]['bounds']:java.append('        {'+', '.join(map(str,a+c))+'},')
    java+=['    });','    private static final VoxelShape[][] PILLARS = {']
    for tier in (1,2,3):
        java.append('        {')
        for section in ('single','bottom','middle','top'):
            entry=next(m for m in models if m['name']==f'array_{tier}_pillar_{section}')
            java.append('            shape(new double[][] {')
            for a,c in entry['bounds']:java.append('                {'+', '.join(map(str,a+c))+'},')
            java.append('            }),')
        java.append('        },')
    java+=['    };','    public static VoxelShape pillar(int tier, int section) { return PILLARS[tier-1][section]; }',
           '    private static VoxelShape shape(double[][] boxes) {','        VoxelShape value = Shapes.empty();',
           '        for (double[] b : boxes) value = Shapes.or(value, Block.box(b[0], b[1], b[2], b[3], b[4], b[5]));',
           '        return value.optimize();','    }','}']
    (ROOT/'src/main/java/com/longju/xiantu/formation/FormationVisualShapes.java').write_text('\n'.join(java)+'\n',encoding='utf-8')
    print('COMPLETE',len(models),'editable/runtime models',flush=True)

if __name__=='__main__':main()
