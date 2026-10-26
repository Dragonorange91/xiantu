"""Synchronize book appearance decisions and truthful asset completion status."""
from pathlib import Path
import argparse,json
from generate_gongfa_books import ROOT,OUT,catalog,TIERS

DOC=(Path.home() / 'Documents/Codex/2026-10-05/f/outputs')
QA=ROOT/'work/qa/gongfa-books-20261011'
TITLE='2026-10-11 功法线装古籍外观'

def section(path,body):
    content=path.read_text(encoding='utf-8')
    mark='## '+TITLE
    if mark in content:
        before,after=content.split(mark,1)
        next_section=after.find('\n## ')
        content=before.rstrip()+'\n\n'+mark+'\n\n'+body.rstrip()+'\n'+(after[next_section:] if next_section>=0 else '')
    else:content=content.rstrip()+'\n\n'+mark+'\n\n'+body.rstrip()+'\n'
    path.write_text(content,encoding='utf-8')

def main():
    p=argparse.ArgumentParser();p.add_argument('--client-inspected',action='store_true');a=p.parse_args()
    QA.mkdir(parents=True,exist_ok=True)
    manifest=json.loads((OUT/'texture-manifest.json').read_text(encoding='utf-8'))
    models=json.loads((OUT/'model-manifest.json').read_text(encoding='utf-8'))
    validation=json.loads((OUT/'resource-validation.json').read_text(encoding='utf-8'))
    assert len(manifest)==len(models)==24 and validation['books']==24
    deployed=(QA/'deployment.json').exists()
    if a.client_inspected:
        assert deployed
        assert (QA/'inventory-final.png').exists()
        assert (QA/'hand-final.png').exists()
    state=('已制作、检查、建模、构建、部署、客户端外观检查；待玩家验收' if a.client_inspected
           else '已制作、检查、建模、构建'+('、部署' if deployed else '')+'；客户端外观检查和玩家验收待定')
    for e in manifest:e['state']='integrated_deployed_client_inspected_pending_player_acceptance' if a.client_inspected else 'integrated_pending_client_inspection'
    (OUT/'texture-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8')
    status=dict(implemented=True,built=True,deployed=deployed,client_visual_inspected=a.client_inspected,player_accepted=False,
                book_count=24,api_masters=25,runtime_textures=49,static_models=24,editable_models=24,cubes_per_model=4,
                resource_checks=validation['resources'],rules_changed=False,image_api_host='us.777358.xyz')
    (QA/'status.json').write_text(json.dumps(status,ensure_ascii=False,indent=2),encoding='utf-8')
    body='24本入门及主功法统一为中式线装薄册：左侧线装、右侧竖签写本书完整中文名字，保留白色纸页及书皮厚度。入门为靛青布面；黄阶按五行设置简洁印记；玄/地阶采用金锋、藤根、沧澜、离火、山岳、百草等对应纹饰；天阶及以上体现五行归藏、紫府星宿、太清云纹、养神莲纹、八卦衍道、无极玄纹及混沌本源等独立设计。不得以简单换色代替各本身份。阶位仍表示原功法强度，不给功法引入装备品质系统，不修改名称、物品ID、入门资格、10秒盘坐研习、书不消耗或散功代价。\n\n'
    body+='制作使用已配置图像API，24张独立1024×1024封面及1张纸页母图，缩至64×64后统一检查；Blockbench桌面MCP导出24份静态Java JSON及24份可编辑bbmodel。每本4个cuboid，缝线/纹饰由贴图表现，使用原版静态物品烘焙，不使用GLB动态渲染。第一人称显示单独校正封面朝向、比例和位置，避免只显示纸页侧面或书名出屏。源文件asset_sources/gongfa-books-20261011/，工程证据work/qa/gongfa-books-20261011/。\n\n当前状态：'+state+'。'
    section(DOC/'仙途模组大纲_功能模块实施版.md',body)
    table='| 功法 | 阶位 | 独立封面/64px纹理/原生模型 |\n| --- | --- | --- |\n'
    for e in catalog():table+=f"| {e['name']} | {TIERS[e['tier']]} | {state} |\n"
    section(DOC/'仙途模组_完整素材制作清单.md',
        '本批覆盖此前24本书复用9张占位封面的历史状态。按顺序完成全批贴图，再全批模型；不增加物品种类、声音或动画。\n\n'+table+
        '\n共享纸页母图1张及64px运行纹理1张；书脊/背面另用各本已有API封面无字材质裁切，共49张运行贴图。全部高清母图及bbmodel保留在制作目录，不装入运行包。')
    section(ROOT/'仙途工程交接文档.md',body+
        '\n\n生成工具tools/generate_gongfa_books.py（串行、已有母图不重生成）；建模工具tools/build_gongfa_book_models.py（全部纹理检查后才建模）；显示修正tools/revise_gongfa_book_display.py通过已打开的Blockbench项目更新bbmodel与Java导出，保持生成脚本DISPLAY一致；检查tools/verify_gongfa_book_assets.py支持源码或已部署JAR逐字节验证。原24份物品模型备份在素材目录previous-models。静态模型共73份资源，部署前保存退出并校验实际Forge配置目录。不要恢复旧共享封面JSON覆盖新资源。此轮只更换资源，既有功法功能测试不能冒称新外观玩家验收。')
    print('Synchronized outline, asset checklist and handoff:',state)

if __name__=='__main__':main()
