package com.wallpaperplus.module.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.wallpaperplus.module.R;
import com.wallpaperplus.module.ui.MainActivity;
import com.wallpaperplus.module.util.ConfigManager;
import com.wallpaperplus.module.util.LogUtil;

/**
 * 运动传感器服务
 * 为3D壁纸晃动提供传感器数据监听
 */
public class MotionSensorService extends Service implements SensorEventListener {

    private static final String TAG = "MotionSensorService";
    private static final String CHANNEL_ID = "wallpaper_plus_motion";
    private static final int NOTIFICATION_ID = 1002;

    private SensorManager sensorManager;
    private Sensor gyroscopeSensor;
    private Sensor accelerometerSensor;
    private ConfigManager config;

    // 传感器数据回调接口
    private MotionDataCallback callback;

    // 原始传感器数据
    private float[] gyroValues = new float[3];
    private float[] accelValues = new float[3];
    private float[] rotationMatrix = new float[9];
    private float[] orientationAngles = new float[3];

    // 平滑后的输出数据
    private float smoothedPitch = 0f;
    private float smoothedRoll = 0f;

    public interface MotionDataCallback {
        void onMotionData(float pitch, float roll, float yaw);
    }

    public class LocalBinder extends Binder {
        public MotionSensorService getService() {
            return MotionSensorService.this;
        }
    }

    private final IBinder binder = new LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        config = ConfigManager.getInstance();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());

        LogUtil.i(TAG, "Motion sensor service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (config.isMotionEnabled()) {
            registerSensors();
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterSensors();
        LogUtil.i(TAG, "Motion sensor service destroyed");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "3D壁纸晃动服务",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("保持3D壁纸传感器监听");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WallpaperPlus")
            .setContentText("3D壁纸晃动服务运行中")
            .setSmallIcon(R.drawable.ic_motion)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }

    public void registerSensors() {
        if (sensorManager == null) return;

        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (gyroscopeSensor != null) {
            sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME);
            LogUtil.i(TAG, "Gyroscope registered");
        }

        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometerSensor != null) {
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
            LogUtil.i(TAG, "Accelerometer registered");
        }
    }

    public void unregisterSensors() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            LogUtil.i(TAG, "Sensors unregistered");
        }
    }

    public void setCallback(MotionDataCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!config.isMotionEnabled()) return;

        float smooth = config.getMotionSmooth();

        switch (event.sensor.getType()) {
            case Sensor.TYPE_GYROSCOPE:
                System.arraycopy(event.values, 0, gyroValues, 0, 3);
                break;

            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(event.values, 0, accelValues, 0, 3);

                // 计算倾斜角度
                float pitch = (float) Math.atan2(accelValues[1], 
                    Math.sqrt(accelValues[0] * accelValues[0] + accelValues[2] * accelValues[2]));
                float roll = (float) Math.atan2(-accelValues[0], accelValues[2]);

                // 平滑滤波
                smoothedPitch = smooth * smoothedPitch + (1 - smooth) * (pitch * 57.2958f);
                smoothedRoll = smooth * smoothedRoll + (1 - smooth) * (roll * 57.2958f);

                // 回调
                if (callback != null) {
                    callback.onMotionData(smoothedPitch, smoothedRoll, gyroValues[2]);
                }
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // 不需要处理
    }

    public float getSmoothedPitch() {
        return smoothedPitch;
    }

    public float getSmoothedRoll() {
        return smoothedRoll;
    }
}
