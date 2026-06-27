# WallpaperPlus 构建指南

## 环境要求

- Android Studio Hedgehog (2023.1.1) 或更新版本
- JDK 17
- Android SDK 34
- Gradle 8.2

## 快速开始

### 1. 下载项目并解压

```bash
unzip WallpaperPlus.zip -d WallpaperPlus
cd WallpaperPlus
```

### 2. 生成 Gradle Wrapper

**方式一（推荐）：使用本地 Gradle**
```bash
# 确保已安装 Gradle 8.2+
gradle wrapper --gradle-version 8.2
```

**方式二：手动下载**
```bash
mkdir -p gradle/wrapper
cd gradle/wrapper
# 下载 gradle-wrapper.jar
wget https://raw.githubusercontent.com/gradle/gradle/v8.2.0/subprojects/wrapper/src/main/resources/gradle-wrapper.jar
# 或从其他项目复制
cd ../..
```

### 3. 添加应用图标

由于版权原因，项目中未包含应用图标。你需要创建或下载图标：

**临时方案（纯色图标）：**
```bash
# 使用 ImageMagick 生成纯色图标（如有安装）
mkdir -p app/src/main/res/mipmap-xxxhdpi
convert -size 192x192 xc:\#FF6B6B app/src/main/res/mipmap-xxxhdpi/ic_launcher.png
convert -size 192x192 xc:\#FF6B6B app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png
```

**正式方案：**
使用 [Android Asset Studio](https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html) 生成完整图标集，放到 `app/src/main/res/` 对应目录。

### 4. 编译 APK

```bash
# Linux/Mac
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

编译成功后 APK 位于：
```
app/build/outputs/apk/debug/app-debug.apk
```

### 5. 安装到设备

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 常见问题

### Q: 编译报错 "Could not find gradle-wrapper.jar"
**A:** 按照步骤2生成 Gradle Wrapper。

### Q: 编译报错 "No mipmap/ic_launcher"
**A:** 按照步骤3添加应用图标。

### Q: 安装后 LSPosed 不识别模块
**A:** 检查：
1. `assets/xposed_init` 文件存在且内容为 `com.wallpaperplus.module.MainHook`
2. AndroidManifest.xml 中 `<meta-data android:name="xposedmodule" android:value="true"/>` 存在
3. 安装后重启 LSPosed 管理器

### Q: 功能不生效
**A:** 检查：
1. LSPosed 中已启用模块
2. 已勾选目标应用作用域（SystemUI、桌面、音乐应用）
3. 已强制停止并重启目标应用

### Q: Android 14+ 无法读取配置
**A:** 本项目已修复 `MODE_WORLD_READABLE` 问题，使用 `File.setReadable()` 方案。如仍有问题，尝试：
```bash
adb shell chmod 644 /data/user_de/0/com.wallpaperplus.module/shared_prefs/wallpaper_plus_prefs.xml
```

---

## 项目结构说明

```
app/src/main/
├── assets/xposed_init              # Xposed 入口点
├── java/com/wallpaperplus/module/
│   ├── MainHook.java               # Xposed 主入口
│   ├── hook/                       # Hook 实现
│   │   ├── RasterWallpaperHook.java    # 光栅壁纸
│   │   ├── LockscreenLyricHook.java    # 锁屏歌词
│   │   ├── MotionWallpaperHook.java    # 3D晃动
│   │   └── SystemUIHook.java           # SystemUI增强
│   ├── ui/                         # 设置界面
│   ├── service/                    # 后台服务
│   ├── receiver/                   # 广播接收器
│   └── util/                       # 工具类
└── res/                            # 资源文件
```

---

## 技术细节

### Xposed 作用域配置
模块支持的作用域在 `res/values/arrays.xml` 中定义：
- `com.android.systemui` - 锁屏歌词必需
- `com.coloros.launcher` / `com.oppo.launcher` / `com.heytap.launcher` - 壁纸功能
- `com.netease.cloudmusic` - 网易云音乐歌词
- `com.tencent.qqmusic` - QQ音乐歌词
- `com.kugou.android` - 酷狗音乐歌词
- 其他音乐应用...

### 配置存储
使用 `XSharedPreferences` 读取配置，存储路径：
```
/data/user_de/0/com.wallpaperplus.module/shared_prefs/wallpaper_plus_prefs.xml
```

---

## 开源协议

本项目仅供学习交流使用。
