# C6F真实盾兼容：当前验证

2026-10-12，本机接续dev.29，未发布。

单类ShieldRigClientTest已实际通过（/tmp/wildcraft-shield-rig-client-v3.log，29秒）：标准/纤细×左右主手，各自真实DEFEND进入raise/hold/lower、真实原版皮肤/盾、盾面朝目标而头部独立、实际普通盾格挡与耐久、持盾剑一次原生attack/主剑耐久且盾不增加攻击磨损、暂停/GUI取消和恢复。17张专项截图已保存。暂停记录为before=lower_R:0.00071041664，after=native，using=false，locked=false：这是实际输入取消，不能宣称退出的hold还在冻结播放。

第一轮把暂停时仍有hold当作必然条件；第二轮仍要求换段前后都有同一帧，均为测试假设错误。按实际输入取消边界校验后通过，没有为了测试改变举盾/锁定控制。主要16帧在初次失败前已完成，失败不能算全测试通过；v3才是通过记录。

当前增加蓝色/glint/Fuse真实盾显示，并运行7类成品客户端回归：ShieldRig、RigRuntime、Guard、Combat、Equipment、Grip、Melee。尚在执行，不先冒称通过。最终构建、服务端与dev.30安装包归档待完成后更新。

## dev.30最终实际验证

EquipmentClock新独立反馈（120/10FPS）、7类盾相关成品客户端全部通过（/tmp/wildcraft-c6f-clock-regression-final.log，3分57秒）。蓝色/glint/Fuse真实盾同模型已目视与实际GPU检查；保持原模型/图案/光效/附着，不复制库存。原EquipmentClientTest1.2秒5TPS条件未放宽，暂停、GUI、原生使用与备件检查原样通过。计时修复和失败证据详见C6F-CLOCK-REGRESSION.md。

独立runDatagen后build及既有纯规则、125/125服务端实际通过。资源45用途ID/4底部变体/2107源记录/57副本保持，原件与dev29旧JAR/源资源保存。此批8个客户端类；同本轮dev29的20类合并为22个不同类，不冒称全部22在这次再次运行。

本机JAR/sources JAR与14剪辑私有资源快照位于development-assets/local-only/dev30，完整身份与SHA见development-assets/Wildcraft-dev30-local-manifest.json；都含混合来源玩家派生数据，仅本机。盔甲、移动举盾、主手盾、非锁定举盾与其余核心动作/第一人称正式源稿仍未完成，既有原生路径保留。
