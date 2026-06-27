package com.wallpaperplus.module.ui;

import android.content.Context;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.wallpaperplus.module.R;
import com.wallpaperplus.module.util.ConfigManager;
import java.io.File;

/**
 * 光栅壁纸设置界面
 */
public class WallpaperSettingsActivity extends AppCompatActivity {

    private Switch switchEnable;
    private SeekBar seekIntensity, seekLayers, seekScale, seekSpeed;
    private TextView tvIntensityVal, tvLayersVal, tvScaleVal, tvSpeedVal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallpaper_settings);

        initViews();
        loadSettings();
        setupListeners();
    }

    private void initViews() {
        switchEnable = findViewById(R.id.switch_enable_raster);
        seekIntensity = findViewById(R.id.seek_intensity);
        seekLayers = findViewById(R.id.seek_layers);
        seekScale = findViewById(R.id.seek_scale);
        seekSpeed = findViewById(R.id.seek_speed);

        tvIntensityVal = findViewById(R.id.tv_intensity_val);
        tvLayersVal = findViewById(R.id.tv_layers_val);
        tvScaleVal = findViewById(R.id.tv_scale_val);
        tvSpeedVal = findViewById(R.id.tv_speed_val);
    }

    private void loadSettings() {
        ConfigManager config = ConfigManager.getInstance();

        switchEnable.setChecked(config.isRasterEnabled());

        seekIntensity.setProgress((int) (config.getRasterIntensity() * 100));
        tvIntensityVal.setText(String.format("%.0f%%", config.getRasterIntensity() * 100));

        seekLayers.setProgress(config.getLayerCount());
        tvLayersVal.setText(String.valueOf(config.getLayerCount()));

        seekScale.setProgress((int) (config.getParallaxScale() * 50));
        tvScaleVal.setText(String.format("%.1fx", config.getParallaxScale()));

        seekSpeed.setProgress((int) (config.getRasterSpeed() * 50));
        tvSpeedVal.setText(String.format("%.1fx", config.getRasterSpeed()));
    }

    private void setupListeners() {
        switchEnable.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveBoolean(ConfigManager.KEY_ENABLE_RASTER, isChecked);
        });

        seekIntensity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float val = progress / 100f;
                tvIntensityVal.setText(String.format("%.0f%%", val * 100));
                saveFloat(ConfigManager.KEY_RASTER_INTENSITY, val);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekLayers.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int val = Math.max(2, progress);
                tvLayersVal.setText(String.valueOf(val));
                saveInt(ConfigManager.KEY_LAYER_COUNT, val);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekScale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float val = progress / 50f;
                tvScaleVal.setText(String.format("%.1fx", val));
                saveFloat(ConfigManager.KEY_PARALLAX_SCALE, val);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float val = progress / 50f;
                tvSpeedVal.setText(String.format("%.1fx", val));
                saveFloat(ConfigManager.KEY_RASTER_SPEED, val);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        findViewById(R.id.btn_save).setOnClickListener(v -> {
            Toast.makeText(this, "设置已保存，请重启桌面生效", Toast.LENGTH_SHORT).show();
            finish();
        });

        findViewById(R.id.btn_reset).setOnClickListener(v -> {
            seekIntensity.setProgress(50);
            seekLayers.setProgress(3);
            seekScale.setProgress(50);
            seekSpeed.setProgress(50);
            switchEnable.setChecked(false);
            Toast.makeText(this, "已恢复默认设置", Toast.LENGTH_SHORT).show();
        });
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

    private void saveFloat(String key, float value) {
        getSharedPreferences("wallpaper_plus_prefs", Context.MODE_PRIVATE)
            .edit().putFloat(key, value).apply();
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
