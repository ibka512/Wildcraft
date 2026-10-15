# 本机测试缓存归档与空间恢复

2026-10-13续跑先核对额度：五小时剩99%、周45%，未使用重置券。Git接续3fc3fc56/feature/adopted-art-runtime，正式dev37包和原源保持。

原32Gi APFS开发映像仅33Mi可用；已核对现有开发入口与映像 `/Volumes/仕事/Wildcraft/Wildcraft-开发环境.sparsebundle`，继续使用原工程，没有新建项目或更改挂载/文件系统。恢复前未发现活动Java/Gradle/游戏进程。

将当前构建编号f51c43e6462c的专用run测试目录完整写入外层同盘 `/Volumes/仕事/Wildcraft/归档/测试缓存-2026-10-13/f51c43e6462c-run.tar`。tar成功后逐成员核对路径/类型、文件大小和归档内容对原文件SHA256，核对成员集完全一致：65365文件/82163成员，原文件6686667391字节，归档6832657920字节；归档SHA256 `342d91e9eb9b28125f8cfac9d054b7aa962b3aeed72201c4dbb168b8588cdfb5`。完整逐文件清单VERIFIED-MANIFEST.json与恢复说明.md同目录，仅本机，不上传测试世界/运行文件。

校验通过后才删除该专用run缓存，源码、Git、美术原件、所有历史安装包与快照、其他缓存和其他美术任务变更保持。开发卷实测恢复6.5Gi可用，原缓存可从tar恢复到空目录；重新运行测试将生成新专用世界，不接入个人存档。映像容量仍32Gi，并未扩容。

此前manual probe v1停在Gradle配置No space left on device，不是玩法测试失败。空间恢复后运行v2，结论以实际日志为准，不把环境恢复写成手动源通过。

## dev44后的第二次专用测试缓存归档

2026-10-13完成26类848图本机44后开发卷约1.8GiB。确认完整客户端退出且run无打开文件后，仅八个专用*-client-test目录完整写入外层同盘归档/测试缓存-2026-10-13-dev44/verified-specialized-run.tar，16666成员/11795文件/2649100694原文件字节，逐路径类型/大小/SHA核对，再核归档SHA dfe8d3d4f5d320aad56216bc7b496743803a2c4826d727289a0c13afd645ee48和源mtime/大小稳定后释放。完整清单与恢复说明同目录，仅本机。源码/Git/原美术/全部历史包/正式44和其他任务保持，常规client与其他run目录不清；原32GiB映像未改变。实际释放后的空间见该清单freeBytesAfter/df，下一测试会重建专用目录。

## dev46后空间根因与恢复

三个已结束专用hop/hurt/sword client-test目录完整tar+6147成员4416文件1028042095字节逐SHA核对，归档SHA99d7a938109f1075c959f8a1658c693203f57e79f32a23d7e75d107700638849，确认run无打开文件后释放，恢复1.2GiB。过程中47converter写入No space失败，仅候选资源未完成、未启动游戏；修复空间后必须重新完整转换验证旧211。

进一步git count-objects实测四个闲置tmp_pack垃圾4.96GiB，正式五pack4.29GiB保持。lsof无持有、无git进程/gc.pid，四文件完整复制外层归档/Git临时打包残留-2026-10-13并逐SHA/mtime/size验证，VERIFIED-MANIFEST记录；再次无打开文件及源稳定后仅删除这四tmp文件，未动.pack/.idx/提交/标签/index/源码或美术。开发卷实测恢复6.17GiB。git gc.auto本机配置0防自动失败repack再次写满，不新增生产依赖；正式对象完整检查结果见实际fsck日志。原32GiB映像未调整、其他美术WD与真实数据/历史包保持。

## dev49后完整专用缓存归档

2026-10-13，映像剩2.4GiB，完成48/49实际图形JVM结束后，仅bow-transition/shield-transition/axe-transition三run目录完整tar/8957成员6551文件1553673304原字节逐SHA和archiveSHA/源mtime-size再次核对、lsof无持有后释放；archive SHA8166ddf9c2d34ed4062f7073b50ac75624c781c3c24eea7fba3d94705e63dfa7，外层归档/测试缓存-2026-10-13-dev49，VERIFIED-MANIFEST含全部记录与恢复入口。空闲4246974464字节约4GiB。没动source/Git/正式包/原art/真实存档；当前32GiB映像不改，gc.auto0保持。候选50创建独立trident-transition目录，不重复空项目。


## 23:33续跑后开发缓存恢复

2026-10-13先核实额度primary剩100%、周69%，实际卷剩51Mi。已终止trident/spear专用run完整归档到外层测试缓存-2026-10-13-dev51，6222成员/4592文件、1107419033原始文件字节，每成员SHA/源mtime大小再核/无打开句柄确认后才仅删两缓存目录，正式50/51证据与包保持。archive SHA d7cd7f3e665b2b5aac08c3d6c7fd135d3491377a81cb8206e45c1d718d3f8e86，恢复1324662784字节可用。

另发现原正式pack合计已增长（保留全部5正式pack/multi-pack-index），两个闲置tmp_pack_OnXkCx与tmp_pack_kSD5sY合计1135607808字节；完整外层复制到Git临时打包残留-2026-10-13-dev51，逐SHA/大小mtime稳定/无打开句柄确认后仅移除两临时残留。不能把正式pack增长称缓存或删正式pack；谁触发此前打包未核实。git fsck实测结果以/tmp/wildcraft-dev52-git-fsck.log与进程exit为准，未完成前不称通过。本机gc.auto=0保持，额外maintenance.auto=false防本任务自动维护，未修改全局设置。开发空间恢复约2.2GiB。

git fsck --full已实际结束exit0；全部正式pack/refs/index未修改，两个已备份临时残留以清单为准。


## 正式52后仅回收已完成专用缓存

2026-10-14实际full35/1170截图和正式52 CRC/SHA已独立保存后，完整外层归档mace-transition-client-test，3221成员/2395文件、581101674原始文件字节，逐成员SHA/源稳定/无打开句柄确认后仅删除可再生成专用缓存。路径测试缓存-2026-10-14-dev52/verified-completed-transitions-run.tar，SHA d8e53cdd8eb08c7e58ba9f73c7d30c04358723de4bbe1e78288d45d17484adde。正式52/final和完整证据/其他历史均保持，恢复1835503616字节可用。


## 正式53后可再生成缓存回收

完整36类/1220截图与正式53 CRC/SHA独立保存后，完整外层归档crossbow-transition-client-test，3546成员/2609文件、625384555原始文件字节，逐成员SHA/源稳定/无打开句柄确认后才只删专用缓存。路径测试缓存-2026-10-14-dev53/verified-completed-transitions-run.tar，SHA 92cd34bf278513fbaa938f25945fbf745c1ae5621352ccc7206b9c37d43aca31；恢复1157230592字节可用，正式包/候选/独立证据/原源/Git/真实存档与其他美术WD保持。


## 完整54期间闲置旧版本构建缓存

54图形客户端运行期间，只读取/归档无打开句柄的版本<54旧generated libs，176文件676166164字节。每成员SHA/源mtime大小稳定/无打开句柄再次验证后仅删原build/libs缓存副本；正在运行的54 JAR/依赖/源码/Git和所有development-assets正式历史包均保持。外层旧版本构建缓存-2026-10-14-dev54/verified-old-generated-libs.tar，SHA 2b2f5e3a8f8ae69aceade2e26570fb3495851dcbaa2200ad64c7aba0bf3b8ce3，清单保留全部原件成员，可恢复旧缓存，恢复1428008960字节可用。


## 2026-10-14 dev54完整验收后缓存归档

已结束的ground-trident-client-test单目录完整外层归档，3903成员/2837文件/672461610字节；逐成员与文件SHA、源稳定和无打开句柄核验后仅释放可再生成缓存。外层归档/测试缓存-2026-10-14-dev54/verified-completed-transitions-run.tar，SHA256 9fa2ce686af4cd9cfc2f5cf5e86fe008232fb46530ec8270a1effaa218dc541d；释放后1103503360字节可用。原件/Git/正式历史包/真实数据/其他美术WD保持。


## dev55完整验收后完整run归档

{"localOnly": true, "scope": "Only one completed generated test run directories; production source/Git/art/real saves/history packages untouched", "archive": "/Volumes/仕事/Wildcraft/归档/测试缓存-2026-10-14-dev55-full/verified-completed-transitions-run.tar", "archiveSHA256": "3023d50680a20e55496a1be3e7ec84605ccd4e09b9a72c95da61052fc417cd5d", "names": ["ground-spear-client-test"], "members": 3590, "files": 2720, "rawBytes": 680169388, "verifiedEveryMemberAndFileSHA": true, "deletedOriginalGeneratedDirectories": true, "freeBytesAfter": 1003900928}

实际screenshots从该已验证tar恢复到verification/dev55/full-client-v1，完整缓存原样仍在外层，无历史原件或真实数据删除。


## 2026-10-14 dev58运行期间安全缓存归档

结束的gameTest完整1538成员1511文件25071230字节逐SHA/源稳定/无句柄验证外层测试缓存-2026-10-14-dev58-server，tar SHA6865f30cb291533b2a163084c562f1bf3f61044de932150c49902d3ed5846e8d，仅释放可再生成测试缓存，实际Spear客户端在外层独立run保持。

libs仅版本<58的12个闲置generated JAR共141716271字节，完整外层归档/旧版本构建缓存-2026-10-14-dev58/verified-old-generated-libs.tar，SHA9508a7de399afff16889aa214fbac21b88d7c8957f66c6d50e7272d48d8387d9；逐成员SHA、源mtime/size/hash稳定、无打开句柄验证后只释放这些缓存副本。实际58运行JAR/依赖/源/Git/正式历史包和源数据/其他美术WD不动，恢复375791616字节可用。


## 2026-10-14 dev59完成后外层缓存与旧libs

C6AH完整42已结束，7337文件的generatedrun保存为cache tar和已核实正式proof的1729张截图双来源备份；每个文件SHA、tar成员/源稳定和无打开句柄核实后仅释放原generatedrun。归档44c6a8110f292a94365241e4eda7d23d158c70a61d5f1e698f54a33701fc237e，恢复映射见归档/测试缓存-2026-10-14-dev59-external/VERIFIED-MANIFEST.json。Formalproof必须保留，它是完整备份的一部分。外层约4.6Gi可用。旧58/59 libs六文件83697062字节tar逐成员SHA与源稳定无句柄验证后仅删生成cache副本，归档SHA953b00feae57dbe2cb9eebceab6855c258fafe6467bcdeb45231392c12a5b92d；原APFS大小/Git/正式历史包/原件/真实save/其他美术WD保持，未resize或迁移。


## dev60完整43结束后外层generated缓存备份

7416文件/1780实际PNG，formalproof中的截图逐SHA与生成run比对一致，其余所有成员保存tar并逐SHA/原source稳定/无句柄核对；两来源完整backup manifest在归档/测试缓存-2026-10-14-dev60-external。归档SHA e6ecda7a50e0b3abddb33e716f1c82ae6a70c6316f5e0f32576363ae082f4fc7，核对后只删已结束run-full，formalproof是backup一部分必须保留。外层恢复约2.9GiB；原件/Git/正式历史包/真实data/美术WD/APFS大小保持，无迁移发布。


2026-10-14 C6AL：仅13个已结束专用测试目录完整gzip归档，逐成员SHA/源稳定/两轮无打开句柄核对后释放；活动run-full-v2、全部proof/candidate/formal/历史/原件/Git/真实数据保留。实际3717成员、3160文件，archive SHA 401c0f722f7cedf230961f4416b30d8fb4ece0c54832a5fa53f2641318f6428a；路径归档/测试缓存-2026-10-14-C6AL-ended-runs，完成后外层约1.7Gi可用。源临时sources JAR只在候选CRC/SHA完整副本后释放缓存，不改源稿。


2026-10-15空间恢复补充：旧测试缓存f51c43e6462c-run.tar由6832657920字节无损gzip为1550311024字节，原tar SHA342d91e9eb9b28125f8cfac9d054b7aa962b3aeed72201c4dbb168b8588cdfb5，gzip SHAede367fa9e2096219d260dc74cea9fe178f61c412e3e966fa365f33c1b00f773。完整暂存副本解压字节SHA/原源稳定无句柄核对后，仅替换可再生成cache的归档载体，外层永久复制再压缩/解压双检后清理临时副本；恢复原tar需解压到原路径并验原SHA，原文件成员VERIFIED-MANIFEST不改，索引LOSSLESS-CACHE-REPACK.json。历史安装包/美术原件/Git/真实存档保持。

当前32GiAPFS仍不足时，旧hash260a5f2b3758仅两个已终止自动packaged-client-test/gameTest缓存完整gzip外层归档逐SHA/源稳定/无句柄后释放，10460成员7333文件，SHAef076322a6a01cf27b9105afbe0ed1d83b03c0290574334bb5401067783e082a，路径归档/旧工程生成测试缓存-2026-10-15。旧client/server目录、libs/源/历史/所有proof保持。开发内部恢复约1.2Gi，外层约5.8Gi。当前构建/125服务端24秒clean成功，主/源包SHA与冻结候选一致，完整46v4正在重跑。v2游戏任务47分11秒1872PNG已完成但wrapper缓存关闭失败不得认整体验收；v3在jar构建15秒失败未入游戏。
