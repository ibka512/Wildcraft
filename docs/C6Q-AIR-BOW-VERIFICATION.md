# C6Q空中左握弓本机dev42限定验收

2026-10-13，feature/adopted-art-runtime，正式源码b25dea4/tag v0.1.0-dev.42+mc26.3，实际回归源码c922a59；完整24类16分9秒/786图通过，专项v9 66秒29图，正式本机42已具备归档条件。未发布/上传。

已采用bow-air/v01，两变体/8源、22骨/28引用/12网格与当前底盘绑定、皮肤网格完全相等，根固定；源previewLift .65未加入玩家位置或视觉根。GLB draw/cancel1.90秒与metadata1.90333秒记录；仅每变体draw/hold新增2，共171/169可选，原169/几何/绑定逐键保持；参考弓mesh/morph/preview/long release-cancel不导入，所有数据混合许可本机。

源子段1.275–1.90真实拉弦手距逆表读native getTicksUsingItem/20，保持原FocusTime有效补偿；实际菜单探针120FPS后standard .0035084276/slim .003509181通过，仅源读取证明。原20刻蓄力、13/18刻原生pulling_0/1/2、实际原BOW尺寸/display/UV/glint/木握位/弦箭尾由原模型负责，源原段长右臂适配误差约3e-7；身体/根/骨盆/腿/持弓左臂保持源，头为当时原生独立瞄准。正式第一人称仍旧路，不将程序适配冒新动画资产。

AirBowRig只真实LEFT使用臂mainLEFT或空main+offbow/mainRIGHT、另一手真空、默认无盔甲/披风/可用腾空姿态，实际use归属与当时玩家位置；没有写位置/速度/碰撞/重力、输入锁、时间租约、射箭/伤害/库存。真实停止use立即交出，落地交ground/当前原生；不强改主手、转移物品或套用长返回。无合格源/复杂状态保持原路。

## 已实际执行

v3 28秒/16图；v6 56秒核心退出；v8 62秒/28图；最终v9 66秒/29图。所有正向案例用实际自然跳跃、原使用键和原集成单人技能，未用NoGravity/虚构air renderState或直接赠Focus。

两皮肤×两真实左臂归属四组合：实际腾空draw/20刻hold、真实三档原sprite、nock/grip/源段长、矩阵保护/原生头、真实皮肤和实际GPU、前后腰线与收腿、根无previewLift。原native5TPS与10/有效实际秒精力成本，真正一箭实体、ammo32→31/wear1、真实停止use后20TPS恢复，无额外箭、费用或免疫。真实10FPS下source按实际进入包络与真实use进度，不跟世界刻拖慢。

manual只拉弓不自动发动，经真实H确认才原lease；disabled由地面预蓄后自然跳起进入合格air（不伪造长悬空），原20TPS且普通拉弓零新增精力。自然落地时air源交出/ground或旧路、旧Focus时间结束恢复20TPS。实际InventoryScreen、原生generic伤害（health真实下降/native hurtTime）、实际盔甲、offbow另一手火把、真实快捷栏火把native取消零arrow/ammo/wear与lease恢复、原生RIGHT用弓（不改主手）旧路、独立look与PauseScreen真实native-use hold时钟冻结通过。

实际G键意图同样交出air：只证明真实攀爬意图优先。使用中的Bow不符合空手合格Climb source，不能把此图说成同时已进入真实登墙；攀爬/滑翔本体旧专项仍回归。实际坐骑/全部维度死亡/装备引用替换等高级生命周期的每一源图形交接没有在本专项覆盖，不能以旧技能功能测试充当这些美术证明。

## 失败和修复

v1/v2发生同一native use中Focus客户端预测被真实采样向后校准，原候选按ticks下降重建进场，造成6/14刻weight0丢Frame。诊断证明原use对象/按键/站姿仍合格；只Air改按实际当前use对象/手/维度/skin归属判进场，正常20→19校准允许准确draw/hold变化、不中断身体，不改Focus预测/原计时。覆盖检查观察失选帧，以真实.18进场+.02容差及全权重后的持续性判断，地面已蓄力跳起不拿旧native tick作为air进场年龄。

v4夹具没有等Mode prefs真实发送回合，准备失败；等待3原tick，没直接写服务端设置。v5受伤等待近战hurt失败，旧MeleeActions在Focus active会清近战hurt（保留旧行为）；源加入原native p.hurtTime守卫，真实受击立即交原身体，不加新公共hurt时钟。v7把不支持RIGHT路径误设为正向source覆盖期待，正确区分支持与原生fallback，未启用右镜像；v8/v9通过。所有失败日志与先前证据保留。

## 剩余与交付

独立datagen68零写、build/纯规则/125必需服务端与45用途/4底部/2107源/57运行副本已通过。完整24类日志与786图已保存development-assets/verification/dev42/full-client-v1；原23类含ground最终native替换基准及所有已有回归通过。最终仅来源文本中性化后build再通过，未改Java或171源姿态。正式42/sources/私有171资源只local-only/dev42/final，CRC/SHA/版本与旧169深比较清单Wildcraft-dev42-local-manifest.json，旧包保持。正式41和40包/私有资源SHA再核对不变，原其他美术WD保留，无新生产依赖、真实迁移或公开上传。

非默认形态/盔甲披风/占手/右握/正式第一人称、所有高级生命周期和C6整体未完成；多人暂缓。下一C6R已采用弓专注薄腿层仅380参考源资格约1.05e-6准备，通过十腿骨/受保护矩阵完全不变，但未运行包络或GPU，不增全身剪辑或改网页示例.15到游戏速度，见C6R规格。
