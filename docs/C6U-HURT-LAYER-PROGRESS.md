# C6U候选46进度

正式本机45源码1bce0f6c/tag v0.1.0-dev.45+mc26.3，交接bd2ac2fa；用户持续授权，当前参数46候选。没有正式46标签/公开发布。

已采用hurt-reaction/v01只选chest48源（各24），211/选209，旧187、几何/绑定逐值相同；不接head变体/部位伤害或重击硬直。根/有限数/绑定/首尾复位/头方向资格通过；去pelvis后双臂与挂点和body共享刚性delta，48×61样本最坏约1.013e-15。源站立腿/骨盆buffer排除，当前native base的root/pelvis/腿受保护，头只跟原生瞄准。源全局head-forward按当前head相对body heading映射，相邻四方向delta连续混合。

HurtRig只真实hurt6/hurtYaw、默认无盔甲披风主剑与empty/carry/实际offshield guard，每次重新构建当时原生姿态（原MeleePoses.SOURCE_HURT只合格时屏蔽旧body受击）与实际native物品参考，再施一个刚性上身delta。只渲染标记和矩阵，不改entity、输入、物品或server数据；原6世界刻+partial，暂停不加partial。GUI/不合格/缺源保留原程序。ClimbRig仅补当前玩家Motion清理接口，保留faces/corners/既有攀爬规则。

编译v1失败缺ClimbRig.cancel(Player)，补单玩家Motion清理；编译v2失败测试wildcard org.joml.Math与java.lang.Math重名，换显式类型imports，失败保存；不是玩法测试通过。独立datagen-v2 68零写通过。真正客户端v3 87秒48图通过：真实native mobAttack损血/原击退，两身形左右手三装备×四来袭方向；guard组使用真实原生indirectMagic绕盾损血，不把它算成功格挡。实际body/原皮肤/物品GPU，原root/pelvis/腿矩阵1e-6保持、当前native头quat/臂段长2e-5、反馈远离真实来袭、原hurt6终止、无新增attack/wear-Fuse-cost通过，已实际看首轮红色受击与原生腾空图。

v4正在补真正成功格挡无hurt、10FPS、5TPS/look相邻方向、pause、GUI/新物品/盔甲回退。与实际移动/攻击共存尚待，build/125/完整28类尚待；未完成不宣称46整体或C6整体通过。包/源仅本机，其他美术WD、所有历史包保持，无依赖/迁移/上传，额度到任一≤5按实际reset+一分钟续调、不用券。

最终专项v5 103秒58图通过；v4 96秒55图成功blocked/10FPS/5TPS/look/pause/GUI/new item/armor，v5再加真实按住前进移动、真实跳跃腾空与实际原生swing同时受伤，当前原root/pelvis/腿/原生头及臂段长仍保持。真正攻击保留一次native hit/wear/Fuse（1/31），没有输入/腿锁或source fixed idle基底。所有高级生命周期/复杂/正式第一人称及C6整体仍待。下一build/125/完整28类，尚未正式46。

候选源码e9dfb6cc。独立datagen-v2/build/125服务端通过，28类完整在/tmp/wildcraft-dev46-full-client-v1.log。候选包/资源CRC/实际版本/SHA已保存dev46/candidate。下一C6V已采用弓取收32源只资格，通过原根/脚/头/绑定/时长，16默认back候选，未激活。真实native挂点保持，不把辅助伸取当精确抓柄。

完整28类v1在12分15秒旧HopRigClientTest停下：真实损血/原生HOP取消已经通过，旧断言要求所有FRAME=null，与新合法hurt层冲突。只将该断言改为dodge_源必须退出，继续保留实际HOP取消/损血及本批root-legs/head/臂长独立断言，没有改生产姿态或放宽数值容差。完整失败日志/图片保留full-client-v1-failed；下一先跑对应Hop专项，再完整v2，不提前算整体通过。

对应Hop专项v1 80秒52图已通过，真实损血/原生HOP取消/无免疫及原三方向/费用/盾释放恢复、root/GPU等断言保持；仅源所有权断言允许当前独立hurt，不允许dodge_残留。生产数学未改变，下一完整28类v2。

完整v2实际28类20分32秒933图全部成功；原生产e9dfb6cc不变、测试ef0db728明确旧Hop所有权退出。正式46包/资源与标签见清单，下一C6V未激活，仍在已授权范围继续。
