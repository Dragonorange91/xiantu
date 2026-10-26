# 第三方组件与素材

| 组件 | 版本与用途 | 权利与处理 |
| --- | --- | --- |
| GFBS-glTF / LytharaLab | 1.5.1，仙途优化版内置运行库 | MIT；原版权及完整许可证保留在 `third_party/gfbs/LICENSE`、`libs/GFBS-glTF-LICENSE.txt`，发行包内含 `META-INF/LICENSE_GFBS` |
| Minecraft / Mojang | 1.20.1，游戏与构建参考 | 不属于本项目 MIT 范围，不分发游戏 JAR |
| Minecraft Forge | 47.4.26，加载器与开发工具链 | 保留其原许可，通过官方依赖系统获取 |
| TerraBlender / Glitchfiend | 1.20.1-3.0.1.11，生物群系集成 | 外部依赖，不纳入仙途 MIT 授权 |
| Photon、LDLib / Low-Drag-MC | 1.1.17、1.0.42，特效 API | 外部依赖，二进制默认不入代码仓库；适用原作者发布条款 |
| 购买的特效素材 | 部分武器技能图集 | 已集成图集按作者要求纳入工程公开清单；不改授 MIT，来源及散列见 `tools/data/weapon-art-sources.json`，第三方再分发条款仍需核实；原始参考包不包含 |

GFBS 上游：<https://github.com/LytharaLab/GFBS-glTF>。`third_party/gfbs` 包含本地优化修改，未保留嵌套 Git 仓库及构建缓存。GFBS 自带的六个嵌套库保持其原始包与许可，本项目 MIT 不替代这些许可。

Forge：<https://files.minecraftforge.net/>；TerraBlender：<https://github.com/Glitchfiend/TerraBlender>；Photon：<https://github.com/Low-Drag-MC/Photon>；LDLib：<https://github.com/Low-Drag-MC/LDLib>。
