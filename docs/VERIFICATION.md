# P0 验证记录

日期：2026-10-02（Asia/Shanghai）。这是本机执行结果，未发布或上传。

## 实际环境

| 项目 | 实际版本 |
| --- | --- |
| 机器 | Apple M4 / arm64 |
| 系统 | macOS 27.0.1 |
| JDK | Homebrew OpenJDK 25.0.4.1 |
| Minecraft | 26.3 |
| Fabric Loader | 0.19.5 |
| Fabric API | 0.161.0+26.3 |
| Loom | 1.18.2 |
| Gradle Wrapper | 9.7.1 |
| IntelliJ IDEA | 2026.2.3，已安装并完成 Gradle 导入 |
| Wildcraft | 0.1.0-dev.1 |

JDK 已注册到当前用户的 JavaVirtualMachines 目录。`java_home -v 25` 和 `java --version` 都能正确找到 JDK 25。项目不修改其他 JDK 安装或全局 shell 配置。

## 已执行的检查

| 检查 | 结果与证据 |
| --- | --- |
| 固定依赖与 Wrapper | 实际完成解析、下载和启动；Gradle 发行包使用校验和 |
| 资源生成 | `runDatagen` 成功；再次生成模型、显示定义、中英语言，四份资源内容完全一致 |
| 清理后构建 | `clean build gameTestJar` 成功，编译公共、客户端与测试来源，导出游戏 JAR |
| IDE 工程导入 | 界面显示 Gradle 同步已完成；项目 SDK、语言级别、Gradle JVM 均为 25 |
| 开发客户端 | 实际 Mac 游戏窗口启动，Apple M4 图形设备正常渲染；公共与客户端入口正常初始化 |
| 物品与资源 | 服务端命令发放两个测试核心，客户端断言 ID 和数量；截图确认贴图与手持显示 |
| 存档重载 | 退出测试世界，再重新打开同一存档；背包中的两个核心仍在，英文名称断言正确 |
| 本机网络连接 | Fabric 客户端测试连接回环地址上的 DedicatedServer，服务端发放物品，客户端背包数量正确同步 |
| 成品 JAR 客户端 | `runPackagedClientTest` 使用游戏 JAR 和单独测试 JAR，完成创建、重载和网络同步后正常退出 |
| 无图形独立服务端 | `runPackagedServer` 加载成品 JAR，环境为 SERVER；监听 `127.0.0.1:25565`，世界生成完成并输出 `Done` |
| 服务端实际响应 | 控制台 `list` 正确返回在线人数；`stop` 保存玩家与世界并正常退出 |
| 客户端边界 | 独立服务端没有执行客户端初始化，未发生客户端类加载错误 |
| 打包完整性 | 元数据、版本、模型、中英语言、贴图与 JDK 25 字节码均检查通过；游戏 JAR 不含测试类或 AppleDouble 附属文件 |

`test` 任务显示 `NO-SOURCE`，当前没有普通单元测试。上面的运行验证来自真实游戏客户端测试及独立服务端，不把空测试任务算作测试通过。

自动网络测试使用 Fabric 官方框架中的专用服务端对象，客户端通过本机 TCP 连接；无图形独立服务端验证另外启动一个 Java 进程。这两种检查分别覆盖网络同步和服务端环境加载。没有使用或登录个人 Minecraft 账号。

## 外置盘问题与处理

源码位于 ExFAT。初次测试中，macOS 创建的 `._` 附属文件导致 Gradle 清理失败，并被 Minecraft 当作贴图或模型读取。修复方式是把 macOS 构建输出、Gradle 缓存和运行目录放到本机 APFS 缓存目录；资源拷贝和 JAR 打包同时过滤附属文件。

修复后，重新运行客户端、成品 JAR、资源生成和清理构建均成功，没有再次出现 Wildcraft 的坏 PNG、模型或语言读取错误。所有普通开发运行配置也采用新的运行目录。成品仍导出到项目 `dist/`。

IDE 还会直接扫描 Wrapper 与运行配置目录，初次把 `._gradle-wrapper.properties` 和 `._Data_Generation.xml` 当成配置。删除对应的已确认 AppleDouble 元数据后，重新同步成功。开发入口也加入相同的格式检查与清理，避免普通源文件被误删。IDE 与终端现在共用已验证的 Gradle 缓存。

## 协议与测试配置

独立服务端首次因未接受 EULA 而退出；该次启动不计入成功。用户明确同意 Minecraft EULA 后，仅在本机临时测试目录写入 `eula=true`，才继续实际运行验证。

客户端网络测试通过显式 `-PacceptMinecraftEula=true` 参数创建测试目录的 EULA 文件。工程默认不自动接受协议，源码包不包含 EULA 接受文件或个人服务端配置。测试服务端使用回环地址和离线身份，RCON 关闭。

IntelliJ 首次启动要求接受自己的用户协议，已在用户单独授权后接受；未启用匿名统计发送。工程导入后，实际在项目结构窗口确认 SDK 为已安装的 JDK 25，语言级别为 JDK_25；Gradle 设置和保存配置中的 JVM 也为 25。

## 日志中保留的上游提示

游戏能完成测试，但日志并非零警告：Fabric 客户端测试默认设置产生 `Anisotropic Filtering` 的取值提示；离线测试身份导致账号属性和 Realms 请求失败；macOS 27 识别和图形驱动也有兼容提示。它们未阻断本次世界加载、资源显示、网络连接和存档检查。没有修改原版或依赖来隐藏这些提示。

IDE 同步界面显示完成并保留一个警告；IDE 自带插件也记录了异常提示。未把这些提示描述为零错误，也未安装推荐弹窗中的额外 Minecraft Development 插件。

## 尚未验证或实施

- 个人账号登录后的常用启动器实例；成品加载由 Loom 的独立运行任务验证。
- GitHub 远程 CI。配置已准备，尚未推送或远程执行。
- 普通 Windows/Linux 桌面、多玩家长期运行和公开服务器。
- 精力、历史等级、攀爬、滑翔、料理、Fuse、能源、机械、制造机与天气规则；这些属于后续阶段。

## 交付与证据

同级输出目录的 `verification/` 保留开发客户端、成品客户端、独立服务端、资源生成和清理构建日志，以及游戏截图。源码包不包含 Minecraft 游戏文件、缓存、个人世界或第三方账号数据。

成品：`wildcraft-0.1.0-dev.1+mc26.3.jar`，用于 Minecraft 26.3 / Fabric。安装时仍需要对应 Fabric Loader 与 Fabric API。

成品 SHA-256：`a610fe8f953923d9c4294e5d0c537faa318f7a5d4456db1a0b8ad14c8e25825f`。

具体启动、安装与自动验证方式见 [README.md](../README.md)。
