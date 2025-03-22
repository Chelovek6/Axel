package com.example.axel;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import org.jtransforms.fft.FloatFFT_1D;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener, DataRecorder.DataListener {

    private LineChartView lineChartView;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private boolean isRecording = false;
    private TextView accelXText, accelYText, accelZText, accelTotalText;
    private DataRecorder dataRecorder;
    private String tempCsvFilePath;
    private FFTProcessor fftProcessor;

    private List<Float> rawBufferX = new ArrayList<>();
    private List<Float> rawBufferY = new ArrayList<>();
    private List<Float> rawBufferZ = new ArrayList<>();
    private int fftBufferSize;
    private List<Float> bufferX = new ArrayList<>();
    private List<Float> bufferY = new ArrayList<>();
    private List<Float> bufferZ = new ArrayList<>();
    private List<Float> bufferTotal = new ArrayList<>();
    private static final int BUFFER_SIZE = 3; // Размер буфера для усреднения
    private boolean isFFTEnabled = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        fftBufferSize = prefs.getInt("BufferSize", 256);
        IntentFilter filter = new IntentFilter("com.example.axel.RECORDING_STARTED");
        registerReceiver(recordingStartedReceiver, filter);

        lineChartView = findViewById(R.id.line_chart_view);
        accelXText = findViewById(R.id.accel_x);
        accelYText = findViewById(R.id.accel_y);
        accelZText = findViewById(R.id.accel_z);
        accelTotalText = findViewById(R.id.accel_total);

        fftBufferSize = prefs.getInt("BufferSize", 256);


        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        dataRecorder = new DataRecorder(this, this);

        ImageButton settingsButton = findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);

            SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
            boolean keepScreenOn = sharedPreferences.getBoolean("KeepScreenOn", false);

            if (keepScreenOn) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
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



    private void startRecording() {
        isRecording = true;

        // Запуск сервиса для фоновой записи
        Intent serviceIntent = new Intent(this, RecordingService.class);
        startService(serviceIntent);

        Toast.makeText(this, "Запись началась", Toast.LENGTH_SHORT).show();
    }

    private void stopRecording() {
        isRecording = false;

        // Остановка сервиса
        Intent serviceIntent = new Intent(this, RecordingService.class);
        stopService(serviceIntent);
        // Используем путь к файлу, полученный из сервиса
        if (tempCsvFilePath != null) {
            File tempCsvFile = new File(tempCsvFilePath);
            showSaveDialog(tempCsvFile);
        } else {
            Toast.makeText(this, "Ошибка: файл не найден", Toast.LENGTH_SHORT).show();
        }
        //showSaveDialog(dataRecorder.getTempCsvFile()); // Передаём файл в метод
    }

    private void showSaveDialog(File tempCsvFile) { // Добавьте параметр File
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Сохранить запись");

        String defaultFileName = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date());

        EditText input = new EditText(this);
        input.setText(defaultFileName);
        input.setSelection(0, defaultFileName.length());
        input.setHint("Введите название файла");
        builder.setView(input);

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String userFileName = input.getText().toString().trim();
            if (userFileName.isEmpty()) {
                userFileName = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date());
            }

            File finalFile = new File(getCacheDir(), userFileName + ".csv");

            if (tempCsvFile.renameTo(finalFile)) {
                shareSavedFile(finalFile);
            } else {
                Toast.makeText(this, "Ошибка сохранения", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void shareSavedFile(File file) {
        Uri fileUri = FileProvider.getUriForFile(this, "com.example.axel.provider", file);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/csv");
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Отправить файл через"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        isFFTEnabled = prefs.getBoolean("FFTFilter", false);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);


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
        sensorManager.unregisterListener(this);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // Добавляем данные в буферы
            bufferX.add(x);
            bufferY.add(y);
            bufferZ.add(z);
            float total = (float) Math.sqrt(x*x + y*y + z*z);
            bufferTotal.add(total);

            // Если буфер заполнен, обрабатываем данные
            if (bufferX.size() >= BUFFER_SIZE) {
                // Вычисляем средние значения
                float avgX = calculateAverage(bufferX);
                float avgY = calculateAverage(bufferY);
                float avgZ = calculateAverage(bufferZ);
                float avgTotal = calculateAverage(bufferTotal);

                // Преобразуем в g-единицы и обновляем график
                lineChartView.addRawData(
                        avgX / 9.8f,
                        avgY / 9.8f,
                        avgZ / 9.8f,
                        avgTotal / 9.8f
                );

                // Обновляем текстовые поля с усредненными значениями
                accelXText.setText(String.format("x=%.6f", avgX / 9.8f));
                accelYText.setText(String.format(" y=%.6f", avgY / 9.8f));
                accelZText.setText(String.format(" z=%.6f", avgZ / 9.8f));
                accelTotalText.setText(String.format(" ОУ=%.6f", avgTotal / 9.8f));

                // Очищаем буферы
                bufferX.clear();
                bufferY.clear();
                bufferZ.clear();
                bufferTotal.clear();
            }

            // Логирование сырых данных (для отладки)
            Log.d("AccelerometerData", String.format(Locale.getDefault(), "Raw: x=%.6f, y=%.6f, z=%.6f", x, y, z));
        }
    }
//    @Override
//    public void onSensorChanged(SensorEvent event) {
//        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
//            float x = event.values[0];
//            float y = event.values[1];
//            float z = event.values[2];
//
//            bufferX.add(x);
//            bufferY.add(y);
//            bufferZ.add(z);
//            float total = (float) Math.sqrt(x*x + y*y + z*z);
//            bufferTotal.add(total);
//
//            Log.d("AccelerometerData", String.format(Locale.getDefault(), "Raw: x=%.6f, y=%.6f, z=%.6f", x, y, z));
//
//            x /= 9.8f;
//            y /= 9.8f;
//            z /= 9.8f;
//
//
//
//            float totalAcceleration = (float) Math.sqrt(x * x + y * y + z * z);
//
//            lineChartView.addRawData(x, y, z, totalAcceleration);
//
//            accelXText.setText(String.format("x=%.6f", x));
//            accelYText.setText(String.format(" y=%.6f", y));
//            accelZText.setText(String.format(" z=%.6f", z));
//            accelTotalText.setText(String.format(" ОУ=%.6f", totalAcceleration));
//
//
//        }
//    }

    private float calculateAverage(List<Float> list) {
        float sum = 0;
        for (float num : list) {
            sum += num;
        }
        return sum / list.size();
    }

    private final BroadcastReceiver recordingStartedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.axel.RECORDING_STARTED".equals(intent.getAction())) {
                tempCsvFilePath = intent.getStringExtra("filePath"); // Получаем путь к файлу
            }
        }
    };

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onDataUpdated(float x, float y, float z, float totalAcceleration) {

        lineChartView.addRawData(x, y, z, totalAcceleration);
        accelXText.setText(String.format("x=%.6f", x));
        accelYText.setText(String.format(" y=%.6f", y));
        accelZText.setText(String.format(" z=%.6f", z));
        accelTotalText.setText(String.format(" ОУ=%.6f", totalAcceleration));
    }
}