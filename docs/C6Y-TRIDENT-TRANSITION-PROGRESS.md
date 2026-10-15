# C6Y三叉戟背部源接入进度

2026-10-13，从正式49/3b0187af、本机交接f048d4fb继续。当前参数50是候选，243总241选，旧235/几何/绑定逐值保持，独立datagen已通过；尚未专项/完整回归，不是正式50。

原source根/脚/头/绑定资格32，而back16 raw握点全部未过.002、4未过native .015（最大.0269513）。数值提案back16×121 current source wrist+Y.0625和native raw pole main1.10/support .70/.40轴、zero22关节/方向/尺寸变更已过；原raw失败保存。真正renderer采样后只重设辅助palms/main reference translation到当前作者wrist，不动作者22皮肤关节或reference旋转/scale。

锁定原native TridentSpecialRenderer直接提交TridentModel与glint分支，DEFAULT_TRANSFORMATION=(1,-1,-1)在resolved layer local保留。新增限定trident_equipment_主TRIDENT识别，原真实native item/SpecialModel/Fuse/UV/尺寸保留；nativeFeedback与heldPose使用raw pole1.10、tip0、normal+Z相对实际display/local，目标source+Y与normal-Z，首末12%native原姿态，中段source .34/.38，原owned main/back事件、原生头/脚与其他action优先。

测试只在gametest增加observer：真实ItemInHandLayer parent/root、原ItemStackRenderState层提交（嵌套栈恢复）、原TridentSpecialRenderer.submit的真实native model握点/轴，不能把diagnostic heldPose算native GPU；另有full source/皮肤/盾handle矩阵、16真实native attack登记/快捷栏取收/wear1-Fuse31、第二真实hit wear2-Fuse30与原MAIN native use退出。源副手empty late support是绕行路径，只有末端contact，不冒称持续两手实握/准确背部抓柄；正式第一人称仍原路径且待审13不采用。

专项v1正在实际运行，未验证；源辅助支撑/高级/复杂/waist/正式第一人称/C6整体未完成。原物理/库存/伤害/世界与技能时间保持，无依赖/真实迁移/上传。所有混合数据与JAR只本机，49包/源/全部历史和其他美术WD保持。

v1真实native模型submit握点/轴、16 source取收与核心退出56秒27图已过；v2新增actual native MAIN use>=10刻原一次ThrownTrident/真实库存移出/Counter仍1/no outgoing proxy，56秒28图全部通过。source skin/真实原生特殊模型/盾handle/full source矩阵、真实一次light hit wear1/Fuse31与第二hit wear2/Fuse30保持，空闲副手仍明确辅助伸取，不称全过程实握。datagen已过；build125/完整32/正式50尚待，无新玩法故障。

独立build/125必需服务端及45-4-2107-57实际通过，完整32类尚待，未正式50。cache三已完成run归档8957成员/6551文件逐SHA后释放，空间恢复约4GiB，见CACHE-RECOVERY；当前独立trident测试目录和原件/Git/历史包保持。

完整32类23分25秒1047图实际通过，生产bf778a1d；限定main back辅助源完成，双手常驻/waist/复杂/正式第一人称/C6整体仍未完成，见VERIFICATION。下一Spear只source报告与数值提案，不能当native玩法通过。
