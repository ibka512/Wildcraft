# 编辑、试听与重建

## 直接试听

在当前本机打开 `http://127.0.0.1:8830/preview.html`，选择场景后点击 A 或 B。页面不自动播放；离开或隐藏页面会停止全部当前和未来安排。对照时保持设备与试听总音量一致。

移到其他电脑时，直接打开 `review/preview.html` 可试听安静、机械、六声预算和快速切换。雨／雪地脚步／战斗需要那台电脑的游戏缓存，并重新配置白名单；缺少缓存时显示错误，不代替原版声音。`exports/preview-mixes` 的八份 WAV 全部只含自有声音，能够独立播放。

## 可编辑输入

- `exports/focus-mix-spec.json`：两个方案、音量、专注触发与场景说明。
- `source/mix-policy.js`：固定测试场景与状态边缘参考，复用 `references/s01-policy.js`。
- `source/mix-review.js`：试听播放、停止、控制与缓存逻辑。
- `references/adopted-s01-design.json` 提供每段时长及音量距离；其历史待审字段是原始源稿快照。采用状态及播放政策以 `adopted-s01-playback.json` 为准，不修改历史原文件。
- `references` 的双专注 OGG／WAV、十二段 S01 OGG 均为原件复制；不用重新录制、归一化或压缩。

## 重建顺序

在有 Node、Python／NumPy、ffmpeg 的环境：

1. 用 Node 运行 `source/check-policy.cjs`，生成场景计划并验证边缘／预算。
2. 用 Python 运行 `source/analyze-audio.py`，读取白名单缓存、计算同刻度测量并重建八份自有混音 WAV；不导出原版混音。
3. 用 Python 运行 `source/build-preview.py`，从原自有 OGG、规范和页面逻辑重建便携页面。
4. 用 Python 运行 `source/serve-preview.py`，启动 localhost:8830。本机缓存路径变动时先核对来源和内容哈希，不开放任意文件服务。

脚本记录了本机工具路径；换机需改为相应已安装工具。`prepare-reference.py` 是首次收集脚本，包含制作中排队登记，日常编辑不要重新运行，以免把交付状态改回制作中。`browser-qa.mjs` 保存此次 Ego 浏览器检查方法，spaceId=2 是本次浏览器会话标识，其他会话需使用其自己的任务空间。

所有混音采用共同输出增益，不分别归一化。调参数后同步更新音量 CSV、测量、试听页和验证，不覆盖历史采用包。

本项2026-10-06已采用B方案。`check-delivery.py`和历史技术检查记录保留首次草稿交付状态断言，不代表当前状态；采用后请以 `adoption.json`、当前manifest和 `review/adoption-checks.json` 核对。原历史审稿ZIP不重打包。
