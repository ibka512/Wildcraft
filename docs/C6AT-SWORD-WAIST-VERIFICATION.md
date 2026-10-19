> 本机历史规格或验收；公开版不含许可受限动画数据。本文的本机包/源稿/截图链接不是公开下载，也不是公开版重新执行结果。

# C6AT 披风腰侧剑取收验收

2026-10-19，本机dev68；源提交1560af88cf83189eacaf31d2cb90080d0fc3ebd0，标签v0.1.0-dev.68+mc26.3。只在可见原生披风、全部盔甲槽空、静止平地、实际主手剑与空副手/盾时使用已采用腰侧源。标准/纤细、真实左右主手、draw/stow共16基础组合实际通过。腰侧布局和库存仍沿原BackEquipment/BackTransitions，源辅助伸手不是精确抓柄拔入鞘。

每体型新增8腰侧剪辑，337→345第三人称、第一人称仍4。原337和全部几何/绑定逐值保持，其余四运行JSON逐SHA不变；原源32段根/脚/头/绑定资格通过，新16候选剪辑的真实远端皮肤手部握位范围另行通过。20骨源不能以派生22骨wrist+.0625定义掌心：早期距离试探仅保留为不适用探针，没有用它认定合格。旧斧waist失败门槛保持。

生产只改SwordTransitionRig及新增CombatRigCapeMixin两个class，总337。披风保留原生模型、纹理与摆动，其body矩阵取同一源躯干；实际模型重建矩阵最大误差2.3841858e-7。真实皮肤/袖层、原生剑与盾模型、即时唯一手/腰侧owner、真实原生剑最终generated-quads握位/轴和盾柄远端手部范围通过；原生物品参考点最大误差8.4293696e-8，源矩阵/头部标准沿原2e-5。

实际一次攻击登记Counter1/wear1/Fuse32→31、单库存、100精力与盾wear0保持；菜单/攻击/原生举盾/换火把/受伤退出、关闭GUI不重播、独立头、实际暂停、10FPS及5TPS原.34/.38实际时间通过。真实鞘翅和胸甲维持原程序路径。本地可解析原版皮肤纹理兼作披风夹具，仅验证原生披风层兼容，不是新增披风美术资产。

第一轮52秒23张验收截图、第二轮53秒27张验收截图资格记录保留；此前24/28个PNG总数各包含一个自动生成的世界icon.png，现已明确剔除，正式215验收截图的计数保持。最终同一生产JAR八类定向回归5分45秒、215有效PNG（不含AppleDouble）clean成功：SwordWaist、原SwordTransition、RigRuntime、FirstPersonSword、Equipment、BackEquipmentSmoke、Guard、HeavyMelee。前两轮不重复计入215，也不称新full48。独立datagen6秒/68文件零写零移除；build12秒/125必需服务端全部通过；采用资产45用途/4底部/2107源记录/57运行副本通过。没有新增游戏失败；归档脚本初次将ExFAT AppleDouble当JSON，过滤边车后实际归档成功，未修改游戏或覆盖旧证据。

正式主包22409372字节，SHA256 `1247530881fc764e21364c7145a9da367a98ba5c0fe527a9cf029ca51a82a6a4`；sources21932220字节，SHA256 `b52e1535721eaaaeddb55a9e2413cadacadbfefd43f107c3fc0d4a9c57796cee`。包/许可/7私有资源快照仅外层本机安装包/dev68/final（工程local-only/dev68/final入口），CRC与逐文件SHA已核对；development-assets/Wildcraft-dev68-local-manifest.json和外层2026-10-19-C6AT/RESULT.json列出实际执行metadata/源SHA/全部PNG和日志。

仍未完成：其他武器腰侧、副手盾取收、盔甲/鞘翅源、复杂动作/其他正式第一人称、C6整体及C7人工长期生存。无新依赖/迁移/GitHub上传，其他美术四WD/untracked与历史原件/包/证据保持。下一C6AU先按保留的斧原腰侧失败与1936样本腕握位提案验证，不把数值提案当运行完成。
