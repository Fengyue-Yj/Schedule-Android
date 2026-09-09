# 且行 (Schedule Android)

<div align="center">
  <img src="art/icon.png" width="128" height="128" alt="且行 Logo" />
  <h3>清新、纯粹、优雅的高校课表与日程待办管理应用</h3>
  <p>原生移植自 iOS 版 <a href="https://github.com/aidenlee2005/Schedule-Public">Schedule-Public</a>，针对 Android 15 / 小米澎湃 OS (HyperOS) 深度优化。</p>
</div>

---

## 🌟 核心特性

- **📅 课表矩阵视图 (Schedule)**
  - 支持 1~12 节课矩阵布局，精确支持多节连堂、单双周模式与全周显示。
  - 周视图横向平滑滑动切换，直观呈现当前周数与上课教室。
  - 支持 5 天制 / 7 天制（含周末）一键切换。
  - 支持 CSV 格式课表批量导入（中英文表头自适应识别）。

- **📆 49 个月日历视图 (Calendar)**
  - 以当前月为基准支持前后跨越 49 个月平滑左右滑动。
  - 日历单元格显示作业（蓝色圆点）与考试（粉色圆点）标记。
  - 选中日期即时展现当日详细日程日程清单。

- **✅ 任务规划与倒计时 (Tasks)**
  - **作业管理**：按截止时间智能排序，已完成自动归档，超期/临近即时提醒。
  - **考试倒计时**：清晰醒目的 `D-N` 倒计时胶囊标签。
  - **灵活规划**：支持制定“下一步小目标”（Next Step），随时暂停与推进。

- **🌱 洞察与互动伴侣 (Insights & Companion)**
  - 课程、作业、考试宏观数据统计卡片。
  - **Canvas 原生矢量萌宠**：呼吸动画、轻触互动（摸摸、挠痒痒、小憩）、温馨智能对话。

- **🎓 北大教学网 (PKU Blackboard) 对接**
  - 内置 Web 统一认证登录与 Cookie 会话管理。
  - 课程通知、作业与课件资料解析展示，并支持一键导入到本地日程。

- **🎨 纯正 iOS 质感设计 & Material 3**
  - 1:1 还原原版的绿意调色板与优雅圆角卡片。
  - 完美适配 Android 15 Edge-to-Edge 边到边沉浸式全屏与高刷新率触控。

---

## 🛠️ 技术架构

- **UI 框架**：Jetpack Compose + Material 3
- **编程语言**：Kotlin 1.9.24
- **数据持久化**：Room 2.6.1 (SQLite) + DataStore Preferences
- **网络与解析**：Jsoup 1.17.2 + Gson 2.10.1 + Android WebKit
- **异步响应式**：Kotlin Coroutines + StateFlow
- **最小 SDK**：Android 8.0 (API 26)
- **目标 SDK**：Android 14/15 (API 34)

---

## 📥 安装包下载

您可以直接从 [Releases](../../releases) 页面下载预编译的安装包 `且行_v1.0.apk` 安装到 Android 手机。

---

## 🏗️ 源码构建

1. 克隆代码仓库：
   ```bash
   git clone https://github.com/Fengyue-Yj/Schedule-Android.git
   ```
2. 在 **Android Studio** 中打开项目根目录。
3. 等待 Gradle 同步完成，连接手机或模拟器后点击 **Run ▶️**；或在终端执行：
   ```bash
   ./gradlew assembleDebug
   ```
   生成的 APK 文件位于 `app/build/outputs/apk/debug/app-debug.apk`。

---

## 📄 开源协议与致谢

- 本项目基于 [MIT License](LICENSE) 开源。
- 感谢原 iOS 版本作者 [@aidenlee2005](https://github.com/aidenlee2005) 提供的 [Schedule-Public](https://github.com/aidenlee2005/Schedule-Public) 项目设计与创意。
