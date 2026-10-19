> 本机历史规格或验收；公开版不含许可受限动画数据。本文的本机包/源稿/截图链接不是公开下载，也不是公开版重新执行结果。

# C6AO 已采用第一人称普通剑击验收

2026-10-16。本机dev63，源码提交/tag见本机manifest及ACTIVE-CHECKPOINT。当前只接入已采用first-person-sword/v01，C6整体/其余正式第一人称/复杂第三人称及C7人工长期生存未完成，未发布上传。

## 实际行为

独立6变形骨＋隐含root、原版64×64皮肤UV、连续手臂与袖层。原生地面未蓄力SWING驱动A的0.65–2.05秒，两身形×左右主手×真实空副手/盾共8变体；地面移动/蹲行使用同一相机空间手臂。源WXYZ明确转XYZW，摄像机(-x,z-eyeZ,y)正旋转，无镜像UV。原生真实ItemStackRenderState只提交一次并保留显示变换、原尺寸与Fuse层，不加载参考剑盾。

强击、空中、受伤、使用物品、GUI、伞/攀爬等依原优先级；缺少可选源仍原手臂。没有新的输入、连招、伤害、耐久、精力、物理、计时或存档协议。B/C/AB/Combo不启用。

## 本次实测

- 源Blender SHA b1ddc3e2d01d60215de0df24023cf38a8e759010e5ccd3903c2a43110e6f6dd2；数据JSON SHA 4c876cf0587a2fcdffd3029fa2abe67c8297161878edaf94462245764f594ee7，均与采用目录核对。
- 八个运行变体8网格、1352源帧、951808采样顶点位置对照原GLB，最大网格误差5.7657353672e-07格、握点6.91482070003e-07格、盾中心6.97467364382e-07格，剑轴误差1.39890203117e-06。40作者剪辑只检查数值掌点，不冒称40段玩法启用。
- 最终同一个dev63成品JAR：125服务端、构建、7类成品客户端（FirstPersonSword/Grip/Equipment/Guard/GlideRig/CoreArt/BackEquipment）clean通过，耗时4m 26s，228张真实PNG。原先4m46s/227图候选和19图初版保留，最终以本轮为准。
- 16标准/纤细、真实左右主手、空副手/盾、袖层组合；实际native皮肤材质Sampler0、延后GPU手臂回调、原剑base quads与真实盾special model均观察。每源帧只提交一次原剑base，袖层开关对应2/4个皮肤网格；真实掌点及柄点/原native quads验证。一次实际输入只原native hit/wear/Fuse，轻击精力不增扣。
- 实际移动/蹲行源、火把占手/蓄力原路径、恢复结束、原生5TPS/10FPS、真正PauseScreen和InventoryScreen退出通过。不是人工手感或性能认证。
- 缺源成品测试clean通过，1张PNG；仅去掉新可选数据，实际旧皮肤手臂/原生物品仍渲染。
- datagen单独先执行，68文件/零写；资产核验45ID、4底部、2107源记录、57运行副本通过。旧第三人称两体型各337段及focus/provenance四文件逐SHA保持，新第一人称每体型4段独立计数。

## 真实失败与修复

初版误读客户端Fusion DATA隐私投影：wear=1而projection.remaining=0。改检查公开VIEW.uses及服务端真实DATA.remaining=31，不修改生产Fuse/隐私协议。v1/v2失败截图与日志保存。

v3/v4游戏断言完成，但Gradle收尾NoSpace，明确不计clean。之后完整Gradle/build/test缓存暂用已复制验证的内部盘生成目录，原工程/资源/包仍外置。合并调用build/runDatagen触发Gradle任务输出依赖校验；采用先单独datagen、再build的流程，没有改松依赖检查。短编译错误未进游戏，不作正例。

最终加严真实暂停fraction反例：原code暂停时清0小数进度，34秒实际红 source=1.21、原生应为1.2226007、partial=.04500234。最小修复保留原版已冻结的render partial；33秒绿source=1.2351992与expected相等，随后最终7类全部重跑。没有新增自建时钟、世界tick/技能计时变更。

## 包与复现

主JAR：22241225 bytes，SHA 6f78612dd42f16434fc2e4e68df46c2654ddccebfcb07f1d0062f0e873a3b7d7。
源码JAR：21763847 bytes，SHA f95f6d0770aa10d4feb1bfab8fb749b8722b7a028e2da6ef73be40bbb751570d。

包含混合许可玩家派生数据，仅本机。原dev62和初版候选均保留。新FP原始派生数据增量快照在dev63/final/private-animation-resources，旧337段数据复用不可变dev62快照，完整运行包内包含两者。

```sh
# 本批临时生成缓存已准备好；重启清理/tmp后按C6AO环境记录从归档恢复/重新生成。
WILDCRAFT_CACHE_HOME=/tmp/wildcraft-c6ao-generated-cache ./dev.sh runDatagen
WILDCRAFT_CACHE_HOME=/tmp/wildcraft-c6ao-generated-cache ./dev.sh build -PmodExportDir=/tmp/wildcraft-c6ao-generated-cache/export -PacceptMinecraftEula=true
WILDCRAFT_CACHE_HOME=/tmp/wildcraft-c6ao-generated-cache ./dev.sh runFirstPersonSwordClientTest -PfirstPersonSwordRegression=true -PfirstPersonSwordRunDir=/tmp/wildcraft-c6ao-new-regression
# 暂停小数进度小范围复现：-PfirstPersonSwordClockOnly=true
# 缺源实际回退：-PfirstPersonSwordFallback=true（独立客户端）
```

实际入口和完整冻结main/test JAR、metadata、源码SHA、逐PNG SHA、红绿/早期失败均在开发接续记录/2026-10-16-C6AO/verification。不能重复把老partial报告当final。
