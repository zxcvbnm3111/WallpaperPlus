package com.wallpaperplus.module.hook;

import android.animation.ValueAnimator;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.wallpaperplus.module.util.ConfigManager;
import com.wallpaperplus.module.util.LogUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 光栅壁纸 Hook
 * 实现多层视差光栅效果
 * 
 * 原理:
 * 1. 拦截壁纸绘制流程
 * 2. 将壁纸拆分为多层（前景/中景/背景）
 * 3. 根据传感器数据或触摸偏移计算各层位移
 * 4. 合成最终画面
 */
public class RasterWallpaperHook {

    private static final String TAG = "RasterWallpaper";

    private ConfigManager config;
    private List<RasterLayer> layers = new ArrayList<>();
    private float offsetX = 0f, offsetY = 0f;
    private float targetOffsetX = 0f, targetOffsetY = 0f;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private ValueAnimator animator;
    private boolean isActive = false;

    // ColorOS/OPlus 壁纸相关类名
    private static final String[] WALLPAPER_CLASSES = {
        "com.android.launcher3.WallpaperCropView",
        "com.android.launcher3.Workspace",
        "com.oppo.launcher.WallpaperView",
        "com.coloros.launcher.wallpaper.WallpaperManager",
        "com.heytap.launcher.wallpaper.WallpaperLayer"
    };

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        config = ConfigManager.getInstance();
        if (!config.isRasterEnabled()) {
            LogUtil.i(TAG, "Raster wallpaper disabled, skip");
            return;
        }

        LogUtil.i(TAG, "Initializing raster wallpaper hook...");

        // Hook 壁纸绘制相关类
        hookWallpaperDrawing(lpparam);

        // Hook 桌面滑动事件用于视差
        hookWorkspaceScroll(lpparam);

        // 启动动画循环
        startAnimationLoop();
    }

    private void hookWallpaperDrawing(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook ImageView 的 onDraw 方法，拦截壁纸绘制
            XposedHelpers.findAndHookMethod(ImageView.class, "onDraw", Canvas.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!isActive) return;

                    ImageView imageView = (ImageView) param.thisObject;
                    Drawable drawable = imageView.getDrawable();

                    if (drawable == null) return;

                    // 检查是否是壁纸视图
                    if (!isWallpaperView(imageView)) return;

                    // 取消默认绘制，使用我们的光栅绘制
                    param.setResult(null);

                    Canvas canvas = (Canvas) param.args[0];
                    drawRasterWallpaper(canvas, drawable, imageView.getWidth(), imageView.getHeight());
                }
            });

            LogUtil.i(TAG, "Wallpaper drawing hooked");
        } catch (Exception e) {
            LogUtil.e(TAG, "Failed to hook wallpaper drawing", e);
        }
    }

    private void hookWorkspaceScroll(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook 桌面 Workspace 的滚动偏移
            Class<?> workspaceClass = null;
            for (String className : new String[]{
                "com.android.launcher3.Workspace",
                "com.oppo.launcher.Workspace",
                "com.coloros.launcher.Workspace"
            }) {
                try {
                    workspaceClass = XposedHelpers.findClass(className, lpparam.classLoader);
                    break;
                } catch (Exception ignored) {}
            }

            if (workspaceClass == null) {
                LogUtil.w(TAG, "Workspace class not found");
                return;
            }

            XposedHelpers.findAndHookMethod(workspaceClass, "setScrollX", int.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    int scrollX = (int) param.args[0];
                    updateParallaxFromScroll(scrollX);
                }
            });

            LogUtil.i(TAG, "Workspace scroll hooked");
        } catch (Exception e) {
            LogUtil.e(TAG, "Failed to hook workspace scroll", e);
        }
    }

    private boolean isWallpaperView(ImageView view) {
        // 通过标签或父级判断是否为壁纸视图
        Object tag = view.getTag();
        if (tag != null && tag.toString().contains("wallpaper")) return true;

        android.view.ViewParent parent = view.getParent();
        if (parent != null) {
            String parentName = parent.getClass().getName().toLowerCase();
            return parentName.contains("wallpaper") || parentName.contains("workspace");
        }
        return false;
    }

    private void drawRasterWallpaper(Canvas canvas, Drawable drawable, int width, int height) {
        if (canvas == null || drawable == null || width <= 0 || height <= 0) return;
        if (!(drawable instanceof BitmapDrawable)) return;

        Bitmap original = ((BitmapDrawable) drawable).getBitmap();
        if (original == null || original.isRecycled()) return;

        int layerCount = config.getLayerCount();
        float intensity = config.getRasterIntensity();
        float scale = config.getParallaxScale();

        // 初始化图层（首次）
        if (layers.isEmpty()) {
            initLayers(original, layerCount, width, height);
        }

        // 绘制背景层（最底层，移动最少）
        for (int i = layers.size() - 1; i >= 0; i--) {
            RasterLayer layer = layers.get(i);

            // 计算该层的视差偏移
            float depth = (float) i / (layers.size() - 1); // 0 = 前景, 1 = 背景
            float layerOffsetX = offsetX * depth * intensity * scale;
            float layerOffsetY = offsetY * depth * intensity * scale * 0.3f; // Y轴移动较少

            canvas.save();
            canvas.translate(layerOffsetX, layerOffsetY);

            // 应用模糊和透明度（越远越模糊越透明）
            Paint paint = new Paint();
            paint.setAlpha((int) (255 * (1 - depth * 0.3f)));

            if (depth > 0.5f) {
                // 背景层添加轻微模糊效果
                paint.setColorFilter(createBlurFilter(depth));
            }

            canvas.drawBitmap(layer.bitmap, 0, 0, paint);
            canvas.restore();
        }
    }

    private void initLayers(Bitmap source, int count, int width, int height) {
        layers.clear();

        for (int i = 0; i < count; i++) {
            float depth = (float) i / (count - 1);

            // 根据深度创建不同处理方式的图层
            Bitmap processed = processLayerBitmap(source, depth, width, height);
            layers.add(new RasterLayer(processed, depth));
        }

        LogUtil.i(TAG, "Initialized " + count + " raster layers");
    }

    private Bitmap processLayerBitmap(Bitmap source, float depth, int targetWidth, int targetHeight) {
        // 缩放以适应目标尺寸
        float scale = Math.max(
            (float) targetWidth / source.getWidth(),
            (float) targetHeight / source.getHeight()
        ) * (1 + depth * 0.1f); // 深层稍微放大，防止边缘穿帮

        int newWidth = (int) (source.getWidth() * scale);
        int newHeight = (int) (source.getHeight() * scale);

        Bitmap scaled = Bitmap.createScaledBitmap(source, newWidth, newHeight, true);

        // 深层添加轻微模糊（模拟景深）
        if (depth > 0.6f) {
            scaled = applyFastBlur(scaled, (int) ((depth - 0.6f) * 10));
        }

        return scaled;
    }

    private Bitmap applyFastBlur(Bitmap sentBitmap, int radius) {
        // 简化的快速模糊实现
        if (radius < 1) return sentBitmap;

        Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        // 简化的 box blur
        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int vmin[] = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int dv[] = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }

        yw = yi = 0;
        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;

            for (x = 0; x < w; x++) {
                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }

        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + radius];

                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);

                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h);
        return bitmap;
    }

    private ColorMatrixColorFilter createBlurFilter(float depth) {
        ColorMatrix matrix = new ColorMatrix();
        // 降低对比度和饱和度模拟景深
        float factor = 1 - (depth - 0.5f) * 0.3f;
        matrix.setScale(factor, factor, factor, 1);
        return new ColorMatrixColorFilter(matrix);
    }

    private void updateParallaxFromScroll(int scrollX) {
        // 根据桌面滑动更新视差偏移
        float maxScroll = 1000f; // 估算最大滚动距离
        targetOffsetX = -(scrollX / maxScroll) * 100f;
    }

    private void startAnimationLoop() {
        isActive = true;

        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(16); // ~60fps
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(animation -> {
            // 平滑插值
            float smooth = config.getMotionSmooth();
            offsetX += (targetOffsetX - offsetX) * (1 - smooth);
            offsetY += (targetOffsetY - offsetY) * (1 - smooth);
        });
        animator.start();

        LogUtil.i(TAG, "Animation loop started");
    }

    public void updateOffset(float x, float y) {
        targetOffsetX = x;
        targetOffsetY = y;
    }

    public void destroy() {
        isActive = false;
        if (animator != null) {
            animator.cancel();
        }
        for (RasterLayer layer : layers) {
            if (layer.bitmap != null && !layer.bitmap.isRecycled()) {
                layer.bitmap.recycle();
            }
        }
        layers.clear();
    }

    private static class RasterLayer {
        Bitmap bitmap;
        float depth;

        RasterLayer(Bitmap bitmap, float depth) {
            this.bitmap = bitmap;
            this.depth = depth;
        }
    }
}
