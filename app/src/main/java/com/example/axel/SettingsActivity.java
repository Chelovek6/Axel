package com.example.axel;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import java.util.Arrays;

public class SettingsActivity extends BaseActivity {

    private SharedPreferences sharedPreferences;
    private Spinner frequencySpinner;
    private SensorDataManager sensorDataManager;
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
        setupFrequencySpinner();
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

    }

    private void setupFrequencySpinner() {
        frequencySpinner = findViewById(R.id.spinner_accelerometer_frequency);

        // Используем кастомный адаптер с белым текстом
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.accelerometer_frequency_entries,
                R.layout.spinner_item_white // Создайте этот файл макета
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        frequencySpinner.setAdapter(adapter);

        // Сопоставление значений с константами SensorManager
        int[] delayValues = {
                SensorManager.SENSOR_DELAY_FASTEST,
                SensorManager.SENSOR_DELAY_GAME,
                SensorManager.SENSOR_DELAY_UI,
                SensorManager.SENSOR_DELAY_NORMAL
        };

        // Загрузка сохраненного значения
        int savedDelay = sharedPreferences.getInt("accelerometer_delay", SensorManager.SENSOR_DELAY_FASTEST);
        int position = Arrays.binarySearch(delayValues, savedDelay);
        frequencySpinner.setSelection(position >= 0 ? position : 0);
        int[] values = getResources().getIntArray(R.array.accelerometer_frequency_values);
        for (int i = 0; i < values.length; i++) {
            if (values[i] == savedDelay) {
                frequencySpinner.setSelection(i);
                break;
            }
        }
        frequencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int selectedDelay = delayValues[position];
                sharedPreferences.edit()
                        .putInt("accelerometer_delay", selectedDelay)
                        .apply();

                // Важно: Перезапустить сенсор с новыми настройками
                restartSensorService();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }
    private void restartSensorService() {
        if (sensorDataManager != null) {
            sensorDataManager.unregisterListener();
            sensorDataManager.registerListener();
        }
    }
    private void setSpinnerSelection(int delayValue) {
        int[] values = getResources().getIntArray(R.array.accelerometer_frequency_values);
        for (int i = 0; i < values.length; i++) {
            if (values[i] == delayValue) {
                frequencySpinner.setSelection(i);
                break;
            }
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
