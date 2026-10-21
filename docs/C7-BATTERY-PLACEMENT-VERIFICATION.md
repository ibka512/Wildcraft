> 本机阶段记录；截图、世界、源稿和含私有数据包未公开。本文是历史结果，不是本次公开克隆的执行回执。

# C7 电池原生手持定位验收

2026-10-20，本机正式dev78/source5cdea4a1。dev77图集修复保持，只有BatteryItemRenderer.java生产源码移除重复半格平移，getExtents使用原始0..1几何；原生ItemTransform.apply本已中心化-.5。没有改模型/display、geometry、32px纹理、动态电量窗口、库存、费用、物理或任何计时。

实际先记录稳定等待真实1.5秒的dev77视图：TP电池脚边，FP看不到；诊断native local矩阵为单位阵，原生ItemTransform自动-.5，Renderer再-.5造成三轴重复。已排除单位和截图过早。原生class只读核对，未复制游戏源到项目；临时DEBUG已从源码清除，原始诊断快照仅外层。

固定dev77真实红28秒：从已加载原生item state读取display/local矩阵，独立将不带人工偏移的已采用raw geometry extents送入原生矩阵，得到期望；实际getModelBoundingBox整体多-.5，误差0.866025。这个反馈覆盖真实模型调用，不是只检查字符串。只删重复平移/偏移后，原BatteryPlacementClientTest、Audit与两Mixin class字节不变，候选78实际绿30秒/8PNG。

四个实际标准/纤细×左右主手变体，每种六native context（FP左右、TP左右、GROUND/FIXED）边界正确；八个实际FP/TP绘制场景的deferred geometry矩阵与进入special renderer时已完成原生变换的父矩阵一致，误差门槛2e-5。没有另报未导出的最大误差为0。八张PNG已逐一读取：FP电池可见，TP在对应手边，左右相反正确，电池真实堆栈仍1/1000。不是精确指节握持或新动作源。

同一candidate生产JAR随后第二批美术/能源/机械三类实际59秒/17PNG通过；测试JAR成员/class字节与首绿全部相同，仅fabric.mod.json改真实entrypoint。合计4不同客户端类25PNG。独立datagen7秒68零写、build9秒125必需服务端通过，不把test NO-SOURCE算测试，不重复稳定full48。全部PNG/日志/源/执行包SHA在本机C7-BATTERY-POSE。

338生产class中BatteryItemRenderer本体变化，嵌套Unbaked仅编译行号等元数据变化，javap-c-p指令文本相同；其他class/全部现有资源逐字节保持，除版本fabric.mod.json。七私有动画JSON和401/FP4计数、原source/model/texture/geometry不变。dev77的items atlas继续生效，其他四美术WD/untracked保持。当前包/源码JAR/许可/七私有源本机安装包/dev78/final，清单development-assets/Wildcraft-dev78-local-manifest.json。

没有新生产依赖/真实迁移/新素材采用/GitHub上传。C6整体、副盾STOW/复杂盔甲/其他正式FP、自然地形/人工获取平衡/长期性能仍未完成。此前C7受控左右/倒车/撞墙/下一格及低FPS数据是固定dev76的单独实际检查，不称dev78全套重新跑过；生产main权威代码与数值没有改变。下一更新本机试玩包，并核对受控水面浮力/载重，不改原生物理。
