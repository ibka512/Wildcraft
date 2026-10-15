# C6AA重锤背部取收候选52

2026-10-13，正式51/e98c616b保持，当前候选52尚未正式标签。已接main MACE默认平地静止无盔甲披风/真实原reference与owner1-3/原.34-.38 Settle。source+Y按原native16px MACE(+x,+y)/normal+Z与第三人称作者raw(.08,.08,.5)解真实模型变换，保留native model1/1/1 scale/UV/glint/Fuse。新增nativeFeedbackReference artMace重载仅本slice调用，旧调用仍false；第一人称MeleeHands .23/.35未改。

32原源根脚头绑定报告与raw grip失败保留，采样时只derive非skin palms current wrist .0625与actual main reference平移，原22皮肤关节/方向/尺寸保持，新增8 clips/身形共259/257选，旧251及所有非clips数据逐值相同。不能把数值方案说成raw源合格，也不称持续双手实握/精确背部抓柄。

首次转换KeyError left_mace_reference因Export palette原filter未含mace_reference，修正显式加入后转换v2成功；v1未写新资源，失败日志保留。实际native客户端27图/1分7秒专项成功：两skin左右主手empty/actual shield16取收、原skin/native items/back sole owner/GPU；真正SubmitNodeStorage/Collection native quad pose main anchor和shaft轴及真carry盾handle握点通过，不以诊断heldPose代GPU。原单次nativehit wear1/Fuse31及source零新增费用、第二hit wear2/Fuse30、暂停/10FPS/5TPS clock、独立头、GUI/新torch/actual use shield/hurt/不同reference spare/ELYTRA waist回旧路通过。

证据development-assets/verification/dev52/mace-v1含27截图/gradle.log/RESULT。独立datagen实际13秒68零写；build/125服务端与资产检查运行结果单独保存，完整35类（新Mace＋原33＋既有WeaponClientTest覆盖强击旧第一人称与投掷）尚未验收。现阶段只candidate，本机混合许可源/派生数据/含数据JAR不上传；其他美术WD保持，无新依赖/真实迁移/发布。
