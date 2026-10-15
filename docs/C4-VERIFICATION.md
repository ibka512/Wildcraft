# C4 验收 / dev.22

C4本机dev.22已完成，feature/perfect-guard；103项服务端/8类成品客户端回归通过，49张正式窗口截图。僵尸/尸壳10刻前摇；独立DEFEND新鲜举盾0–5刻完美窗口、12刻重新开启间隔、一次成功；普通盾延迟/耐久/Fuse保持。成功12刻敌方硬直，C3强击8刻；普通Mob箭真实方向反射，速度最高3，不自动瞄准。非合格来源保持原版。按世界刻，不冒称C5真实时间反击。无新依赖/存档迁移/美术编号；公开仍dev.18，本批未上传；下一C5完美闪避/短林克时间/反击。

正式资源生成、构建、103项GameTest（新增8项）和8类成品客户端（Guard、Melee、Combat、Input、Survival、Focus、CoreArt、BackEquipment）实际通过。前摇同目标/距离/墙/世界刻、原生一次攻击、新鲜/长按/冷却/重复/方向/换盾、耐久/Fuse/费用、实际箭碰撞与反射、穿透/燃烧/药/Fuse/玩家箭排除、强击硬直与过期均验证。客户端自然AI准备→完美格挡→恢复普通攻击，真实按键改绑、菜单隔离、维度/重生清理，原生两模型/左右手/四槽盔甲与怪物骨骼遮罩通过；旧单人生存/弓/滑翔/机械/料理/风/背负回归保持。

[证据](../development-assets/verification/guard/README.md)，49张正式截图；候选/失败记录与最终结果分开。SHA-256：`897f0fb2fda58a746ffbaa3cd6c1fae7b1e055a3cea446eb7313210d306387bc`。

[规格](C4-SPEC.md)/[正式包](../development-assets/wildcraft-0.1.0-dev.22+mc26.3.jar)/[生产源码](../development-assets/wildcraft-0.1.0-dev.22-sources.jar)/[阶段快照](../development-assets/Wildcraft-C4-source.zip)。本机标签v0.1.0-dev.22+mc26.3。旧包/标签/校验固定。

复现：`./dev.sh --no-configuration-cache runDatagen`，随后`./dev.sh -PacceptMinecraftEula=true --no-configuration-cache build runGuardClientTest`，再执行`python3 tools/verify-adopted-assets.py`；新操作者自行接受EULA。测试Mod/游戏文件/个人世界/缓存不进入正式包。

世界慢时间下窗口/硬直也变慢，C5近战实际时间反击未实施。投射物范围仅合同中的普通箭，不扩展所有生物/爆炸/PVP。未验收人工手感、长期生存、物理控制器/Steam Deck、新独立服务端或多人专项。程序姿态不等于正式动画素材，43项采用ID不变。
