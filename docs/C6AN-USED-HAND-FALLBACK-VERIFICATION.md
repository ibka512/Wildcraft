# C6AN 已有原生回退与真实归属验收

2026-10-15。正式本机dev62/01707b84/tag v0.1.0-dev.62+mc26.3保持；只新增gametest观察、可重复任务与文档，没有改生产玩法/版本/337动画资源/私有包，也不认定C6整体完成。最终同一客户端完整64使用场景/128真实PNG实际3m 35s clean通过，非AppleDouble计数；原生皮肤/实际PlayerModel宽窄几何、材质Sampler0、弓及盾/火把/装备真正collector提交可复核。

16种两皮肤/真实mainArm/使用手/另一手盾或火把，地面＋自然jump32场景。12实际弓use各一次箭/ammo/wear；4主盾+副弓由真实native MAIN盾优先，只格挡不射箭/消耗，不能强造副弓use。使用弓的原规则将盾从手部收起，背负需先真实使用登记；用实际slot8空主手→真实off盾use→回slot0弓的玩家操作注册，不直接写记忆/交换库存造取收事件。真库存和view归属、手/背盾native model均保留；其他空手源未越权接入。

8真正支持的AIR draw/hold→server同步另一手火把→原生body/item回退→移除火把，实际原native use继续8、取消0；原有use恢复源/真skin-item GPU，单次原箭/弹药/耐久及Focus lease完整恢复。它是同步装备更新夹具，不冒称玩家在GUI里不断弓重新装备。没有设置使用状态、onGround、无重力或动画Frame。源码支持条件原样保持。

四个铁盔甲槽位32真实使用组合，地面/自然jump64画面，native32单次射箭与盔甲不损、原精力/20TPS/重力通过。原生装备鞘翅8普通起跳使用组合/16画面，真实wings材质和模型、原箭-耗材-耐久/非fallFlying通过；不是鞘翅飞行操控、复杂动画/正式第一人称或披风通过。回退通过不能冒称盔甲/鞘翅retarget已完成。

第一盾prototype31秒红；独立诊断重现同样31秒，真实body/bow/torch有提交而盾手部0。原BackAvatar规则明确使用远程时清盾手模型，BackEquipment最近使用规则清晰；测试先给未用盾再要求手内GPU是错误预期，修为实际举盾登记与真back item scope，不改生产行为。注册盾16矩阵66秒16图、ground-air65秒32图、repository70秒32图通过。Armor观察三次35秒红因原生盔甲也是PlayerModel，不能以类名区分皮肤；真实材质probe确认iron equipment texture，与skin texture区分并读实际Sampler0，native111秒64图通过。两次短编译错误为包私有TextureBinding与无公开textures getter，用测试accessor读取真实字段，不改材质。临时C6AN-PROBE/SHIELD/ARMOR日志清理。

组合首轮native48画面/late8和12箭4格挡已过但Gradle退出NoSpace，SUCCESS和FAILED同时存在，不计clean。完整核验归档已结束C6AL两个自动run，逐成员SHA/源稳定/无句柄及1872已有不可变proof副本一致后只释放生成cache，所有源/Git/真实存档/原件/历史包/proof保持；outer恢复约3.7Gi，组合114秒48图clean过。再本轮最终128全过。原JAR/资源SHA与历史包应继续按manifest核验，禁止新依赖/真实迁移/发布上传/待审13或另6采用。

证据development-assets/verification/c6an/complete-v1与外层2026-10-15-C6AN/verification，逐PNG SHA和错误/专项/最终结果分开。生产源码与mod版本保持；下一仍为未验证披风/生命周期和人工长期生存范围，不加入多人或原排除玩法。

共享gametest观察器完整回归实际48类51m 6s/2021真实PNG clean通过，前述独立128场景纳入原47全部路径。源ba211055/正式mod62和337私有资源不变；测试源码cab38488，完整证据development-assets/verification/c6an/full48-v1。没有因为测试observer新增假游戏版本。
