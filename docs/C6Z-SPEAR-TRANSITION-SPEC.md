# C6Z已采用长矛取收源准备

2026-10-13，正式49/3b0187af与本机f048d4fb保持，候选50/bf778a1d完整32类回归中。spear-transitions/v01已采用2026-10-10、sample iron代表材质、22骨32段/default back或waist .34 draw/.38 stow/empty或真正shield/左右主手。只source报告，未Java renderer/运行资源转换/51版本。

32源根/绑定/脚/头资格通过，back16 grip全部未过.002、4未过.015，最大.026951309，和trident同类sampled reference与wrist差，不能直接冒称native抓握合格。需要重算当前source wrist .0625辅助palms/reference translation，保留22皮肤关节/方向/尺寸和原.34/.38 Settle/native instant新owner；目前还没做source51数值/真实GPU验证。

真实MC26.3 hand-model是generated32px texture：main raw(.70,.30,.5)、secondary(.53,.47,.5)、原display scale(1.7,1.7,.85)只保留一次；作者GLB已烘这一比例，owner scale1，不能double scale。source map_raw的game basis=(1.7*(dx+dy)/sqrt2,1.7*(-dx+dy)/sqrt2,.85*dz)，因此main shaft tip沿native(-x,+y)、source+Y，normal raw+Z=>source+Z；不可照抄剑(.23,+x+y)或Trident(raw1.10/normal-Z)。native display/local每手需实际测。七种原生材料32px/alpha轮廓SHA相同只是代表源选择事实，须保留真正当前材质texture/tip与Fuse位置，不用观察mesh/纹理代替。

first qualified main nativeSPEAR原MELEE0同reference owner1/3、default flat stationary unarmored/capeless；真实native piercing attack登记/wear/Fuse/ready冷却和cannotAttack限制、原原生speed/contact/mount/effective-time/C6B路径保持。源free support late绕行不能称持续两手实握或正式第一人称；GUI/use/throw(若适用)/attack/hurt/newitem/armor/waist与其他原native路径优先。必须真七材质左右主手/两skin/native held/back唯一归属与pixel tip/轴/GPU及原一次piercing实际验证，不用CounterAttackAudit一般attack错误统计，不制造source hit。

仅source资格不是玩法完成，51 runtime未启用，副手/waist/复杂/正式第一人称/C6整体仍待。混合源/派生资源/含数据JAR只本机，所有历史/其他美术WD保持，无依赖/迁移/上传，不采用待审13/另6。

数字提案back16×121已实际通过：sampled source wrist .0625重设reference translation、raw spear main(.70,.30,.5)/support(.53,.47,.5)映射source .4087077，22关节/原ref方向/经native1.7/1.7/.85映射逆回后的基底零变更，native握点/轴<1e-12；仅数值/原source raw geometry，真实七材质display/local/GPU/玩法尚待。第一轮错误地拿native model各列norm除scale后直接同source非完全正交浮点矩阵列norm比较而失败（未入游戏/未改native容差）；改成可逆映射基底差验证，保留解释，不当native模型通过。
