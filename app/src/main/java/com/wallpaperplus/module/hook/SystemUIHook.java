package com.wallpaperplus.module.hook;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.wallpaperplus.module.util.ConfigManager;
import com.wallpaperplus.module.util.LogUtil;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * SystemUI 增强 Hook
 * 
 * 提供 SystemUI 层面的增强功能:
 * - 锁屏界面定制
 * - 状态栏美化
 * - 快捷设置增强
 * 
 * 为其他功能模块提供 SystemUI 层面的支持
 */
public class SystemUIHook {

    private static final String TAG = "SystemUIHook";
    private ConfigManager config;
    private LockscreenLyricHook lyricHook;

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        config = ConfigManager.getInstance();

        LogUtil.i(TAG, "Initializing SystemUI hook...");

        // 初始化歌词 Hook（SystemUI 上下文）
        if (config.isLyricEnabled()) {
            lyricHook = new LockscreenLyricHook();
            hookSystemUIContext(lpparam);
        }

        // 锁屏界面增强
        hookKeyguardView(lpparam);

        // 状态栏增强
        hookStatusBar(lpparam);
    }

    private void hookSystemUIContext(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook SystemUI Application attach 获取 Context
            XposedHelpers.findAndHookMethod("android.app.Application", lpparam.classLoader,
                "attach", Context.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Context context = (Context) param.args[0];
                        if (lyricHook != null) {
                            lyricHook.setSystemContext(context);
                        }
                        LogUtil.i(TAG, "SystemUI context obtained");
                    }
                });
        } catch (Exception e) {
            LogUtil.e(TAG, "Failed to hook SystemUI context", e);
        }
    }

    private void hookKeyguardView(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook 锁屏视图创建
            Class<?> keyguardStatusBarView = XposedHelpers.findClass(
                "com.android.systemui.statusbar.phone.KeyguardStatusBarView", lpparam.classLoader);

            XposedHelpers.findAndHookConstructor(keyguardStatusBarView, Context.class, android.util.AttributeSet.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        LogUtil.i(TAG, "KeyguardStatusBarView created");
                    }
                });

            // Hook 锁屏时钟
            Class<?> keyguardClock = XposedHelpers.findClass(
                "com.android.keyguard.KeyguardClockSwitch", lpparam.classLoader);

            XposedHelpers.findAndHookMethod(keyguardClock, "onFinishInflate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    View view = (View) param.thisObject;
                    // 可以在这里修改时钟样式
                    LogUtil.i(TAG, "KeyguardClockSwitch inflated");
                }
            });

            LogUtil.i(TAG, "Keyguard view hooked");
        } catch (Exception e) {
            LogUtil.e(TAG, "Failed to hook keyguard view", e);
        }
    }

    private void hookStatusBar(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook 状态栏图标区域
            Class<?> statusBarIconView = XposedHelpers.findClass(
                "com.android.systemui.statusbar.StatusBarIconView", lpparam.classLoader);

            XposedHelpers.findAndHookMethod(statusBarIconView, "setIcon",
                "com.android.internal.statusbar.StatusBarIcon", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        // 可以在这里修改图标显示
                    }
                });

            LogUtil.i(TAG, "StatusBar hooked");
        } catch (Exception e) {
            LogUtil.e(TAG, "Failed to hook status bar", e);
        }
    }

    /**
     * 发送歌词更新广播（供通知监听器使用）
     */
    public static void sendLyricBroadcast(Context context, String lyric, String song, String artist) {
        Intent intent = new Intent("com.wallpaperplus.module.LYRIC_UPDATE");
        intent.putExtra("lyric", lyric);
        intent.putExtra("song", song);
        intent.putExtra("artist", artist);
        context.sendBroadcast(intent);
    }
}
