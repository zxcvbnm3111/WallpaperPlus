# WallpaperPlus

<p align="center">
  <img src="https://img.shields.io/badge/Android-9.0+-green?logo=android" />
  <img src="https://img.shields.io/badge/Xposed-LSPosed-blue" />
  <img src="https://img.shields.io/badge/OPlus-ColorOS-orange" />
</p>

**WallpaperPlus** 是一款面向 OPlus / ColorOS 设备的 Xposed 壁纸增强模块，提供光栅壁纸、锁屏歌词、3D壁纸晃动等功能。

## ✨ 功能特性

| 功能 | 描述 | 状态 |
|------|------|------|
| 🎨 光栅壁纸 | 多层视差景深效果，支持自定义图层数和强度 | ✅ |
| 🎵 锁屏歌词 | 在锁屏界面显示实时歌词，支持多种音乐应用 | ✅ |
| 🔄 3D壁纸晃动 | 陀螺仪驱动的3D透视变换 | ✅ |
| 📱 多应用适配 | 网易云/QQ音乐/酷狗/Spotify/Apple Music | ✅ |

## 📥 下载安装

### 从 GitHub Actions 获取（推荐）

1. 点击本仓库顶部的 **Actions** 标签
2. 选择最新的 **Build APK** 工作流
3. 在 Artifacts 中下载 `WallpaperPlus-debug` 或 `WallpaperPlus-release`

### 从 Releases 获取

访问 [Releases](https://github.com/YOUR_USERNAME/WallpaperPlus/releases) 页面下载最新版本。

## 🚀 使用说明

### 环境要求
- Android 9.0+ (API 28+)
- LSPosed / EdXposed / 其他 Xposed 框架
- Root 权限（用于 Xposed 框架）

### 安装步骤

1. **安装 APK**
   ```bash
   adb install WallpaperPlus.apk
   ```

2. **启用模块**
   - 打开 LSPosed 管理器
   - 进入「模块」标签
   - 找到 WallpaperPlus，启用开关

3. **勾选作用域**
   | 功能 | 必需作用域 |
   |------|-----------|
   | 光栅壁纸 + 3D晃动 | 桌面应用（如 `com.coloros.launcher`） |
   | 锁屏歌词 | `com.android.systemui` |
   | 歌词提取 | 对应音乐应用 |

4. **重启生效**
   - 强制停止并重启目标应用
   - 或重启设备

### 配置说明

打开 WallpaperPlus 应用，可以配置：

**光栅壁纸**
- 启用/禁用
- 光栅强度（0-100%）
- 图层数量（2-7层）
- 视差缩放比例
- 动画速度

**锁屏歌词**
- 启用/禁用
- 歌词位置（顶部/中部/底部）
- 显示样式（单行/双行/卡拉OK）
- 字体大小和颜色

**3D壁纸晃动**
- 启用/禁用
- 响应方向（水平/垂直/双向）
- 灵敏度和平滑度

## 🏗️ 自行编译

```bash
# 克隆仓库
git clone https://github.com/YOUR_USERNAME/WallpaperPlus.git
cd WallpaperPlus

# 生成 Gradle Wrapper
gradle wrapper --gradle-version 8.2

# 编译 Debug APK
./gradlew assembleDebug

# 编译 Release APK
./gradlew assembleRelease
```

APK 输出路径：
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release-unsigned.apk`

## 🎵 支持的音乐应用

- [x] 网易云音乐 (`com.netease.cloudmusic`)
- [x] QQ音乐 (`com.tencent.qqmusic`)
- [x] 酷狗音乐 (`com.kugou.android` / `com.kugou.android.lite`)
- [x] 咪咕音乐 (`cmccwm.mobilemusic`)
- [x] Spotify (`com.spotify.music`)
- [x] Apple Music (`com.apple.android.music`)

## ⚠️ 注意事项

1. **本模块依赖 Xposed 环境**，无框架时功能不生效
2. **不保证所有系统版本完全兼容**，如遇问题请检查：
   - 模块是否已在 LSPosed 中启用
   - 作用域是否已正确勾选
   - 目标应用是否已重启
3. **部分功能需要特定硬件**：
   - 3D壁纸晃动需要陀螺仪和加速度计
   - 锁屏歌词需要通知读取权限
4. **后续更新**：计划增加动态壁纸、时钟样式、手势操作等功能

## 📄 开源协议

本项目仅供学习交流使用。

## 🙏 致谢

- [LSPosed](https://github.com/LSPosed/LSPosed) - 现代 Android 设备的 Xposed 框架
- [Xposed](https://github.com/rovo89/Xposed) - 原始 Xposed 框架
- OPlus / ColorOS 开发社区
