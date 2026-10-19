> 本机历史规格或验收；公开版不含许可受限动画数据。本文的本机包/源稿/截图链接不是公开下载，也不是公开版重新执行结果。

# C6BC 副盾披风/原生鞘翅腰侧取出验收

2026-10-19，本机dev76/source fc24678e5339df2a7f07fdbbee477da0b509d7a4/tag v0.1.0-dev.76+mc26.3。只真visible cape或CHEST ELYTRA、其余盔甲空、站立静止、main真空的副盾DRAW2；default-back dev75保持。真实main Bow20tick拉弓、原生一次箭/消耗1ammo/正常最后耐久损坏，恢复原offShield同reference owner3→2；盾之前实际原生格挡登记wear7/Fuse31。物理盾臂始终mainArm反向，不以F/main-off/迟到GUI或交换库存制造back事件。

生产只ShieldTransitionRig一个class移除owner2的waist拒绝；原先其他互斥/mainEmpty/原.32/实际owner/原生物理库存伤害费用use/block/wear/Fuse都不动，off STOW仍拒绝，其他盔甲仍原路。已有401第三人称/4FP七JSON逐SHA不变，旧几何绑定不变，无新增源或资源ID。

首24实际1m30s/80验收PNG通过：cape/ELYTRA×两skin×真实左右主手八基本；各style pause/GUI/use/item/原生FP观察/hurt六项，合计20正例；未损坏Bow与真实helmet各style两项合计4负例。真正source skin/全部源矩阵/独立头/原native shield柄与掌(.015)/轴/唯一owner与一次原结算通过。cape观察真实PlayerCapeModel.texture/body矩阵/原flap；ELYTRA观察真实native model/root/texture优先级/foil与原state折叠/pop；不是把源资格当GPU通过。FP观察仍为原路径，没有采用待审FP。

独立clock4实际53秒/12PNG通过：两style各原生10FPS、5TPS；真实Bow20tick使用/原生磨损损坏之后，source首次full至原.32结束<.9实际秒，没有因world慢刻拉长，没有修改任何生产时长、原生输入或physics。原24测试类字节没有改变，只新增独立clock类，不为添测试重跑24。

随后相关12类9m 13s实际clean，三组共14不同类/530验收PNG：
- dev.wildcraft.test.OffShieldWaistDrawRigClientTest
- dev.wildcraft.test.OffShieldWaistClockClientTest
- dev.wildcraft.test.OffShieldDrawRigClientTest
- dev.wildcraft.test.OffShieldLandingClientProbe
- dev.wildcraft.test.ShieldTransitionRigClientTest
- dev.wildcraft.test.ShieldWaistRigClientTest
- dev.wildcraft.test.ShieldWingsRigClientTest
- dev.wildcraft.test.BowTransitionRigClientTest
- dev.wildcraft.test.CrossbowTransitionRigClientTest
- dev.wildcraft.test.EquipmentClientTest
- dev.wildcraft.test.BackEquipmentClientSmokeTest
- dev.wildcraft.test.GuardClientTest
- dev.wildcraft.test.FirstPersonSwordClientTest
- dev.wildcraft.test.GlideRigClientTest

三组主生产JAR SHA相同；clock/related测试JAR逐成员只有fabric.mod.json不同，首24到clock只新增一个clock class/实际entrypoint，其余class字节相同，全部实际源码/metadata/JAR/日志/每PNG SHA冻结外层2026-10-19-C6BC。不是单次14类或full48，不计世界icon/AppleDouble。旧default14的cape/ELYTRA-only负例按新明确资格改为cape+真实helmet、ELYTRA+真实helmet并等头槽同步；旧dev75原件/截图/源码/JAR不改，没有重设golden。新相关普通landing四例仍有同reference ground draw，但0静止记录、无shield_equipment源，原移动路径保持。

本批无实际游戏失败。独立compile8秒/datagen6秒68零写/build9秒125必需服务端/采用资产45-4-2107-57通过。338生产classes仅ShieldRig不同，四其他美术WD与七私有JSON SHA保持。主包23506190字节 SHA256 `83bb8fa41439a60ca580c589e514b1173958ba0f29d809a12eef14ce8b16175a`；sources23027953字节 SHA256 `0468961a878851a8377715b905353a7246829f5fbb97d9869dd4f5e59041ee1d`，CRC/资源SHA/实际source commit核对后归档本机安装包/dev76/final。清单development-assets/Wildcraft-dev76-local-manifest.json。

仍未完成副盾STOW与其他占用主手形态、复杂盔甲、其他正式FP、C6整体、C7人工生存/平衡/长期性能；没有为全部剪辑修改玩法。C6BB头盔只读研究未实施。无新生产依赖/真实迁移/GitHub上传/待审13FP或另6采用。原件/Git/历史包/proof/真实数据/其他四美术WD和untracked保持。下一更新仅本机试玩交接包，继续留人工表为空，不冒称人工通过。

归档脚本v1曾在复制正式包前停止：文件名前缀同时匹配了bow-break-source报告，错误套用landing负例断言。保留失败脚本/错误记录；改为按报告实际scenario=landing选择，原四个landing断言保持并通过。未修改游戏或重跑稳定验收。
