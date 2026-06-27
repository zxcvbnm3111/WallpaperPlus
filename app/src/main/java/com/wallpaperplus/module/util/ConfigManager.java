package com.wallpaperplus.module.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import java.io.File;

import de.robv.android.xposed.XSharedPreferences;

/**
 * 配置管理器
 * 管理模块所有功能开关和参数
 */
public class ConfigManager {
    private static final String PREFS_NAME = "wallpaper_plus_prefs";
    private static final String PREFS_FILE = "/data/user_de/0/com.wallpaperplus.module/shared_prefs/" + PREFS_NAME + ".xml";

    private static ConfigManager instance;
    private XSharedPreferences xPrefs;
    private Context context;

    // 功能开关 Key
    public static final String KEY_ENABLE_RASTER = "enable_raster";
    public static final String KEY_ENABLE_LYRIC = "enable_lyric";
    public static final String KEY_ENABLE_MOTION = "enable_motion";

    // 光栅壁纸参数
    public static final String KEY_RASTER_INTENSITY = "raster_intensity";
    public static final String KEY_LAYER_COUNT = "layer_count";
    public static final String KEY_PARALLAX_SCALE = "parallax_scale";
    public static final String KEY_RASTER_SPEED = "raster_speed";

    // 歌词参数
    public static final String KEY_LYRIC_POSITION = "lyric_position";
    public static final String KEY_LYRIC_SIZE = "lyric_size";
    public static final String KEY_LYRIC_COLOR = "lyric_color";
    public static final String KEY_LYRIC_STYLE = "lyric_style";
    public static final String KEY_LYRIC_APPS = "lyric_apps";

    // 3D晃动参数
    public static final String KEY_MOTION_SENSITIVITY = "motion_sensitivity";
    public static final String KEY_MOTION_SMOOTH = "motion_smooth";
    public static final String KEY_MOTION_DIRECTION = "motion_direction";

    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    public void init(Context ctx) {
        this.context = ctx;
        reload();
    }

    public void reload() {
        try {
            xPrefs = new XSharedPreferences("com.wallpaperplus.module", PREFS_NAME);
            xPrefs.makeWorldReadable();
        } catch (Exception e) {
            LogUtil.e("ConfigManager", "Failed to load XSharedPreferences", e);
        }
    }

    public boolean isRasterEnabled() {
        return getBoolean(KEY_ENABLE_RASTER, false);
    }

    public boolean isLyricEnabled() {
        return getBoolean(KEY_ENABLE_LYRIC, false);
    }

    public boolean isMotionEnabled() {
        return getBoolean(KEY_ENABLE_MOTION, false);
    }

    public float getRasterIntensity() {
        return getFloat(KEY_RASTER_INTENSITY, 0.5f);
    }

    public int getLayerCount() {
        return getInt(KEY_LAYER_COUNT, 3);
    }

    public float getParallaxScale() {
        return getFloat(KEY_PARALLAX_SCALE, 1.0f);
    }

    public float getRasterSpeed() {
        return getFloat(KEY_RASTER_SPEED, 1.0f);
    }

    public String getLyricPosition() {
        return getString(KEY_LYRIC_POSITION, "bottom");
    }

    public int getLyricSize() {
        return getInt(KEY_LYRIC_SIZE, 18);
    }

    public int getLyricColor() {
        return getInt(KEY_LYRIC_COLOR, 0xFFFFFFFF);
    }

    public String getLyricStyle() {
        return getString(KEY_LYRIC_STYLE, "single");
    }

    public float getMotionSensitivity() {
        return getFloat(KEY_MOTION_SENSITIVITY, 1.0f);
    }

    public float getMotionSmooth() {
        return getFloat(KEY_MOTION_SMOOTH, 0.8f);
    }

    public String getMotionDirection() {
        return getString(KEY_MOTION_DIRECTION, "both");
    }

    private boolean getBoolean(String key, boolean def) {
        if (xPrefs != null) {
            try {
                return xPrefs.getBoolean(key, def);
            } catch (Exception e) {
                LogUtil.e("ConfigManager", "getBoolean failed for " + key, e);
            }
        }
        return def;
    }

    private int getInt(String key, int def) {
        if (xPrefs != null) {
            try {
                return xPrefs.getInt(key, def);
            } catch (Exception e) {
                LogUtil.e("ConfigManager", "getInt failed for " + key, e);
            }
        }
        return def;
    }

    private float getFloat(String key, float def) {
        if (xPrefs != null) {
            try {
                return xPrefs.getFloat(key, def);
            } catch (Exception e) {
                LogUtil.e("ConfigManager", "getFloat failed for " + key, e);
            }
        }
        return def;
    }

    private String getString(String key, String def) {
        if (xPrefs != null) {
            try {
                return xPrefs.getString(key, def);
            } catch (Exception e) {
                LogUtil.e("ConfigManager", "getString failed for " + key, e);
            }
        }
        return def;
    }

    public String getHostPackage() {
        return "com.wallpaperplus.module";
    }
}
