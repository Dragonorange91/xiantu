# 仙途模组联动接口文档

适用版本：仙途 0.1.0，Minecraft Java 1.20.1，Forge 47.4.26，Java 17。包名前缀为 `com.longju.xiantu`，模组 ID 为 `xiantu`。

本文根据当前源码核对，区分可调用 Java 方法、Forge 事件、标签扩展与仅供阅读的内部目录。当前没有独立的稳定 `xiantu-api` 发行包；下列 Java 类型来自完整模组，升级仙途时需要重新核对签名。没有独立 HTTP、REST、IMC 或跨版本网络协议接口。

## 接入导航

| 需求 | 入口 | 限制 |
| --- | --- | --- |
| 查询玩家境界、灵力、修为、灵根 | `CultivationCapability.DATA` / `CultivationData` | 服务端真实状态；客户端只能读同步快照 |
| 发放修为、扣除灵力 | `CultivationData.addProgress` / `setPower`，随后同步 | 仅服务端线程；需要自行校验奖励与消耗 |
| 请求突破 | `BreakthroughService.tryBreakthrough` | 保留功法、心魔、天劫和冷却；返回值不是异步突破完成通知 |
| 查询主功法及加成 | `GongfaService.active` / `GongfaRegistry.get` | 功法目录固定，不支持外部动态注册 |
| 读取法术、通过现有槽位施法 | `SpellCatalog` / `SpellService.begin` | 走已有学习、槽位、灵力和冷却校验 |
| 接收法术/装备技能通知 | `SpellLifecycleEvent` / `EquipmentSkillEvent` | Forge 总线，只读、不可取消；不在通知中再次结算伤害 |
| 生成有品质的丹药、装备、符箓、妖丹 | 各模块的物品工厂 | 保留对应 NBT、规则与物品类型 |
| 为外部材料启用品质鉴定 | `xiantu:identifiable_materials` 标签 | 被排除的灵石、妖丹及仙途金属仍不参加 |
| 外部武器元素、实体元素亲和 | `elemental_weapons/*` / `elemental_affinity/*` 标签 | 六属性；多属性武器回退物理 |
| 附加元素伤害、抗性、穿透 | `ElementalDamageSource` / `ElementalAttributes` | 不绕过伤害事件链；数值按比例 |
| 外部戒指、项链进入饰品栏 | `accessories/rings` / `accessories/necklaces` 标签 | 仅开放槽位接受，不自动生成属性或 Curios 适配 |
| 查询或消耗区块灵气 | `ChunkAuraData` | 每维度 SavedData，16×16 区块；读取可能创建池记录 |
| 查询妖兽、原版敌对怪物修为 | `BeastRealmService` / `HostileRealmService` | 外部模组所有怪物并不会自动适配 |
| 添加宝箱战利品、普通合成配方 | Forge 全局战利品修改器、标准数据配方 | 炼器/炼丹等固定目录不是 RecipeManager 扩展点 |
| 查询秘境与区域权限 | `InstanceService` / `RuinProtection` | 内部服务，不直接编辑账本或绕过保护 |

## 依赖与侧别

开发时把经过 ForgeGradle 重混淆映射的仙途包加入联动模组：

```groovy
repositories { flatDir { dirs 'libs' } }
dependencies {
    implementation fg.deobf('com.longju.xiantu:xiantu:0.1.0')
}
```

将正式包保存为 `libs/xiantu-0.1.0.jar`。这不是已上线的 Maven 坐标；示例通过本地 `flatDir` 查找。仙途内置 GFBS，客户端运行仍按 README 安装 Photon、TerraBlender 等依赖。

必需联动在你的 `META-INF/mods.toml` 中声明：

```toml
[[dependencies.yourmod]]
modId="xiantu"
mandatory=true
versionRange="[0.1.0,0.2.0)"
ordering="AFTER"
side="BOTH"
```

`yourmod` 换成联动模组自己的 ID。可选联动应将仙途设为可选依赖，并在 `ModList.get().isLoaded("xiantu")` 为真时才加载引用仙途类型的独立兼容类；不要让不存在的类型出现在总会加载的事件订阅类中。

所有状态写入、背包发奖、伤害和世界操作必须在服务端主线程执行。网络处理使用 `enqueueWork`；异步计算完成后交回服务器线程。专用服务器不能加载 `com.longju.xiantu.client` 下的类型。

## 玩家修炼数据

源码：[`CultivationCapability`](../src/main/java/com/longju/xiantu/cultivation/CultivationCapability.java)、[`CultivationData`](../src/main/java/com/longju/xiantu/cultivation/CultivationData.java)、[`RealmRegistry`](../src/main/java/com/longju/xiantu/cultivation/RealmRegistry.java)。

```java
player.getCapability(CultivationCapability.DATA).ifPresent(data -> {
    int major = data.majorRealm();
    int stage = data.stage();
    double power = data.power();
    double cap = data.progressMax();
    boolean fireRoot = data.hasSpiritRoot(SpiritRootType.FIRE);
});
```

| 值 | 含义与单位 |
| --- | --- |
| `majorRealm()` | 0 练气、1 筑基、2 金丹、3 元婴、4 化神、5 炼虚、6 合体、7 大乘、8 渡劫 |
| `stage()` | 从 0 开始；练气 0–8，其他大阶段 0–3 |
| `realm()` | 当前小阶段在全部 41 阶段中的展平序号 0–40，**不是大境界** |
| `minorRealm()` | 兼容字段；接入使用明确的 `stage()` |
| `cultivationUnlocked()` | 是否已开启修炼；不能把未解锁玩家的 `majorRealm()==0` 当成已修炼 |
| `progress()` / `progressMax()` | 修为点数与当前小阶段上限 |
| `power()` / `powerMax()` | 灵力点数；网络中的 milli 字段是另一个单位 |
| `spiritRootCount()` / `hasSpiritRoot(type)` | 灵根数量、指定灵根是否存在 |
| `gongfa()` / `spells()` / `medicine()` | 功法、法术、丹药状态子对象 |
| `copy()` | 数据副本，修改副本不会保存回玩家 |

`RealmRegistry.get(major, stage)`、`indexOf(major, stage)`、`baseHealthExact`、`baseAttackExact` 可读取境界定义。部分读取方法会夹紧越界输入，外部请求仍应先校验，不要依赖夹紧纠正非法权限。

发放修为与扣费示例见 [`XiantuIntegrationExamples.java`](integration/examples/XiantuIntegrationExamples.java)。写入后调用 `CultivationEvents.sync(ServerPlayer)`；该方法会检查境界成就并向该玩家同步修炼状态。法术配装或熟练度变更另需 `SpellService.sync`，不能只同步修炼包。

`addProgress` 忽略非正或非有限数值，并将溢出截断到当前阶段上限，不自动跨阶段存储多余修为。`setPower` 不负责完整的余额/上限业务检查，扣费前自行检查余额，补充灵力时夹紧到 `powerMax()`。

正常突破调用 `BreakthroughService.tryBreakthrough(player)`。方法可能启动延迟结算的心魔/天劫流程，返回 `false` 也不能一概解释为没有启动；不要因返回 `false` 自动重复扣费或重复请求。当前没有独立的公开“突破完成事件”。

管理工具强制改境界可用 `setRealmStage(major, stage)`，之后调用 `GongfaService.refresh(player)` 刷新属性并 `CultivationEvents.sync(player)`；这会绕过自然突破流程，应自行限制权限。旧 `setRealm(int, int)` 的第一个参数是展平序号，第二个参数被忽略，不推荐外部使用。

Capability 的 `LazyOptional` 可能随死亡、换实体而失效，不长期缓存其中对象。让仙途处理自己的 Clone、死亡和存档恢复，不额外挂第二份同名 Capability，不直接改 `ForgeCaps` 序列化字段。

客户端 HUD 可读 `CultivationCapability.clientData()`，它是本地玩家的同步快照，并不包含全部服务端数据、其他玩家信息或最新法术状态。客户端修改该对象不会成为服务器事实。

## 功法、法术与技能事件

源码：[`GongfaService`](../src/main/java/com/longju/xiantu/gongfa/GongfaService.java)、[`SpellService`](../src/main/java/com/longju/xiantu/spell/SpellService.java)、[`SpellData`](../src/main/java/com/longju/xiantu/spell/SpellData.java)。

- `GongfaRegistry.ALL/get(id)`：功法目录；`GongfaService.active(data)`：当前有效主功法，可能为 null。
- `GongfaService.cultivationMultiplier/attackBonus/healthBonus/armorBonus`：按当前功法等级取得加成；部分是比例，不能全部当绝对属性点。
- `SpellCatalog.ALL/find(id)`：当前五种法术 `fire_bolt`、`thunder_strike`、`great_fireball`、`rejuvenation`、`sword_flight`。
- `SpellService.data(player)`：法术状态，缺少 Capability 时抛异常；先检查数据存在。
- `SpellData.knows/slot/mastery/cooldown`：是否学会、槽位内容、熟练等级 0–9、剩余冷却 tick。
- `SpellData.ACTIVE_SLOTS=4`、`PASSIVE_SLOTS=2`；主动槽 0–3，被动槽 4–5。
- `SpellData.configure(revision, ids)` 要求当前版本号、完整六个 ID、已学会、主动/被动匹配且无重复；空槽用空字符串。
- `SpellService.begin(player, slot)`：尝试发动已有主动槽法术，返回空字符串表示请求被接受，非空是拒绝原因；蓄力法术需专门的按下/保持/松开协议，不能用它代替蓄力。
- `SpellService.cancel(player)`：取消当前施法；`damagePreview` 是减免前预览，不是最终伤害。
- `SpellService.cost(player, spell)` 返回 **千分之一灵力点**；扣除灵力时需 `/1000.0`，不要直接当灵力点。
- `SpellData.learn(id)` 是底层写入，不执行完整的最低境界、消耗书本等学习流程。管理发奖才可在自行校验后调用；普通学习通过原有书本界面。

下面两个自定义事件发布到 `MinecraftForge.EVENT_BUS`，不是 MOD 生命周期总线：

| 事件 | 字段 | 阶段 |
| --- | --- | --- |
| `spell.SpellLifecycleEvent` | `caster`、`spellId`、`transaction`、`target`、`phase`、`position`、`direction` | `WINDUP, RELEASE, FLIGHT, HIT, END, CANCEL` |
| `refining.EquipmentSkillEvent` | `owner`、`skill`、`transaction`、`target`、`phase`、`position`、`direction` | `START, RELEASE, HIT, CANCEL, END` |

事件为服务端只读表现通知，没有 `@Cancelable`，不能拦截施法或修改结算。`target` 可为空，`transaction` 用于归属一次施法并去重；同一事务会出现多个阶段/飞行通知。事件字段为 final，但实体引用仍可变，订阅者应遵守只读约定。

目前这两个事件不保证覆盖所有被动、所有武器技能和全部命中类型：法术事件来自 `SpellService`、`GreatFireballService`；装备事件来自 `QitianSkills`。外部表现模组应按实际发出的 ID 处理，不能假定每次普通攻击都有事件。不要手工 `post` 这些事件来造成伤害。

示例订阅类见 [examples](integration/examples/XiantuIntegrationExamples.java)。需要客户端表现时发送联动模组自己的 S2C 包，并校验维度、跟踪范围及事务生命期；不要复用仙途的内部消息编号。

## 物品、阶位、品质与职业

源码：[`MaterialQuality`](../src/main/java/com/longju/xiantu/material/MaterialQuality.java)、[`RefinedEquipment`](../src/main/java/com/longju/xiantu/refining/RefinedEquipment.java)、[`PillCatalog`](../src/main/java/com/longju/xiantu/alchemy/PillCatalog.java)、[`TalismanItems`](../src/main/java/com/longju/xiantu/talisman/TalismanItems.java)。物品通过 `ForgeRegistries.ITEMS` 和完整资源 ID 查询，注册完成前不要 `.get()`。

| 对象 | 建议调用 | 约定 |
| --- | --- | --- |
| 鉴定材料 | `MaterialQuality.eligible/get/set/manufacturingBonus` | 0 未鉴定，1 凡品至 5 极品；`set` 原地修改 ItemStack |
| 丹药 | `PillCatalog.stack(id, quality)`、`quality(stack)` | ID 用不带 `xiantu:` 的目录键；品质 1–5；只对已注册丹药有效 |
| 炼制装备 | `RefiningRecipes.find(id)` → `RefinedEquipment.finish(recipe, quality)` | 保留配方 ID 与品质，保证属性能识别阶位；未知 ID 先检查 null |
| 装备读取 | `RefinedEquipment.recipe/quality/multiplier/usable/infinite` | 品质倍率 1、1.15、1.35、1.6、2；齐天无限耐久由规则判断 |
| 妖丹 | `DemonCoreItem.setData(stack, grade, element)`、`grade/element` | 阶位 1–9，属性 `metal/wood/water/fire/earth/thunder`；不走材料鉴定 |
| 妖兽材料 | `BeastMaterialItem.setData/grade/element` | 阶位 1–9；属性是否保存还取决于具体物品类型 |
| 符纸 | `TalismanItems.paper(tier, quality, count)` | 一至八阶，材料品质 0–5 |
| 成品符箓 | `TalismanItems.symbol(id, tier, quality)` | 已有符 ID、合法阶位、品质 1–5；`rebirth` 仅三阶 |
| 符箓典籍 | `TalismanItems.blueprint(tomeId)` | 现仅三本典籍 ID，不能传旧版单符图纸键 |
| 炼器职业 | `RefiningProgress.level/xp/knows/learn/award` | 图纸使用归一化键；底层 learn 不代替购买/学习条件 |

丹药效果需要走 `MedicineService.consume(player, stack)`，它保留合法性校验、扣除和效果流程；不要为了加效果调用低层 `commitDose`，再从背包重复扣一次丹药。`MedicineService.effect(player,key)` 是当前服务端生效值查询，客户端返回 0。

阶位与品质是两条独立维度。不要把材料的 `xiantu_material_quality` 写成装备/丹药品质；也不要仅给一个普通 ItemStack 改名字冒充炼制成品。

## 标签形式的低耦合接入

你的模组可以在自己的资源中写入 `data/xiantu/tags/...`，以 `replace:false` 合并仙途标签。条目使用你的真实注册 ID；可选条目写成 `{ "id": "othermod:xxx", "required": false }`。示例：

`data/xiantu/tags/items/elemental_weapons/fire.json`：

```json
{
  "replace": false,
  "values": [{"id": "yourmod:flame_sword", "required": false}]
}
```

| 路径（位于 `data/xiantu/tags/`） | 用途 |
| --- | --- |
| `items/elemental_weapons/{element}.json` | 武器元素；同一武器同时落入多个元素标签时回退物理 |
| `entity_types/elemental_affinity/{element}.json` | 非玩家实体元素亲和 |
| `items/identifiable_materials.json` | 材料可鉴定；不自动加入炼器输入目录 |
| `items/accessories/rings.json` | 接受进入两个戒指位 |
| `items/accessories/necklaces.json` | 接受进入一个项链位 |
| `worldgen/biome/aura_rich.json` | 灵气浓郁生物群系，浓度基数 140 |
| `worldgen/biome/aura_sparse.json` | 灵气稀薄生物群系，浓度基数 60；普通为 100，rich 优先 |
| `mob_effect/poison.json` | 仙途丹药的毒性免疫判定识别的外部效果 |

六元素均为 `metal, wood, water, fire, earth, thunder`。玩家元素亲和由自身灵根提供，不从实体标签读取。外部饰品放入槽位不代表仙途会调用外部装备回调；目前没有通用饰品效果注册接口，也没有自动 Curios/Trinkets 桥接。

## 元素伤害与属性

源码：[`ElementalDamageSource`](../src/main/java/com/longju/xiantu/combat/ElementalDamageSource.java)、[`ElementalAttributes`](../src/main/java/com/longju/xiantu/combat/ElementalAttributes.java)、[`ElementalCombat`](../src/main/java/com/longju/xiantu/combat/ElementalCombat.java)。

```java
target.hurt(new ElementalDamageSource(
    caster.damageSources().indirectMagic(caster, caster),
    Element.FIRE, 0.75), 12.0F);
```

`elementalFraction` 范围 0–1，上例代表该次伤害 75% 参加元素计算。伤害源保留原类型、所有者、直接实体与死亡消息；投射物应把真正投射物作为直接实体。其伤害仍通过原版、Forge、仙途的抗性和其他战斗规则，不是无视护甲的伤害。

`ElementalAttributes.RESISTANCE.get(element).get()` 和 `PENETRATION` 是已注册 Attribute；分别为 `xiantu:{element}_resistance`、`xiantu:{element}_penetration`。抗性取值 -0.25–0.80，穿透 0–1，`0.20` 表示 20%，不是 `20`。推荐用自己的固定 UUID 添加/移除修饰符，不覆盖实体基础值；先检查 `getAttribute` 非 null。

五行按金克木、木克土、土克水、水克火、火克金；雷在五行循环之外。默认的普通武器标签判定主要用于原版玩家近战；外部自定义弹射物/技能伤害应显式使用 `ElementalDamageSource`。不要既手算元素倍率又包装来源，以免重复计算。

## 妖兽与其他怪物修为

`BeastRealmService.valid(entity)` 表示实体具有有效的仙途修为记录；只有该值为真时才读取 `realm(entity)`。缺失 NBT 时 `realm()` 默认整数 0，并不证明已初始化。小阶段读 `entity.getPersistentData().getInt(BeastRealmService.STAGE)`，范围仍遵循 `RealmRegistry.STAGE_COUNTS`。

`isBeast(entity)` / `species(entity)` 只识别仙途已有物种名单；`initialize(Mob)` 不会把任意其他模组生物变成仙途妖兽。`HostileRealmService.eligible/boss/initialize` 覆盖原版敌对生物和源码中列出的 Boss，**不是**其他模组任意 Boss 的通用注册接口。`KillCultivationService.registerEncounter` 是特定遭遇目标标记，不是完整的属性、掉落、Boss 血条注册。

外部怪物加入元素亲和标签后可以参加元素战斗，但这不会自动赋予随机境界、妖丹掉落、专属血条或 Boss 技能。当前没有动态物种/Boss 注册表，完整联动需补充适配代码，不能靠写几个持久化 NBT 字段声称完成接入。

## 区块灵气、饰品及区域权限

```java
var aura = ChunkAuraData.get(serverLevel);
var pool = aura.pool(serverLevel, new ChunkPos(position));
double remaining = pool.remaining();
double capacity = pool.capacity;
double concentration = pool.concentration;
```

每维度独立、按原版 16×16 区块。`pool` 是惰性初始化查询，并可能标记 SavedData 变化。`consume(pool,demand)` 返回 `ChunkAuraRules.Allocation` 并保存扣除；返回值含 `remaining/concentration/supply`，供应不足时按分配规则处理。不要自行写 `capacity` 或强制加载区块来刷灵气，也不要重跑 `ChunkAuraService.settleSecond`，它是全区块修炼事务。

`AccessoryInventory(player)` 提供三个槽位：0、1 戒指，2 项链。构造对象读取持久化快照，写入会保存整组槽位；不要长期缓存多个副本互相覆盖，不要在玩家正打开饰品菜单时另建副本写入。通常仅用标签开放物品接受即可。

秘境/洞府状态可读 `InstanceService.isDimension(id)`、`temporary(level)`、`slot(serverLevel)`；`slot` 可能为空。区域操作必须遵守 `InstanceService.mayAct(player,pos)`，普通遗迹还受 `RuinProtection.protectedAt(level,pos)` 约束。`InstanceService.internal`、`trusted` 与账本方法属于内部事务，不应作为跨模组绕过权限的入口。没有公开的通用“注册新秘境主题”事件。

## 数据配方、战利品与不能直接扩展的目录

普通合成、熔炉/高炉、原版战利品表和标签遵循 Forge 1.20.1 数据系统。添加遗迹战利品推荐使用你的 Forge Global Loot Modifier，按目标宝箱 LootTable ID 追加条目，不全量覆盖仙途战利品。当前宝箱表清单见 [`runtime-catalogs.json`](integration/runtime-catalogs.json)，ID 来自实际 `data/xiantu/loot_tables/chests` 文件。

下列数据虽然以 JSON 保存，却由 `Class.getResourceAsStream` 或 Java 固定列表初始化，**不通过服务端数据包重载或 RecipeManager 动态合并**：

| 模块 | 当前数据/源码入口 | 外部扩展现状 |
| --- | --- | --- |
| 炼器 | `data/xiantu/refining/catalog.json` → `RefiningRecipes` | 可读取 find/tier/ALL；无公开 register/add |
| 设施制作 | `data/xiantu/refining/facilities.json` → `FacilityRecipes` | 固定目录，无数据包追加协议 |
| 炼丹 | `data/xiantu/alchemy/recipes.json` → `AlchemyRecipes` | 固定目录；多处校验数量和物品目录 |
| 丹药效果 | `data/xiantu/pills.json` → `PillCatalog` | 初始化时校验 36 种定义，注册对应物品 |
| 法术 | `SpellCatalog.ALL` | Java 固定五种，网络与 GUI 按现有目录 |
| 功法 | `GongfaRegistry.ALL` | Java 固定目录 |
| 制符 | `TalismanCatalog` / `TalismanRecipes` | Java 生成的固定列表 |
| 熔炼 | `SmeltingRecipes.ALL` | 固定六种三至八阶粗矿处理 |
| 商人 | `TownTradeService.OFFERS` | 固定商品目录，无通用交易注入事件 |

不要把同名 JSON 放进数据包后期待它自动生效。要开放新增法术、装备工艺或其他模组材料的炼制路径，需要后续增加注册/重载接口并验证专用服务器同步，本次文档没有虚构这些接口。

已有特殊配方序列化器包括 `xiantu:facility_crafting`、`xiantu:spirit_stone_downcraft`、`xiantu:spirit_soil`；它们有自己的字段和规则，不能视为任意产物的通用炼器序列化器。简单物品配方优先用 `minecraft:crafting_shaped` 等标准类型。

## 可编译示例与核验

[`integration/examples/XiantuIntegrationExamples.java`](integration/examples/XiantuIntegrationExamples.java) 包含读境界、扣灵力、奖励修为、监听技能和创建成品丹药/装备/妖丹的示例。示例没有自动注册为运行中模组，也不会修改玩家存档；联动作者需要接入自己的合法触发点。

[`integration/runtime-catalogs.json`](integration/runtime-catalogs.json) 收录当前真实 ID、目录来源和注册表字段，便于查询。构建开发命令见 README；文档和示例基于当前源码，不承诺其他版本二进制兼容。

在完整开发工程中可用 Gradle 8.8 执行 `gradle verifyIntegrationExamples -I docs/integration/verify-examples.gradle`，按当前模组的真实编译类路径核对示例。该任务仅编译示例，不将其打入正式模组。2026-10-27 已用 Java 17.0.17 编译通过。
