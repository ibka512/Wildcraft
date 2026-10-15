# C7 普通客户端短时基线与未解决的设置干扰

2026-10-15。仅新增独立gametest源集中的normal-client探针、专用Mixin/metadata/任务；正式生产JAR与配置/存档格式保持dev62。runC7NativeRenderBaseline用冻结正式21Mi JAR、既有生产依赖、独立probeJar，明确不设置fabric.client.gametest，metadata不加载旧研究时钟/命令/观察器。原generated test world复制到新外层目录，不覆盖/升级任何真实用户存档。

第一次16秒启动失败：Mixin的保留package包含普通探针类，改为子package。v2普通客户端3m10s实际停在Missing content detected确认，未进入世界；v3只补原生游戏截图和屏幕状态，实际确认DetailedBackupConfirmScreen，随后只终止该专用客户端，不点“丢失内容继续”、不迁移数据。来源世界含原研究registry IDs；C7RegistryCompatibility只恢复原静态两个component和一个entityType及NoopRenderer，未调用ResearchFixtures.onInitialize的研究时钟/探针。

v4实际普通QuickPlay成功，clean1m22s。0/4/16个原机械空主体停机，各5秒暖场＋15秒采样；3样本各301真实world ticks，测得20.050/20.048/20.013 TPS，没有加速调度或自设5TPS。真实client runTick循环率119.698/119.491/73.603，p95循环间隔8.341/8.355/33.338ms，末FPS120/120/30；堆792756168/681437512/678075688bytes，GC4/21/33ms。实际像素2560×1564、renderDistance16，不能把启动文件写的4格或1280×960冒称实际设置。循环间隔不是GPU完成耗时。三张场景截图加一张启动状态保存。

停止时周剩5%，没有继续改设置/重跑。最后样本接近超过一分钟的静止输入，可能受到原生inactivity限帧干扰；当前采样没有记录实际inactivityFpsLimit值或ThrottleReason，因果尚未实测排除，不能归因为16主体的性能回归，也不称短时性能目标达标。下一先实际读取并明确设置隔离客户端的renderDistance与inactivityFpsLimit，保留v4原结果，再按同样场景确认变化；不改真实用户配置、不改生产时钟或物理。

只统计停机空主体，不代表带全部部件/驾驶/复杂物理/大量实体/长期生存或主观手感。当前CPU实际日志Apple M4/10核、macOS27.0.1/Java25，环境与采样严格分开。4版日志/失败图/实际JSON和v4源SHA仅外层C7/verification/native-baseline-v1..v4，源码/proof可复核。C7整体、C6整体和正式第一人称仍未完成。
