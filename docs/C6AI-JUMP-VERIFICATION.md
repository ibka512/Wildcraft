# C6AI 本机dev60普通跳跃源限定验收

运行源码c4b37cef、完整回归432c94ab，实际43类45m 21s/1780真实PNG通过；专项v2 87秒47图、边界36秒4图，最终build12秒/125全部通过、独立datagen14秒68零写及45/30/2107/57/4资产检查通过。

已采用jump-takeoff-landing/v01三段18剪辑，两身形20→22既有腕部派生；333源/选331、旧315及非clips几何绑定逐值保持。review cycle/previewLift不导入，源400采样实际足部误差.0000893，对比60采样.013047，不改变游戏FPS。

默认无盔甲披风main剑-empty/carry/真实offguard，原跳键正向离地→air→实际onGround选择land；无.2起步等待、额外根升力/控制锁、原jump高度-碰撞-重力-精力-伤害不改。Land从.06双足接触起，余.24艺术回落可被原动作立即接管，真实碰撞顶面不支持双足时原路。Native普通apex1.15..1.35/回120地面、移动和原fall damage通过。

真skin/source根与非head矩阵、独立native头、实际变形脚底/foot origin.125、原native剑生成quads .23握点和轴及真实手末端体积、原盾GPU通过。源20骨palm标记与剑参考.055横差仅辅点差，本批以真hilt在皮肤手部体积/实际生成模型引用轴为准，不冒称22骨.0625掌心或两掌≤.015；原模型/skin保持。

真实GUI/newitem/hurt无免疫、AIR攻击接管/无旧land重播、armor/fall/平台边缘、pause/10FPS/5TPS与旧HOP/攀爬/滑翔完整回归通过。第一测试仅server选槽而client仍slot8，最小红38秒→绿25秒只增加真实slot0按键；第二旧基础断言拒绝合法jump_takeoff/nativeIDLE，23秒红→21秒绿只允许native空或jump_takeoff/air，仍拒绝ground和air中的land；生产不变、DEBUG标签清理，失败证据保留C6AI/verification/dev60。

Native库存/一次damage-wear-Fuse/use-block/技能时间、MACE smash/fall reset与既有第一人称保持。复杂/其他held/正式FP/C6整体未完；下一C6AJ仅private未编译输入交接草稿。所有混合源/数据/JAR与新大证据只本机新外层/项目别名，AppleDouble不计PNG，原件/Git/正式历史包/真实data/其他美术WD保持；无新依赖/真实迁移/上传或新采用待审13/另6。
