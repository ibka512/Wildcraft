# Wildcraft Java Edition

Minecraft Java Edition 的 Fabric Mod。当前阶段为 **P0 工程骨架**，版本 `0.1.0-dev.1`。

当前物品：**Wildcraft 测试核心**，ID 为 `wildcraft:test_core`。它用于验证注册、贴图、资源生成、游戏加载和物品保存，没有玩法效果。

完整开发顺序见 [开发路线](docs/ROADMAP.md)，系统边界与保存方案见 [架构计划](docs/ARCHITECTURE.md)，本次已执行的检查见 [验证记录](docs/VERIFICATION.md)。

## 固定环境

| 项目 | 版本 |
| --- | --- |
| Minecraft | 26.3 |
| JDK | 25 |
| Fabric Loader | 0.19.5 |
| Fabric API | 0.161.0+26.3 |
| Fabric Loom | 1.18.2 |
| Gradle Wrapper | 9.7.1 |

本机使用 Homebrew OpenJDK 25 和 IntelliJ IDEA 2026.2.3。`dev.sh` 显式选择 JDK 25。其他机器可设置 `WILDCRAFT_JAVA_HOME` 指向自己的 JDK 25。

当前源码位于 ExFAT 外置盘。macOS 的附属文件会干扰 Gradle 清理，并被 Minecraft 误识别为资源，因此 macOS 下构建缓存、编译输出和自动测试运行目录位于本机 `~/Library/Caches/Wildcraft/`。源码仍在项目目录，`build` 会把可安装的成品导出到项目的 `dist/`；缓存可以重新生成。

`dev.sh` 还会清理已确认是 AppleDouble 格式的 Wrapper 和 IDE 运行配置附属文件，避免 IntelliJ 误认多个配置。外置盘上首次导入前，先运行一次 `./dev.sh --version`。临时开发世界也位于运行目录，不把个人存档放到缓存中。

## 在 macOS 开发

在项目目录中运行：

```sh
./dev.sh --version
./dev.sh runDatagen
./dev.sh build
./dev.sh runClient
```

首次构建需要联网下载 Gradle、Minecraft 和 Fabric 依赖。之后可使用缓存。

`./gradlew` 是标准入口；直接使用它时，需要先让 `JAVA_HOME` 指向 JDK 25。Windows 使用 `gradlew.bat`。

资源生成输出在 `src/main/generated`。物品模型与语言文件由生成器维护，不手改生成文件。原始像素图保存在 `art/source/test_core.json`。

资源生成与构建按上面的顺序分两次执行，保证生成后的资源被打入成品包。

## 在 IntelliJ IDEA 打开

1. 选择 **Open**，打开这个项目目录。
2. 按 Gradle 项目导入。
3. 将 Project SDK 和 Gradle JVM 都设为 JDK 25。本机路径：`/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home`。
4. 等待 Gradle 同步，再使用 Minecraft Client 运行配置，或在终端执行 `./dev.sh runClient`。

本机已完成 Gradle 导入，项目 SDK、语言级别和 Gradle JVM 均为 25。IDE 的 Gradle 用户目录使用 `/Users/zhou/Library/Caches/Wildcraft/gradle`，与终端共用依赖缓存；其他电脑可使用自己的本机缓存路径。

首次启动 IntelliJ IDEA 可能显示 JetBrains 用户协议；这与 Minecraft EULA 是独立的协议，需要用户自行决定是否接受。

IntelliJ IDEA 统一版的基础 Java 功能可免费使用；本项目不需要 Ultimate 订阅。

## 在游戏中验证

创建一份独立测试世界，启用命令。输入：

```mcfunction
/give @s wildcraft:test_core
```

测试核心也加入了创造模式的原材料分类，可在创造物品搜索中查找其名称。

检查背包图标、手持和丢到地上的显示；退出并重新进入世界后，物品应仍在背包中。

## 安装构建结果

构建出的游戏 Mod 在 `dist/wildcraft-0.1.0-dev.1.jar`。构建缓存中的 `-sources` 文件用于查看源代码，`-gametest` 文件用于自动验证，均不安装到日常游玩实例。

在独立的 Minecraft 26.3 实例中安装 Fabric Loader 0.19.5，将 Wildcraft 游戏 JAR 与 Fabric API 0.161.0+26.3 放进该实例的 `mods` 目录。客户端和服务端都需要安装。

请使用专门的测试实例和世界。开发客户端的运行目录、日志、缓存与构建输出不进入 Git。服务端需要的公共逻辑与客户端代码包含在同一份游戏 JAR 中，由 Fabric 按环境选择入口。

## 自动运行验证

检查包含真实游戏窗口、测试世界的创建与重载、物品名称和数量断言，以及本机网络连接后的服务端发放与背包同步。测试代码只在 `src/gametest` 和测试 JAR 中，不进入游戏 Mod。

先阅读 [Minecraft EULA](https://www.minecraft.net/en-us/eula)。同意协议后，可以明确添加以下参数，允许工具在临时测试目录写入 `eula=true`：

```sh
./dev.sh -PacceptMinecraftEula=true runClientGameTest
./dev.sh -PacceptMinecraftEula=true runPackagedClientTest
```

第一个入口使用开发环境；第二个入口使用打包后的游戏 JAR 和独立测试 JAR。两个入口都自动退出，失败时返回非零状态。测试使用回环地址和离线测试身份，不使用你的 Minecraft 账号。运行目录是临时数据，测试可能清理它；不要把个人世界放进去。

未添加参数时不会自动接受 EULA。一般构建与资源生成不需要该参数。GitHub 构建流程只检查资源生成与编译，不会自动接受 EULA 或运行桌面客户端。

## 独立服务端

开发服务端运行入口是 `./dev.sh runServer`，用成品 JAR 验证的入口是 `./dev.sh runPackagedServer`。Minecraft 服务端首次运行要求阅读并同意 Minecraft EULA；仅在同意后，按其提示修改对应本地测试目录中的 `eula.txt`。控制台输入 `stop` 会保存并关闭服务端。

macOS 运行目录位于 `~/Library/Caches/Wildcraft/builds/<项目路径标识>/run/`，其中 `client`、`server`、`clientGameTest` 和 `packaged-server` 相互隔离。改变项目位置后会使用新的缓存与测试目录。

本机验证服务端的 `server.properties` 使用 `server-ip=127.0.0.1`、`online-mode=false`，仅用于本机连接；这些设置和 EULA 文件都不随源码分发。首次运行即使因 EULA 退出，Gradle 也可能显示成功；应以服务端日志中的 `Done ... For help`、实际响应和正常保存为准。

本机离线开发测试使用专门的测试目录与回环地址。面向实际玩家的服务端配置不沿用离线测试配置。

## 项目结构

- `src/main/java`：公共注册和服务端可加载的代码。
- `src/client/java`：客户端入口和资源生成代码。
- `src/main/resources`：Mod 元数据、手写贴图。
- `src/main/generated`：生成的模型、物品显示定义、语言资源。
- `src/gametest`：自动客户端与网络验证，和游戏 JAR 隔离。
- `art/source`：美术源数据。
- `docs/ROADMAP.md`：后续开发顺序和验收要求。
- `docs/ARCHITECTURE.md`：模块、客户端/服务端边界、保存与依赖计划。
- `docs/VERIFICATION.md`：本次实际验证记录。

## 后续工作

P0 完成后，依次进行高风险技术验证和玩家数据、历史最高等级与精力系统。完整路线见 `docs/ROADMAP.md`。

运行依赖只有 Fabric Loader 与 Fabric API。公开发行许可证尚未选择；模板与第三方工具来源见 `NOTICE.md`。

版本固定在 `gradle.properties`。不跟随快照或每日构建自动升级；Minecraft 升级在独立分支中完成，确认存档、联网与资源兼容后再合并。
