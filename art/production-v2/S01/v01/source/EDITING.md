# 编辑与重新导出

已有环境使用bundled Python + NumPy、Homebrew FFmpeg，不安装新依赖。

修改sound-design.json内每个cue的events（材质、起点、频率、增益、衰减），用render-audio.py重建12母带、36分层WAV和12 OGG。所有事件来自固定seed，没有外部采样。impact为敲击噪声、body为材质/短轴共鸣、air为释放气流；空层保留为静音以便编辑器统一排列。三层以同一cue增益缩放，不能各自归一化后直接相加。

48kHz母带尾部保留18ms静音、32ms收尾淡出、4ms边缘保护。物理共鸣频率不按乐谱制作，避免提示音形成长期旋律。首次修改时先听成对动作，再听多声场景。

check-audio.py实际解码OGG/母带并验证文件、时长、四倍插值峰值估计、削波、DC和分层重建。check-policy.cjs验证独立审稿准入规则；build-review.py将实际OGG与源嵌入便携试听页。修改音频后需更新这些检查与审稿页，历史已采用版本不得覆盖。

可在支持WAV的音频编辑器中把同一cue的三层从0秒排列、统一48kHz、保持原增益，再做试听；OGG是游戏导出，WAV分层与参数才是编辑源。
