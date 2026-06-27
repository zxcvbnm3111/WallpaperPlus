package com.wallpaperplus.module.ui;

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
 * 3D壁纸晃动设置界面
 */
public class MotionSettingsActivity extends AppCompatActivity {

    private Switch switchEnable;
    private SeekBar seekSensitivity, seekSmooth;
    private RadioGroup rgDirection;
    private TextView tvSensitivityVal, tvSmoothVal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_motion_settings);

        initViews();
        loadSettings();
        setupListeners();
    }

    private void initViews() {
        switchEnable = findViewById(R.id.switch_enable_motion);
        seekSensitivity = findViewById(R.id.seek_sensitivity);
        seekSmooth = findViewById(R.id.seek_smooth);
        rgDirection = findViewById(R.id.rg_direction);
        tvSensitivityVal = findViewById(R.id.tv_sensitivity_val);
        tvSmoothVal = findViewById(R.id.tv_smooth_val);
    }

    private void loadSettings() {
        ConfigManager config = ConfigManager.getInstance();

        switchEnable.setChecked(config.isMotionEnabled());

        seekSensitivity.setProgress((int) (config.getMotionSensitivity() * 100));
        tvSensitivityVal.setText(String.format("%.0f%%", config.getMotionSensitivity() * 100));

        seekSmooth.setProgress((int) (config.getMotionSmooth() * 100));
        tvSmoothVal.setText(String.format("%.0f%%", config.getMotionSmooth() * 100));

        String direction = config.getMotionDirection();
        switch (direction) {
            case "horizontal": rgDirection.check(R.id.rb_horizontal); break;
            case "vertical": rgDirection.check(R.id.rb_vertical); break;
            case "both": default: rgDirection.check(R.id.rb_both); break;
        }
    }

    private void setupListeners() {
        switchEnable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveBoolean(ConfigManager.KEY_ENABLE_MOTION, isChecked);
        });

        seekSensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float val = progress / 100f;
                tvSensitivityVal.setText(String.format("%.0f%%", val * 100));
                saveFloat(ConfigManager.KEY_MOTION_SENSITIVITY, val);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekSmooth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float val = progress / 100f;
                tvSmoothVal.setText(String.format("%.0f%%", val * 100));
                saveFloat(ConfigManager.KEY_MOTION_SMOOTH, val);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        rgDirection.setOnCheckedChangeListener((group, checkedId) -> {
            String direction = "both";
            if (checkedId == R.id.rb_horizontal) direction = "horizontal";
            else if (checkedId == R.id.rb_vertical) direction = "vertical";
            saveString(ConfigManager.KEY_MOTION_DIRECTION, direction);
        });

        findViewById(R.id.btn_save).setOnClickListener(v -> {
            Toast.makeText(this, "设置已保存，请重启桌面生效", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void saveBoolean(String key, boolean value) {
        getSharedPreferences("wallpaper_plus_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean(key, value).apply();
        makePrefsReadable();
    }

    private void saveFloat(String key, float value) {
        getSharedPreferences("wallpaper_plus_prefs", Context.MODE_PRIVATE)
            .edit().putFloat(key, value).apply();
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
