# C6AC三叉戟地面攻击候选54

2026-10-14正式53/5e7426f6、交接46820bdf保持。已采用trident-attacks/v01 light与heavy两skin/RL/empty-shield原8/身形，runtime275/273选，旧267/全部几何绑定逐值保持，只本机。default flat stationary unarmored/capeless actual main TRIDENT/native phase，原light7、charge16/实际variable hold/strong12映射源light0..1与heavy0..0.8/hold0.8/release1.0..1.6，不施加源review .2hold期限；原费用14/native hit-wear-Fuse/投掷/物理/库存/时间保持。

主手原reference/sliding曲线不重锚到wrist，actual raw逻辑1.10与pole range .125..1.6875/native SpecialModel原flip/尺寸/UV/glint/Fuse保持；真main/current wrist .0625/empty support palm横向误差≤.015，沿真实pole滑动y1.10..1.60，作者22皮肤关节/根/reference方向尺寸保持，只非skin palms与真carry盾handle适配，头独立。真TridentSpecialRenderer.submit parent/ItemStackRenderState身份与嵌套还原/原raw model逻辑握位轴及真正杆两掌contact正计数/source非head矩阵/真实皮肤与carry盾handle逐项验证，不能拿固定1.10 grasp错误标准抹掉刺击。

实际v1/v2在测试世界初始化wait target失败：高处120目标、玩家仍默认-60，未执行攻击/未验证renderer；v3修正先实际teleport进入追踪范围，8 light/29秒通过。v4原16tick prepare/8tick实际hold不自动释放/native strong14cost/once wear-Fuse8，共24图65秒通过。v5严格要求pause surviving phase失败，37字段测试边界只到25图；最小pause probe23秒记录before SWING age2/time.2857/gameTime82/serverPausedfalse→after IDLE/sourcefalse/gameTime82/serverPausedtrue，原输入取消而非新源时钟漂移。复用原AirMelee规则，冻结仍有效阶段或honor原CANCEL，禁止冻住过期姿态；probe-v2 18秒通过，无生产时钟改动，DEBUG探针已清。

v6/v7到33图后自然jump等待失败：Jump用pressKey点击队列而非实际held input，改为holdKey直到原native离地后释放；同时setup显式清空装备并等待native chest empty，避免下一fixture继承ELYTRA（该独立清理不是生产修改）。最终v8实际1分17秒37图通过：普通8/charge-release16、10FPS/原5TPS7世界刻进度、真实pause取消或冻结、独立头、GUI关闭不重播/新torch/真实occupied/hurt/ELTYRA腰旧路、自然跳跃/低精力原取消零hit-wear-Fuse、native MAIN trident use取消准备/原一次投掷与库存归属。既有恢复/强击/伤害/费用均不改变。

尚未完整37类回归、正式54标签或包归档；独立datagen/build/125服务端及45-4-2107-57继续实际验证。source root/foot动态contact数字报告不是游戏physics完成；空中三叉戟/其余长兵器攻击、复杂/高级/盔甲/正式第一人称及C6整体未完。私有原源/派生数据/含数据JAR不上传，未新增生产依赖/真实迁移，待审13第一人称或另6补齐不采用，其他美术WD/全部历史保持。
