# P9 最终成品证据

2026-10-04，dev.14，正式JAR c81ce6d7a960d67c3cd2125a2c426a62216e3b5f784c8e2b99d8afc6792a91ac。

build-server.log与server-tests.xml是71项服务端通过；datagen.log为独立资源生成。full-client.log为16类完整回归，实际5分49秒；0083–0089七张P9截图来自这次最终完整回归，非此前旧版专项。ordinary三日志是两个独立普通客户端（fabric.client.gametest=null）和服务端；p9-observer-0–5六张各由观察者自己的网络视图生成。standalone-server.log只装正式Mod/API，达到Done并保存退出。

双客户端实际测试：V融合一次、原版攻击一次、切弓背负、真实丢弃/另一人拾取、满库存拆卸拒绝、腾位后第二人拆卸原材料和31次磨损。完整材料在服务端逐步核对；客户端只看安全显示和4字节原版槽位哈希，不访问另一JVM对象。驱动/附件/场景命令均仅测试JAR，平台/材料为专用生成场景。

原型材料模型复用原版资源，不是最终原创美术；飞行截图为发射后第一帧，不能单凭图证明最终挂点品质。归档旧失败日志留在本机归档，不冒充最终通过。详情见docs/P9-VERIFICATION.md。
