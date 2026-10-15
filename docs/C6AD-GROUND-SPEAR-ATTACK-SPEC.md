# C6AD已采用长矛地面攻击源准备

2026-10-14正式53/5e7426f6保持，候选54/b3414561实际37图专项/125服务端已过、完整37类进行中。本项只有源测量，无55版本/renderer或转换启用。spear-attacks/v01用户2026-10-11“全部采用”工作包已采用，原16剪辑两skin/light-heavy/empty-shield/左右主手；沿真实杆滑动不是固定握点误差，不能用equipment wrist重锚抹掉刺击。

作者native32px spear/原1.7第三人称尺寸一次，raw逻辑(.70,.30,.5)、source+Y沿native(-x,+y)、normal+Z；sourceLocal轴参数s对应raw(x=.70-s/(1.7sqrt2),y=.30+s/(1.7sqrt2),z=.5)。当前161样本/clip测量main s -0.6000004..0.00000036（peak接近raw .9496,.0504），位于native tail -.3sqrt2*1.7≈-.72125到tip .7sqrt2*1.7≈1.68291轴段；main横向误差最大.00896753、free off .00537339，均在既有native palm .015内。root/bind原样，脚origin最大偏差.000106663只报告不称游戏foot/碰撞/GPU通过。数值测量第一轮仅np.bool_写JSON失败，修正显式bool后完整报告保存，未写任何运行资源。

原light .4秒对应8 worldtick；heavy1.75源prepare0..0.9对应原18 ticks，0.9..1.1作者review hold不作为游戏deadline，native variable hold可保持prepare end；1.1..1.75 release .65对应原13 ticks。原native最小ready5/距离与墙截断/多目标piercing、卸骑与接触冷却、费用16、每原目标wear/Fuse/原生stab及STAB包零泄漏保持，不用Source hit帧制造额外伤害或修改游戏计时。

先default flat stationary unarmored/capeless实际main nativeSPEAR/empty或actual carry盾，原同一库存与当前身份；实际七种材质/原尖与手持尺寸/UV/glint/Fuse/GPU、真实main/空副手杆contact、root/sole/head/真实carry盾handle与原light-charge-hold-strong逐项验证。source iron仅代表动作，不替代wooden/stone/copper/iron/golden/diamond/netherite真模型；不再次烘焙1.7。sanitized back仅nativeItem allowlist识别、不能扩大PIERCING/嵌套数据同步规则；本项也不改变它。

复用已验证时序，但nativeSPEAR不是普通Player.attack；实际piercing/stab与多目标/遮挡/ready回归、frame各项退出、低精力、nativeuse/GUI/伤害/新槽/占手/盔甲/空中旧路验证后才算玩法通过。数字报告不是actual renderer/Gameplay验收。复杂/空中源/高级/正式第一人称以及C6整体仍未完成；保留原源/失败/旧包/真实数据/Git/其他美术WD，不上传混合源和含数据JAR，不新增生产依赖或真实迁移/发布，不采用待审13或另6。
