# Mirai - Bots

**目录**

- [1. 创建和配置 `Bot`](#1-创建和配置-bot)
  - [配置 Bot](#配置-bot)
  - [重要配置](#重要配置)
    - [切换心跳策略](#切换心跳策略)
    - [切换登录协议](#切换登录协议)
    - [覆盖登录解决器](#覆盖登录解决器)
  - [常用配置](#常用配置)
    - [修改运行目录](#修改运行目录)
    - [修改 Bot 缓存目录](#修改-bot-缓存目录)
    - [设备信息](#设备信息)
    - [使用其他日志库接管 mirai 日志系统](#使用其他日志库接管-mirai-日志系统)
    - [重定向日志](#重定向日志)
    - [启用列表缓存](#启用列表缓存)
    - [更多配置](#更多配置)
  - [获取当前所有 `Bot` 实例](#获取当前所有-bot-实例)
- [2. 登录](#2-登录)
  - [处理滑动验证码](#处理滑动验证码)
  - [常见登录失败原因](#常见登录失败原因)
- [附录: 调试网络层](#附录-调试网络层)
- [附录: 模拟测试框架](#附录-模拟测试框架)

## 1. 创建和配置 `Bot`

一个机器人被以 `Bot` 对象描述。mirai 的交互入口点是 `Bot`。`Bot` 只可通过 [`BotFactory`](../mirai-core-api/src/commonMain/kotlin/BotFactory.kt#L22-L87) 内的 `newBot` 方法获得：

```kotlin
interface BotFactory {
    fun newBot(qq: Long, password: String, configuration: BotConfiguration): Bot
    fun newBot(qq: Long, password: String): Bot
    fun newBot(qq: Long, passwordMd5: ByteArray, configuration: BotConfiguration): Bot
    fun newBot(qq: Long, passwordMd5: ByteArray): Bot
    
    companion object : BotFactory by BotFactoryImpl
}
```

通常的调用方法为：
```
// Kotlin
val bot = BotFactory.newBot(    )

// Java
Bot bot = BotFactory.INSTANCE.newBot(    );
```

> Scala 使用者请查看 [#834](https://github.com/mamoe/mirai/issues/834)

### 配置 Bot
可以切换使用的协议、控制日志输出等。

仅能在构造 Bot 时修改其配置：
```
// Kotlin
val bot = BotFactory.newBot(qq, password) {
    // 配置，例如：
    fileBasedDeviceInfo()
}

// Java
Bot bot = BotFactory.INSTANCE.newBot(qq, password, new BotConfiguration() {{
    // 配置，例如：
    fileBasedDeviceInfo()
}})
```

下文示例代码都要放入 `// 配置` 中。

> 可在 [BotConfiguration.kt](../mirai-core-api/src/commonMain/kotlin/utils/BotConfiguration.kt#L23) 查看完整配置列表

### 重要配置

#### 切换心跳策略

心跳策略默认为最佳的 `STAT_HB`，但不适用于一些账号。

如果遇到 Bot **闲置一段时间后**，发消息返回成功但群内收不到的情况，请切换心跳策略，依次尝试 `STAT_HB`、`REGISTER` 和 `NONE`。

```
// Kotlin
heartbeatStrategy = BotConfiguration.HeartbeatStrategy.REGISTER

// Java
setHeartbeatStrategy(BotConfiguration.HeartbeatStrategy.REGISTER)
```

#### 切换登录协议
Mirai 支持多种登录协议：`ANDROID_PHONE`，`ANDROID_PAD`，`ANDROID_WATCH`，`IPAD`，`MACOS` 默认使用 `ANDROID_PHONE`。

若登录失败，可尝试切换协议。**但注意部分功能在部分协议上不受支持**，详见源码内注释。

要切换协议：
```
// Kotlin
protocol = BotConfiguration.MiraiProtocol.ANDROID_PAD

// Java
setProtocol(MiraiProtocol.ANDROID_PAD)
```

#### 覆盖登录解决器

在登录时可能遇到图形验证码或滑动验证码，Mirai 会使用 `LoginSolver` 解决验证码。

- 在 JVM, Mirai 提供默认的命令行实现
- 在 Android 需要手动实现 `LoginSolver`

若要覆盖默认的 `LoginSolver` （通常不需要）：
```
// Kotlin
loginSolver = YourLoginSolver

// Java
setLoginSolver(new YourLoginSolver())
```

> 要获取更多有关 `LoginSolver` 的信息，查看 [LoginSolver.kt](../mirai-core-api/src/commonMain/kotlin/utils/LoginSolver.kt#L32)

### 常用配置

#### 修改运行目录
默认为 `File(".")`

```
// Kotlin
workingDir = File("C:/mirai")

// Java
setWorkingDir(new File("C:/mirai"))
```

#### 修改 Bot 缓存目录

缓存目录会相对于 `workingDir` 解析。如 `File("cache")` 将会解析为 `workingDir` 内的 `cache` 目录。而 `File("C:/cache")` 将会解析为绝对的 `C:/cache` 目录。

默认为 `File("cache")`

要修改缓存目录（自 mirai 2.4.0）：
```
// Kotlin
cacheDir = File("cache") // 最终为 workingDir 目录中的 cache 目录
cacheDir = File("C:/cache") // 最终为 C:/cache

// Java
setCacheDir(new File("cache")) // 最终为 workingDir 目录中的 cache 目录
setCacheDir(new File("C:/cache")) // 最终为 C:/cache
```

目前缓存目录会存储列表缓存、登录服务器、资源会话秘钥等。这些数据的存储方式有可能变化，请不要修改缓存目录中的文件。

[FileCacheStrategy]: ../mirai-core-api/src/commonMain/kotlin/utils/FileCacheStrategy.kt#L55

注意，`cacheDir` 仅存储与 Bot 相关的上述信息。其他的如 `InputStream` 的缓存由 [FileCacheStrategy] 管理，默认使用系统临时文件。

#### 设备信息
Bot 默认使用全随机的设备信息。**在更换账号地点时候使用随机设备信息可能会导致无法登录**，当然，**成功登录时使用的设备信息也可以保存后在新的设备使用**。

若要在服务器部署，可以先在本地完成登录，再将设备信息上传到服务器。一个设备信息可以一直使用。

要使用 `device.json` 存储设备信息：
```
fileBasedDeviceInfo() // 存储为 "device.json" 
// 或
fileBasedDeviceInfo("myDeviceInfo.json") // 存储为 "myDeviceInfo.json"
```

要自定义设备信息：
```
// Kotlin
deviceInfo = { bot ->  /* create device info */   }

// Java
setDeviceInfo(bot -> /* create device info */)
```

在线生成自定义设备信息的 `device.json`: https://ryoii.github.io/mirai-devicejs-generator/

#### 使用其他日志库接管 mirai 日志系统
*mirai 2.7 起支持*

使用 Log4J, SLF4J 等接管 mirai 日志系统后则可使用它们的过滤等高级功能。参阅 [mirai-logging](../logging/README.md) 以获取更多信息。

#### 重定向日志
Bot 有两个日志类别，`Bot` 或 `Net`。`Bot` 为通常日志，如收到事件。`Net` 为网络日志，包含收到和发出的每一个包和网络层解析时遇到的错误。

重定向日志到文件：
```
redirectBotLogToFile()
redirectBotLogToDirectory()

redirectNetworkLogToFile()
redirectNetworkLogToDirectory()
```

关闭日志(将会完全禁用日志功能, 无论是否已经通过第三方日志库接管日志系统)：
```
noNetworkLog()
noBotLog()
```

手动覆盖日志(不建议[(?)](../logging/README.md))：
```
// Kotlin
networkLoggerSupplier = { bot -> /* create logger */ }
botLoggerSupplier = { bot -> /* create logger */ }

// Java
setNetworkLoggerSupplier(bot -> /* create logger */)
setBotLoggerSupplier(bot -> /* create logger */)
```

#### 启用列表缓存
Mirai 在启动时会拉取全部好友列表和群成员列表。当账号拥有过多群时登录可能缓慢，开启列表缓存会大幅加速登录过程。

Mirai 自动根据事件更新列表，并在每次登录时与服务器校验缓存有效性，**但有时候可能发生意外情况导致列表没有同步。如果出现找不到群员或好友等不同步情况，请关闭缓存并[提交 Bug](https://github.com/mamoe/mirai/issues/new?assignees=&labels=question&template=bug.md)**

建议在测试环境使用缓存，而在正式环境关闭缓存（默认关闭缓存）。

要开启列表缓存（自 mirai 2.4.0）：
```
// 开启所有列表缓存
enableContactCache()
```

也可以只开启部分缓存：
```
// Kotlin
contactListCache {
    friendListCacheEnabled = true // 开启好友列表缓存
    groupMemberListCacheEnabled = true // 开启群成员列表缓存
    
    saveIntervalMillis = 60_000 // 可选设置有更新时的保存时间间隔, 默认 60 秒
}

// Java
contactListCache.setFriendListCacheEnabled(true) // 开启好友列表缓存
contactListCache.setGroupMemberListCacheEnabled(true) // 开启群成员列表缓存
contactListCache.setSaveIntervalMillis(60000) // 可选设置有更新时的保存时间间隔, 默认 60 秒
```

#### 更多配置

参阅 `BotConfiguration` 源码内注释。

### 获取当前所有 `Bot` 实例

在登录后 `Bot` 实例会被自动记录。可在 `Bot.instances` 获取到当前**在线**的所有 `Bot` 列表。

## 2. 登录

创建 `Bot` 后不会自动登录，需要手动调用其 `login()` 方法。只需要调用一次 `login()` 即可，`Bot` 掉线时会自动重连。

### 处理滑动验证码

[project-mirai/mirai-login-solver-selenium]: https://github.com/project-mirai/mirai-login-solver-selenium

服务器正在大力推广滑块验证码。

部分账号可以跳过滑块验证码，Mirai 会自动尝试。  
若你的账号无法跳过验证，可在 [project-mirai/mirai-login-solver-selenium] 查看处理方案。

**若遇到滑块验证问题无法解决，可以参考[论坛帮助页面](https://mirai.mamoe.net/topic/223/%E6%97%A0%E6%B3%95%E7%99%BB%E5%BD%95%E7%9A%84%E4%B8%B4%E6%97%B6%E5%A4%84%E7%90%86%E6%96%B9%E6%A1%88)。**

### 常见登录失败原因

[#993]: https://github.com/mamoe/mirai/discussions/993

| 错误信息       | 可能的原因        | 可能的解决方案                                               |
|:--------------|:---------------|:-----------------------------------------------------------|
| 当前版本过低    | 密码错误         | 检查密码或修改密码到 16 位以内                                  |
| 当前上网环境异常 | 设备锁           | 开启或关闭设备锁 (登录保护)                                    |
| 禁止登录       | 需要处理滑块验证码 | [project-mirai/mirai-login-solver-selenium]                |
| 密码错误       | 密码错误或过长     | 手机协议最大支持 16 位密码 ([#993]). 在官方 PC 客户端登录后修改密码 |

若以上方案无法解决问题，请尝试 [切换登录协议](#切换登录协议) 和 **[处理滑动验证码](#处理滑动验证码)**。

> 下一步，[Contacts](Contacts.md)
>
> [回到 Mirai 文档索引](CoreAPI.md)

## 附录: 调试网络层

参阅 [DebuggingNetwork.md](DebuggingNetwork.md)

## 附录: 模拟测试框架

参阅 [Mocking.md](mocking/Mocking.md)

> 下一步，[Contacts](Contacts.md)
>
> [回到 Mirai 文档索引](CoreAPI.md)


<!-- BEGIN DROP A

## 附录

[Ktor]: https://github.com/ktor.io/ktor

### 使用 Log4J2 接管 mirai 日志系统

添加依赖:

|         Group ID         |   Artifact ID    | 最低版本号 | 说明                                          |
|:------------------------:|:----------------:|:--------:|:---------------------------------------------|
| org.apache.logging.log4j |    log4j-api     |  2.14.1  | Log4J2 API 模块, 将会覆盖 mirai 依赖的 log4j-api |
| org.apache.logging.log4j |    log4j-core    |  2.14.1  | Log4J2 核心实现模块, 所有相关模块版本必须相同        |
| org.apache.logging.log4j | log4j-slf4j-impl |  2.14.1  | 用于将对 SLF4J 的调用转换为对 Log4J 的调用         |

要添加 `log4j-slf4j-impl` 是因为 [Ktor] 使用了 SLF4J. 添加该模块可以让整个应用统一使用同一个日志库.

#### Gradle 示例

*`build.gradle.kts`*
```kotlin
dependencies {
    val log4jVersion = "2.14.1"
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:$log4jVersion")

    implementation("net.mamoe:mirai-core:2.7.0") // 示例版本号
}
```

#### Maven 示例

*`pom.xml`*
```xml
<dependencies>
    <dependency>
        <groupId>org.apache.logging.log4j</groupId>
        <artifactId>log4j-api</artifactId>
        <version>2.14.1</version>
    </dependency>
    <dependency>
        <groupId>org.apache.logging.log4j</groupId>
        <artifactId>log4j-core</artifactId>
        <version>2.14.1</version>
    </dependency>
    <dependency>
        <groupId>org.apache.logging.log4j</groupId>
        <artifactId>log4j-slf4j-impl</artifactId>
        <version>2.14.1</version>
    </dependency>
    <dependency>
        <groupId>net.mamoe</groupId>
        <artifactId>mirai-core</artifactId>
        <version>2.7.0</version>
    </dependency>
</dependencies>
```

### 使用 SLF4J 接管 mirai 日志系统

添加依赖:

|         Group ID         |  Artifact ID   | 最低版本号 | 说明                                          |
|:------------------------:|:--------------:|:--------:|:---------------------------------------------|
| org.apache.logging.log4j |   log4j-api    |  2.14.1  | Log4J2 API 模块, 将会覆盖 mirai 依赖的 log4j-api |
| org.apache.logging.log4j | log4j-to-slf4j |  2.14.1  | 用于将对 Log4J 的调用转换为对 SLF4J 的调用         |
|        org.slf4j         |   slf4j-api    |  1.7.32  | SLF4J API 模块, 将会覆盖 [Ktor] 依赖的 slf4j-api |

除上以外, 还要添加一个 SLF4J 的实现, 例如 `slf4j-simple` 或 `logback-classic`.
这与通常的操作方式相同.

#### Gradle 示例

*`build.gradle.kts`*
```kotlin
dependencies {
    implementation("org.apache.logging.log4j:log4j-api:2.14.1")
    implementation("org.apache.logging.log4j:log4j-to-slf4j:2.14.1")
    implementation("org.slf4j:slf4j-api:1.7.32")

    implementation("org.slf4j:slf4j-simple:1.7.32") // 若要使用 slf4j-simple
    implementation("ch.qos.logback:logback-classic:1.2.5") // 若要使用 logback


    implementation("net.mamoe:mirai-core:2.7.0") // 示例版本号
}
```

#### Maven 示例

*`pom.xml`*
```xml
<dependencies>
    <dependency>
        <groupId>org.apache.logging.log4j</groupId>
        <artifactId>log4j-api</artifactId>
        <version>2.14.1</version>
    </dependency>
    <dependency>
        <groupId>org.apache.logging.log4j</groupId>
        <artifactId>log4j-to-slf4j</artifactId>
        <version>2.14.1</version>
    </dependency>
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>1.7.32</version>
    </dependency>

    <dependency> <!-- 若要使用 slf4j-simple --
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-simple</artifactId>
        <version>1.7.32</version>
    </dependency>

    <dependency> <!-- 若要使用 logback --
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.2.5</version>
    </dependency>

    <dependency>
        <groupId>net.mamoe</groupId>
        <artifactId>mirai-core</artifactId>
        <version>2.7.0</version> <!--示例版本号--
    </dependency>
</dependencies>
```

END DROP A -->
