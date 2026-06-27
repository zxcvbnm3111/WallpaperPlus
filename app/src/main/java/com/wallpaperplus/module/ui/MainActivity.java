package com.wallpaperplus.module.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.wallpaperplus.module.R;
import com.wallpaperplus.module.util.ConfigManager;

import de.robv.android.xposed.XposedBridge;

/**
 * WallpaperPlus 主界面
 * 提供功能入口和状态展示
 */
public class MainActivity extends AppCompatActivity {

    private CardView cardWallpaper, cardLyric, cardMotion, cardAbout;
    private TextView tvWallpaperStatus, tvLyricStatus, tvMotionStatus;
    private ImageView ivWallpaperIcon, ivLyricIcon, ivMotionIcon;
    private ConfigManager config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initListeners();
        updateStatus();

        // 检查 Xposed 是否激活
        checkXposed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    private void initViews() {
        cardWallpaper = findViewById(R.id.card_wallpaper);
        cardLyric = findViewById(R.id.card_lyric);
        cardMotion = findViewById(R.id.card_motion);
        cardAbout = findViewById(R.id.card_about);

        tvWallpaperStatus = findViewById(R.id.tv_wallpaper_status);
        tvLyricStatus = findViewById(R.id.tv_lyric_status);
        tvMotionStatus = findViewById(R.id.tv_motion_status);

        ivWallpaperIcon = findViewById(R.id.iv_wallpaper_icon);
        ivLyricIcon = findViewById(R.id.iv_lyric_icon);
        ivMotionIcon = findViewById(R.id.iv_motion_icon);
    }

    private void initListeners() {
        cardWallpaper.setOnClickListener(v -> {
            startActivity(new Intent(this, WallpaperSettingsActivity.class));
        });

        cardLyric.setOnClickListener(v -> {
            startActivity(new Intent(this, LyricSettingsActivity.class));
        });

        cardMotion.setOnClickListener(v -> {
            startActivity(new Intent(this, MotionSettingsActivity.class));
        });

        cardAbout.setOnClickListener(v -> {
            showAboutDialog();
        });
    }

    private void updateStatus() {
        config = ConfigManager.getInstance();

        // 更新各功能状态显示
        updateCardStatus(tvWallpaperStatus, ivWallpaperIcon, config.isRasterEnabled());
        updateCardStatus(tvLyricStatus, ivLyricIcon, config.isLyricEnabled());
        updateCardStatus(tvMotionStatus, ivMotionIcon, config.isMotionEnabled());
    }

    private void updateCardStatus(TextView statusView, ImageView iconView, boolean enabled) {
        if (enabled) {
            statusView.setText(R.string.status_enabled);
            statusView.setTextColor(getColor(R.color.success));
            iconView.setColorFilter(getColor(R.color.primary));
        } else {
            statusView.setText(R.string.status_disabled);
            statusView.setTextColor(getColor(R.color.text_secondary));
            iconView.setColorFilter(getColor(R.color.text_secondary));
        }
    }

    private void checkXposed() {
        boolean isXposedActive = false;
        try {
            // 检查 XposedBridge 类是否存在
            Class.forName("de.robv.android.xposed.XposedBridge");
            isXposedActive = true;
        } catch (ClassNotFoundException e) {
            isXposedActive = false;
        }

        // 二次检查：通过系统属性判断
        if (!isXposedActive) {
            try {
                String xpVersion = System.getProperty("xposed.version");
                if (xpVersion != null) {
                    isXposedActive = true;
                }
            } catch (Exception ignored) {}
        }

        if (!isXposedActive) {
            Toast.makeText(this, 
                "未检测到 Xposed 框架，模块功能将不会生效\n请在 LSPosed 中启用本模块", 
                Toast.LENGTH_LONG).show();
        }
    }

    private void showAboutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this, R.style.Theme_WallpaperPlus)
            .setTitle(R.string.app_name)
            .setMessage("版本: 1.0.0\n\n"
                + "OPlus / ColorOS 壁纸增强模块\n\n"
                + "功能:\n"
                + "• 光栅壁纸 - 多层视差效果\n"
                + "• 锁屏歌词 - 支持多音乐应用\n"
                + "• 3D壁纸晃动 - 陀螺仪驱动\n\n"
                + "使用说明:\n"
                + "1. 在 LSPosed 中启用模块\n"
                + "2. 勾选 SystemUI 和桌面作用域\n"
                + "3. 勾选需要歌词支持的音乐应用\n"
                + "4. 重启目标应用生效\n\n"
                + "注意事项:\n"
                + "• 需要 Xposed 环境\n"
                + "• 不保证所有系统版本兼容\n"
                + "• 功能不生效时请检查作用域和重启")
            .setPositiveButton("确定", null)
            .show();
    }
}
