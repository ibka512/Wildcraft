> 当前验收：隔离 v2 56秒/32图与固定任务1分12秒/32图通过，见 C6AJ-ACTION-HANDOVER-VERIFICATION.md。持续 ATTACK/DEFEND 按现有取物规则取消开伞；释放后实际落地、新跳和新鲜跳键才验证合法开伞。以下草稿与首次准备保留为历史。主版本仍dev60，无正式61。

# C6AJ 单人动作与探索输入交接回归规格

接续C6AI候选dev60，必须先完成其完整43类回归并归档；当前不改运行renderer/版本/资源或已启动测试。C6整体未完成；本批不以新的机制、源稿采用或战斗连击替代现有范围。

当前已采用资产基本动作已有各自限定测试，但需要把真实输入串起来检查优先级，重点为：普通jump→再次跳键开伞，真实剑盾同时收为视觉空手；地面实际剑蓄力→自然jump→再次跳键开伞，旧蓄力取消且在空中松攻击不会追加一次攻击、wear或Fuse；实际DEFEND举盾→jump/open，原use/格挡状态正确退出，伞真正拥有双手；实际快捷栏取火把即时关伞/交真实物品，回剑不重播已取消的jump/charge。两身形、实际左右主手以及真实物理/库存/GliderEquipment槽，沿原生按键与既有服务端动作处理。

只新测试世界、原装备和已采用source；没有真实存档迁移、无新生产依赖、控制器、combo输入缓冲或额外源采用。源稿不足的下落强击、复杂/盔甲/副手取收和正式第一人称仍原路径。普通剑三连源的浏览器点按queue/rate仅是艺术审阅，不移入游戏或改原有命中/恢复/技能时钟。

验收：真实各模式/动作包和输入、实际Gliding/MeleeView/Combat.view/use、source Frame/Canopy与原native held-stack最终GPU；打开只有两空视觉手+一个真伞，实际原Sword/Shield及Fuse计数不改变或复制；关闭无旧武器代理、无伞残影；未释放强击不会追加费/伤害/wear/Fuse，滑翔固有精力消耗保持。若失败先最小真实输入红绿再修，不通过不推进正式包/整体验收。既有43类覆盖历史；新source未变时相关125/datagen只因实际修改/问题再跑，不无变化重复全套。

草稿和源只本机，无发布上传。本规格是已授权既有路线的后续整合验证，当前未实测，不冒称C7整体验收或正式美术完整。


## 当前准备状态

2026-10-14私有ActionHandoverClientTest.java已保存在development-assets/local-only/dev61/draft。只用现有锁定Classpath的独立JavaCompile实际6秒通过：main/client/gametest六任务全部UP-TO-DATE，只有草稿编译至外层C6AJ/qualified-draft-classes。未改变主JAR/资源/43类测试/当前游戏目录，未注册客户端entrypoint，尚未执行任何handover玩法测试，不能称已完成。


## 启动前准备

外层C6AJ/handover-draft-tests.jar已由既有test输出和独立编译草稿组成，CRC/唯一新客户端entrypoint核对通过；不是新main mod安装包或正式61。Init脚本首次dry-run因初始化脚本不持有Loom类命名空间而配置失败978ms，未入游戏/未改当前运行；改为复用已登记runJumpRigClientTest的实际task类型，第二次dry-run857ms通过，所有任务SKIPPED，没有main JAR/资源再生成。两脚本已保存在local-only/dev61/draft。完整43结束后才能启动隔离探索输入测试；dry-run和类型检查不能代替玩法验收。
