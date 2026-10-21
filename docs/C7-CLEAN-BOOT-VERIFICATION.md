> 本机阶段记录；截图、世界、源稿和含私有数据包未公开。本文是历史结果，不是本次公开克隆的执行回执。

# C7 普通新世界与安装包启动预检

2026-10-20，正式dev81/主SHA6bb3de81b0f85b42c447bb854826281a1f68bf36a4ce15eb9d7122f3eb7ec10e与v08冻结包保持，本批仅测试工具。

最小菜单观察器v2实际22秒clean/8PNG：标题原生Singleplayer→CreateWorld普通生存seed20261020/无命令奖励箱→真实E空背包与47槽（1专用伞槽）→真实F8设置/Done→暂停Save and Quit→原生世界列表Play Selected World。没有提供物品、传送、无重力、时间修改或旧研究registry。玩家UUID edacb72d-ea94-3c36-9934-4fb899d7f58c，前后峰值0、精力100/100、空背包/空伞位保持，世界实际Time59→97。专用JAR只有两个class与两metadata/manifest，原wildcraft-test和c7-performance均缺失，注册表中无wildcraft-test实体，fabric.client.gametest属性为null。程序激活按钮回调和绑定队列，不冒称人工硬件输入。

v1只因测试观察器与Mixin放在同一个受保护包，15秒加载红，未进入原菜单操作；改为独立mixin子包，正式生产JAR不变。该失败源码/JAR/日志原样保留。

另实际去掉全部观察器/测试JAR，使用v08中逐SHA相同的主Mod和FabricAPI两个文件普通QuickPlay打开刚生成的测试世界。最初bare-v1用了Loom全解析48个Mod文件（原API聚合和同源单模块），确实无观察器并进入世界，但不能冒称严格两个文件。随后bare-v2仅2个实际addMods路径、主/API SHA分别与包一致、无gametest属性/观察器，实际C7CleanBoot joined the game且无内容缺失页面或客户端异常。API内部自带模块仍由Loader正常解包，不是新增依赖。

无观察器客户端由本机在观察到真实入场后发送SIGTERM有界结束，Loom报143/非clean退出，分别58/47秒；不能把它写成BUILD SUCCESSFUL或正常渲染器退出通过。官方已装Main Shutdown Thread的原生halt路径只读核对，实际停后只读level.dat的Time从原97变为957/754，证明停机钩子保存了已推进世界；没有替此轮补造逐帧/库存或UI退出证据，正常菜单保存重开由前述8图独立覆盖。进程筛选首读误匹配shell文本的结果标UNQUALIFIED保留，正式记录随后按java可执行文件和唯一run目录核对。NBT初读假定旧DayTime键失败，改为可选读取，仅Time作为已验证时钟。

证据：外层2026-10-20-C7-CLEAN-BOOT的执行源码/JAR SHA、v1/v2日志/8图、两次无观察器进程路径和SHA/日志、只读原生保存时钟。主包/私有资源/其他四美术WD保持，无新依赖、迁移、素材采用或上传；没有重跑无变化125或全48。

锁屏下没有前台性能资格，既有六场不作为前台性能结论。人工自然生存/资源成本/手感/长期性能、C6整体和其他未支持资产仍未完成。下一普通时间覆盖真实固定红石充电预算和制造100世界刻的单次结果，先读原FixedEnergy20/刻及Fabricator100刻，不凭预估修改规则。


追加严格API范围（本轮后续实测）：旧v2菜单观察器除主Mod/观察器还使用Loom解析的API单模块，其中可选fabric-gametest-api-v1数据包会写进生成测试世界；它不含wildcraft-test研究实体，但该世界在纯聚合API打开时出现Missing data pack警告（没有内容缺失确认页面）。因此没有把旧22秒样本冒称完全不含可选测试模块。

已改菜单观察器只加载与v08一致的API聚合原件，并每帧断言两个可选gametest模块均未加载；v3-exact-api从全新空目录原生建世界、E/F8/保存列表重开实际28秒8PNG clean，没有Missing data pack警告。旧v2/8图保留，新的8图单独归档，不累加成同一趟16图。随后从这份全新世界复制的bare-v3-pure仅2个运行文件、无观察器/测试模块再次真实入场且无缺包警告，仍是受控SIGTERM143而非正常渲染器退出；Time92→1202实际保存。真正前台性能和人工体验仍未验证。
