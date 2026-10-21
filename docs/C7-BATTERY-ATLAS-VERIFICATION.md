> 本机阶段记录；截图、世界、源稿和含私有数据包未公开。本文是历史结果，不是本次公开克隆的执行回执。

> 后续本机dev78已单独修复手持重复居中，见C7-BATTERY-PLACEMENT-VERIFICATION。本文dev77时的观察缺口保留原事实，不追改成当时通过。

# C7 电池原生图集修复验收

2026-10-20，本机dev77/source4cefd95d。只新增items atlas single source登记现有32px电池纹理，原始模型/纹理/几何/采用编号和电池容量费用不变。全部338生产class逐字节等于dev76，旧七份动画JSON同SHA；JAR只有新atlas文件及其目录项、fabric.mod版本变化。类源码/数据格式/依赖不变。

实际红绿：首v1测试误将GPU texture path当AtlasManager definition ID，Invalid atlas id，没有到达目标断言，不能当目标bug复现。核对原生AtlasManager/AtlasIds后，v2在固定dev76客户端20秒真实断言：item atlas battery为minecraft:missingno，并有原battery_3d警告。保留失败源与JAR/截图。只补items atlas single source，候选77同一BatteryAtlasClientTest.class逐字节不变，18秒真实通过32px图块、19档GUI、左右FP/TP和GROUND五基础材质、四vanilla材质及真实电池1000不变；警告消失。不重绘源稿、不伪造sprite、不改阈值。

随后固定同一候选77 JAR，第二批美术/能源/机械三类1m2s实际通过17PNG；probe与首绿已有class全部相同，只fabric.mod.json改实际entrypoint。真实数据生成7秒68零写且无generated差异，build9秒125必需服务端通过。本批已实际执行采用来源校验，45用途/30版本/2107原件/57副本/4变体通过。

原生视图独立观察31秒3PNG：等待真实1.5秒渲染稳定而非只等加速worldTick后，电池仍显示在第三人称脚边，FP没有可见电池。这是预存手持定位缺口；观察执行完不等于定位通过，dev77仅图集修复，不能声称完整电池手持美术验收。下一按真实special模型原生矩阵定位，不用缩小/遮挡错误掩盖，不改原稿/物理/库存。

当前限定回归4不同客户端类19张材质/相关测试PNG，另3张不接受的定位观察；不计为full48或全部美术。source/真实日志、红绿执行包、每图SHA、候选与正式产物CRC/内容检查均在本机开发接续记录/2026-10-20-C7-BATTERY。最初归档断言只列atlas目录而漏minecraft父目录时在复制前停止，核实额外项仅三目录/文件后修正枚举，未改产物或放宽内容允许集。

包/源码包/许可/七私有源只本机dev77/final，清单development-assets/Wildcraft-dev77-local-manifest.json。本次无GitHub上传/新依赖/真实迁移/待审资产采用，其他四美术WD和untracked保持。复杂盔甲/副盾STOW/其他FP/C6整体、自然地形与人工生存平衡/长期性能仍未完。C7受控四场景120/10FPS及旧driver guard低帧复核另见C7-TERRAIN-STEERING-VERIFICATION，不混成本次修改后的全套验证。
