# WheelchairMod（轮椅Mod）

Minecraft **1.7.10**（Forge 10.13.4.1614）作弊/便利向模组。核心是一套网络式存储终端，围绕它做了合成/拆解、附魔、能源、大堆叠与多功能手杖。

按 **O 键**打开终端（可在按键设置里改），四大页面：存储 / 附魔 / 能源 / 手杖。

## 功能

**存储终端**
- 网络式存储：存入/取出/ Shift 批量，物品数字缩写（K/M/G）
- 搜索：中文拼音首字母（基于 TinyPinyin）、NBT 过滤
- 排序四种模式 + 分类过滤
- 合成带：Shift 批量合成、‹› 切换同名配方冲突
- 拆解带：反合成索引，变体循环、批量拆解
- 流体：桶倒取、空手借桶盛水，mB 单位显示
- 经验瓶：存取经验

**附魔页**
- 目标槽 + 分级数字框批量应用
- 互斥附魔 Shift 强制
- 突破原版等级上限、经验存取

**大堆叠**
- 可堆叠物品上限提升至 int 最大值（背包/箱子/网络全链路）
- K/M/G 数字缩写渲染
- 与 InfiniStack 同装自动让位（检测到即不注入，互不冲突）

**手杖**
- 挖掘 / 六种形状 / 挖法三档
- 顶模式三态：普通 / 连锁挖矿 / 设置电力（Shift+右键循环）
- 连锁采集（自动收拢掉落物）

**能源系统**
- 电池 / 无限源 / 拉电销毁测试方块
- 手杖右键标记任意机器：存（绿）/ 取（红）能量入池
- RF（CoFH）+ EU（IC2）双协议，取电爬坡防炸机
- 世界内标记渲染 + 能源页遥测（逐机速率 /t）

## 安装依赖

| 依赖 | 说明 |
|---|---|
| Minecraft 1.7.10 + Forge 10.13.4.1614 | 平台 |
| UniMixins 0.2.x | mixin 运行时（本模组 jar 仅声明 TweakClass，框架由环境提供） |

## 构建

```bash
./gradlew build        # Linux/macOS
gradlew.bat build      # Windows
```

- 需要 **JDK 8**
- 第三方依赖已 vendored 在 `libs/`（TinyPinyin 构建时直接 embed 进 jar；UniMixins 仅开发期类路径）
- 产物：`build/libs/WheelchairMod-<version>.jar`
- 开发调试：`./gradlew runClient`（运行目录为 `eclipse/`）

## 开发说明

- `-source 1.6` 语法目标，UTF-8 源码
- mixin 注入：网络数量 VarInt、ItemStack NBT Count int、大堆叠背包槽上限
- 调试桥（可选）：`eclipse/config/wheelchair_debug.enable` 开启后 HTTP `127.0.0.1:8123` 提供 /state /screenshot 等端点，供自动化测试
- GUI 规格原型见 `prototype/terminal.html`（CSS px = GUI px × 3）
