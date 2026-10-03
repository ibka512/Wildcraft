# 本机迁移后的开发入口

2026-10-03，项目根目录为 `/Volumes/仕事/Wildcraft`。本机目录布局与 GitHub 克隆的普通工程布局不同；后者无需磁盘映像。

## 为什么使用盘内 APFS

外置盘 `仕事` 为 ExFAT。原工程将缓存放在内置盘以规避 AppleDouble、文件锁与清理问题。为将项目运行文件也迁到外置盘，本次在项目根建立 `Wildcraft-开发环境.sparsebundle`，内为 APFS，挂载在 `开发环境/`。32 GiB 是容量上限，实际空间按内容增长。本次没有格式化或改变外置盘原文件系统。

```text
/Volumes/仕事/Wildcraft/
  开发入口.command
  README-从这里开始.md
  Wildcraft-开发环境.sparsebundle
  开发环境/                  挂载点
    .wildcraft-runtime       仅本机的识别标记
    project/                 当前 Git 工程
    cache/                   Gradle/Loom/构建/测试与历史实验目录
    tools/                   独立 JDK 25 和本机原生库
  原始交付/                  原设计与桌面美术
  归档/                      旧交付、旧工作、迁移清单和验证
  交付包/                    GitHub Release 下载文件
```

从项目根双击开发入口可挂载并打开 IntelliJ。也可执行：

```sh
./开发入口.command --version
./开发入口.command runDatagen
./开发入口.command build gameTestJar
./开发入口.command runClient
```

挂载后在 `开发环境/project` 执行 `./dev.sh` 效果相同。该入口识别相邻 `.wildcraft-runtime`，选择相邻 JDK 与缓存。环境变量 `WILDCRAFT_JAVA_HOME`、`WILDCRAFT_CACHE_HOME`、`GRADLE_USER_HOME`、`WILDCRAFT_PROJECT_CACHE` 仍允许显式覆盖。

本机 JDK 为原已验证 Homebrew OpenJDK 25.0.4.1 的独立副本；其第三方原生库也复制到 tools 内，原生加载路径已重定位并本地签名，记录在本机 `tools/PORTABLE-JDK.json`。原系统 JDK、IntelliJ、Blender 等共享软件不卸载。构建无需原项目目录或原内置盘专用缓存；IDE 中若已有全局 JDK 配置仍可使用，也可将 SDK/Gradle JVM 设为 tools 中的 Contents/Home。

新路径产生新的构建/项目缓存编号。旧运行世界保存在迁入的旧编号和 labs 下，不因路径变化自动接入新测试。自动测试只使用专用世界，不迁移个人存档。

## 普通克隆与其他机器

未发现本机标记时，macOS 继续默认使用 `~/Library/Caches/Wildcraft`。需要外置 APFS 缓存时设置：

```sh
export WILDCRAFT_CACHE_HOME=/你的APFS位置/Wildcraft-cache
export WILDCRAFT_JAVA_HOME=/你的JDK25/Contents/Home
./dev.sh build gameTestJar
```

Windows/Linux 仍使用自己的 JDK 25 与 Wrapper，不要求本机路径、JDK 副本或映像。缓存可以重建；源码、可编辑美术和原始资料不能以可重建缓存替代。

## 使用与备份

映像未挂载时目录暂时不显示工程，先使用开发入口。完成使用后关闭游戏、服务端、Gradle 和 IDE，正常卸载开发卷，再推出外置盘。备份磁盘映像也应在卸载后进行，避免复制写入中的映像。若移动整个项目根，使用根目录开发入口以新根位置重新挂载；不要在未挂载的同名目录继续写入工程。

映像、缓存、JDK、Minecraft 游戏文件、运行 EULA 和测试世界只在本机保存；GitHub 仅分发可公开的项目源码、资料、美术和校验。迁移实测见 MIGRATION-HANDOFF-2026-10-03.md。
