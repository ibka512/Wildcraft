> 本机历史规格或验收；公开版不含许可受限动画数据。本文的本机包/源稿/截图链接不是公开下载，也不是公开版重新执行结果。

# C6AU 披风腰侧斧取收验收

2026-10-19，本机dev69，source 3323114c5b89c0fcd76892acfec85ef5304e29d7，tag v0.1.0-dev.69+mc26.3。仅原生可见披风、全部盔甲空、静止平地、实际主手斧与空副手/盾，两体型和真实左右主手16取收组合；原category MELEE、同reference即时1↔3和.34/.38实际时间保持。既有披风腰侧剑保持并补清晰正视图。

原axe-transitions/v01腰侧raw失败保留：旧40样本最大.002792，1936重采.003036157947。原.002门槛不变；只把新辅助axe_reference translation派生为该源声明的真实wrist+.0625，参考rotation/scale与22蒙皮骨不变。1936插值样本最大.000376349；实际full-source腕误差最大.00023913149仍满足原标准。每体型345→353第三人称/FP仍4；原345及几何绑定逐值、其余四JSON逐SHA保持。旧源与旧back取收未重写，不冒称raw握点合格或精确拔入鞘。

实际真实skin、native斧generated-quads握位/轴、真实盾柄full-source远端皮肤范围、原生披风模型/纹理/相同body矩阵通过。native参考点最大误差8.940697e-8，披风body矩阵最大3.5762787e-7；完整矩阵标准2e-5不变。主剑/斧物理握位及真实原生物品参考点/斧quads、披风矩阵检查包含起止衔接；盾的source掌心范围只限定full-source，native衔接不称掌心连续握持。

实际一次原版斧攻击登记Counter1/wear2/Fuse32→31、单一真实库存/100精力/盾wear0保持。实际攻击中断仍原2点耐久（下一击wear4），原生盾使用、伤害/换torch、GUI立即取消/关闭不重播、独立头、暂停、10FPS、实际5TPS及真实ELYTRA/胸甲回退通过。未改原物理/库存/伤害/计时/use/block/wear/Fuse/存档或依赖；生产相对68仅AxeTransitionRig与CapeMixin两个class变化，总337。

初轮59秒27张验收截图（另有1世界icon不计）通过。八类定向回归6分钟211张：AxeWaist/AxeTransition/SwordWaist/HeavyMelee/FirstPersonSword/Equipment/BackEquipmentSmoke/Guard。查看截图发现测试HUSK遮挡前视图，于实际登记后把其移到侧面、真实下一击前移回并等同步；此为gametest夹具修正，不改游戏。visual-v2在Gradle解析1秒失败未进入游戏，显式条件修正后进入实际验证。

扩展全blend范围检查的visual-v3 30秒red，v4 24秒独立native矩阵对照：weight .001029834、实际forearm-localY -.1251，与纯原版 -.125一致。失败来自将完整源掌心范围套到保留的native持盾起点，非raw握点门槛失败或生产新错位。保留native衔接，仅分开source掌心与native参考点断言；.002/2e-5没有放宽。临时DEBUG/nativeShieldLocal/observedState已清除，两个red执行包/源/log/crash完整保留。

最终visual-v5两类1m36s、54张无遮挡验收图全部通过；人工查看两侧握斧/持盾图，玩家、真实武器与披风可见。八类211＋两类补验54＝265张最终验收截图，8个不同客户端类，世界icon和AppleDouble不计。旧前视遮挡图保留为原证据，清晰视觉验收以v5为准。两组gametest JAR/metadata/源快照分别冻结，四处测试源变化明确核对；两组生产主JAR SHA完全相同，不冒称同一test JAR。没有重复无变化full48。

独立datagen5秒/68文件零写零移除，build10秒/125必需服务端全部通过，45用途/4底部/2107源记录/57运行副本通过。正式主包22582517字节 SHA256 `4c21ca1aa92fe8e10286f10d1b51c8b17cafc824f8123396398226af6a2cce4b`；sources22105332字节 SHA256 `16319d3f94896b68579d730ce258ee34967fdcb44afc26c2e49b871d9ddca8d1`。7私有资源与许可/CRC/SHA仅本机安装包/dev69/final，工程local-only入口；详见development-assets/Wildcraft-dev69-local-manifest.json及外层2026-10-19-C6AU/RESULT.json。

未完成：其他武器腰侧、副手盾真实取收、盔甲/鞘翅源、其他正式FP/复杂动作、C6整体/C7人工长期生存。其他美术四WD/untracked与历史保持，无迁移/上传。下一C6AV三叉戟/重锤仅原32源资格和计划，不能称70或运行通过。
