> 本机历史规格或验收；公开版不含许可受限动画数据。本文的本机包/源稿/截图链接不是公开下载，也不是公开版重新执行结果。

# C6AY 原生鞘翅下的剑腰侧动作

基线dev72/5d111fc5，15类15m38s/530图已正式归档。先实现真实CHEST ELYTRA、其他三槽空、静止平地、两体型左右主手、空副手或盾的剑腰侧取收；只有SwordTransitionRig门禁扩展，其他武器仍保留原路。本批不新增源剪辑，七JSON与401/FP4保持。

本地26.3原版WingsLayer在pushPose后只有Z+.125，再交给EquipmentLayerRenderer；没有parent.body。在该push后附加已限定源body矩阵，保留原版translate、wing setupAnim、纹理优先级、foil和pop。不替换原翼模型/材质或飞行机制。仅实际waist sword frame且真实chestELYTRA生效，无frame/空中/飞行/复杂盔甲均原路。

独立观察原WingsLayer进入/退出parent矩阵、实际ElytraModel submit矩阵与Sampler0、glint、原生翼折叠角，源body同根2e-5；不得用生产计算结果自证。两skin×左右×empty/shield共16，原伤害/耐久/Fuse/瞬时引用归属/10FPS/5TPS/暂停/GUI/攻击/盾使用/换物/受伤退出全部实际验证。新增真实自然跳起和原生鞘翅起飞退出，不改物理或假造flight姿态。其他盔甲负例保留，旧ELYTRA-only负例按新明确范围改为ELYTRA+真实helmet，不重写旧证据。

首切片实际clean后，原SwordWaist/cape、SwordTransition/back、Equipment、Guard、FirstPerson、Glide及适当其他waist回归；独立datagen/build125/采用资产验证；freeze实际源码/JAR/metadata/log/PNG，CRC/SHA及保护文件核对后才正式dev73。源文件及含数据包只本机，不上传、不新增依赖/迁移/采用待审资产。C6整体、所有武器翼兼容、复杂盔甲及人工生存未完成。额度任一余量<=5保存并实际reset+60秒续调。
