package com.wallpaperplus.module;

import android.app.Application;
import android.content.Context;

import com.wallpaperplus.module.hook.LockscreenLyricHook;
import com.wallpaperplus.module.hook.RasterWallpaperHook;
import com.wallpaperplus.module.hook.MotionWallpaperHook;
import com.wallpaperplus.module.hook.SystemUIHook;
import com.wallpaperplus.module.util.ConfigManager;
import com.wallpaperplus.module.util.LogUtil;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * WallpaperPlus - Xposed 主入口
 * 支持 OPlus / ColorOS 设备
 * 
 * 功能模块:
 * 1. RasterWallpaperHook - 光栅壁纸
 * 2. LockscreenLyricHook - 锁屏歌词
 * 3. MotionWallpaperHook - 3D壁纸晃动
 * 4. SystemUIHook - SystemUI 增强
 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "WallpaperPlus";
    private static boolean configInitialized = false;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        String packageName = lpparam.packageName;

        LogUtil.i(TAG, "Loading package: " + packageName);

        // 初始化配置管理器（延迟到 Application attach）
        if (!configInitialized) {
            initConfig(lpparam);
        }

        // 根据包名分发 Hook
        switch (packageName) {
            case "com.android.systemui":
                hookSystemUI(lpparam);
                break;
            case "com.android.launcher":
            case "com.android.launcher3":
            case "com.oppo.launcher":
            case "com.coloros.launcher":
            case "com.heytap.launcher":
                hookLauncher(lpparam);
                break;
            case "com.netease.cloudmusic":
            case "com.tencent.qqmusic":
            case "com.kugou.android":
            case "com.kugou.android.lite":
            case "cmccwm.mobilemusic":
            case "com.spotify.music":
            case "com.apple.android.music":
                hookMusicApp(lpparam);
                break;
            default:
                // 其他包不处理
                break;
        }
    }

    private void initConfig(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(Application.class, "attach", Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Context context = (Context) param.args[0];
                    if (context != null) {
                        ConfigManager.getInstance().init(context);
                        configInitialized = true;
                        LogUtil.i(TAG, "ConfigManager initialized");
                    }
                }
            });
        } catch (Exception e) {
            LogUtil.e(TAG, "Config init failed", e);
        }
    }

    private void hookSystemUI(XC_LoadPackage.LoadPackageParam lpparam) {
        LogUtil.i(TAG, "Hooking SystemUI...");
        try {
            new LockscreenLyricHook().handleLoadPackage(lpparam);
            new SystemUIHook().handleLoadPackage(lpparam);
        } catch (Exception e) {
            LogUtil.e(TAG, "SystemUI hook failed", e);
        }
    }

    private void hookLauncher(XC_LoadPackage.LoadPackageParam lpparam) {
        LogUtil.i(TAG, "Hooking Launcher...");
        try {
            new RasterWallpaperHook().handleLoadPackage(lpparam);
            new MotionWallpaperHook().handleLoadPackage(lpparam);
        } catch (Exception e) {
            LogUtil.e(TAG, "Launcher hook failed", e);
        }
    }

    private void hookMusicApp(XC_LoadPackage.LoadPackageParam lpparam) {
        LogUtil.i(TAG, "Hooking Music App: " + lpparam.packageName);
        try {
            new LockscreenLyricHook().hookMusicApp(lpparam);
        } catch (Exception e) {
            LogUtil.e(TAG, "Music app hook failed", e);
        }
    }
}
