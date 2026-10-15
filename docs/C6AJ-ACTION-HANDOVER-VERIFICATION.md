# C6AJ 真实动作交接验收

2026-10-14：隔离草稿 v2 实际 56 秒 / 32 张真实 PNG 通过；纳入 src/gametest 与固定 runActionHandoverClientTest 后，实际 1 分 12 秒 / 32 张真实 PNG 再次通过。主运行版本仍 dev60，未改生产代码、资源、计时、伤害、库存或装备位，没有正式 dev61。两类证据不可合并成 64 个不同场景。

标准/纤细皮肤 × 实际左右主手分别验证：普通真实跳跃→再次跳键开伞；持攻击蓄力→真实跳跃→请求开伞；实际副手举盾→真实跳跃→请求开伞；释放后合法新跳开伞；真实快捷栏取火把立即交回物品。开伞状态观察现有 glide_hold、原伞 GPU 和真实皮肤，视觉双手空、原剑盾仍在库存、唯一真实伞装备位保持。退出没有旧 jump/glide 源重播，库存不复制；取消蓄力后松开不追加攻击、耐久或 Fuse 消耗。

## 失败与最小回归

首次 v1 31 秒 / 2 图在持攻击开伞处失败。28 秒最小红确认既有 ActionFrame.triggered 包含持续按住：取用 ATTACK/DEFEND 时 GlideControls 正确收伞并清理蓄力。因此“按住攻击还能持续开伞”的测试预期不符合用户设计，生产逻辑没有修改。

第一次修正后同一跳跃立即再开伞仍失败：实际位置接近落地，不能把临近落地的瞬间视为可持续 glide_hold。保留真实重力和位置，测试改为松键后等实际落地、重新真实跳跃再开伞；最小绿 18 秒 / 2 图，随后全矩阵隔离和固定任务均通过。未放宽库存/攻击/退出断言，临时诊断前缀已清理。

## 复现与保存

`./dev.sh runActionHandoverClientTest -PhandoverRunDir=<新的专用测试目录> --no-configuration-cache`；测试会清理该专用目录的旧截图，不指向真实游戏存档或已有证据。默认任务在开发缓存目录运行；当前磁盘余量有限，实际验证使用外层新专用目录。

实际证据：`/Volumes/仕事/Wildcraft/开发接续记录/2026-10-14-C6AJ/verification/isolated-v2-passed` 与 `integrated-v1-passed`；RESULT.json 保存逐 PNG SHA256，失败/最小红绿目录保留。测试源码 src/gametest/java/dev/wildcraft/test/ActionHandoverClientTest.java；不随主运行 JAR 注册。

本项仅既有动作输入交接，不新增动画源、连击、设备输入或生产依赖；不代替 C6 整体、复杂装备、正式第一人称、长期人工平衡和 C7 生存整合。混合许可资源与含数据包只本机，没有上传。


2026-10-14顺序回归补充：C6AL连续调用时确认普通jump可能在.30秒源进入结束前被原server接地合法关闭。C6AJ当前验证active的enter/hold和真实双手/库存/伞GPU，不强求短跳必有hold；高处持续hold仍由GlideRig/C6AK覆盖。最小两类连续运行红→43秒22图绿，生产控制/物理/时间不改，详见C6AL进度。此前独立hold通过日志作为历史保留。
