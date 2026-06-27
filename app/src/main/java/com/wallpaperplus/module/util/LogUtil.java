package com.wallpaperplus.module.util;

import android.util.Log;

import de.robv.android.xposed.XposedBridge;

/**
 * 日志工具
 */
public class LogUtil {
    private static final String PREFIX = "[WallpaperPlus] ";
    private static final boolean DEBUG = true;

    public static void i(String tag, String msg) {
        String log = PREFIX + "[" + tag + "] " + msg;
        if (DEBUG) {
            Log.i(tag, log);
            XposedBridge.log(log);
        }
    }

    public static void d(String tag, String msg) {
        String log = PREFIX + "[" + tag + "] " + msg;
        if (DEBUG) {
            Log.d(tag, log);
            XposedBridge.log(log);
        }
    }

    public static void e(String tag, String msg, Throwable t) {
        String log = PREFIX + "[" + tag + "] " + msg;
        Log.e(tag, log, t);
        XposedBridge.log(log + "\n" + Log.getStackTraceString(t));
    }

    public static void w(String tag, String msg) {
        String log = PREFIX + "[" + tag + "] " + msg;
        Log.w(tag, log);
        XposedBridge.log(log);
    }
}
