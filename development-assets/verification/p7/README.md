# P7 最终证据索引

正式包：wildcraft-0.1.0-dev.12+mc26.3.jar，SHA-256 `986706154f0ead68ea57a1a22177ab1a23cd0cde8b8c4c2751298c2da66d3568`。build-server-tests、full-client、two-client-*、standalone-server为同一正式包；源码和机器清单见上级Wildcraft-P7-source.zip/manifest/SHA256SUMS。

- full-client.log：完整14类回归，5分8秒。
- parts-client.log：完整同包P7专项，最终截图只增加水面拍照等待；无正式代码变化。
- gametest-results.xml与build-server-tests.log：56项必需服务端、规则和构建。
- two-client-*：两个普通独立客户端，fabric.client.gametest=null；ordinary-observer内8张是各阶段实际接收视图。
- standalone-server.log：不含测试Mod，达到Done后stop正常保存退出。

本目录根下10张p7-*.png只从最后parts-client实际运行复制；精确路径与哈希按manifest核对。旧先行验证在verification/p7-wing-wheel，旧停线在P7-CHECKPOINT，均保留历史，不混作本次完整结果。图形自动检查不等于最终美术或人工手感验收。
