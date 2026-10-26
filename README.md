# 仙途 Xiantu

Minecraft Java 1.20.1 / Forge 47.4.26 修仙模组。包含境界成长、灵根与功法、法术、御剑、炼丹、炼器、符箓、妖兽、城镇、遗迹、秘境和洞府。

## 安装

客户端与服务器安装正式 `xiantu-0.1.0.jar` 及 TerraBlender Forge 1.20.1-3.0.1.11；客户端另装 Photon Forge 1.20.1-1.1.17（包含配套 LDLib）。仙途已内置优化版 GFBS 1.5.1，无需另装 GFBS 或 GeckoLib。`xiantu-qa` 是独立开发测试工具，不随正式服分发。

## 构建

需要 JDK 17、Gradle 8.8、Python 3.11+，以及 `requirements-dev.txt` 中的 Python 库。

```powershell
py -3 -m pip install -r requirements-dev.txt
gradle check build qaJar --no-daemon --console=plain
```

使用已安装的 Gradle 8.8。保留的历史 wrapper 为 8.6，不作为当前构建入口。Windows 默认通过 `py -3` 执行 Python；其他系统可传 `-PpythonCommand=python3`。

将以下依赖放入 `libs`：

| 文件 | 用途 |
| --- | --- |
| `gfbs_gltf-1.5.1-all.jar` | 官方 GFBS 编译 API |
| `gfbs_gltf-1.5.1-xiantu-optimized.jar` | 固定散列的内置运行库 |
| `photon-forge-1.20.1-1.1.17.jar` | 客户端特效编译 API |
| `ldlib-forge-1.20.1-1.0.42.jar` | Photon 配套 API |

TerraBlender 从 Forge Maven 解析。GFBS 修改源码保存在 `third_party/gfbs`，上游及许可见 `THIRD_PARTY_NOTICES.md`。内置运行库必须通过 `tools/verify_gfbs_runtime.py --source-only` 的固定散列校验，源码重建出的包不能未经验证直接替换。

部分资源检查读取 ForgeGradle 下载的原版 `client.jar`、`client-extra.jar`。默认从 `GRADLE_USER_HOME` 查找，也可设置 `XIANTU_MINECRAFT_REFERENCE_DIR` 指向其目录。资源源文件位于 `asset_sources`，运行资源位于 `src/main/resources`；二者需在完整本地工程中存在才能完成全部检查和打包。

产物位于 `build/libs`。自动检查通过不等于所有存档、多人环境和客户端视觉表现均已验证。

## 目录与开源范围

- `src/main/java`：模组代码。
- `src/test`：自动回归检查。
- `src/qa`：独立测试模组。
- `tools`：构建、资源校验及素材制作脚本。
- `third_party/gfbs`：保留原许可的优化版 GFBS 源码。
- `asset_sources`：公开的美术源文件及校验基线（Git LFS）。
- `docs/API.md`：模组联动接口总览与可编译示例。
- `docs/integration`：接口示例、真实目录 ID 索引。

原创代码采用 MIT。运行素材、美术源文件和资源清单均纳入公开范围，素材的再使用权利仍见 `ASSET_LICENSE.md` 与 `THIRD_PARTY_NOTICES.md`。历史测试记录、日志、缓存及外部依赖二进制不入仓库。第三方部分不由本项目重新授权。

拉取仓库时安装 Git LFS 并执行 `git lfs pull`，不要拿 LFS 指针文件当作完整模型或贴图。源码素材体积较大；仅安装游玩请使用正式发行 JAR。构建仍需准备 `libs` 表内依赖。

联动开发请从 [接口文档](docs/API.md) 开始；公开内容清单与清理说明见 [OPEN_SOURCE_CONTENTS](docs/OPEN_SOURCE_CONTENTS.md)。
