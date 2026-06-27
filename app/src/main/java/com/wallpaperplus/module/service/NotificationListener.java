package com.wallpaperplus.module.service;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import com.wallpaperplus.module.util.ConfigManager;
import com.wallpaperplus.module.util.LogUtil;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 通知监听器
 * 监听音乐应用的通知，提取歌词和播放信息
 */
public class NotificationListener extends NotificationListenerService {

    private static final String TAG = "NotificationListener";

    private static final Set<String> MUSIC_PACKAGES = new HashSet<>(Arrays.asList(
        "com.netease.cloudmusic",
        "com.tencent.qqmusic",
        "com.kugou.android",
        "com.kugou.android.lite",
        "cmccwm.mobilemusic",
        "com.spotify.music",
        "com.apple.android.music"
    ));

    private ConfigManager config;

    @Override
    public void onCreate() {
        super.onCreate();
        config = ConfigManager.getInstance();
        LogUtil.i(TAG, "Notification listener created");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();

        if (!MUSIC_PACKAGES.contains(packageName)) return;
        if (!config.isLyricEnabled()) return;

        try {
            String title = "";
            String text = "";
            String subText = "";

            // 提取通知内容
            if (sbn.getNotification().extras != null) {
                CharSequence titleCs = sbn.getNotification().extras.getCharSequence("android.title");
                CharSequence textCs = sbn.getNotification().extras.getCharSequence("android.text");
                CharSequence subTextCs = sbn.getNotification().extras.getCharSequence("android.subText");

                if (titleCs != null) title = titleCs.toString();
                if (textCs != null) text = textCs.toString();
                if (subTextCs != null) subText = subTextCs.toString();
            }

            // 尝试提取歌词
            String lyric = extractLyric(title, text, subText);

            if (!TextUtils.isEmpty(lyric)) {
                // 发送广播更新歌词
                sendLyricBroadcast(lyric, title, text);
                LogUtil.d(TAG, "Lyric extracted: " + lyric);
            }

        } catch (Exception e) {
            LogUtil.e(TAG, "Error processing notification", e);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // 通知移除时的处理
    }

    private String extractLyric(String title, String text, String subText) {
        // 优先使用 subText 作为歌词
        if (!TextUtils.isEmpty(subText) && subText.length() > 3) {
            return subText;
        }

        // 次选 text
        if (!TextUtils.isEmpty(text) && text.length() > 3 && !text.contains(" - ")) {
            return text;
        }

        return null;
    }

    private void sendLyricBroadcast(String lyric, String song, String artist) {
        android.content.Intent intent = new android.content.Intent("com.wallpaperplus.module.LYRIC_UPDATE");
        intent.putExtra("lyric", lyric);
        intent.putExtra("song", song);
        intent.putExtra("artist", artist);
        sendBroadcast(intent);
    }
}
