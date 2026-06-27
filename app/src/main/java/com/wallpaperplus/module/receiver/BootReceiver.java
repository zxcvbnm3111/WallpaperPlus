package com.wallpaperplus.module.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.wallpaperplus.module.service.LyricOverlayService;
import com.wallpaperplus.module.service.MotionSensorService;
import com.wallpaperplus.module.util.ConfigManager;
import com.wallpaperplus.module.util.LogUtil;

/**
 * 开机广播接收器
 * 设备重启后自动启动相关服务
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            LogUtil.i(TAG, "Boot completed, starting services...");

            ConfigManager config = ConfigManager.getInstance();

            // 启动歌词服务
            if (config.isLyricEnabled()) {
                Intent lyricIntent = new Intent(context, LyricOverlayService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(lyricIntent);
                } else {
                    context.startService(lyricIntent);
                }
                LogUtil.i(TAG, "Lyric overlay service started");
            }

            // 启动运动传感器服务
            if (config.isMotionEnabled()) {
                Intent motionIntent = new Intent(context, MotionSensorService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(motionIntent);
                } else {
                    context.startService(motionIntent);
                }
                LogUtil.i(TAG, "Motion sensor service started");
            }
        }
    }
}
