# RekamAudio 交接文档

> 本文件供新会话快速理解项目状态，避免重复踩坑。
> 最后更新：2026-09-01

---

## 项目概况

- **项目**：RekamAudio — Android 内部音频录音 App
- **仓库**：https://github.com/254918/RekamAudio（fork 自 SiddiqMSF/RekamAudio）
- **分支**：`feature/chinese-localization-seekbar-overlay`（同时也是 `master`，已同步推送）
- **PR**：https://github.com/SiddiqMSF/RekamAudio/pull/1（草稿状态，等待原仓库作者合并）
- **APK 发布页**：https://github.com/254918/RekamAudio/releases/tag/apk
- **本地路径**：`C:\Users\Qqz\DeskBox\文档\RekamAudio\RekamAudio`
- **技术栈**：Kotlin + Jetpack Compose (Material 3) + MVVM + Hilt + DataStore + FFmpegKit
- **minSdk**：29 (Android 10)，**targetSdk**：35 (Android 15)
- **CPU 架构**：arm64 单架构（`app/build.gradle.kts` 中 `ndk.abiFilters` 限定）

---

## 已完成的全部功能

### 1. 中文汉化（简体 + 繁体）
- 提取所有硬编码字符串到 `strings.xml`
- 资源目录：`values-zh-rCN/`、`values-zh-rTW/`
- 三语言同步：英语、简体中文、繁體中文
- 相关文件：`app/src/main/res/values*/strings.xml`

### 2. 音频播放进度条 + 拖动跳转
- 播放中录音项显示可拖动的滑块 + 两侧时间标签
- 调用 `AudioPlayer.seekTo()` 实现跳转
- 相关文件：`ui/components/RecordingItem.kt`、`ui/MainViewModel.kt`

### 3. 悬浮按钮重绘 + 可拖动
- 52dp 圆形按钮（原为 110×64dp 胶囊条）
- 空闲显示麦克风图标，录制中红色停止键 + 脉冲光晕
- 右上角 24dp 关闭角标
- 使用 `detectDragGestures` 实现拖动
- 相关文件：`service/OverlayManager.kt`

### 4. MP3 录制格式
- 设置中三档可选：WAV（无损）/ M4A（压缩）/ MP3（通用）
- 使用 ffmpeg-kit 的 `libmp3lame` 编码器转码 PCM → MP3
- 相关文件：`data/model/AudioQuality.kt`、`service/AudioCaptureService.kt`

### 5. MP3 码率三档可选
- 128 kbps、192 kbps（默认）、320 kbps
- 存入 DataStore，Service 读取并传入 ffmpeg 命令
- 相关文件：`data/model/Mp3Bitrate.kt`、`ui/SettingsScreen.kt`

### 6. 录音列表显示文件大小
- 在录制日期旁显示 `· 3.2 MB` 格式
- 相关文件：`data/repository/AudioCaptureRepositoryImpl.kt`、`ui/components/RecordingItem.kt`

### 7. 固定签名密钥（解决覆盖安装问题）
- 生成 `app/rekam.keystore`（30 年有效期，密码 `rekamaudio`）
- debug 构建也使用此密钥签名
- CI 工作流自动使用此密钥，确保每次构建的 APK 可覆盖安装
- **重要**：`rekam.keystore` 被 `.gitignore` 排除，但有 `!app/rekam.keystore` 例外规则强制提交
- 相关文件：`app/build.gradle.kts`、`.gitignore`

### 8. README 三语版
- 英语 · 简体中文 · 印尼语三语并列
- 功能列表同步更新
- 相关文件：`README.md`

### 9. Release 说明双语版
- 每条功能一行内中英并列
- 安装说明中英双语
- 相关文件：`.github/workflows/debug-apk.yml`（模板）、已直接更新线上 Release

### 10. 转码失败通知带诊断详情 + WAV 降级保存
- 失败通知展开可看到 ffmpeg 具体错误
- 转码失败时自动将 PCM 包 WAV 头保存为 WAV 文件，录音不丢失
- 相关文件：`service/AudioCaptureService.kt`

---

## 当前正在做的任务（未完成）

### 实时流式 MP3 编码（Streaming MP3 Encoding）

**本会话中的最后一项任务，以下代码已写但未验证编译和运行。**

#### 设计思路
当前 MP3 录制是"先录 PCM → 停止后 ffmpeg 转码"模式。流式编码改为"边录边写 FIFO 管道 → ffmpeg 实时读取编码"，停止后立即出文件。

#### 实现方案
使用 Android 的 `Os.mkfifo()` 创建命名管道（FIFO），ffmpeg 从管道读取 PCM 数据实时编码为 MP3。

#### 新增/修改的文件

1. **`data/model/Mp3Bitrate.kt`** — 已改完
   - 添加 `streamingSupported: Boolean` 属性
   - `BITRATE_128` 和 `BITRATE_192` 为 `true`，`BITRATE_320` 为 `false`

2. **`data/repository/SettingsRepository.kt`** — 已改完
   - 新增 `streamingEncoding: Flow<Boolean>` 和 `setStreamingEncoding(enabled: Boolean)`

3. **`data/repository/SettingsRepositoryImpl.kt`** — 已改完
   - 新增 `STREAMING_ENCODING_KEY` DataStore 键
   - 默认值 `false`

4. **`ui/SettingsViewModel.kt`** — 已改完
   - 新增 `streamingEncoding: StateFlow<Boolean>`
   - 新增 `setStreamingEnabled(enabled: Boolean)` — 开启时自动降级 320k → 192k

5. **`ui/SettingsScreen.kt`** — 已改完
   - 新增 `SettingsSwitchItem` 组件（开关行）
   - MP3 选中时显示"实时流式 MP3 编码"开关
   - `Mp3BitrateDialog` 接受 `streamingEnabled` 参数，过滤不支持的码率选项

6. **`service/AudioCaptureService.kt`** — 已改完，但**未验证编译**
   - 新增 `recordMp3Streaming(bufferSize, bitrate)` 方法
   - `startRecording()` 中读取 `streamingEncoding` 设置并路由
   - 新增 `import android.system.Os` 和 `import android.system.ErrnoException`
   - FIFO 创建失败时自动回退到非流式 `recordMp3Audio()`

7. **`service/OverlayManager.kt`** — 已改完，但**存在已知编译问题**
   - 新增不定进度环动画（流式编码时显示旋转的 90° 弧段）
   - **问题**：使用了 `mutableFloatStateOf` 但可能缺少 import（`androidx.compose.runtime.mutableFloatStateOf`）
   - 还使用了 `LinearEasing` 但可能缺少 import（`androidx.compose.animation.core.LinearEasing`）

8. **字符串资源** — 已改完
   - `values/strings.xml`：`streaming_encoding`、`streaming_encoding_on`、`streaming_encoding_off`
   - `values-zh-rCN/strings.xml`：同上中文
   - `values-zh-rTW/strings.xml`：同上繁体

#### 需验证/修复的问题
- [ ] `OverlayManager.kt` 缺少 `mutableFloatStateOf` 和 `LinearEasing` 的 import
- [ ] 编译构建验证（CI 构建可能需要 4 分钟）
- [ ] 真机测试 FIFO 流式编码是否正常工作
- [ ] 真机测试 `Os.mkfifo()` 是否在所有 Android 10+ 设备上可用

---

## 踩过的坑（绝对不要重复踩）

### 坑 1：smart-exception-java 依赖缺失
- **症状**：MP3 转码时崩溃，进程被杀，悬浮按钮消失，列表无文件，通知显示 `NoClassDefFoundError: Failed resolution of: Lcom/arthenica/smartexception/java/Exceptions;`
- **原因**：ffmpeg-kit-maintained 的 POM 没有 `<dependencies>` 段，`smart-exception-java` 未被声明为传递依赖
- **修复**：在 `gradle/libs.versions.toml` 中显式添加 `com.arthenica:smart-exception-java:0.2.1`
- **教训**：第三方库 POM 不可信，运行时缺类必须查 POM 的 dependencies 段

### 坑 2：GitHub Actions 构建签名不一致
- **症状**：新版本 APK 无法覆盖安装旧版本，必须卸载重装
- **原因**：CI 每次构建自动生成临时 debug 签名，两次签名不同
- **修复**：生成固定 keystore 提交到仓库，配置 `signingConfigs` 让 debug 和 release 构建都用同一密钥
- **教训**：任何分发 APK 的 CI 流都必须固定签名

### 坑 3：Hilt 版本与 Kotlin 版本不兼容
- **症状**：添加 `@Inject lateinit var` 字段注入后编译失败，Hilt 报 Kotlin metadata 错误
- **原因**：Hilt 2.52 不支持 Kotlin 2.1 元数据格式
- **修复**：升级 Hilt 到 2.56.2
- **教训**：Hilt + Kotlin 组合必须查兼容性表

### 坑 4：列表不刷新问题
- **症状**：录音完成后文件实际已保存，但列表不显示
- **原因**：`callbackFlow` 中用 `trySend()` 而非 `send()`，内容观察者的事件可能丢失
- **修复**：改用 `launch { send() }` 保证事件不丢失
- **教训**：`callbackFlow` 中用 `trySend` 要谨慎，高频场景可能丢事件

### 坑 5：协程取消与 ffmpeg 同步执行冲突
- **症状**：MP3 停止录音时进程崩溃
- **原因**：`FFmpegKit.execute()` 同步阻塞调用在已取消的协程里跑，抛出 `Error` 类异常（`catch(Exception)` 接不住）
- **修复**：改用 `FFmpegKit.executeAsync()` + 全路径 `catch(Throwable)`
- **教训**：任何可能抛出 `Error` 的代码（JNI、原生库调用）必须用 `catch(Throwable)` 兜底

### 坑 6：GitHub CLI 转义问题
- **症状**：Windows PowerShell 下 `gh api` 命令的 `--jq` 参数频繁报错
- **原因**：PowerShell 对单引号/双引号/反引号的解析与 bash 不同
- **修复**：避免复杂 jq 表达式，用 `Select-Object` 或管道拆分 JSON 输出
- **教训**：Windows 上 gh CLI 的 jq 参数尽量简单，或直接用 `gh api ... | ConvertFrom-Json`

---

## 技术决策记录

### 为什么用 `Os.mkfifo()` 而不是 ffmpeg-kit 的 Pipe 类
- `Pipe` 类在 ffmpeg-kit 中已标记为 deprecated
- 维护版 fork 可能不包含 `Pipe` 类
- `Os.mkfifo()` 是 Android SDK 标准 API（API 21+），更可靠

### 为什么用 FFmpegKit 而不是 Android MediaCodec 做 MP3 编码
- `MediaCodec` 不保证在所有设备上支持 MP3 编码（MIME type `audio/mpeg` 可选的）
- ffmpeg-kit 的 `libmp3lame` 编码器在所有设备上一致可用

### 为什么只保留 arm64 架构
- APK 体积从 100MB 降到 77MB（砍掉 3 份原生库：armeabi-v7a / x86 / x86_64）
- `minSdk = 29`（Android 10）的市售设备几乎全是 arm64
- 代价：PC 模拟器无法安装（测试需用真机）

---

## 项目文件结构（关键文件）

```
RekamAudio/
├── app/
│   ├── build.gradle.kts              # 签名配置、abiFilters、版本号
│   ├── rekam.keystore                # 固定签名密钥（已提交到 git）
│   └── src/main/
│       ├── java/com/example/rekamaudio/
│       │   ├── data/
│       │   │   ├── model/
│       │   │   │   ├── AudioQuality.kt
│       │   │   │   ├── Mp3Bitrate.kt          # 已改（加 streamingSupported）
│       │   │   │   └── Recording.kt           # 已改（加 fileSizeBytes）
│       │   │   └── repository/
│       │   │       ├── SettingsRepository.kt   # 已改（加 streamingEncoding）
│       │   │       ├── SettingsRepositoryImpl.kt # 已改
│       │   │       └── AudioCaptureRepositoryImpl.kt # 已改
│       │   ├── service/
│       │   │   ├── AudioCaptureService.kt      # 已改（加 recordMp3Streaming）
│       │   │   └── OverlayManager.kt           # 已改（不定进度环）
│       │   └── ui/
│       │       ├── MainViewModel.kt
│       │       ├── SettingsScreen.kt           # 已改（流式开关+码率过滤）
│       │       ├── SettingsViewModel.kt        # 已改
│       │       └── components/
│       │           └── RecordingItem.kt        # 已改（文件大小）
│       ├── res/
│       │   ├── values/strings.xml
│       │   ├── values-zh-rCN/strings.xml
│       │   └── values-zh-rTW/strings.xml
│       └── AndroidManifest.xml
├── gradle/
│   └── libs.versions.toml           # 已改（smart-exception-java, ffmpeg-kit）
├── .github/workflows/
│   └── debug-apk.yml                # 已改（双语说明）
├── README.md                        # 已改（三语版）
├── .gitignore                       # 已改（keystore 例外规则）
└── HANDOFF.md                       # 本文档
```

---

## 下一步计划

### 优先级 1：完成流式编码功能
1. 补全 `OverlayManager.kt` 缺少的 import（`mutableFloatStateOf`、`LinearEasing`）
2. 构建验证（本地或 CI）
3. 真机测试 MP3 流式编码
4. 如果 `Os.mkfifo()` 在部分设备失败，确认回退到非流式正常工作

### 优先级 2：功能优化
- 如果用户反馈流式编码 192k 延迟高，可考虑降低缓冲区大小或设为 128k 默认
- 流式编码时通知栏进度可改为显示"正在实时编码…"而非百分比

### 优先级 3：原仓库 PR 合并
- 当前 PR 是草稿状态，待自行验证全部功能正常后
- 在 PR 页面点击 "Ready for review" 标记为可审阅

---

## 构建与发布

### 触发 CI 构建
```powershell
$env:HTTPS_PROXY="http://127.0.0.1:21081"; gh workflow run debug-apk.yml --repo 254918/RekamAudio --ref feature/chinese-localization-seekbar-overlay
```

### 推送代码到 fork
```powershell
git push fork feature/chinese-localization-seekbar-overlay
git push fork feature/chinese-localization-seekbar-overlay:master
```

### 代理说明
系统代理端口 21081，但有时不可用。直连 GitHub 在国内可能超时，建议先检查代理是否可用。

### 本地 keystore 信息
- 路径：`app/rekam.keystore`
- 密码：`rekamaudio`
- 别名：`rekam`
- 有效期：30 年