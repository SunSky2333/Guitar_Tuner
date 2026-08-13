# M2 前置 · :app Android 模块搭建记录

> 日期：2026-08-13 · 状态：`./gradlew :app:assembleDebug` 通过（BUILD SUCCESSFUL）  
> 对应文档：《吉他调音器开发文档.md》§10/§11 · 里程碑 M2/M3 前置骨架

---

## 1. 版本基线（兼容现有 Gradle 8.10.2，无需动 wrapper）

| 组件 | 版本 |
| ---- | ---- |
| AGP | 8.7.3 |
| Kotlin（jvm + android + compose 插件） | 2.0.21 |
| Compose BOM | 2024.09.03 |
| activity-compose | 1.9.3 |
| material3 | 由 BOM 管理 |
| compileSdk / targetSdk | 35 |
| minSdk | 26 |

---

## 2. 关键决策

- **SDK 定位**：当前 shell 中 `JAVA_HOME`/`ANDROID_HOME` 等环境变量为空（`setx` 未生效到本会话），改用 `local.properties` 显式指定 `sdk.dir=C:/Android/Sdk`，保证构建确定性；该文件已加入 `.gitignore`。
- **DSP 复用**：`:app` 通过 `implementation(project(":dsp"))` 依赖 M1 核心，YIN/音分/调弦表零改动复用。
- **编译目标**：app 字节码目标 Java 17（`jvmTarget 17`），`:dsp` 保持 `jvmToolchain(21)`，D8 兼容 JVM 21 class 文件，无需降级 DSP 模块。
- **Compose 编译器**：Kotlin 2.0 起随 `org.jetbrains.kotlin.plugin.compose` 插件（版本与 Kotlin 一致），无需 `composeOptions.kotlinCompilerExtensionVersion`。

---

## 3. 构建经验（沙箱环境）

- Gradle daemon 在本沙箱会被杀（报 `daemon disappeared unexpectedly`，卡在 `:app:checkDebugAarMetadata`），**必须 `--no-daemon` 单进程构建**。
- AGP 8.7.3 默认 build-tools 34，首次构建自动补装 34.0.0（已接受许可）。

---

## 4. 新增文件

```
app/
├── build.gradle.kts
├── proguard-rules.pro
└── src/main/
    ├── AndroidManifest.xml            # RECORD_AUDIO 权限
    ├── java/com/example/guitartuner/
    │   ├── MainActivity.kt            # 入口 + Compose
    │   └── ui/
    │       ├── TunerScreen.kt         # 表盘占位（M2/M3 替换）
    │       └── theme/
    │           ├── Color.kt           # InTune/Sharp/Flat 语义色
    │           └── Theme.kt           # Material3 亮/暗主题
    └── res/values/
        ├── strings.xml
        └── themes.xml
local.properties                       # sdk.dir（已 gitignore）
.gitignore                             # 新增
```

---

## 5. 下一步（A 线剩余）

- P0：`dsp` 补纯函数 `RmsGate`（静音门限 + 迟滞）、`Smoother`（中值/指数平滑）+ 滑动窗（hop 512），带 JUnit。
- P1：`AudioEngine` + `AudioSource` 接口（`AudioRecordSource` / `FakeAudioSource`）；`TunerViewModel` + StateFlow（30fps 节流）；表盘 Canvas + 乐器→调弦→弦位三级选择。
- P2（真机到位后）：RECORD_AUDIO 运行时权限 + M2 验收；M4 调优；M5 签名打包。
