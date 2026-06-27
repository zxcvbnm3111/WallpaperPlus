package com.wallpaperplus.module.hook;

import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.wallpaperplus.module.util.ConfigManager;
import com.wallpaperplus.module.util.LogUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 锁屏歌词 Hook
 * 
 * 支持的音乐应用:
 * - 网易云音乐 (com.netease.cloudmusic)
 * - QQ音乐 (com.tencent.qqmusic)
 * - 酷狗音乐 (com.kugou.android / com.kugou.android.lite)
 * - 咪咕音乐 (cmccwm.mobilemusic)
 * - Spotify (com.spotify.music)
 * - Apple Music (com.apple.android.music)
 * 
 * 实现原理:
 * 1. Hook 音乐应用的通知，提取歌词信息
 * 2. 在 SystemUI 锁屏界面上叠加歌词视图
 * 3. 支持多种显示样式和位置
 */
public class LockscreenLyricHook {

    private static final String TAG = "LockscreenLyric";

    private ConfigManager config;
    private Context systemContext;
    private WindowManager windowManager;
    private FrameLayout lyricContainer;
    private TextView lyricTextView;
    private TextView lyricTextView2; // 双行模式用
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    // 当前歌词状态
    private String currentLyric = "";
    private String nextLyric = "";
    private String currentSong = "";
    private String currentArtist = "";
    private boolean isPlaying = false;
    private boolean isShowing = false;

    // 各应用的通知字段映射
    private static final Map<String, NotificationField> APP_FIELDS = new HashMap<>();

    static {
        // 网易云音乐
        APP_FIELDS.put("com.netease.cloudmusic", new NotificationField(
            "android.title",           // 歌曲名
            "android.text",            // 艺术家
            "android.subText",         // 歌词
            "android.mediaSession"     // 播放状态
        ));

        // QQ音乐
        APP_FIELDS.put("com.tencent.qqmusic", new NotificationField(
            "android.title",
            "android.text",
            "android.subText",
            null
        ));

        // 酷狗音乐
        APP_FIELDS.put("com.kugou.android", new NotificationField(
            "android.title",
            "android.text",
            "android.subText",
            null
        ));
        APP_FIELDS.put("com.kugou.android.lite", new NotificationField(
            "android.title",
            "android.text",
            "android.subText",
            null
        ));

        // 咪咕音乐
        APP_FIELDS.put("cmccwm.mobilemusic", new NotificationField(
            "android.title",
            "android.text",
            "android.subText",
            null
        ));

        // Spotify
        APP_FIELDS.put("com.spotify.music", new NotificationField(
            "android.title",
            "android.text",
            null,
            null
        ));

        // Apple Music
        APP_FIELDS.put("com.apple.android.music", new NotificationField(
            "android.title",
            "android.text",
            null,
            null
        ));
    }

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        config = ConfigManager.getInstance();
        if (!config.isLyricEnabled()) {
            LogUtil.i(TAG, "Lockscreen lyric disabled, skip");
            return;
        }

        LogUtil.i(TAG, "Initializing lockscreen lyric hook...");

        // Hook SystemUI 的锁屏界面
        hookKeyguard(lpparam);

        // Hook 通知服务
        hookNotificationService(lpparam);
    }

    public void hookMusicApp(XC_LoadPackage.LoadPackageParam lpparam) {
        // 在音乐应用内 Hook 歌词相关方法（更精确的歌词获取）
        String packageName = lpparam.packageName;

        try {
            switch (packageName) {
                case "com.netease.cloudmusic":
                    hookNeteaseMusic(lpparam);
                    break;
                case "com.tencent.qqmusic":
                    hookQQMusic(lpparam);
                    break;
                case "com.kugou.android":
                case "com.kugou.android.lite":
                    hookKugouMusic(lpparam);
                    break;
            }
        } catch (Exception e) {
            LogUtil.e(TAG, "Failed to hook music app: " + packageName, e);
        }
    }

    private void hookKeyguard(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook SystemUI 的锁屏状态变化
            Class<?> keyguardUpdateMonitor = XposedHelpers.findClass(
                "com.android.keyguard.KeyguardUpdateMonitor", lpparam.classLoader);

            XposedHelpers.findAndHookMethod(keyguardUpdateMonitor, "onKeyguardVisibilityChanged", boolean.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    boolean showing = (boolean) param.args[0];
                    if (showing && config.isLyricEnabled()) {
                        showLyricOverlay();
                    } else {
                        hideLyricOverlay();
                    }
                }
            });

            LogUtil.i(TAG, "Keyguard hooked");
        } catch (Exception e) {
            LogUtil.e(TAG, "Failed to hook keyguard", e);
        }
    }

    private void hookNotificationService(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook NotificationListenerService 的 onNotificationPosted
            XposedHelpers.findAndHookMethod(NotificationListenerService.class, "onNotificationPosted",
                StatusBarNotification.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        StatusBarNotification sbn = (StatusBarNotification) param.args[0];
                        String pkg = sbn.getPackageName();

                        if (APP_FIELDS.containsKey(pkg)) {
                            parseMusicNotification(sbn);
                        }
                    }
                });

            LogUtil.i(TAG, "Notification service hooked");
        } catch (Exception e) {
            LogUtil.e(TAG, "Failed to hook notification service", e);
        }
    }

    private void parseMusicNotification(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) return;

        Notification notification = sbn.getNotification();
        Bundle extras = notification.extras;
        String pkg = sbn.getPackageName();

        NotificationField fields = APP_FIELDS.get(pkg);
        if (fields == null) return;

        // 提取歌曲信息
        String title = getStringExtra(extras, fields.titleKey);
        String artist = getStringExtra(extras, fields.artistKey);
        String lyric = getStringExtra(extras, fields.lyricKey);

        // 如果 extras 中没有歌词，尝试从通知文本中提取
        if (TextUtils.isEmpty(lyric)) {
            lyric = extractLyricFromText(extras);
        }

        // 更新状态
        if (!TextUtils.isEmpty(title) && !title.equals(currentSong)) {
            currentSong = title;
            currentArtist = artist;
        }

        if (!TextUtils.isEmpty(lyric) && !lyric.equals(currentLyric)) {
            currentLyric = lyric;
            updateLyricDisplay();
        }

        // 判断播放状态
        isPlaying = !notification.actions.isEmpty();
    }

    private String getStringExtra(Bundle extras, String key) {
        if (key == null) return null;
        CharSequence cs = extras.getCharSequence(key);
        return cs != null ? cs.toString() : null;
    }

    private String extractLyricFromText(Bundle extras) {
        // 尝试从通知文本中提取歌词
        CharSequence text = extras.getCharSequence("android.text");
        CharSequence subText = extras.getCharSequence("android.subText");

        // 歌词通常包含时间戳或特殊标记
        if (text != null) {
            String str = text.toString();
            // 过滤掉艺术家名等，尝试获取歌词
            if (str.length() > 5 && !str.contains(" - ") && !str.contains("/")) {
                return str;
            }
        }

        if (subText != null) {
            return subText.toString();
        }

        return null;
    }

    private void hookNeteaseMusic(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook 网易云的歌词更新方法
            Class<?> lyricController = XposedHelpers.findClass(
                "com.netease.cloudmusic.module.lyric.a", lpparam.classLoader);

            XposedHelpers.findAndHookMethod(lyricController, "a", String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    String lyric = (String) param.args[0];
                    if (!TextUtils.isEmpty(lyric)) {
                        currentLyric = lyric;
                        updateLyricDisplay();
                    }
                }
            });

            LogUtil.i(TAG, "Netease music lyric hooked");
        } catch (Exception e) {
            LogUtil.w(TAG, "Netease lyric hook failed, fallback to notification");
        }
    }

    private void hookQQMusic(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> lyricMgr = XposedHelpers.findClass(
                "com.tencent.qqmusic.business.lyric.newlyric.LyricManager", lpparam.classLoader);

            XposedHelpers.findAndHookMethod(lyricMgr, "updateCurrentLyric", String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    String lyric = (String) param.args[0];
                    if (!TextUtils.isEmpty(lyric)) {
                        currentLyric = lyric;
                        updateLyricDisplay();
                    }
                }
            });

            LogUtil.i(TAG, "QQ music lyric hooked");
        } catch (Exception e) {
            LogUtil.w(TAG, "QQ music lyric hook failed, fallback to notification");
        }
    }

    private void hookKugouMusic(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> lyricView = XposedHelpers.findClass(
                "com.kugou.android.media.lyric.LyricView", lpparam.classLoader);

            XposedHelpers.findAndHookMethod(lyricView, "setCurrentLyric", String.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    String lyric = (String) param.args[0];
                    if (!TextUtils.isEmpty(lyric)) {
                        currentLyric = lyric;
                        updateLyricDisplay();
                    }
                }
            });

            LogUtil.i(TAG, "Kugou music lyric hooked");
        } catch (Exception e) {
            LogUtil.w(TAG, "Kugou lyric hook failed, fallback to notification");
        }
    }

    private void showLyricOverlay() {
        if (isShowing || systemContext == null) return;

        mainHandler.post(() -> {
            try {
                if (lyricContainer == null) {
                    createLyricView();
                }

                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O 
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
                        : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE 
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT
                );

                // 根据设置调整位置
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
                        params.y = 300;
                        break;
                }

                windowManager.addView(lyricContainer, params);
                isShowing = true;
                updateLyricDisplay();

                LogUtil.i(TAG, "Lyric overlay shown");
            } catch (Exception e) {
                LogUtil.e(TAG, "Failed to show lyric overlay", e);
            }
        });
    }

    private void hideLyricOverlay() {
        if (!isShowing) return;

        mainHandler.post(() -> {
            try {
                if (lyricContainer != null && lyricContainer.getParent() != null) {
                    windowManager.removeView(lyricContainer);
                }
                isShowing = false;
                LogUtil.i(TAG, "Lyric overlay hidden");
            } catch (Exception e) {
                LogUtil.e(TAG, "Failed to hide lyric overlay", e);
            }
        });
    }

    private void createLyricView() {
        lyricContainer = new FrameLayout(systemContext);
        lyricContainer.setBackgroundColor(0x00000000); // 透明背景

        // 主歌词文本
        lyricTextView = new TextView(systemContext);
        lyricTextView.setTextSize(config.getLyricSize());
        lyricTextView.setTextColor(config.getLyricColor());
        lyricTextView.setGravity(Gravity.CENTER);
        lyricTextView.setShadowLayer(4f, 0f, 2f, 0xCC000000); // 文字阴影
        lyricTextView.setMaxLines(2);
        lyricTextView.setEllipsize(TextUtils.TruncateAt.END);

        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        );
        textParams.gravity = Gravity.CENTER;
        lyricContainer.addView(lyricTextView, textParams);

        // 双行模式：下一行歌词
        String style = config.getLyricStyle();
        if ("dual".equals(style)) {
            lyricTextView2 = new TextView(systemContext);
            lyricTextView2.setTextSize(config.getLyricSize() * 0.8f);
            lyricTextView2.setTextColor(config.getLyricColor() & 0x80FFFFFF); // 半透明
            lyricTextView2.setGravity(Gravity.CENTER);
            lyricTextView2.setShadowLayer(3f, 0f, 1f, 0xCC000000);

            FrameLayout.LayoutParams text2Params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            );
            text2Params.gravity = Gravity.CENTER;
            text2Params.topMargin = (int) (config.getLyricSize() * 2.5f);
            lyricContainer.addView(lyricTextView2, text2Params);
        }
    }

    private void updateLyricDisplay() {
        if (!isShowing || lyricTextView == null) return;

        mainHandler.post(() -> {
            String style = config.getLyricStyle();

            switch (style) {
                case "single":
                    lyricTextView.setText(currentLyric);
                    break;
                case "dual":
                    case "karaoke":
                    default:
                        lyricTextView.setText(currentLyric);
                        if (lyricTextView2 != null) {
                            lyricTextView2.setText(nextLyric);
                        }
                        break;
            }

            // 卡拉OK效果：高亮当前字
            if ("karaoke".equals(style)) {
                applyKaraokeEffect();
            }
        });
    }

    private void applyKaraokeEffect() {
        // 简化的卡拉OK效果实现
        // 实际实现需要解析歌词时间戳，这里提供框架
        // 可以通过 SpannableString 实现逐字高亮
    }

    public void setSystemContext(Context context) {
        this.systemContext = context;
        this.windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    private static class NotificationField {
        String titleKey;
        String artistKey;
        String lyricKey;
        String sessionKey;

        NotificationField(String titleKey, String artistKey, String lyricKey, String sessionKey) {
            this.titleKey = titleKey;
            this.artistKey = artistKey;
            this.lyricKey = lyricKey;
            this.sessionKey = sessionKey;
        }
    }
}
