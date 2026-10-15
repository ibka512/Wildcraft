# C6AB本机dev53弩背部源限定验收

2026-10-14生产/回归9ddd5e5d，完整36类28m 29s/1220实际BUILD SUCCESSFUL，最终专项v3 2分3秒50图、原v2 1分38秒42图、最小native instant use green25秒1图；独立datagen5秒68零写、build9秒/125必需服务端与45用途-4底部-2107源记录-57运行副本检查通过。

真实main CROSSBOW/同RANGED2 reference与即时owner1-3，原.32 Settle/default flat stationary unarmored/capeless，两skin左右main empty或实际carry盾16 unloaded＋16 loaded arrow取收。作者main current wrist .125/reference位置方向保持；actual carry盾非skin palm .0625/原handle偏置适配，不改22皮肤关节、源网格纹理挂点不导入。native16px raw(.90625,.09375,.5)/原sprite平面Y轴与normal+Z/原.9 display尺寸一次、未装填/载箭/载烟花真模型UV/glint尺寸保留。267/265选，旧259与所有非clips字段逐值相同。

原native25tick装填/一次ammo消费、原一次发射wear1及第二wear2/source零新增ammo/伤害/费用、native back actual use记录保持。真实两次F交换和空slot选取后四skin-main火箭载弹组合8图，原一次rocket与wear3/剩余ammo和同reference库存完整保持；没有假库存交换造back事件。sanitized背负继续旧standby，不公开嵌套CHARGED_PROJECTILES；真正手持/库存载弹保持。Fusion不扩弩本体host，仍箭或盾；本轮不假称弩本体Fuse功能。

真正SubmitNodeStorage/Collection原生生成模型quad矩阵及ItemStackRenderState identity/嵌套还原、握点2e-5/轴dot.99999/true native skin-items/back正计数与截图通过，不以诊断heldPose代真实模型提交。原pause/10FPS/5TPS/GUI关闭不重播/新torch/实际use与第二native shot/hurt/不同reference spare/ELYTRA侧腰旧路通过。

实际失败与修复：v1通过32图后instant loaded fire仍active；最小red-v1因旧swinging字段编译失败未进游戏，red-v2 22秒复现引用1/owner1不变、短按后keyDown/using/swing全false且source进度.443仍active。根因是native接受瞬时use可在avatar渲染间完成，帧内轮询漏事件。只在MultiPlayerGameMode.useItem RETURN consumesAction真实接受事件调用既有视觉interrupt，原use/库存/伤害/时间/返回值不改；green25秒同ref/owner下进度1/source=false且正常wear/ammo，原完整v2与v3均通过。DEBUG探针已从源码移除，最小真实回归入口保留；完整36验证共用路径兼容。

A10当前BackTransitions源码SHA因新增nativeUse方法变化，首次资产检查失败已保存旧-新校验；只更新当前runtime_files源码SHA，历史C1-C6D源清单/旧包/美术原件和图片golden不变。其他美术WD保持。正式源码/标签与CRC/SHA见Wildcraft-dev53-local-manifest；混合许可源/数据/包含数据包仅local-only/dev53/final，候选独立，无新生产依赖/真实迁移/发布上传。

已完成仅默认主手背部辅助动作，不称精确背部拔入鞘或持续双手实握；副手盾源、侧腰/复杂/正式第一人称/C6整体仍未完成，13第一人称/另6补齐未采用。下一C6AC三叉戟地面16源只root/bind/动态滑杆contact测量（main y1.10..1.60、横向.0102873/off .0068895、脚origin偏差.000103），私有light草稿，未54 renderer/资源。必须保留作者滑杆，不套equipment固定握点强制重锚；原7tick/强击16tick14精力及native once hit-wear-Fuse逐项实测后才称玩法。
