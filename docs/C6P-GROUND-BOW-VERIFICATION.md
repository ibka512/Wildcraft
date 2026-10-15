# C6P本机dev41地面左握弓验收

2026-10-13，feature/adopted-art-runtime，正式源码6d7cd66/tag v0.1.0-dev.41+mc26.3。限定真实LEFT使用臂（mainLEFT、或空main+offbow/mainRIGHT）、另一手真空、默认平地静止无盔甲披风的已采用bow-ground/v05 draw/hold完成。停止使用立即交当前原生/已验证待机；右握/移动/空中/占手/复杂形态/正式第一人称和长release/cancel未并入。C6整体未完成，未公开上传或发布。

源资格两变体8剪辑、根静止/22皮肤骨/28引用/12网格、绑定与底盘皮肤网格完全相等，实际左bow-reference/palm与hold闭合通过。第一批只新增每变体draw/hold两段，169/167可选，原167/几何/绑定逐键保持。参考弓mesh与无skin的*_bow morph只作源参考排除，玩家skin weights若未知则拒绝。实际GLB1.90与metadata1.90333差别保留记录。

整段draw有1.275秒举弓铺垫，源相对hold的作者progress .871759不改。源右手到握点距离单调逆表（101项/401未见采样）映射真实native getTicksUsingItem/20，只读取原20刻/既有Focus有效计时，20刻转hold，不以源时间射箭。60FPS源菜单探针误差.005498746失败，只新ground120FPS后standard .0035084276/slim .003509181通过；这是源读探针，实际使用另验。选用子段与整个hold101皮肤脚底样本最大偏差1.363e-7，未选举弓前段包含抬步，不假装全段平地承重。

首轮游戏真弦距离约1.317格，源骨架资格没有代替实际模型验证。原26.3 BOW木握位/长轴按实际原模型display/local transform求解，ground参考平面法线反向（进入的原生参考对应处理避免瞬翻），原尺寸/UV/glint/三档pulling_0/1/2和13/18刻阈值保留。右拉弦臂按两段原长度、源肘弯平面旋转适配真正弦/箭尾像素，不拉长骨骼/放大弓/渲染参考装备；不可达时原路径。真实GPU帧误差约3e-7，身体/根/骨盆/腿/持弓左臂保持源，独立原生头旋转与实际镜头保持；只称程序握持适配，不称新增完整动画资产或连续弦morph。

v10专项52秒/29图、最终v13 57秒/31图通过：两皮肤×两种真实左臂归属、真实原使用键、原三个pull模型/原20刻、真实皮肤和原BOW GPU、逐实际使用帧观察无第5刻后失选（包括观察无Frame）、实际nock/grip/源段长/保持遮罩/原生头、一次真正箭实体/ammo32→31/wear1。真实10FPS、独立look/侧面、pause保持nativeUsing且hold时钟冻结、非暂停GUI/实际移动/真实跳到空中/盔甲/真实伤害无免疫、另一手火把/真正快捷栏选火把零箭零ammo-wear、右握原路/无强改主手/空弹药都通过。

完整23类runGroundBowRigClientTest，15分10秒/755图通过，测试源码检查点1eb399b。包含新ground以及原22类bowIdle/shieldWalk/手动与自动闪避10FPS/反击/HOP/近战/guard/axe/walk/glide及投影/climb/shield/连续皮肤/GUI迟到归属/装备/取收5TPS与暂停/风/生存制造保存重开/背负。完整后增加严格actual use stack == 当前该手stack选择条件，最终v13再跑全部ground专项及真正replacement/native armor baseline；其余客户端路径没有改动。完整回归与这个最终定向补验分别保留，不隐去执行次序。独立datagen68零写、build/纯规则和125必需服务端都实际通过，最终build/125再次通过；采用45用途/4底部/2107源/57运行副本核对通过。

同类弓替换的原生事实：当前native会转交真实use与20刻到新弓，按键实际松开时一箭/31ammo/1wear。源码只接受当前真正UseItem归属，若短暂旧引用不匹配交原路；不能为动画私改native结算。v11/v12夹具错误要求替换后零箭零wear，与原生事实冲突；记录before release零额外效果后，用真实带盔甲原生fallback为基准，与新source最终均1:31:1。不是删断言放过额外结算，保留原玩法并逐项对照真实原生。弓仍不是Fusion宿主，原箭/Fuse/伤害与保存/网络/时间租约不改。

暂停v6/v7失败是PauseScreen挂入后isPaused置位前键映射先释放，Motion被移除后暂停dt=0不能再进入；仅真实暂停过渡保留已存姿态，非暂停GUI仍立即交出，v8和最终通过。v9一帧consumeClick未到原生movement tick没真正跳，改持续按跳键到真实onGround=false再松开，不改跳跃。最初通配JOML Math/ModelPart签名编译失败、真弦失败、暂停和替换夹具日志均保留。

正式包/sources、169私有剪辑与provenance保存到development-assets/local-only/dev41/final；原源候选candidate与既有private-animation-resources保留不覆盖，先前正式40及历史包SHA保持。CRC/SHA/版本与来源见Wildcraft-dev41-local-manifest.json。其他美术任务未提交内容保持，没有新生产依赖/真实迁移/公开上传。

下一C6Q已采用空中bow-air/v01仅只读资格8与几何逆表通过，尚未转换/renderer/GPU，不称玩法。需要验证真实跳跃/坠落、原5TPS时间租约与10/有效秒成本、有效拉弓、原生三档弦/原箭与wear、source腰线/腿/头和互斥；不引入previewLift .65到物理。正式第一人称13/另6补齐仍待审，复杂形态/其余语义和C6整体仍待；多人暂缓。
