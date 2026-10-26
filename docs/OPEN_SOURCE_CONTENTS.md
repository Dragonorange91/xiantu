# 公开内容清单

工程公开清单于 2026-10-27 更新。

| 目录 | 内容 |
| --- | --- |
| `src/main/java` | 仙途程序源码 |
| `src/main/resources` | 游戏运行资源：模型、贴图、动画、音效、建筑、配方与战利品 |
| `src/test`、`src/qa` | 自动检查与独立 QA 模组源码 |
| `asset_sources` | 美术源文件、制作记录与回归检查所需对照素材 |
| `tools`、`tools/data` | 制作/校验脚本、资源来源与散列清单 |
| `third_party/gfbs` | 优化版 GFBS 源码及其原许可证 |
| `docs/API.md`、`docs/integration` | 联动接口说明、Java 示例与实际目录 ID |

代码许可为 MIT。素材公开供查看和参与本项目开发，独立权利见 [ASSET_LICENSE](../ASSET_LICENSE.md)；第三方依赖及素材见 [THIRD_PARTY_NOTICES](../THIRD_PARTY_NOTICES.md)，不由仙途重新授予许可。

所有美术源文件以及大型运行模型、建筑目录通过 Git LFS 保存。克隆前安装 Git LFS，克隆后执行 `git lfs pull`。GitHub 的普通源码 ZIP 可能只包含 LFS 指针；完整构建需要实际资源文件及 README 中列出的外部依赖。

不公开日志、性能采样、崩溃记录、缓存、测试截图目录、游戏存档、部署备份、账号或外部依赖 JAR。修复前模型与固定散列的输入基线由自动检查使用，予以保留，不属于无用备份。历史素材记录中的个人绝对路径已去除；`local-reference/` 表示未随仓库提供的旧外部文件，并非可执行构建路径。

本轮额外清除了约 103 MB 日志和旧测试输出；之前的大批缓存清理不重复计入。构建时会重新产生本地 `build` 报告，这些文件持续由 `.gitignore` 排除。

构建和联动说明见 [README](../README.md) 与 [API](API.md)。
