package com.wallpaperplus.module.ui;

import android.graphics.Color;
import android.content.Context;
import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.wallpaperplus.module.R;
import com.wallpaperplus.module.util.ConfigManager;
import java.io.File;

/**
 * 锁屏歌词设置界面
 */
public class LyricSettingsActivity extends AppCompatActivity {

    private Switch switchEnable;
    private RadioGroup rgPosition, rgStyle;
    private SeekBar seekSize;
    private TextView tvSizeVal, tvColorPreview;
    private int selectedColor = Color.WHITE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lyric_settings);

        initViews();
        loadSettings();
        setupListeners();
    }

    private void initViews() {
        switchEnable = findViewById(R.id.switch_enable_lyric);
        rgPosition = findViewById(R.id.rg_position);
        rgStyle = findViewById(R.id.rg_style);
        seekSize = findViewById(R.id.seek_size);
        tvSizeVal = findViewById(R.id.tv_size_val);
        tvColorPreview = findViewById(R.id.tv_color_preview);
    }

    private void loadSettings() {
        ConfigManager config = ConfigManager.getInstance();

        switchEnable.setChecked(config.isLyricEnabled());

        String position = config.getLyricPosition();
        switch (position) {
            case "top": rgPosition.check(R.id.rb_top); break;
            case "center": rgPosition.check(R.id.rb_center); break;
            case "bottom": default: rgPosition.check(R.id.rb_bottom); break;
        }

        String style = config.getLyricStyle();
        switch (style) {
            case "single": rgStyle.check(R.id.rb_single); break;
            case "dual": rgStyle.check(R.id.rb_dual); break;
            case "karaoke": rgStyle.check(R.id.rb_karaoke); break;
        }

        seekSize.setProgress(config.getLyricSize());
        tvSizeVal.setText(config.getLyricSize() + "sp");

        selectedColor = config.getLyricColor();
        tvColorPreview.setBackgroundColor(selectedColor);
    }

    private void setupListeners() {
        switchEnable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveBoolean(ConfigManager.KEY_ENABLE_LYRIC, isChecked);
        });

        rgPosition.setOnCheckedChangeListener((group, checkedId) -> {
            String position = "bottom";
            if (checkedId == R.id.rb_top) position = "top";
            else if (checkedId == R.id.rb_center) position = "center";
            saveString(ConfigManager.KEY_LYRIC_POSITION, position);
        });

        rgStyle.setOnCheckedChangeListener((group, checkedId) -> {
            String style = "single";
            if (checkedId == R.id.rb_dual) style = "dual";
            else if (checkedId == R.id.rb_karaoke) style = "karaoke";
            saveString(ConfigManager.KEY_LYRIC_STYLE, style);
        });

        seekSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int val = Math.max(12, progress);
                tvSizeVal.setText(val + "sp");
                saveInt(ConfigManager.KEY_LYRIC_SIZE, val);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        tvColorPreview.setOnClickListener(v -> {
            showColorPicker();
        });

        findViewById(R.id.btn_save).setOnClickListener(v -> {
            Toast.makeText(this, "设置已保存，请重启 SystemUI 生效", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void showColorPicker() {
        // 简化的颜色选择，实际可用第三方库
        int[] colors = {Color.WHITE, Color.BLACK, Color.RED, Color.GREEN, Color.BLUE, 
                        Color.YELLOW, Color.CYAN, Color.MAGENTA, 0xFFFF6B6B, 0xFF48DBFB};
        String[] colorNames = {"白色", "黑色", "红色", "绿色", "蓝色", "黄色", "青色", "紫色", "珊瑚红", "天蓝"};

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("选择歌词颜色")
            .setItems(colorNames, (dialog, which) -> {
                selectedColor = colors[which];
                tvColorPreview.setBackgroundColor(selectedColor);
                saveInt(ConfigManager.KEY_LYRIC_COLOR, selectedColor);
            })
            .show();
    }

    private void saveBoolean(String key, boolean value) {
        getSharedPreferences("wallpaper_plus_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean(key, value).apply();
        makePrefsReadable();
    }

    private void saveInt(String key, int value) {
        getSharedPreferences("wallpaper_plus_prefs", Context.MODE_PRIVATE)
            .edit().putInt(key, value).apply();
        makePrefsReadable();
    }

    private void saveString(String key, String value) {
        getSharedPreferences("wallpaper_plus_prefs", Context.MODE_PRIVATE)
            .edit().putString(key, value).apply();
        makePrefsReadable();
    }

    private void makePrefsReadable() {
        try {
            File prefsFile = new File(getApplicationInfo().dataDir, "shared_prefs/wallpaper_plus_prefs.xml");
            if (prefsFile.exists()) {
                prefsFile.setReadable(true, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
