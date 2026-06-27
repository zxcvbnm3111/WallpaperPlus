package com.wallpaperplus.module.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

import com.wallpaperplus.module.R;
import com.wallpaperplus.module.ui.MainActivity;
import com.wallpaperplus.module.util.ConfigManager;
import com.wallpaperplus.module.util.LogUtil;

/**
 * 歌词悬浮窗服务
 * 在锁屏时显示歌词覆盖层
 */
public class LyricOverlayService extends Service {

    private static final String TAG = "LyricOverlayService";
    private static final String CHANNEL_ID = "wallpaper_plus_lyric";
    private static final int NOTIFICATION_ID = 1001;

    private WindowManager windowManager;
    private FrameLayout lyricContainer;
    private TextView lyricTextView;
    private TextView lyricTextView2;
    private ConfigManager config;
    private LyricReceiver lyricReceiver;

    private String currentLyric = "";
    private String nextLyric = "";
    private boolean isShowing = false;

    @Override
    public void onCreate() {
        super.onCreate();
        config = ConfigManager.getInstance();
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());

        // 注册歌词更新广播接收器
        lyricReceiver = new LyricReceiver();
        IntentFilter filter = new IntentFilter("com.wallpaperplus.module.LYRIC_UPDATE");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(lyricReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(lyricReceiver, filter);
        }

        LogUtil.i(TAG, "Lyric overlay service started");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        hideLyricOverlay();
        if (lyricReceiver != null) {
            unregisterReceiver(lyricReceiver);
        }
        LogUtil.i(TAG, "Lyric overlay service destroyed");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "锁屏歌词服务",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("保持歌词显示服务运行");
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
            .setContentText("锁屏歌词服务运行中")
            .setSmallIcon(R.drawable.ic_music)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build();
    }

    private void showLyricOverlay() {
        if (isShowing || !config.isLyricEnabled()) return;

        if (lyricContainer == null) {
            createLyricView();
        }

        try {
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                    ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT
            );

            String position = config.getLyricPosition();
            switch (position) {
                case "top":
                    params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                    params.y = 200;
                    break;
                case "center":
                    params.gravity = Gravity.CENTER;
                    break;
                case "bottom":
                default:
                    params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                    params.y = 350;
                    break;
            }

            windowManager.addView(lyricContainer, params);
            isShowing = true;
            updateLyricDisplay();

            LogUtil.i(TAG, "Lyric overlay shown");
        } catch (Exception e) {
            LogUtil.e(TAG, "Failed to show lyric overlay", e);
        }
    }

    private void hideLyricOverlay() {
        if (!isShowing) return;

        try {
            if (lyricContainer != null && lyricContainer.getParent() != null) {
                windowManager.removeView(lyricContainer);
            }
            isShowing = false;
            LogUtil.i(TAG, "Lyric overlay hidden");
        } catch (Exception e) {
            LogUtil.e(TAG, "Failed to hide lyric overlay", e);
        }
    }

    private void createLyricView() {
        lyricContainer = new FrameLayout(this);
        lyricContainer.setBackgroundColor(0x00000000);

        lyricTextView = new TextView(this);
        lyricTextView.setTextSize(config.getLyricSize());
        lyricTextView.setTextColor(config.getLyricColor());
        lyricTextView.setGravity(Gravity.CENTER);
        lyricTextView.setShadowLayer(6f, 0f, 2f, 0xCC000000);
        lyricTextView.setMaxLines(2);
        lyricTextView.setEllipsize(TextUtils.TruncateAt.END);
        lyricTextView.setPadding(40, 20, 40, 20);

        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        textParams.gravity = Gravity.CENTER;
        lyricContainer.addView(lyricTextView, textParams);

        String style = config.getLyricStyle();
        if ("dual".equals(style)) {
            lyricTextView2 = new TextView(this);
            lyricTextView2.setTextSize(config.getLyricSize() * 0.75f);
            lyricTextView2.setTextColor(config.getLyricColor() & 0xB0FFFFFF);
            lyricTextView2.setGravity(Gravity.CENTER);
            lyricTextView2.setShadowLayer(4f, 0f, 1f, 0xCC000000);
            lyricTextView2.setMaxLines(1);
            lyricTextView2.setEllipsize(TextUtils.TruncateAt.END);

            FrameLayout.LayoutParams text2Params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            );
            text2Params.gravity = Gravity.CENTER;
            text2Params.topMargin = (int) (config.getLyricSize() * 3f);
            lyricContainer.addView(lyricTextView2, text2Params);
        }
    }

    private void updateLyricDisplay() {
        if (!isShowing || lyricTextView == null) return;

        String style = config.getLyricStyle();

        switch (style) {
            case "single":
                lyricTextView.setText(currentLyric);
                break;
            case "dual":
            case "karaoke":
                lyricTextView.setText(currentLyric);
                if (lyricTextView2 != null) {
                    lyricTextView2.setText(nextLyric);
                }
                break;
        }
    }

    private void updateLyric(String lyric, String song, String artist) {
        if (!TextUtils.isEmpty(lyric)) {
            currentLyric = lyric;
            updateLyricDisplay();
            showLyricOverlay();
        }
    }

    private class LyricReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.wallpaperplus.module.LYRIC_UPDATE".equals(intent.getAction())) {
                String lyric = intent.getStringExtra("lyric");
                String song = intent.getStringExtra("song");
                String artist = intent.getStringExtra("artist");
                updateLyric(lyric, song, artist);
            }
        }
    }
}
