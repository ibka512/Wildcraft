# dev76-public 公开源码交接

2026-10-19。用户明确授权更新GitHub，本次仅同步源码、已有测试工具与交接文档；未继续开发玩法、创建Release或上传二进制包。独立克隆以原公开main c0bf3f7c为父提交，选择本机1a14a9c5（正式dev76 fc24678e及C7工具），不合并私有历史/标签。源码版本0.1.0-dev.76-public，环境与存档格式/依赖保持。

## 公开范围

main/client/gametest/rulesTest源码、注册和生成资源、当前转换与检查工具、36份技术规格/验收文本。原有公开45用途/30交付版本/2107原件记录/57运行副本/4底部变体沿用。新增逐目录美术状态JSON/文档说明96条目录中90条有限运行用途与6参考，未改变采用编号或公开许可。

玩家原源/派生骨架/网格/剪辑JSON/GLB、私有截图包、含数据JAR/试玩ZIP、真实世界/凭据/缓存/游戏代码不公开。公开缺动画资源时沿CombatRig与FirstPersonSword原生回退，不冒称已获得本机动画或401/FP4资源。原LICENSE/NOTICE保持，旧公开Release不改写。部分历史技术文本引用本机证据和私有原源，只解释历史，不是公开下载。

## 验证层次

本机dev76的24专项+clock4+相关12共14不同类530PNG/125服务端，以及C7停机空主体短测/有限驾驶记录，均为本机历史事实。新驾驶guard低帧率、自然地形/转向、从零获取/平衡/长期性能尚未完成；C6整体、副盾STOW/其他主手占用、复杂盔甲/其他正式第一人称未完成。公开构建的实际检查结果与产物SHA另记PUBLIC-VALIDATION-dev76.json；未执行项目不写通过，Linux无图形不代表图形验收。

## 接手

README → AGENTS → AI-HANDOFF → ACTIVE-CHECKPOINT → RUNTIME-STATUS。普通公开构建不需要私有素材；需要Rig专项者先取得对应素材与许可。保留原生伤害、物理、库存、精力费用和时长，源动画不能制造伤害或修改归属。下一开发按明确任务接续C7，不能把本次公开同步授权变成所有后续自动发布。

## 本次真正执行的公开版检查（macOS）

- 公开范围与采用来源校验通过：45用途/30版本/2107原件/57副本/4底部变体。
- 独立datagen15秒，68文件，生成后与公开父及本机来源逐文件一致；build+gameTestJar11秒，125项服务端通过，test NO-SOURCE不计通过。
- 基础美术客户端18秒/4PNG；独立缺第一人称资源回退17秒/1PNG，实际确认不加载标准/纤细新源、原生手与剑仍提交。公开图形范围只有这两项，不把私有Rig全套算公开通过。
- main/sources/gametest三个JAR均CRC/受限目录检查通过。五张截图是本次公开包实际原生回退画面，可在development-assets/verification/dev76-public复核；不是本机私有动画截图。
- 日志仍有既存battery_3d的wildcraft:entity/battery缺贴图警告；旧dev62-public同样存在。本次没有修复或完成该模型专项视觉验收，不能据整体测试绿声称所有材质无警告。

各产物独立SHA/图片SHA见PUBLIC-VALIDATION-dev76.json。尚未创建Release或上传本机JAR；远程Linux运行结果在推送后单独补记。

## 远端核实与Linux回执

公开源码提交[f0c50fc](https://github.com/ibka512/Wildcraft/commit/f0c50fcf73782194b4f88db90e01406a1a875027)已推送main；[Linux Actions](https://github.com/ibka512/Wildcraft/actions/runs/39268843470)实际success，日志确认125项必需服务端通过。公开范围、采用来源、datagen无差异、构建和公共JAR检查全部通过。远端4577 tree条目无受限目录，全部155个变更blob与本地提交一致；README/AGENTS/AI交接/ACTIVE/美术状态及翼Mixin六文件匿名逐字节读取匹配。

三个Linux产物已下载并CRC/受限目录检查；与macOS逐解压比较，所有class/资源/源码一致。主JAR仅MANIFEST中client-only条目排序不同、条目集合及其他属性一致，其他两JAR成员一致；不同平台归档SHA分别登记，不能混用。核验脚本首次误把外置卷AppleDouble当JAR读取，筛掉非实际产物的._元数据后完成三份检查，未修改构建物。

当前回执提交只更新文档/机器回执，源码仍为上述快照；之后Actions结果以对应实际提交为准。不创建Release、不推私有tag、本机素材/安装包不公开。
