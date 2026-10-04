# P6 原创机械原型美术

本阶段新增原创 16×16 机械主体/风扇图标和 8×8 白色材质。像素源为本目录 JSON，执行 `python3 art/tools/generate_mechanics_art.py` 导出游戏 PNG。沿用原版低分辨率材质，不使用任天堂图片或外部库。

实体几何的可编辑源为 `src/client/java/dev/wildcraft/client/mechanics/MachineRenderer.java`：1.4×0.65 主体、六个节点、外壳与旋转风叶、动态电量条。各节点朝向使用与服务端一致的 MachineNodes 坐标。视觉部件伸出主体，碰撞仍是单一轴对齐主体，复杂复合碰撞暂缓。美术可后续替换，当前为可辨认的游戏原型。
