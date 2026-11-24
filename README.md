# 刻度 / Tick

一个只在 Android 本地运行的克制打卡与冲动急救应用。包名 `com.tick.app`。

## 技术栈

- Kotlin 2.3.20 + Jetpack Compose Compiler 2.3.20
- AGP 8.9.1 / Gradle 8.13 / JDK 17
- `minSdk 31`，`targetSdk 35`，`compileSdk 36`
- Miuix `0.9.0`（`miuix-ui`、`miuix-icons`）
- Kyant Backdrop `1.0.6`（Apache-2.0，液态玻璃/镜头折射）
- 无 Retrofit / OkHttp / Gson；AI 请求使用 `HttpURLConnection` + `org.json` + 协程 `Flow` 手写 SSE

## 构建

```bash
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew testDebugUnitTest
./gradlew lintDebug
```

APK 输出：

- `app/build/outputs/apk/release/app-release.apk`（推荐安装，已签名、非 debuggable）
- `app/build/outputs/apk/debug/app-debug.apk`（调试备用）

release 签名文件在 `keystore/tick-release.jks`，口令在同目录之外的 `keystore.properties`。请自行保管；若要上架应用商店，请替换为自己的正式签名。

## GitHub 更新源

- 仓库：https://github.com/yiyun-556/tick
- Releases：https://github.com/yiyun-556/tick/releases
- 应用内更新读取 `releases/latest/download/update.json`
- Obtainium 可直接添加仓库地址追踪更新

## 1.0.0 首个公开版本

- 版本号归一到 `1.0.0`，versionCode 从 1 开始。
- 包含本地记录、日历与按日期备注、通知、AI 伴侣、隐私门、液态玻璃与统一动画。
- 包含 GitHub Releases 手动/自动更新检查。
- 覆盖安装旧测试版（versionCode 24）会被 Android 当作降级拒绝，需要先卸载旧版；卸载会清空本地数据。