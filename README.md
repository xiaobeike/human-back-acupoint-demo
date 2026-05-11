# Human Back Acupoint Demo

![Demo Screenshot](assets/screenshots/demo-screen.png)

一个面向`理疗机器人/康复设备能力展示`的 Android 原生 Demo。

这个项目不是临床级自动定穴系统，也不是已经接好真实机器人协议的产品版本。它当前解决的是一条更适合演示和方案验证的链路：

- 手机后摄预览人体背部
- 操作员手动对齐背部轮廓和关键标志位
- 锁定后自动生成背部穴位
- 输出整套穴位或单个穴位的 JSON 数据
- 模拟“设备已接收当前穴位”的联动画面

## 项目定位

这个 Demo 适合：

- 客户演示
- 方案路演
- 理疗机器人前端交互原型
- 背部穴位视觉链路的第一阶段 PoC
- 后续接入真实视觉模型、真实设备协议前的展示底座

这个 Demo 不适合直接作为：

- 医疗产品
- 临床落点工具
- 真实机器人控制终版

## 当前能力

- `CameraX` 后摄实时预览
- 固定背部轮廓模板
- 三种体型模板：`偏瘦 / 标准 / 偏宽`
- 三步定位流程：
  - 对齐肩线
  - 对齐脊柱中线
  - 调整腰侧胖瘦
- 六个可拖拽锚点：
  - 左肩
  - 右肩
  - 脊柱上端
  - 脊柱下端
  - 左腰侧
  - 右腰侧
- 锁定后显示背部穴位
- 当前点位说明卡片
- 整套穴位 JSON 页面
- 单点穴位 JSON 页面
- 模拟设备接收页
- 当前画面导出

## 当前穴位范围

当前内置 `30` 个背部演示穴位，覆盖督脉与背部双侧俞穴的展示需求：

- 督脉：`陶道`、`大椎`、`神道`、`命门`
- 膀胱经双侧：`大杼`、`风门`、`肺俞`、`厥阴俞`、`心俞`、`膈俞`、`肝俞`、`胆俞`、`脾俞`、`胃俞`、`三焦俞`、`肾俞`、`大肠俞`

这些点位当前都不是通过 AI 自动识别，而是通过：

- 纵向比例 `vertical_t`
- 横向比例 `lateral_t`
- 中轴线
- 肩宽与腰宽混合尺度

进行规则映射生成。

## 当前演示流程

1. 打开 App，用后摄拍摄背部
2. 选择最接近的人体模板
3. 点击`开始校准`
4. 依次完成肩线、中线、腰侧胖瘦调整
5. 点击锁定
6. 查看穴位覆盖结果
7. 点选某个穴位
8. 查看：
   - 当前点说明
   - 单点 JSON
   - 整套 JSON
9. 点击`发送到模拟设备`
10. 跳转到模拟设备接收页，展示设备已收到当前点位数据

## JSON 能力

当前支持两种 JSON 结果：

- `整套穴位 JSON`
  - 包含 6 个锚点
  - 包含当前全部穴位点
  - 包含像素坐标、归一化坐标、身体相对坐标
- `单点穴位 JSON`
  - 面向“当前选中穴位”
  - 适合演示设备端只接收一个治疗点的场景

当前 JSON 中包含：

- 模板名
- 采集时间
- 画布尺寸
- 肩宽 / 腰宽 / 脊柱轴长度
- 锚点坐标
- 穴位像素坐标
- 穴位归一化坐标
- `vertical_t / lateral_t`

## 技术栈

- `Kotlin`
- `Android View`
- `CameraX`
- 自定义 `Canvas` 蒙层
- `JSONObject` / `JSONArray`

## 项目结构

关键文件：

- [MainActivity.kt](app/src/main/java/com/humanacupoints/demo/MainActivity.kt)
- [BackOverlayView.kt](app/src/main/java/com/humanacupoints/demo/overlay/BackOverlayView.kt)
- [DemoBackModel.kt](app/src/main/java/com/humanacupoints/demo/model/DemoBackModel.kt)
- [PayloadFormatter.kt](app/src/main/java/com/humanacupoints/demo/model/PayloadFormatter.kt)
- [PayloadViewerActivity.kt](app/src/main/java/com/humanacupoints/demo/PayloadViewerActivity.kt)
- [DeviceReceiverActivity.kt](app/src/main/java/com/humanacupoints/demo/DeviceReceiverActivity.kt)
- [activity_main.xml](app/src/main/res/layout/activity_main.xml)
- [PROJECT_DOC.md](PROJECT_DOC.md)
- [docs/DEMO_GUIDE.md](docs/DEMO_GUIDE.md)

## 本地运行

前提：

- Android Studio / Android SDK 已安装
- `local.properties` 指向本机 SDK
- Android 手机已开启开发者选项与 USB / Wi-Fi 调试

构建：

```bash
./gradlew assembleDebug
```

安装到设备：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

启动：

```bash
adb shell am start -n com.humanacupoints.demo/.MainActivity
```

## 文档

- [PROJECT_DOC.md](PROJECT_DOC.md)：项目目标、定位模型、JSON 结构、页面职责
- [docs/DEMO_GUIDE.md](docs/DEMO_GUIDE.md)：演示建议、已知边界、后续路线

## License

MIT
