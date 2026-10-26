"""Record evidence only after all three final client screenshots exist."""
from build_paths import review_font
from pathlib import Path
import json
from PIL import Image,ImageDraw,ImageFont
ROOT=Path(__file__).resolve().parents[1]
QA=ROOT/'work/qa/formation-traces-20261011'
ART=ROOT/'asset_sources/formation-traces-20261011'
DOC=(Path.home() / 'Documents/Codex/2026-10-05/f/outputs')
TITLE='2026-10-11 聚灵阵连续纹路修正'

def append_once(path,text):
    value=path.read_text(encoding='utf-8')
    if TITLE not in value:path.write_text(value.rstrip()+'\n\n## '+TITLE+'\n\n'+text+'\n',encoding='utf-8')

def main():
    shots=[QA/f'tier-{tier}-final.png' for tier in (1,2,3)]
    assert all(p.exists() for p in shots),'Need final actual client screenshots'
    runtime=(QA/'runtime.log').read_text(encoding='utf-8')
    assert '[Formation balance QA] All 216 assertions passed' in runtime
    build=(QA/'build.log').read_text(encoding='utf-8',errors='replace')
    assert 'Formation regression: 531 passed' in build and 'BUILD SUCCESSFUL' in build
    # Crop/scale actual game captures for a compact comparison, never paint game content.
    sheet=Image.new('RGB',(1500,535),(25,27,30));draw=ImageDraw.Draw(sheet)
    font=review_font(22)
    for tier,path in enumerate(shots,1):
        source=Image.open(path).convert('RGB');w,h=source.size
        half=round(min(w,h)*.26)
        crop=source.crop((w//2-half,h//2-half,w//2+half,h//2+half)).resize((500,500),Image.Resampling.LANCZOS)
        sheet.paste(crop,((tier-1)*500,35));draw.text(((tier-1)*500+15,5),f'{tier}级聚灵阵 · 实机俯视',font=font,fill='white')
    sheet.save(QA/'三级聚灵阵_连续纹路实机.png')
    deploy=json.loads((QA/'deployment.json').read_text(encoding='utf-8'))
    status=dict(implemented=True,built=True,deployed=True,math_assertions=531,runtime_assertions=216,
                resource_byte_comparisons=348,visual_tiers_inspected=[1,2,3],player_accepted=False,
                texture_count=168,native_editable_models=6,geometry_cubes_per_tile=3,**{k:v for k,v in deploy.items() if k.endswith('sha256')})
    (QA/'status.json').write_text(json.dumps(status,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    manifest=json.loads((ART/'texture-manifest.json').read_text(encoding='utf-8'))
    for e in manifest:e['state']='integrated_deployed_client_inspected_pending_player_acceptance'
    (ART/'texture-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    outline=DOC/'仙途模组大纲_功能模块实施版.md'
    value=outline.read_text(encoding='utf-8').replace('阵纹朝阵眼汇流；中央使用草编圆形蒲团',
        '阵基与阵纹砖沿各自圆环连续连接，直线、转角及斜角双端对应；中央使用草编圆形蒲团')
    outline.write_text(value,encoding='utf-8')
    append_once(outline,'三级聚灵阵保留既定圆环坐标、方块数量与材料成本。完整结构的阵基及阵纹砖按圆周顺序自动设置双端纹路，支持八方向侧边/角点相接，形成内外两个闭环；不再把所有直线纹路朝向阵眼。中央符纹仅叠加金属纹样，不以方形石底遮断斜线。独立阵砖保留默认物品外观，拆后补回自动重新连纹；可选装饰铺地不参与结构判定。使用静态区块烘焙，不增加动态绘制或区块票据。531项数学回归、216项实机功能断言通过；最终348份实际PCL资源逐字节比对通过，三级俯视接缝已检查，玩家外观验收待定。')
    append_once(DOC/'仙途模组_完整素材制作清单.md',
        '| 顺序 | 内容 | 数量 | 来源/方式 | 状态 |\n| --- | --- | --- | --- | --- |\n'
        '| 1 | 三级阵基、阵纹砖双端连纹64×64贴图 | 168张 | 复用已生成API1024母图对应的石材、铜/银纹、符纹，UV裁切重排；不重新生图 | 已制作、检查、接入、部署、三级实机检查；待玩家验收 |\n'
        '| 2 | 连纹静态Java模型与bbmodel | 6对 | Blockbench桌面MCP；每块仍3cuboids，168个继承模型只替换顶面 | 已导出、接入、部署 |\n'
        '| 3 | 三级实际圆环接缝与自动重连检查 | 3套 | 六个内外闭环；拆装、旋转、镜像及原双槽功能 | 531项数学与216项实机通过 |\n\n'
        '源目录asset_sources/formation-traces-20261011/；证据work/qa/formation-traces-20261011/。此前31张基础材质、阵柱与蒲团仍复用，本次不新增声音、物品ID、结构用料。')
    append_once(ROOT/'仙途工程交接文档.md',
        '已实现/已构建/已部署：FormationTrace沿固定圆环序列分配双端八方向trace状态；FormationStructure只在完整结构时对齐，修复孤立轴端和斜角断纹。方块rotate/mirror同步转换端口。新工具tools/revise_formation_traces.py先重排168张已验API材质，再通过桌面Blockbench导出6份模型；继承模型只更换顶面，静态烘焙且无额外geometry。\n\n'
        '已实测：build/check/qaJar通过，531项Formation数学回归；formation_balance216项（原功能加双端互通、镜像、旋转、拆装重连）通过。最终像素修订只重打jar/reobfJar，Java字节码与216项实测版本一致；最终348资源与PCL加载JAR逐字节比对，重启后的三级俯视接缝已检查，无相关模型缺失/配方错误。玩家未验收。\n\n'
        f'正式SHA256：{status["formal_sha256"]}；QA：{status["qa_sha256"]}。证据work/qa/formation-traces-20261011/，旧包pre-deploy已保留。原25份模型的柱、阵眼、蒲团不变。勿直接重跑旧build_formation_models.py覆盖连纹blockstates；修复经验已同步xiantu-mod-development、xiantu-asset-workflow技能。')
    print('Recorded implementation/build/deployment/runtime/visual evidence; player acceptance pending.')

if __name__=='__main__':main()
