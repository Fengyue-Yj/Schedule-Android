### 🌟 且行 (Schedule Android) v1.0.7 更新日志

#### 🎨 悬浮式底部导航栏 (Floating Bottom Dock)
- **1:1 还原 iOS 悬浮 Dock 设计**：
  - 彻底摆脱贴合底部的死板长条导航栏，采用高质感圆角胶囊药丸造型（`32dp` 大圆角、`64dp` 精巧高度）。
  - 两侧预留 `20dp` 边距，底部浮空抬起并自适应全面屏手势导航条（`navigationBarsPadding`）。
  - 配备 `14dp` 层次化自然阴影与超细 `0.8dp` 边框，支持深浅色模式磨砂玻璃感背景。
  - 选中项以柔和浅绿胶囊背景（`selectedFill`）与薄荷绿（`accent`）高亮呈现，切换带平滑色彩过渡动效。
  - 课表、日历、待办与洞察四大模块内容滚动时可穿透悬浮栏背后，并在底部预留充足安全内边距（`100dp`），确保内容完全展示不遮挡。

#### 📐 待办页面 (Tasks) 边缘对齐与视觉统一
- **修复页面标题与左边缘紧贴的问题**：
  - 为 `TasksScreen` 顶部栏统一注入 `AppTheme.Spacing.page`（`24dp`）标准内边距。
  - 标题 `Tasks` 与右侧操作按钮现在与下方卡片完全对齐，不再紧贴屏幕物理边缘。

#### 🔘 标签选择器去除对勾图标
- **分段选择器 (`AppSegmentedPicker`) 去除多余对勾**：
  - 移除了 Material 3 默认加在 `Plans`、`Assignments`、`Exams` 前方的 `✓` 对勾图标。
  - 还原 iOS 原生 `UISegmentedControl` 的纯净标签药丸设计，文字居中清爽展示。

#### 📦 安装包
- `且行_v1.0.7.apk`
- `ScheduleAndroid_v1.0.7.apk`
