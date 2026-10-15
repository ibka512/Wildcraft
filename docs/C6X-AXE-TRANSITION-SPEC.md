# C6X已采用斧取收源资格准备

2026-10-13，正式48/25e4a04b与交接8a16e378保持；当前49候选主AXE限定renderer已接，专项27/125构建通过，完整31待；不是正式49。以下source资格与实施验收分别见PROGRESS。

axe-transitions/v01已采用2026-10-10，22骨，32段：draw .34/stow .38 × back/waist × empty/shield ×实际主斧R/L，原category MELEE0 Settle同reference/新owner1/3即为时间和归属。先默认静止平地无盔甲披风back16；waist旧路径，不能把GLB观察装备或dock挂点代替native布局。

元数据gripLocalOffset -.0625、实际GLB wrist +Y，原继承palm空节点仍旧.125；斧axe_reference原点已是实际.0625握点。须限定新斧段重新派生辅助掌心、实际axe_reference明确别名映射，旧227动作/皮肤/绑定不动。携盾源mount同盾取收的握点(0,.125,.03125)，校准真正原生handle/整体bound center差.03125。保留native原模型大小/UV/材质/glint/Fuse，不称精确背部抓柄/拔入鞘。

32源根/绑定/脚/头旋转资格、默认back16握点在.002以内通过。资格第一轮沿盾默认门槛对全部waist握点也强制.002，测到standard stow_waist_empty_L .002792而失败，原根0/脚1.787e-7/头1.104e-7；waist未选运行，改成明确报告-only，未放宽back门槛。不是全部32握点通过，不覆盖失败证据。

接入须真实原斧成功攻击/操作登记、真实原快捷栏取收、两身形左右主手空或真实携盾、实际skin/axe/盾GPU/握点/原 .34-.38时间及原一次hit/wear2/Fuse免费，source本身不增加attack/use/wear。原攻击/GUI/newitem/hurt/use/复杂优先与原脚接触/原生头、第一末尾衔接验证后再正式版本；高级/waist/正式第一人称继续旧路。不新增依赖/数据迁移/发布上传；混合源/派生资源/含数据JAR只本机，所有历史与其他美术WD保持。
