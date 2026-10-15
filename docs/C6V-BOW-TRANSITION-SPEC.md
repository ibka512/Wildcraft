# C6V 已采用弓取收源准备

2026-10-13，正式dev45/1bce0f6c，候选46/e9dfb6cc完整28类回归中。本文件与只读资格不表示已启用弓取收。

采用bow-transitions/v01（APPROVAL 2026-10-10），双身形22骨各16源，draw/stow×back/waist×empty/shield×左右主手。先接默认无盔甲披风平面静止背部16源，侧腰/其他远程与复杂形态、正式第一人称旧程序。源末draw/首stow沿已采用bow-hold/v04握法；原生BOW的木握位/尺寸/display/glint/原生拉弦与箭必须保持。

源掌点沿实际腕局部-0.125，与近战-0.0625不同，必须保留真实bow参考在旧weapon slot的明确alias，不能当剑握点。现有BackTransitions.category RANGED与同一owned reference/新owner/主手真弓draw或真实挂点bow+主手真空stow，原 .32有效实际秒Settle拥有时钟与暂停，禁止新计时或物品代理。实际使用弓后登记，由真实库存/原用弓路径决定，不假登记或新增库存。

源back中心[0,.56,.36]和外侧waist演示与当前游戏挂点不一致；原挂点配置保持，源本身是有界辅助伸取，不宣称手掌精确够到实际挂点或完整拔弓。只源人物和真实native物品/现有back layer，GLB装备网格/纹理/显隐不接。原生开始use/draw/释放、GUI/换新物品、受伤/探索优先，实际放箭/ammo/wear/Focus由既有代码拥有。

资格核对32源根/有限数/绑定、实际时长/腿脚和头旋转、源握点与v04端点；运行需16真实同reference取收/两皮肤左右手carry/empty/原皮肤和实际nativeBOW木握位/背部唯一新owner及GPU、源 .32与pause/10FPS/5TPS/新物品/spare/GUI/实际use-一箭与source互斥。缺源或不合格立即原路；保留原211/几何绑定和历史包/其他美术WD。实际build/125/相关完整回归通过才正式归档。

所有混合源/派生数据与含数据包本机，不新增生产依赖/真实迁移/发布；待审13正式第一人称与另6补齐不采用，C6整体未完，多人暂缓。

按节点名对齐全部28 palette，默认背部draw末/stow首与v04待机16端点只读比较通过，最大约6.132e-7；初次按数组顺序比较1.299差异是节点顺序错误，不是动作偏差，已更正工具。持盾源名字shield对应旧待机carry，原数据/游戏/挂点未改。仍不是原native bow或实际取收GPU通过。
