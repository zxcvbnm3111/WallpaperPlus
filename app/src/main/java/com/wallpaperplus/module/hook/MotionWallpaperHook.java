package com.wallpaperplus.module.hook;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import com.wallpaperplus.module.util.ConfigManager;
import com.wallpaperplus.module.util.LogUtil;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 3D壁纸晃动 Hook
 * 
 * 利用设备的陀螺仪传感器数据，实现壁纸的3D视差晃动效果。
 * 当用户倾斜设备时，壁纸会产生相应的3D透视变换，营造深度感。
 * 
 * 特性:
 * - 支持 X/Y 轴双向晃动
 * - 可自定义灵敏度和平滑度
 * - 低功耗设计，仅在桌面可见时激活
 * - 适配 ColorOS/OPlus 桌面
 */
public class MotionWallpaperHook implements SensorEventListener {

    private static final String TAG = "MotionWallpaper";

    private ConfigManager config;
    private SensorManager sensorManager;
    private Sensor gyroscopeSensor;
    private Sensor accelerometerSensor;

    // 传感器数据
    private float[] gyroValues = new float[3];
    private float[] accelValues = new float[3];
    private float[] rotationMatrix = new float[9];
    private float[] orientationAngles = new float[3];

    // 动画参数
    private float currentRotateX = 0f;
    private float currentRotateY = 0f;
    private float targetRotateX = 0f;
    private float targetRotateY = 0f;

    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private ValueAnimator updateAnimator;
    private boolean isActive = false;
    private boolean isVisible = false;

    // 3D变换
    private Camera camera = new Camera();
    private Matrix matrix = new Matrix();

    // 目标壁纸视图
    private View targetWallpaperView = null;

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        config = ConfigManager.getInstance();
        if (!config.isMotionEnabled()) {
            LogUtil.i(TAG, "3D motion wallpaper disabled, skip");
            return;
        }

        LogUtil.i(TAG, "Initializing 3D motion wallpaper hook...");

        // Hook 桌面壁纸视图
        hookWallpaperView(lpparam);

        // Hook 桌面可见性变化
        hookVisibility(lpparam);

        // 启动更新循环
        startUpdateLoop();
    }

    private void hookWallpaperView(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook ImageView 的 onDraw 方法，应用3D变换
            XposedHelpers.findAndHookMethod(ImageView.class, "onDraw", Canvas.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    if (!isActive || !isVisible) return;

                    ImageView imageView = (ImageView) param.thisObject;

                    // 检查是否是壁纸视图
                    if (!isWallpaperView(imageView)) return;

                    targetWallpaperView = imageView;

                    // 应用3D变换到 Canvas
                    Canvas canvas = (Canvas) param.args[0];
                    apply3DTransform(canvas, imageView.getWidth(), imageView.getHeight());
                }

                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!isActive || !isVisible) return;

                    ImageView imageView = (ImageView) param.thisObject;
                    if (!isWallpaperView(imageView)) return;

                    // 恢复 Canvas 状态
                    Canvas canvas = (Canvas) param.args[0];
                    canvas.restore();
                }
            });

            LogUtil.i(TAG, "Wallpaper view hooked for 3D motion");
        } catch (Exception e) {
            LogUtil.e(TAG, "Failed to hook wallpaper view", e);
        }
    }

    private void hookVisibility(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook 桌面 onResume/onPause 控制传感器
            Class<?> launcherClass = null;
            for (String className : new String[]{
                "com.android.launcher3.Launcher",
                "com.oppo.launcher.Launcher",
                "com.coloros.launcher.Launcher",
                "com.heytap.launcher.Launcher"
            }) {
                try {
                    launcherClass = XposedHelpers.findClass(className, lpparam.classLoader);
                    break;
                } catch (Exception ignored) {}
            }

            if (launcherClass != null) {
                XposedHelpers.findAndHookMethod(launcherClass, "onResume", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        isVisible = true;
                        registerSensors();
                    }
                });

                XposedHelpers.findAndHookMethod(launcherClass, "onPause", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        isVisible = false;
                        unregisterSensors();
                    }
                });

                LogUtil.i(TAG, "Launcher visibility hooked");
            }
        } catch (Exception e) {
            LogUtil.e(TAG, "Failed to hook visibility", e);
        }
    }

    private boolean isWallpaperView(ImageView view) {
        Object tag = view.getTag();
        if (tag != null && tag.toString().toLowerCase().contains("wallpaper")) return true;

        android.view.android.view.ViewParent parent = view.getParent();
        if (parent != null) {
            String parentName = parent.getClass().getName().toLowerCase();
            return parentName.contains("wallpaper") || parentName.contains("workspace") || parentName.contains("draglayer");
        }
        return false;
    }

    private void apply3DTransform(Canvas canvas, int width, int height) {
        if (canvas == null || width <= 0 || height <= 0) return;
        canvas.save();

        float sensitivity = config.getMotionSensitivity();
        String direction = config.getMotionDirection();

        float rotateX = 0f;
        float rotateY = 0f;

        // 根据方向设置旋转
        if ("both".equals(direction) || "vertical".equals(direction)) {
            rotateX = currentRotateX * sensitivity;
        }
        if ("both".equals(direction) || "horizontal".equals(direction)) {
            rotateY = currentRotateY * sensitivity;
        }

        // 限制最大旋转角度
        float maxAngle = 15f;
        rotateX = Math.max(-maxAngle, Math.min(maxAngle, rotateX));
        rotateY = Math.max(-maxAngle, Math.min(maxAngle, rotateY));

        // 使用 Camera 实现3D透视
        camera.save();

        // 设置透视深度
        camera.setLocation(0, 0, -width * 2f);

        // 应用旋转
        camera.rotateX(rotateX);
        camera.rotateY(rotateY);

        // 获取变换矩阵
        matrix.reset();
        camera.getMatrix(matrix);
        camera.restore();

        // 以中心点为变换中心
        matrix.preTranslate(-width / 2f, -height / 2f);
        matrix.postTranslate(width / 2f, height / 2f);

        // 应用矩阵到 Canvas
        canvas.concat(matrix);
    }

    private void registerSensors() {
        if (sensorManager != null) return;

        try {
            // 获取 SensorManager
            Context context = getSystemContext();
            if (context == null) return;

            sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager == null) return;

            // 注册陀螺仪
            gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            if (gyroscopeSensor != null) {
                sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME);
            }

            // 注册加速度计（用于辅助计算）
            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometerSensor != null) {
                sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
            }

            isActive = true;
            LogUtil.i(TAG, "Sensors registered");
        } catch (Exception e) {
            LogUtil.e(TAG, "Failed to register sensors", e);
        }
    }

    private void unregisterSensors() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            sensorManager = null;
        }
        isActive = false;
        LogUtil.i(TAG, "Sensors unregistered");
    }

    private void startUpdateLoop() {
        updateAnimator = ValueAnimator.ofFloat(0f, 1f);
        updateAnimator.setDuration(16); // ~60fps
        updateAnimator.setRepeatCount(ValueAnimator.INFINITE);
        updateAnimator.setInterpolator(new DecelerateInterpolator());
        updateAnimator.addUpdateListener(animation -> {
            if (!isActive) return;

            // 平滑插值
            float smooth = config.getMotionSmooth();
            currentRotateX += (targetRotateX - currentRotateX) * (1 - smooth);
            currentRotateY += (targetRotateY - currentRotateY) * (1 - smooth);

            // 触发重绘
            if (targetWallpaperView != null) {
                targetWallpaperView.invalidate();
            }
        });
        updateAnimator.start();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isActive) return;

        switch (event.sensor.getType()) {
            case Sensor.TYPE_GYROSCOPE:
                // 处理陀螺仪数据
                gyroValues[0] = event.values[0]; // X轴角速度
                gyroValues[1] = event.values[1]; // Y轴角速度
                gyroValues[2] = event.values[2]; // Z轴角速度

                // 积分计算角度（简化版）
                float dt = 0.016f; // 假设60fps
                targetRotateX += gyroValues[0] * dt * 57.2958f; // rad to deg
                targetRotateY += gyroValues[1] * dt * 57.2958f;

                // 衰减回中
                targetRotateX *= 0.95f;
                targetRotateY *= 0.95f;
                break;

            case Sensor.TYPE_ACCELEROMETER:
                // 使用加速度计辅助校正
                accelValues[0] = event.values[0];
                accelValues[1] = event.values[1];
                accelValues[2] = event.values[2];

                // 计算倾斜角度
                float pitch = (float) Math.atan2(accelValues[1], 
                    Math.sqrt(accelValues[0] * accelValues[0] + accelValues[2] * accelValues[2]));
                float roll = (float) Math.atan2(-accelValues[0], accelValues[2]);

                // 融合数据（互补滤波）
                float alpha = 0.8f;
                targetRotateX = alpha * targetRotateX + (1 - alpha) * (pitch * 57.2958f);
                targetRotateY = alpha * targetRotateY + (1 - alpha) * (roll * 57.2958f);
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 不需要处理
    }

    private Context getSystemContext() {
        try {
            return (Context) XposedHelpers.callMethod(
                XposedHelpers.callStaticMethod(
                    XposedHelpers.findClass("android.app.ActivityThread", null),
                    "currentActivityThread"
                ),
                "getSystemContext"
            );
        } catch (Exception e) {
            LogUtil.e(TAG, "Failed to get system context", e);
            return null;
        }
    }

    public void destroy() {
        unregisterSensors();
        if (updateAnimator != null) {
            updateAnimator.cancel();
        }
    }
}
