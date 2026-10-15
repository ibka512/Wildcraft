# C6AL 实际右手地面拉弓适配

基线dev60与C6AJ/C6AK固定专项通过。现有已采用bow-ground/v05只制作LEFT持弓；本批尝试由同一已采用draw/hold严格X反射和左右骨骼配对生成RIGHT运行派生，不能改称作者提供RIGHT动画或采用待审第一人称。左源、旧333clips、几何/皮肤绑定及原pullTimes保持逐值；源长release/cancel不接。

初始整palette镜像资格报告失败：22个真实蒙皮关节全部对称，只有不参与蒙皮的左右sword_reference具有不同rest偏移/旋转，最大元素差约1；因此不能用整palette的rest相同冒称通过，也不能把真正bow握点强行套成旧剑rest。正确候选仅要求22蒙皮bind配对对称，物理弓辅助reference随源动作反射，最终由真实native弓木握位/长轴/原三档弦/GPU检验。双镜像误差0、390帧/体型源段长误差0；这仍不是实际RIGHT玩法通过。

真实使用臂RIGHT：mainRIGHT，或空main+offbow/mainLEFT；原native20刻/13-18模型阈值/一次箭、弹药、wear、暂停/林克时间保留。支撑弓的right手与实际left拉弦臂分别适配；固定源段长、源肘平面，独立native头和镜像身体腿、真实静止平地/无盔甲披风。不可达或unsupported立即原路，不借占用副手、不改主手设置/库存/物理/攻击/时钟。

先candidate源和两皮肤2使用归属/实际native三档、出箭和退出专项；相关旧LEFT、AIR/Focus和handover回归。只有真实通过才正式dev61归档；初始数值资格不等于GPU/完整C6通过。混合许可资源仅本机，无新生产依赖、真实迁移、待审13/另6采用或公开上传。
