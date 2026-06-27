#!/bin/bash
# WallpaperPlus 快速构建脚本

set -e

echo "=== WallpaperPlus 构建脚本 ==="

# 检查 Gradle
if [ ! -f "gradlew" ]; then
    echo "正在生成 Gradle Wrapper..."
    if command -v gradle &> /dev/null; then
        gradle wrapper --gradle-version 8.2
    else
        echo "错误: 未找到 gradle，请先安装 Gradle 8.2+"
        echo "或使用以下命令手动下载 wrapper:"
        echo "  wget https://services.gradle.org/distributions/gradle-8.2-bin.zip"
        exit 1
    fi
fi

# 检查图标
if [ ! -f "app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" ]; then
    echo "警告: 未找到应用图标，正在生成临时图标..."
    mkdir -p app/src/main/res/mipmap-xxxhdpi
    # 尝试用 python PIL 生成
    python3 -c "
from PIL import Image
img = Image.new('RGB', (192, 192), '#FF6B6B')
img.save('app/src/main/res/mipmap-xxxhdpi/ic_launcher.png')
img.save('app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png')
print('临时图标已生成')
" 2>/dev/null || echo "请手动添加图标到 app/src/main/res/mipmap-xxxhdpi/"
fi

# 编译
echo "开始编译..."
./gradlew assembleDebug

echo ""
echo "=== 编译完成 ==="
echo "APK 路径: app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "安装命令:"
echo "  adb install app/build/outputs/apk/debug/app-debug.apk"
