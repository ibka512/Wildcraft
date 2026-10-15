# C6AL 当前候选与失败定位

正式本机仍dev60/64188217，C6AJ/C6AK已独立通过，候选参数dev61/335clips未正式归档。旧333每键、全部非clips几何绑定逐值不变；新的RIGHT两段是已采用LEFT源的运行派生，不改作者原件/APPROVAL或冒称正式RIGHT/第一人称美术。

首次真实RIGHT专项43秒红，在native use第5刻因源缺失终止。先排名三个可核对原因：真实右手模型的弦目标不可达；镜像足部支撑退出；新使用手归属/进入状态错误。只在gametest加入RightBowReachProbe，不向生产代码插日志，不改库存/重力/计时。

实际原生模型探针28秒：5/10/13/18/20刻目标距拉弦肩0.927/1.033/1.194/1.360/1.379，源两段长度总约.5625，全部fit=false；同时实际ground/STANDING/RIGHT/sameUse/native bow nonempty/native phaseIDLE。确认左右原生display平面不同，不能照搬LEFT平面。

最小修正只限定ground真正LEFT继续旧反向，ground真正RIGHT使用对应原生平面，进入混合的当前native reference也按该实际臂一致；airLEFT/待机/长武器等原接口与轴不改。复跑同探针27秒，五点肩距.406/.486/.494/.504/.518全部fit=true，实际源draw/hold出现；不伸长骨骼、不缩放弓、不改游戏20刻。两轮完整native source/生成quads/一次箭-wear-ammo仍需专项验收，目前四组合v2运行中，不据数值绿宣告完成。

数值初探整palette不对称的失败报告保留；22蒙皮关节配对一致，两个不蒙皮weapon references有不同旧剑rest偏移/方向，因此派生中辅助bow reference按实际源反射，再由真实弓握点检验，不硬套剑rest。390帧/体型双反射和源段长误差0不等于游戏通过。

失败/最小数值红绿与真实截图在外层C6AL/verification/red-green-native-reach。正式历史包/原件/Git/真实存档/其他美术WD保持；构建缓存5文件57735632字节经完整tar逐SHA/无句柄/源稳定核对后释放，记录归档/构建缓存-2026-10-14-C6AL，SHA59538c5ecba5117db09ca64747051405fb8cb3f19c4993473ea7f039ae926358。无上传、新依赖、迁移、采用待审13/另6或重置券。


第二轮完整RIGHT专项28秒在实际hold等待真实持物绘制时失败，已有1张draw截图，native source/nock数值正确。固定RigAudit.hands仍为0，明确定位ItemInHandLayer仍只允许LEFT bowUse，实际RIGHT弓未进入同源持物路径；没有删除GPU断言，最小修改按当前已确认源后缀的真正持弓臂选择，AIR现有_L保持。继续第三轮真实GPU/箭验收，未通过前仍候选。


## 真实RIGHT与退出专项已过，完整46进行中

第三轮RIGHT四组合实际45秒16真实PNG通过：两皮肤 × mainRIGHT/offbow-mainLEFT，真实原20刻/三档native sprite、source木握点/实际弦与固定两段长度/native head及原皮肤GPU，一次实际箭、弹药31、wear1，原使用臂不强改。独立边界38秒4图通过：真实slot取火把零箭/wear/ammo、穿盔甲、背包和行走及时交原路；未删真实持物GPU断言。截图已人工检查，反射是运行派生，不冒称新正式RIGHT美术。

独立datagen8秒/68零写和资产45/30/2107/57/4通过。最初build/runGameTest因APFS卷写满（df虽32Mi保留量仍无法写入）错误，停止确认为run/gameTest的进程，未计通过；2041成员2014文件逐SHA、源稳定/无句柄完整归档后仅释放失败生成缓存，SHA5b38dfcdc517c60e1a994c87428f53e0f711bddde110d38c49e5b3f07ddc4bf0。首次保存私有脚本也因空间失败、文件0字节，未运行归档；随后从外层/内存执行的实际完整归档通过，没有拿空脚本称成功。

按原已授权本机EULA，用私有初始化配置将本次新server测试目录指向外层新run-server-v2，真实存档和原EULA不动；后续build/server实际33秒通过，All125 required tests passed及XML保存。只归档可再生成缓存，未改32Gi映像/项目路径/Git/历史包/真实数据。生成sources JAR已完整CRC/SHA复制到候选后核对无句柄/源稳定，仅释放其重复缓存副本。

当前完整46实际metadata唯一46：新RightGroundBow、ActionHandover、TraversalHandover＋既有完整43。外层run-full-v1、/tmp/wildcraft-dev61-full46-v1.log已启动，尚未结束，不能写成完整46或正式dev61。候选包/335源快照保存在development-assets/local-only/dev61/candidate（外层别名），actual SHA与CANDIDATE-QUALIFICATION.json明确not formal。无新依赖、真实迁移、待审13/另6采用、上传或重置券。


## 完整回归的顺序问题：已最小红绿，重跑46

完整46首轮53秒/20图在RIGHT通过后C6AJ普通jump等待hold超时；连续两类最小重现1分/55秒/59秒红均显示开伞真实有效。输入observer确认gameplay正常、slot/main/off/item/use/attack/defend均未触发取消；服务端只读gametest observer最终明确since=2、原input仍open、物品不变、rate20，原server onGround=true使eligible=false，属于接近落地的合法收伞。Native客户端当时尚显示Y250.36，不为了保源改变server/physics或允许落地滑翔。

第四次诊断2秒仅测试private server字段引用编译失败，未进入游戏；修成已有level().getServer()后第五次诊断43秒偶然赶上hold而通过，但同一次仍记录相同合法接地关闭，不能用这次偶然绿证明旧hold要求正确。均保留真实失败/观察与测试源。

只修C6AJ测试：在实际active时检查合法glide_enter或hold、原伞真实GPU与唯一双视觉手/真实库存/取消攻击；普通跳跃没有保证.30秒进入完成前一直保持空中。持续hold仍由高处GlideRig与C6AK真墙高度检查，不减少持续滑翔验收。清理全部临时observer/mixins和OPEN诊断日志，飞行/收伞/源时间不改；原两类最小第六次43秒22图绿，库存/物品takeover/一次结算断言保留。全46第二轮尚待运行完结，不正式归档61。


完整v2：46游戏检查实际跑完47分11秒、1872真实PNG，客户端任务未报失败；但root生命周期/last-build.bin/daemon registry缓存退出写入NoSpace使最终Gradle退出失败，不能写整体完整成功。完整原日志和逐PNG SHA记录在full-client-v2-native-task，截图同卷移动且逐SHA核对，未正式归档61。

旧可再生成测试缓存tar6832657920字节已无损压缩1550311024字节，原tar SHA342d91e9eb9b28125f8cfac9d054b7aa962b3aeed72201c4dbb168b8588cdfb5，压缩SHAede367fa9e2096219d260dc74cea9fe178f61c412e3e966fa365f33c1b00f773。完整临时压缩副本解压逐字节哈希验证/原件稳定无句柄后退休tar，再外层永久gzip复制及压缩/解压哈希双检，临时副本已清理；原VERIFIED-MANIFEST与文件成员SHA保持，恢复原tar需解压并验原SHA，详见LOSSLESS-CACHE-REPACK.json。只改再生成cache归档载体，不改源/真实数据/历史安装包/美术原件。

外层恢复约5.3Gi；实际help4秒clean成功、project小文件写读删除验证成功。为取得真正完整零退出错误，保持运行代码/资源/测试断言不变重跑完整v3，仍未正式61。归档脚本移至外层prepared-archive-tools且实际解析/字节复制检查，追加写空间失败的旧脚本不当作新逻辑通过。


2026-10-15重跑v3在jar构建15秒因内部空间不足失败，未进入游戏，不计第二次46检查。旧工程hash260缓存中两个明确已结束自动测试目录完整外层gzip逐SHA/源稳定/无句柄后释放，内部1.2Gi、外层5.8Gi恢复；详见缓存恢复文档。重新build/125实际24秒clean，主/源包字节SHA与冻结候选完全相同。当前完整v4已再次进入游戏，等待真正零退出错误才正式归档。下一C6AM源4派生与独立草稿编译5秒只准备，未renderer/资源激活/玩法验证。


正式归档前实测：完整v4实际46分46秒、46类/1872真实PNG、Gradle exit0且最终clean成功；125服务端/build24秒、datagen8秒68零写与资产检查通过。所有旧333/非clips几何绑定逐值保持、candidate到final版本/CRC/SHA及57..60历史哈希通过；本机final61和完整证据已保存，见C6AL验收。此前47分11秒游戏任务但wrapper失败仍单独保留，不混为clean完整通过。正式源/标签随后由当前Git实际提交核对，不上传。
