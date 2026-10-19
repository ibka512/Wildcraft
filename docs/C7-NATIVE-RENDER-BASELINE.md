> 本机历史规格或验收；公开版不含许可受限动画数据。本文的本机包/源稿/截图链接不是公开下载，也不是公开版重新执行结果。

# C7 普通客户端短时基线与闲置限帧对照

> 当前短时闲置原因已由v6对照确认。新逐帧窗口校验正常75秒/4图与真实resize反例29秒见C7-NATIVE-VIEWPORT-GUARD-VERIFICATION；长时数据有尺寸变化限制，见C7-NATIVE-SOAK-VERIFICATION。

2026-10-15。仅新增独立gametest源集中的normal-client探针、专用Mixin/metadata/任务；正式生产JAR与配置/存档格式保持dev62。runC7NativeRenderBaseline用冻结正式21Mi JAR、既有生产依赖、独立probeJar，明确不设置fabric.client.gametest，metadata不加载旧研究时钟/命令/观察器。原generated test world复制到新外层目录，不覆盖/升级任何真实用户存档。

第一次16秒启动失败：Mixin的保留package包含普通探针类，改为子package。v2普通客户端3m10s实际停在Missing content detected确认，未进入世界；v3只补原生游戏截图和屏幕状态，实际确认DetailedBackupConfirmScreen，随后只终止该专用客户端，不点“丢失内容继续”、不迁移数据。来源世界含原研究registry IDs；C7RegistryCompatibility只恢复原静态两个component和一个entityType及NoopRenderer，未调用ResearchFixtures.onInitialize的研究时钟/探针。

v4实际普通QuickPlay成功，clean1m22s。0/4/16个原机械空主体停机，各5秒暖场＋15秒采样；3样本各301真实world ticks，测得20.050/20.048/20.013 TPS，没有加速调度或自设5TPS。真实client runTick循环率119.698/119.491/73.603，p95循环间隔8.341/8.355/33.338ms，末FPS120/120/30；堆792756168/681437512/678075688bytes，GC4/21/33ms。实际像素2560×1564、renderDistance16，不能把启动文件写的4格或1280×960冒称实际设置。循环间隔不是GPU完成耗时。三张场景截图加一张启动状态保存。

停止时周剩5%，没有继续改设置/重跑。最后样本接近超过一分钟的静止输入，可能受到原生inactivity限帧干扰；当前采样没有记录实际inactivityFpsLimit值或ThrottleReason，因果尚未实测排除，不能归因为16主体的性能回归，也不称短时性能目标达标。下一先实际读取并明确设置隔离客户端的renderDistance与inactivityFpsLimit，保留v4原结果，再按同样场景确认变化；不改真实用户配置、不改生产时钟或物理。

只统计停机空主体，不代表带全部部件/驾驶/复杂物理/大量实体/长期生存或主观手感。当前CPU实际日志Apple M4/10核、macOS27.0.1/Java25，环境与采样严格分开。4版日志/失败图/实际JSON和v4源SHA仅外层C7/verification/native-baseline-v1..v4，源码/proof可复核。C7整体、C6整体和正式第一人称仍未完成。

## 2026-10-15 后续实际对照

v5首次6场景对照在第5场景的时长/计数合并门槛失败，已保留失败JSON、日志、截图和运行源码，不称整套通过。此前四个实际样本已记录16主体AFK为SHORT_AFK/30上限，切MINIMIZED后相同16主体恢复NONE/120上限。旧门槛没有输出具体失败值，不能据此确定是时长还是数量；随后补齐诊断数值，并以max(帧时间,服务端采样时间)作为15秒截止起点，排除异步时间边界，生产时钟未变。

v6实际完整clean **2分17秒，6×15秒采样、7张游戏截图**。固定实际16格视距、2560×1564像素、原生20TPS、120上限，同一正常客户端按0/4/16 AFK然后16/4/0 MINIMIZED逆序对照；MINIMIZED仅关闭闲置触发，窗口仍不能最小化。没有伪造输入或重置AFK时间。

| 停机空主体 | 闲置选项 | 实测循环/秒 | p95循环间隔ms | 原生限帧原因 |
| --- | --- | ---: | ---: | --- |
| 0 | AFK | 119.625 | 8.342 | {'NONE': 1796} |
| 4 | AFK | 119.563 | 8.343 | {'NONE': 1795} |
| 16 | AFK | 72.561 | 33.338 | {'NONE': 853, 'SHORT_AFK': 238} |
| 16 | MINIMIZED | 119.690 | 8.341 | {'NONE': 1797} |
| 4 | MINIMIZED | 119.799 | 8.338 | {'NONE': 1798} |
| 0 | MINIMIZED | 119.928 | 8.336 | {'NONE': 1800} |

16主体AFK段实际有238帧报告SHORT_AFK，末上限30；MINIMIZED同16主体全部1797循环为NONE，末上限120，循环119.690/秒。三组MINIMIZED实际TPS分别20.048/19.989/19.988。这确认先前30FPS尾段由游戏闲置规则污染；当前短平场景没有观察到16空主体导致的持续明显下降，不推断复杂/动力装置、GPU耗时或长期生存性能过关。

证据：外层C7/verification/native-baseline-v5（失败）与v6（完整），包括SOURCE-SHA256、实际RESULT、日志、PNG及EVIDENCE-SHA256。正式dev62 JAR逐SHA仍8d38c897414475df6c39f0f7ed1fa71893b01b8e9e43f1fc1a13b8c8961ea096。下一独立正常时间有限机械路测见C7-NATIVE-DRIVING-SPEC.md；不重复已有稳定full48或125项来替代新测量。
