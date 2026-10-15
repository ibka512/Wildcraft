# C6E连续骨架第一批：实际验证记录

2026-10-12，feature/adopted-art-runtime。本批代码验证完成，正式归档与下一底部机械批次衔接中；没有上传或发布。安装包未归档前不能把本文件视为dev.29发行记录。

## 已执行

- 离线20骨骼试验与22骨骼共同网格转换；6个GLB来源SHA记录，12皮肤网格共1792顶点、22蒙皮骨与6握点，8源剪辑；运行只选左右锁定待机与普通剑单击。
- 20/22两次38帧GPU诊断：真实原版皮肤、空手源动作与原版铁剑握点。试验不冒称正式完整角色动画。
- `runRigRuntimeClientTest`单类专项通过（/tmp/wildcraft-rig-runtime-test-v5.log）：31张清晰截图、标准/纤细×左右主手×袖子开关的实际原生纹理、独立瞄准、实际普通攻击一次原生attack与一次耐久、暂停、GUI、四部位盔甲、盾/火把占手、移动与空中回原有路径。
- 同任务全回归14类成品客户端通过（/tmp/wildcraft-rig-regression.log，5分50秒）：RigRuntime、Equipment、Grip、Spear、Weapon、Reaction、Guard、Melee、Combat、Input、Survival、Focus、CoreArt、BackEquipment。原C6五类武器、第一人称握持/取收、5TPS实际恢复、原生使用和保留资产检查保持。
- 专项与全回归截图/日志保存在`development-assets/verification/continuous-skin/runtime`与`regression`，两组原型诊断保存在`diagnostics`。

## 实际失败与修正

专项首次未等待ActionInput可用，修正夹具等待；锁定后首帧dt为0又立即移除Motion导致无法开始入场，已在正式逻辑修正。截图曾被测试尸壳挡住，重拍时待机目标移至7米，攻击镜头使用背后观察。一次测试错误地将尸壳减伤后的伤害与玩家攻击属性作直接相等比较；改用已有测试专用CounterAttackAudit记录原生调用一次，同时检查实际健康下降与一次耐久。未改变原版伤害规则以迎合测试。上述失败日志保留/tmp，不计为通过。

## 未完成与保留边界

连续皮肤第一批不支持盔甲/可见披风/占用副手/移动/其他动作，保留既有原生路径；盾源动作虽抽取但未运行选择。背负采用同一躯干变换，复杂装备组合仍需单独扩大专项。现有程序第一人称握持不是新第一人称正式源稿。其余已采用核心动作、驾驶/环境、人工手感与多人未完成。源许可有Kevin/Quaternius混合来源链；包含派生数据的本机JAR和源资源不能未经检查公开上传。

服务端125与最终成品检查将在归档前执行，当前没有声称这些检查已在本批重新执行。无新增生产依赖、存档迁移、账号设置或GitHub发布。

## 最终dev.29检查

2026-10-12：分开执行runDatagen后，build与runGameTest均实际通过；125/125服务端（无失败/错误）、所有既有纯规则检查通过。C6E14类与底部7类成品客户端回归有1类Survival重复，合计20个不同客户端类通过。资源核对45用途ID/4底部变体/2107源记录/57运行副本通过，其中风扇4份采用后说明由另存采用状态快照校验，旧清单保持。C6E154张与底部93张回归截图已保存，另保留31/34帧专项与原型诊断。

本机JAR与sources JAR保存development-assets/local-only/dev29，SHA与数据身份见development-assets/Wildcraft-dev29-local-manifest.json。包含混合许可派生玩家数据，仅本机，不上传。本批不是C6整体完成，也未宣称所有底部结构或完整正式第一人称源稿完成。
