# C2 本机验收：锁定、防御与基础动作 / dev.20

2026-10-06，基线本机dev.19（407113c），分支`feature/combat-foundations`。本批合同见[C2-SPEC](C2-SPEC.md)，后续见[完整路线](COMBAT-INPUT-ROADMAP.md)。用户授权“开始下一批”；本批未上传GitHub，公开main/Release仍dev.18。

## 已实现

Z切换锁定、左右方括号切换目标、C按住独立举盾，均支持原版改绑。F8战斗页独立保存锁定方式、8/12/16格搜索范围、中立目标及后置第三人称方向帮助。默认只选择敌对或实际攻击玩家的生物，不选择无关牛、未激怒的末影人/猪灵/蜘蛛。服务器负责选择、视线与距离资格；死亡/移除、长期遮挡或不兼容状态解除，不自动命中。

锁定后原生移动和疾跑跳跃以目标为方向参考，原版碰撞和斜向归一化保留；头和弓可自由瞄准。第一人称与前置观察镜头不被拉动；后置镜头只在闲置且偏差超过60度时缓慢帮助，可关闭。

独立举盾使用真实手中盾，副手优先，潜行仍独立。保留原版延迟、正背面判定、耐久、冷却及Fuse一次结算，不新收精力。弓/原版USE接管时释放独立防御；同型备用盾替换也需重新按键。正常客户端耐久同步不会误取消盾牌，真实装备身份由服务端确认。GUI/暂停/断线及技能上下文切换不重放持有输入；取盾会先收滑翔伞。

基础部位姿态提供平滑面敌、原版下半身移动、盾臂和上身叠加、独立头部瞄准，拉弓立即接管手臂。标准/纤细、左右手和原生四个盔甲槽使用同一姿态；玩家模型只应用一次，衣袖等原版子节点不重复旋转。第一人称保留原版持盾表现。这是程序基础动作，不是完整新动画素材交付，采用美术仍43项。

## 实际结果

| 检查 | 结果及范围 |
| --- | --- |
| 资源生成 | `runDatagen`成功；原生中英语言生成，无手改生成文件 |
| 正式构建与规则 | `build`成功；精力、温度、风、输入纯规则通过；普通`test`无来源不作证据 |
| 服务端GameTest | 89项全部通过（2.600秒），XML无failure/error；含6项新战斗规则 |
| 成品客户端 | `runCombatClientTest`的6类全部通过（构建及运行2分8秒，36张最终截图）：Combat、Input、Survival、Focus、CoreArt、BackEquipment |
| 新战斗专项 | 实际改绑/锁定/左右切换/按住锁定；真实生存玩家被激怒中立生物；自由瞄准移动及镜头边界；正面盾格挡/耐久同步/潜行/弓优先/菜单隔离；滑翔收伞举盾；配置保存 |
| 原生模型 | 标准/纤细×左右手，盾臂/头/腿遮罩、衣服子节点及四个原生盔甲模型一致；实际穿铁甲截图 |
| 既有玩法 | 单人生存原生合成、料理、攀爬滑翔、充电/制造/驾驶；空中弓；背负/左上HUD/鞘翅；正常与紧凑设置布局 |
| 资产检查 | 43个当前采用ID、30个第二批版本、1779份原稿文件记录、48份运行拷贝校验通过 |

最终日志、XML和实际窗口截图见[证据目录](../development-assets/verification/combat/README.md)。候选通过日志与首次失败诊断单列，不能用候选覆盖最终结果。模拟玩家原生`gameMode()`固定创造模式，不能验证生物真正敌对；该断言改在真实生存客户端验证。其余修复包含原生注入签名、绑定分类重复注册、盾耐久同步归属、异步弓确认及滚动页测试访问；失败日志保留。

## 安装、复现及数据

本机包：[dev.20正式JAR](../development-assets/wildcraft-0.1.0-dev.20+mc26.3.jar)、[生产源码JAR](../development-assets/wildcraft-0.1.0-dev.20-sources.jar)、[阶段源码ZIP](../development-assets/Wildcraft-C2-source.zip)、[清单](../development-assets/Wildcraft-C2-manifest.json)/[SHA256](../development-assets/Wildcraft-C2-SHA256SUMS.txt)。正式JAR SHA-256：`84d1fd0d0fddb238d83ccb1e829ae68e05d4ffe8bab96f905dcaafe40c694407`。游戏标签`v0.1.0-dev.20+mc26.3`为本机固定标签，不覆盖旧版本。正式包不含测试模组/Controlify/YACL。

使用锁定Minecraft26.3、JDK25、Fabric Loader0.19.5/API0.161.0+26.3。操作者自行接受Minecraft EULA后，依次执行：

```sh
./dev.sh --no-configuration-cache runDatagen
./dev.sh -PacceptMinecraftEula=true --no-configuration-cache build runCombatClientTest
python3 tools/verify-adopted-assets.py
```

测试自动创建一次性世界，不依赖个人存档。锁定/防御会话不保存，新客户端偏好`config/wildcraft-combat.json`与旧Focus配置分开，旧数据读取/格式保留，无迁移。回退旧JAR前备份世界，不覆写原稿、历史包和标签。

## 未完成与下一批

下一批C3为剑/斧普通及蓄力攻击、侧跳/后跳、费用/碰撞/单次命中与基础攻击受击动作。完美格挡/闪避、怪物前摇、减速反击、约30–35份正式动画按C4–C6推进。

本批未做真实手柄/Steam Deck、长期人工战斗手感、多客户端或新无测试模组独立服务端专项；没有据自动结果宣称这些通过。既有多人研究保持暂缓。B6风提示、机械底部修订和人工生存/平衡仍待完成。
