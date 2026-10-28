# AGENTS.md — 轮椅Mod 重写树 (wheelchair-next 0.3.x)

> **定位**：MC 1.7.10 + Forge 10.13.4.1614 作弊/便利向模组，从零重写树。旧树全量归档于 `..\wheelchair-next-archive-20261024`（旧 AGENTS.md、测试套件、NEI 集成都在那里，只读参考）。**个人项目：不写工作日志/周报**，知识沉淀在本文件。
> **本树无版本控制**——归档目录是唯一回滚点。

## 跑起来 / 构建

```powershell
cmd /c "gradlew.bat compileJava --offline --console=plain"   # 编译（判据=BUILD SUCCESSFUL）
cmd /c "gradlew.bat runClient --offline --console=plain"     # dev 客户端（首启约 90s 到主菜单）
```
- 编译报错是 GBK 乱码：`> %TEMP%\wcerr.txt` 重定向后 `Get-Content -Encoding Default` 读
- **等待铁律：单次等待 ≤5 秒**，长任务 5 秒探测多轮；就绪判据=日志 `Sound engine started`
- JDK8=`scoop\apps\temurin8-jdk\current`（JAVA_HOME 已设）；`-source 1.6`：无钻石/多 catch/数字下划线
- 源码读写只用 read/edit/write 工具（PS5.1 Get/Set-Content 会毁中文编码）；新 .ps1 纯 ASCII

## 技术栈与结构

- ForgeGradle 1.2 (com.anatawa12 fork)、`libs/tinypinyin-2.0.3.jar`（拼音搜索，embed 进 jar）、`libs/unimixins-all-1.7.10-0.2.1.jar`（mixin 框架，dev 依赖不打包，运行时靠环境提供）
- `client/GuiStoragePage` 存储终端唯一 GUI（动态行数 rowsFor、vanilla 槽循环渲染、自绘按钮/滚动条/流体图标、BigCountRenderItem 全局数字缩写）
- `client/SlotVirtual`+`VirtualSlotHost`：网格物品由 vanilla drawScreen 槽循环渲染（与背包同路径=颜色/高亮天然一致）；点击全在 GUI 拦截，**永不落入 vanilla 槽协议**
- `inventory/ContainerStoragePage`：46 真实槽（合成带 10+背包 36）+ 拆解带 10；SlotBig（int max 堆叠）；客户端容器尾部追加虚拟槽
- `network/`：PacketOpenTerminal(0)/PacketStorageAction(1)/PacketStorageSync(2)，discriminator 3+ 预留；netty 线程一切工作必须过 ServerTaskQueue/ClientTaskQueue
- `mixin/`：MixinPacketBuffer（网络数量 VarInt 扩展）+ MixinItemStack（NBT Count int）+ **WheelchairMixinPlugin 让位**（检测到 InfiniStack 类则全部不注入——同装不冲突）
- `value/`：StorageData（counts/resources LinkedHashMap、quantify-first）、RecipeIndex（反合成索引；wildcard 材料用 `getSubItems(tabAllSearch)` 展开变体）
- `prototype/terminal.html`：**唯一规格**，CSS px = GUI px × 3；原型未定稿不落游戏

## 硬红线（违反即事故）

- MCP 名/Forge API 不凭记忆写——javap/forgeSrc 源码实证（本会话实证过：getFluidForFilledItem、getSubBlocks 的 tab 判定、GuiIngame.itemRenderer 是 static final 等）
- 服务端改 cursor 后必须 `p.updateHeldItem()`；一切玩家可控数值入口钳制上限
- 拆解输出槽 `isItemValid=false`+slotClick 白名单+takeIngredient 从**配方**聚合重算（三重防复制，结构不可绕）
- 批量合成必须在**单次 transferStackInSlot 内循环**完成（vanilla retrySlotClick 递归在 int max 材料下栈溢出，实证崩溃过）
- 布局常量集中在 GuiStoragePage 头部（bandTop=54+rows*18 派生链，容器同公式同步）

## 现状（2026-10-25）与下一步

已交付：存储网格（存/取/搜索含拼音/排序四模式/过滤/NBT/滚动）、合成带（shift 批量合成+‹›配方冲突切换）、拆解带（变体循环/批量/全材料入背包）、流体（倒桶/空手借桶盛水/图标/mB 单位）、经验瓶存经验、附魔页（目标槽+staged 数字框批量应用/互斥 Shift 强制/存取经验/突破上限）、大堆叠（int max 仅原可堆叠物+SlotBig+mixin 网络/NBT/背包+数字缩写 K/M/G）、手杖（挖掘/六形状/挖法三档/**顶模式三态：普通/连锁挖矿/设置电力，Shift+右键循环+物品名后缀 (连锁)/(电力)+手杖页模式 chip**/连锁采集）、能量三测试方块（电池/无限源/拉电销毁，RF+EU 双协议）、debugfill/decontest/mark/wce/wcp 命令、O 键开窗；**能源标记系统**（EnergyMarkHub 全局单份 WorldSavedData：手杖右键标机器/再点取消、Shift+右键切存⇄取、机器拆除自动取消、存=extract/offer+draw 入池、取=receive/inject 爬坡 32×2（电池律防炸机）、逐机速率遥测 20t 窗口、世界内标记渲染（存=呼吸半透明绿、取=恒定半透明红+线框）、能源页 GUI guiId 6（电量+行列表：色块+名+协议徽标+坐标+红字速率/t+X 取消、滚轮滚动）、PacketEnergySync(8)/PacketEnergyAction(9) 镜像广播 20t 心跳、rail 三页签（存储/附魔/能源）+底部（手杖/设置）四 GUI 统一）。

未完：‹› 合成模式外其他页未建；NEI 联动未迁；旧树测试套件（tools/104 点）未迁；**EU 第三方机器标记（IC2 电炉/打粉机 inject）已留手测**（dev 辅助命令 setblock IC2 机器名未定位；代码路径与电池同构+接口类身份已证）；手杖贴图 24×24 手持视角未调；电池配方未重设计落码；机器配置独立页已砍（方向/速率全部并入能源页+手杖手势）。

## 教训（2026-10-25）
- **TE 必须 GameRegistry.registerTileEntity**：不注册 FML 的 writeToNBT 钩子抛 "missing a mapping"（generator 块保存时炸）——generator/sink/battery 三 TE 已补
- **runClient 锁 build/libs jar**：dev 客户端跑着时 `gradlew build` 的 reobf 阶段 FileSystemException——build 前先杀客户端
- python heredoc 写 bat：路径 `\natives` 的 `\n` 在 python 源码里变真换行切断 bat 行——用 os.path.join 拼，不手写反斜杠
