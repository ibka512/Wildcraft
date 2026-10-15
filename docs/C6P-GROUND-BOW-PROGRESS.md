# C6P地面拉弓源候选接续

2026-10-13，正式本机dev40/a5abbad及交接80b54af、tag v0.1.0-dev.40+mc26.3保持；其22类14分34秒/726图＋37专项、125服务端与构建/资产核对通过，167私有剪辑与包仅local-only/dev40。当前参数dev41候选，没有正式41标签或交付，C6P玩法与C6整体未完成，未上传GitHub。

已完成准备：bow-ground/v05两身形/左握右拉的8源骨骼/固定根/真实握点资格。GLB draw/cancel1.90s与metadata1.90333s差别已记录。只有参考弓（无skin的*_bow节点）含morph weights，导入器严格只排除这些参考装备轨道，若遇玩家skin或未知weights即拒绝；真实原生BOW继续负责模型/弦，不导入参考装备mesh或预览镜头。

发现整段draw前1.275s只有举弓，直接按20刻线性映射会原生半蓄时源仍零拉弦。作者末值.871759是其hold值，不强行改源。真实右palm到左bow-reference距离在1.275–GLB1.90段从.283394到.837798单调，工具1201采样/101个pull二分逆解、401未见pull插值验证：GLB表相对误差约.00348。可选Clip.pullTimes解析/有限单调校验与pullSample插值已加入，旧schema1无字段为空保持线性，不改保存/网络/20刻或Focus补偿。

实际成品客户端BowGroundMappingSourceProbe只在菜单读候选资源，未进入世界/实际use或启用渲染：60FPS重采样后真实RuntimePalette相对距离误差.005498746，超过.005门槛被拒；仅新ground剪辑改120FPS，v2实际误差standard=.0035084276/slim=.003509181，11秒源探针通过。失败与通过日志在development-assets/verification/dev41，逆映射/源资格报告在local-only/dev41。不能将这个源读取探针称实际射击或完整玩法通过。

显式--ground-bow-runtime依赖全部先前runtime选项，只新增每变体draw/hold两段，当前资源169；release/cancel源长1.35/1.90s仍未选用。与dev40逐键检查原167剪辑/几何/绑定保持。新增地面段没有renderer selector，当前已接表现仍165选用（旧两shield_ready不用，新增两ground也未用）。候选Clip字段/导入器检查是准备，不等于接入成功。当前dist/缓存的41若构建成功也只是候选，安装用正式40私有包。

下一：依据C6P-GROUND-BOW-SPEC接真实LEFT used arm（mainLEFT或空主手+offbow/mainRIGHT），限制默认无盔甲披风静止平地/另一手真正空，显示实际BOW原生pull模型与已验证木握位/长轴/真实皮肤，按真实getTicksUsingItem()/20通过pullTimes采样draw，满弓hold，独立头瞄准/取消立即交出。不得强制改主手设置、转移库存、强占未批准右握/空中/第一人称补齐，或源时间射箭。需实测真实原生弦/右手拉距/GPU、不同pull/两身形/使用归属/满蓄/空弹药/一次真实箭与wear、GUI/换装/伤害/移动/盔甲/不支持右握，以及C6O/C6N/C6M与125服务端/构建，成功才正式归档41。

所有混合许可源/派生资源/含数据JAR仅本机，不新增生产依赖/迁移/发布，不全目录暂存其他美术变更。32Gi开发映像空间已恢复（完整测试缓存tar与逐文件核验在外层同盘归档/测试缓存-2026-10-13），不需另建工程。连续额度≤5%保存停止、实际五小时重置后一分钟续跑，不用重置券。

停线前补验：独立datagen68文件零写后build/纯规则/125服务端与45/4/2107/57采用核对通过，候选41包/源JAR CRC/SHA及169资源快照仅本机保存；正式40不变，没有GroundBow renderer/GPU/实际use验证。额度5%按停线记录续调。

## 本次接续候选进行中（尚未正式41）

2026-10-13实际GroundBowRig已接入真实LEFT使用臂与另一手空、默认无盔甲平面静止，source draw通过真实native ticks/pullTimes、20刻转hold；实际BOW原三档贴图/尺寸/glint不替换。游戏首探针发现挂点平面和参考弓尺寸造成实际弦距约1.317格，不按源资格冒通过；ground平面反向与固定源段长的右拉弦臂适配后，实际三档原生弦/箭尾握点误差约3e-7，身体/腿/持弓臂和原生独立头不改。只做程序握持适配，不称新增完整动画。

v5实际两皮肤×mainLEFT或offbow/mainRIGHT四组合，三个原生pull模型及源进度/段长/遮罩/头/真实皮肤与GPU、真正一箭实体/弹药32→31/wear1通过。v8新增实际镜头/侧面、暂停、GUI、移动、盔甲、真实伤害、另手火把、真实选槽零射击零wear/箭消耗、右握原路/空弹药通过，46秒；新未完全采用释放取消长段仍不接，停止使用立即交出。

暂停v6/v7未通过：PauseScreen打开到isPaused置位间，原键映射先释放，使候选移除Motion；native use仍在、暂停dt=0让新Motion无法进场。修正只允许真实暂停屏幕过渡保留已存姿态，非暂停GUI仍即时交出。v8真实nativeUsing=true、hold时钟冻结通过。最初编译通配JOML Math及测试ModelPart签名错误、未资格弦距、暂停失败日志保留。

当前正在补低FPS/空中退出与真实使用每帧覆盖（观察失选帧而非只成功GPU）；完整23类/125服务端/构建/正式归档未执行完，仍候选，无正式41标签。正式40和其他美术任务保留，无上传。

v10专项52秒/29图通过：新增实际10FPS、真正按住跳跃的空中退出（v9只触发consumeClick没保持按键到movement tick而超时，改真实hold/release，不改原跳跃）、所有正向使用帧从第5刻后都不失选，原20刻hold/三档native模型仍匹配。独立datagen68零写/build与纯规则/runGameTest125全部实际通过，45/4/2107/57再核对通过。实际选用draw子段与hold的皮肤脚底数值101样本/段误差约1.4e-7；整个未选举弓前段含抬步，不能拿其脚底冒当前选用或强制播放。完整23类正在runGroundBowRigClientTest(/tmp/wildcraft-dev41-full-client-v1.log)，暂未正式41。C6Q已采用空中拉弓8源资格与手距表只读准备通过，尚无运行转换或renderer，见下一规格。

## 正式41限定验收完成

完整23类15分10秒/755图通过，完整后补严格真实UseItem归属与native同类弓转交对照，v13 57秒/31图；替换零箭假设的失败改为真实原生基准：source和盔甲native fallback在松手前零额外效果、实际release后均1:31:1。没有为源改变原结算。最终build/纯规则/125服务端再通过。169/167、三档原真弦、真实归属/蒙皮/GPU/脚底/头/20刻/一箭及互斥通过，详情C6P-GROUND-BOW-VERIFICATION；正式包只local-only/dev41/final，候选与历史保持，无上传。C6Q尚仅资格，C6整体未完，继续。
