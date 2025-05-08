package com.example.axel;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

public class SettingsActivity extends BaseActivity {

    private SharedPreferences sharedPreferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        setupNavigation();
        ImageView menuButton = findViewById(R.id.menu_button);

        menuButton.setOnClickListener(v -> {
            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            drawer.openDrawer(GravityCompat.START);
        });

        CheckBox keepScreenOnCheckBox = findViewById(R.id.keep_screen_on);
        keepScreenOnCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        });
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        CheckBox checkBox = findViewById(R.id.keep_screen_on);

        checkBox.setChecked(sharedPreferences.getBoolean("KeepScreenOn", false));
        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("KeepScreenOn", isChecked).apply();
        });
        Button checkAccelButton = findViewById(R.id.btn_check_accelerometer);
        checkAccelButton.setOnClickListener(v -> showAccelerometerInfo());
    }
    @Override
    protected int getLayoutId() {
        return R.layout.activity_saved_files;
    }
    @Override
    protected void initViews() {

    }

    private void showAccelerometerInfo() {
        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Информация об акселерометре");

        if (accelerometer == null) {
            builder.setMessage("⚠️ Акселерометр не обнаружен");
        } else {
            String info = "Модель: " + accelerometer.getName() + "\n"
                    + "Производитель: " + accelerometer.getVendor() + "\n"
                    + "Максимальный диапазон: " + accelerometer.getMaximumRange() + " m/s²\n";


            builder.setMessage(info);
        }

        builder.setPositiveButton("OK", null);
        AlertDialog dialog = builder.create();
        dialog.show();

        // Стилизация для темной темы
        TextView textView = dialog.findViewById(android.R.id.message);
        if (textView != null) {
            textView.setTextColor(ContextCompat.getColor(this,
                    (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                            == Configuration.UI_MODE_NIGHT_YES ? android.R.color.white : android.R.color.black));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
