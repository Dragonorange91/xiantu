"""Record the observed boundaries of the 2026-10-11 formation art batch."""
from build_paths import review_font
from pathlib import Path
import json
from PIL import Image, ImageDraw, ImageFont

ROOT=Path(__file__).resolve().parents[1]
ART=ROOT/'asset_sources/formation-20261011'
QA=ROOT/'work/qa/formation-visuals-20261011'
DOC=(Path.home() / 'Documents/Codex/2026-10-05/f/outputs')

status=json.loads((QA/'status.json').read_text(encoding='utf-8'))
status.update(client_appearance_review='tier1 and round cushion observed; tier2/3 full-scene review pending',
              rune_right_click_gui=True, detected_manual_input=True, desktop_input_stopped=True,
              runtime_models=25, runtime_textures=31, new_item_ids=0,
              gallery_centers=[[7168,-61,1408],[7168,-61,1472],[7168,-61,1536]],
              accidental_gallery_edit='7168,-61,1410 rune brick restored; GUI confirmed structure complete',
              game_view_mode='F11 fullscreen left active after manual-input detection; Creative/Peaceful unchanged')
(QA/'status.json').write_text(json.dumps(status,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
for name in ('texture-manifest.json','production-plan.json'):
    p=ART/name; data=json.loads(p.read_text(encoding='utf-8'))
    if isinstance(data,list):
        for item in data:item['state']='generated_inspected_integrated_deployed; user_review_pending'
    else:data['state']='generated_inspected_integrated_deployed; user_review_pending'
    p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

note='''

## 2026-10-11 聚灵阵与蒲团素材完成记录（当前批次）

已实现：31张64×64材质、25份Blockbench桌面MCP原生Java模型及可编辑bbmodel。四份1024母图按先全批贴图、后全批建模的顺序制作。三级阵基、阵纹砖、待机/运行阵眼各3层实厚方块；四方阵柱依同级邻接形成独立柱／底段／中段／顶段。柱碰撞随模型收窄；圆形草编蒲团直径1格、高0.3格，顶面环形编织纹，侧面辫边，底面独立材质。布边备选材质已保存，当前草编模型不使用红布边。静态区块烘焙，不接glTF动态渲染。完整结构将必需环纹朝向阵心；结构半径、材料数、双槽供能与空阵耗能不变。

样板：一级青云去纹砖／雕花砖十字铺地，二级云纹玉石／雕花玉石八向铺地，三级玄岩去纹阵石／云纹玉石同心与八向铺地；四方入口各3格。装饰不加入必需结构或配方，不新增注册ID。搭建说明asset_sources/formation-20261011/三级聚灵阵搭建说明.md。

已构建：Java17、Forge47.4.26最终build/check/qaJar离线PASS25秒，聚灵阵数学回归45项及全部现有check通过。已部署正式与QA包，31贴图及25模型的字节内容逐一比对PCL实际JAR一致，SHA256见work/qa/formation-visuals-20261011/deployment.json；原两包已备份。正式68FFF71EA3223C690556B238988B57599C118AAC4C9B6A2EA958A1CC35B6801D，QA F608864BE3CBE9B538BF95BA03B535D5686171CA866FA85203236CB85777C3CD。

已实测：创造和平超平坦Xiantu-QA-Fresh-20261010，formation_balance201项PASS，包含三级柱段生成、拆顶重连、薄柱真实碰撞、0.3格蒲团/空角、环纹朝内及原供能、修炼、危险确认回归。实际右键一级阵纹砖打开双槽GUI，显示结构完整；一级场景与圆形蒲团已观察。新样板中心7168/-61/1408、1472、1536，间距64格，不与旧结构融合。误点破坏的7168/-61/1410阵纹砖已恢复，随后GUI确认结构完整。

未验证：二、三级完整场景尚未完成客户端整体外观巡检，玩家外观验收待定。工具检测到手动输入，已停止游戏输入，不自行关闭或继续操控；F11全屏暂保留，游戏仍创造和平。模型与结构已完成，不把未巡检的外观写成玩家验收。旧功法GUIfixture已恢复原角色；其恢复消息FAIL不作为完整10秒成功证据，玩家此前明确确认10秒长按正常才是本次手动证据。

工具修复：旧Blockbench桥缺set_cube_uv，用已发现risky_eval原生Undo事务修改面纹理；必须IIFE包裹return，并检查普通文本错误和导出纹理引用。顶面赋值修正版已重导出、实测。该问题及GLFW游戏视窗标题栏点击风险已同步技能；两份skill以UTF-8 quick_validate通过。证据目录work/qa/formation-visuals-20261011/；素材源、模型、预览、manifest在asset_sources/formation-20261011/。不开始其他模块。
'''
p=ROOT/'仙途工程交接文档.md';s=p.read_text(encoding='utf-8')
if '聚灵阵与蒲团素材完成记录（当前批次）' not in s:p.write_text(s+note,encoding='utf-8')
p=DOC/'仙途模组_完整素材制作清单.md';s=p.read_text(encoding='utf-8')
s=s.replace('| 图像API1024×1024；各材质格独立缩至64×64 | 生成中，未验收 |','| 图像API1024×1024；各材质格独立缩至64×64 | 已生成、检查、接入、部署；待玩家验收 |')
s=s.replace('| 图像API1024×1024，各面64×64 | 生成中，未验收 |','| 图像API1024×1024，各面64×64 | 已生成、检查、接入、部署；布边为未应用备选 |')
s=s.replace('| 按实际导出清单 | Blockbench桌面MCP，静态Java JSON及可编辑bbmodel | 等待全部贴图检查 |','| 25份模型，两种文件各25份 | Blockbench桌面MCP，静态Java JSON及可编辑bbmodel | 已导出、检查、接入、部署 |')
s=s.replace('| 待模型完成 |','| 已生成3套；201项实机回归通过；二三级整体外观巡检待定 |')
if 'formation-visuals-20261011/status.json' not in s:s+='\n本批状态：素材制作完成，待玩家外观验收；最终源码/资源部署和201项实机通过。一级及蒲团实际可见，阵纹砖右键GUI已验证；二、三级整体外观巡检因检测到玩家输入而停止。准确边界见工程work/qa/formation-visuals-20261011/status.json。\n'
p.write_text(s,encoding='utf-8')
p=DOC/'仙途模组大纲_功能模块实施版.md';s=p.read_text(encoding='utf-8')
if '聚灵阵外观实施补记：31张' not in s:s+='\n聚灵阵外观实施补记：31张64px纹理和25份静态模型已接入部署；完整圆形铺地为可选建筑样板。formation_balance201项实机通过，一级阵纹GUI／蒲团外观已检查，二、三级整体外观巡检及玩家验收待定。当前交接见work/qa/formation-visuals-20261011/。\n'
p.write_text(s,encoding='utf-8')

canvas=Image.new('RGB',(1320,450),(18,21,25));draw=ImageDraw.Draw(canvas)
font=review_font(20)
for index,(file,label) in enumerate([('blockbench-tier1-pillar.png','一级 · 青灰铜纹'),('blockbench-tier2-pillar.png','二级 · 赤髓赤木'),('blockbench-tier3-pillar.png','三级 · 玄铁银青'),('blockbench-cushion.png','圆形草编蒲团')]):
    im=Image.open(ART/file).convert('RGB');box=im.getbbox();im=im.crop(box);im.thumbnail((310,360))
    canvas.paste(im,(index*330+(330-im.width)//2,24+(360-im.height)//2));draw.text((index*330+40,403),label,font=font,fill=(235,230,218))
canvas.save(ART/'模型预览.png')
print('Updated outline/checklist/handoff/status and model preview')
