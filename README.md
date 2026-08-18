# 呼吸放松 · 多节奏训练

一款基于 Jetpack Compose 的 Android 应用，使用 **4-7-8 / 4-2-6 / 盒式 4-4-4-4** 三种呼吸节奏帮助用户放松身心、调节专注。

> 柔和女声逐步播报 + 舒缓环境音 + 跟随阶段可视化光球动画，让每一次呼吸都有节律。

## 三种节奏一览

| 节奏 | 吸气 | 屏息 | 呼气 | 屏息 | 单轮 | 默认轮数 | 适用场景 |
|------|------|------|------|------|------|----------|----------|
| **4-7-8 放松** | 4s | 7s | 8s | – | 19s | 4 轮 | 睡前、焦虑、压力大 |
| **4-2-6 平衡** | 4s | 2s | 6s | – | 12s | 6 轮 | 入门练习、工作间隙 |
| **盒式 4-4-4-4** | 4s | 4s | 4s | 4s | 16s | 5 轮 | 专注、演讲前、冥想 |

> 屏幕顶部有 **节奏选择器**，点击即可在三档之间切换；也可以从右下角齿轮进入「设置」，在「呼吸节奏」分组中查看详细描述。

## 功能特性

- **三档节奏可选**：4-7-8 / 4-2-6 / 盒式 4-4-4-4，引擎抽象为 `BreathingPattern`，可平滑热替换。
- **六种环境音可选**：宁静氛围 / 雨声 / 林间鸟鸣 / 海浪 / 溪水 / 篝火，应用启动后通过 `BackgroundMusicManager.setAmbient()` 平滑切换，无缝循环。
- **可视化呼吸动画**：中心一颗柔和的光球，吸气缓慢放大、屏息保持当前大小、呼气缓慢收缩、停顿再次保持；动画时长自适应每档节奏的最长一步。
- **温柔舒缓女声播报**：使用 Android 系统 TextToSpeech 自动选择中文女声音色（包含 *xiaoxiao*、*yaoyao* 等本地化名称），**慢语速 0.48 + 升调 1.08**，每个阶段开始时按设置切换播报方式：
  - **单词模式**：`吸气 / 屏住 / 呼气 / 停顿`，节奏紧凑，跟随光球同步。
  - **温柔长句模式（默认）**：`慢慢地吸气，让气息充满腹部` / `轻轻地屏住，让氧气在身体里流动` / `缓缓地呼气，把紧张一起带走` / `放松，静静地感受此刻的平静`。
  - 同时播放柔和的钟琴提示音，长句模式还会截断上一句尾音以跟上节奏；切换节奏时会用起始语（如「4-7-8 放松呼吸法，准备开始」）告知用户。
- **舒缓环境音（可切换）**：内置 5 段可热切换的 25–45 秒无缝循环 + 1 段默认和弦音，全部由 `tools/generate_assets.py` 在本地合成（C 大调柔和和弦 / 粉噪 + 雨滴 / 草地 + 鸟鸣 chirp / 浪涌包络 / 高频水流 + 水花 / 燃烧床 + 噼啪），音量跟随系统媒体音量。
- **触感震动反馈**：每个吸/呼阶段切换时给一次 60ms 的轻柔脉冲，屏息不震动；可在设置里独立开关。
- **完整设置**：循环轮数（按节奏上限自动调整，4-7-8 可达 4 轮、4-2-6 可达 6 轮、盒式可达 5 轮）、声音、播报方式（单词 / 长句）、背景音乐（音源可切换）、震动、保持屏幕常亮、当前节奏、当前环境音等都会通过 DataStore 持久化，下次启动自动恢复。
- **节奏专属提示文案**：每种节奏下方都有一句「Tip」，如 4-7-8 主推睡前，盒式主推专注，可帮助用户理解当前节奏的目的。
- **首次启动引导**：开场卡片同时介绍三种节奏、可选环境音与可切换入口。
- **Material 3 主题**：自定义晨雾薄荷色系，浅/深色模式自动适配，支持 Android 12+ 动态色。

## 项目结构

```
BreathTrainer/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/breath/trainer/
│       │   ├── BreathApplication.kt        Application，全局持有 TTS/Music/Haptics/Settings
│       │   ├── MainActivity.kt             ComponentActivity + Compose 入口
│       │   ├── audio/
│       │   │   ├── AmbientSound.kt          环境音数据类 + ALL 列表
│       │   │   ├── TtsManager.kt           TextToSpeech + SoundPool 双通道（柔美女声）
│       │   │   └── BackgroundMusicManager.kt 多音源 MediaPlayer 热切换
│       │   ├── breathing/
│       │   │   ├── BreathingEngine.kt      StateFlow 驱动的多节奏阶段机
│       │   │   ├── HapticsController.kt    Vibrator 封装
│       │   │   ├── SettingsRepository.kt   DataStore 偏好（含 patternId）
│       │   │   └── pattern/                节奏抽象层
│       │   │       ├── BreathingPattern.kt     BreathingStep + 扩展
│       │   │       └── BreathingPatterns.kt    内置三档节奏
│       │   └── ui/
│       │       ├── TrainerViewModel.kt
│       │       ├── theme/                  Color/Type/Theme
│       │       ├── components/
│       │       │   ├── BreathOrb.kt        Canvas 绘制呼吸光球（支持新阶段停顿）
│       │       │   ├── IntroDialog.kt      介绍三档节奏
│       │       │   └── SettingsSheet.kt    节奏切换 + RadioButton 列表
│       │       └── screens/
│       │           └── BreathingScreen.kt  主屏幕：PatternSelector + Round + BreathOrb + PhaseTrack
│       └── res/                            矢量图标 + 音频资源 + Material 主题
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── gradle/wrapper/...                      Gradle 8.7
```

### 节奏抽象的关键类

```kotlin
// 呼吸节奏中的一步
data class BreathingStep(
    val kind: StepKind,   // INHALE / HOLD_AFTER_INHALE / EXHALE / HOLD_AFTER_EXHALE
    val seconds: Int,
)

// 一种完整节奏
data class BreathingPattern(
    val id: String,
    val displayName: String,
    val description: String,
    val totalRounds: Int,
    val steps: List<BreathingStep>,
)

// 内置节奏
object BreathingPatterns {
    val FOUR_SEVEN_EIGHT = …  // 4-7-8
    val FOUR_TWO_SIX      = …  // 4-2-6
    val BOX               = …  // 4-4-4-4
    fun findById(id: String?): BreathingPattern  // 兜底回 4-7-8
}
```

`BreathingEngine` 不再硬编码 INHALE→HOLD→EXHALE，而是顺序遍历 `currentPattern.steps`，因此再加一种节奏只需要在 `BreathingPatterns` 里追加一行 `BreathingPattern(...)`。

## 在 Android Studio 中打开

1. 使用 Android Studio Iguana (2023.2) 或更高版本。
2. *File → Open →* 选择 `BreathTrainer` 根目录。
3. 等待 Gradle Sync 完成（首次会下载 `gradle-8.7-bin.zip` 与依赖）。
4. 选择 "app" 配置，连接 Android 7.0+(API 24+) 设备或模拟器。
5. 点击 ▶ Run。

> 推荐使用 Android Studio 自带的 OpenJDK 17 作为 Gradle JDK。  
> *Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK* 选择 **Embedded JDK 17**。

## 命令行构建

```bash
# Linux / macOS
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

构建产物：`app/build/outputs/apk/debug/app-debug.apk`

## 提示音与环境音资源

- `app/src/main/res/raw/ambient_calm.wav`      30 秒循环的低频氛围音（默认）
- `app/src/main/res/raw/ambient_rain.wav`      雨声（30 秒）
- `app/src/main/res/raw/ambient_birds.wav`     林间鸟鸣（45 秒）
- `app/src/main/res/raw/ambient_ocean.wav`     海浪（32 秒）
- `app/src/main/res/raw/ambient_stream.wav`    溪水（25 秒）
- `app/src/main/res/raw/ambient_campfire.wav`  篝火（35 秒）
- `app/src/main/res/raw/chime_phase.wav`       阶段切换钟铃 (C5)
- `app/src/main/res/raw/chime_breath_in.wav`   吸气开始 (E5)
- `app/src/main/res/raw/chime_breath_out.wav`  呼气开始 (A4)

这些 WAV 全部通过 `tools/generate_assets.py` 在本地合成：

- 默认可听音：30s 循环 + 缓慢 LFO + C 大调柔和和弦 + 粉噪
- 雨声：粉噪底床 + 随机雨滴撞击
- 鸟鸣：低频草地底床 + 不规则 chirp（1800–3200 Hz + 颤音）
- 海浪：每 8 秒一个浪涌，三段式包络（卷起-持续-退潮）
- 溪水：高频水流 + 短促水花 pluck
- 篝火：低频燃烧床 + 随机噼啪白噪脉冲

如需重新生成：

```bash
# 需要 Python 3.10+（含 wave 模块）
python tools/generate_assets.py
```

每个文件末尾都做了 ~0.5 秒的首尾交叉淡入，因此可以做到"听不出接缝"的无缝循环。

> 由于应用内无法直接接入云端语音数据，TTS 默认调用系统引擎。  
> 当系统未安装中文 TTS 引擎时，应用会自动使用本地钟音作为兜底，同时仍然继续尝试初始化中文 TTS。  
> TTS 引擎初始化时会优先匹配名为 *female* / *xiaoxiao* / *yaoyao* 等中文女声，并使用 0.48 的语速 + 1.08 的升调，让每个阶段的引导句听起来温柔舒缓。

如果你想替换为其他音频，把对应的 `.wav`/`.mp3` 文件放入 `app/src/main/res/raw/` 覆盖即可，文件名需要保持一致。

### 环境音的工作机制

1. `AmbientSounds` 内置 6 个候选项（id / raw 文件 / 名称 / 描述）。
2. `BackgroundMusicManager.start()` 加载 `ambient.rawResId` 对应的 WAV 并循环播放。
3. 用户在「设置 → 环境音」里点选新选项 → `TrainerViewModel.selectAmbient()`：
   - 持久化新 id 到 DataStore；
   - 调用 `setAmbient(newSound)` → 内部 stop + start，立即切换；同源则 noop。
4. 当前音源（id）会跟随 `uiSettings` 同步到 `BackgroundMusicManager.ambient`，App 重启后读 DataStore 自动恢复。

## 单元测试

```bash
./gradlew test
```

（默认未启用测试，若需要可在 `app/build.gradle.kts` 里追加 `testImplementation` 依赖。）

## 三种呼吸法简介

### 1. 4-7-8 放松呼吸法

由整合医学专家 **Andrew Weil** 博士提出。整套方法的关键是 **延长呼气** 以激活副交感神经：

| 阶段 | 时长 | 目的 |
|------|------|------|
| 吸气 | 4 秒 | 用鼻子缓慢吸气，让腹部扩张 |
| 屏息 | 7 秒 | 让氧气充分交换 |
| 呼气 | 8 秒 | 用嘴缓慢呼气，把紧张感带走 |

建议每天练习 **4-8 轮**，可在睡前、感到焦虑、刚结束紧张工作等场景使用。

### 2. 4-2-6 平衡呼吸法

一种相对温和的「吸—屏—呼」节奏，吸气与呼气比值为 `4 : 6`，也具备一定的副交感激活效果，但因为屏息只有 2 秒，对初学者与低龄人群更友好，也常用于工作间隙的快速减压（一次 12 秒 ≈ 1 分钟 5 轮）。

| 阶段 | 时长 | 目的 |
|------|------|------|
| 吸气 | 4 秒 | 用鼻子缓慢吸气，让腹部扩张 |
| 屏息 | 2 秒 | 短暂停留 |
| 呼气 | 6 秒 | 用嘴缓慢呼气 |

建议每天 **6-10 轮**，尤其适合刚刚开始练习呼吸法的朋友。

### 3. 盒式 4-4-4-4 呼吸法

也叫「方形呼吸 / Box Breathing」，四段完全相等、节奏对称。海军海豹突击队和警察谈判专家也常用它来稳定情绪：

| 阶段 | 时长 | 目的 |
|------|------|------|
| 吸气 | 4 秒 | 缓慢吸气 |
| 屏息 | 4 秒 | 屏气停留 |
| 呼气 | 4 秒 | 缓慢呼气 |
| 停顿 | 4 秒 | 空肺停顿 |

建议每天 **5-8 轮**，特别适合演讲前、考试前、冥想开始时。

> ⚠️ 温馨提示：本应用不替代专业医疗建议，如有严重呼吸系统疾病、心脏疾病或孕期，请先咨询医生。

## License

仅作为示例项目使用。
