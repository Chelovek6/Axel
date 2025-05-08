package com.example.axel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.io.File;

public class MainActivity extends BaseActivity {
    private LineChartView lineChartView;
    private boolean isRecording = false;
    private TextView accelXText, accelYText, accelZText, accelTotalText;
    private DataRecorder dataRecorder;
    private String tempCsvFilePath;
    private SensorDataManager sensorDataManager;
    private int fftBufferSize;
    private static final int BUFFER_SIZE = 3; // Размер буфера для усреднения
    private TextView samplingRateText;
    private boolean isFFTEnabled = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        fftBufferSize = prefs.getInt("BufferSize", 256);
        IntentFilter filter = new IntentFilter("com.example.axel.RECORDING_STARTED");
        registerReceiver(recordingStartedReceiver, filter);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        ImageView menuButton = findViewById(R.id.menu_button);
        ImageButton scheduleButton = findViewById(R.id.schedule_button);
        lineChartView = findViewById(R.id.line_chart_view);
        accelXText = findViewById(R.id.accel_x);
        accelYText = findViewById(R.id.accel_y);
        accelZText = findViewById(R.id.accel_z);
        accelTotalText = findViewById(R.id.accel_total);
        //Частота
        samplingRateText = findViewById(R.id.sampling_rate_text);
        //Частота
        fftBufferSize = prefs.getInt("BufferSize", 256);

        ImageButton btnSchedule = findViewById(R.id.schedule_button);
        btnSchedule.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ScheduleActivity.class);
            intent.putExtra("type", "main");
            startActivity(intent);
        });
        sensorDataManager = new SensorDataManager(this, new SensorDataManager.SensorDataListener() {
            @Override
            public void onSensorDataUpdated(float x, float y, float z, float total) {
                // Переносим всю логику сюда
                lineChartView.addRawData(x, y, z, total);
                accelXText.setText(String.format("x=%.6f", x));
                accelYText.setText(String.format(" y=%.6f", y));
                accelZText.setText(String.format(" z=%.6f", z));
                accelTotalText.setText(String.format(" Общее ускорение=%.6f", total));

                // Если нужно сохранять данные
                if(isRecording) {
                    dataRecorder.writeDataToCsv(x, y, z, total);
                }
            }
            @Override
            public void onSamplingRateUpdated(float samplingRate) {
                runOnUiThread(() ->
                        samplingRateText.setText(String.format("Частота: %.1f Гц", samplingRate)));
            }
            public void onFFTDataProcessed(float fftX, float fftY, float fftZ) {
                // Пустая реализация, если не используется
            }
            @Override
            public void onFFTDataProcessed(float[] fftX, float[] fftY, float[] fftZ) {
            }
        }, BUFFER_SIZE);
        setupNavigation();
        dataRecorder = new DataRecorder(this);

        menuButton.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        scheduleButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, ScheduleActivity.class);
            intent.putExtra("type", "main");
            startActivity(intent);
        });



        Button recordButton = findViewById(R.id.record_button);
        recordButton.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
                recordButton.setText("▶");
            } else {
                startRecording();
                recordButton.setText("■");
            }
        });



    }
    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }
    @Override
    protected void initViews() {
        // Инициализация всех View-компонентов
        ImageView menuButton = findViewById(R.id.menu_button);
        menuButton.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
    }

    private void startRecording() {
        isRecording = true;

        // Запуск сервиса для фоновой записи
        Intent serviceIntent = new Intent(this, RecordingService.class);
        startService(serviceIntent);

        Toast.makeText(this, "Запись началась", Toast.LENGTH_SHORT).show();
    }

    private void stopRecording() {
        isRecording = false;
        Intent serviceIntent = new Intent(this, RecordingService.class);
        stopService(serviceIntent);

        if (tempCsvFilePath != null) {
            File tempCsvFile = new File(tempCsvFilePath);
            FileUtils.showSaveDialog(MainActivity.this, tempCsvFile,false); // Вызов из FileUtils
        } else {
            Toast.makeText(this, "Ошибка: файл не найден", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        isFFTEnabled = prefs.getBoolean("FFTFilter", false);
        sensorDataManager.registerListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Отменяем регистрацию BroadcastReceiver
        unregisterReceiver(recordingStartedReceiver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorDataManager.unregisterListener();
    }
    //Частота
    private final BroadcastReceiver recordingStartedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.axel.RECORDING_STARTED".equals(intent.getAction())) {
                tempCsvFilePath = intent.getStringExtra("filePath"); // Получаем путь к файлу
            }
        }
    };
}