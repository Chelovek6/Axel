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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import org.jtransforms.fft.FloatFFT_1D;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener, DataRecorder.DataListener {

    private LineChartView lineChartView;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private boolean isRecording = false;
    private TextView accelXText, accelYText, accelZText, accelTotalText;
    private DataRecorder dataRecorder;
    private String tempCsvFilePath;
    //for fft
    private static final int FFT_BUFFER_SIZE = 256;
    private float[] fftBuffer = new float[FFT_BUFFER_SIZE];
    private int bufferIndex = 0;
    private FloatFFT_1D fft;
    private float[] window;
    private boolean isFFTEnabled = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IntentFilter filter = new IntentFilter("com.example.axel.RECORDING_STARTED");
        registerReceiver(recordingStartedReceiver, filter);

        lineChartView = findViewById(R.id.line_chart_view);
        accelXText = findViewById(R.id.accel_x);
        accelYText = findViewById(R.id.accel_y);
        accelZText = findViewById(R.id.accel_z);
        accelTotalText = findViewById(R.id.accel_total);

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
        fft = new FloatFFT_1D(FFT_BUFFER_SIZE);
        initWindow();
    }
    private void initWindow() {
        window = new float[FFT_BUFFER_SIZE];
        for (int i = 0; i < FFT_BUFFER_SIZE; i++) {
            window[i] = (float) (0.5 * (1 - Math.cos(2 * Math.PI * i / (FFT_BUFFER_SIZE - 1))));
        }
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

            Log.d("AccelerometerData", String.format(Locale.getDefault(), "Raw: x=%.6f, y=%.6f, z=%.6f", x, y, z));

            x /= 9.8f;
            y /= 9.8f;
            z /= 9.8f;

            float totalAcceleration = (float) Math.sqrt(x * x + y * y + z * z);

            lineChartView.addDataPoint(x, y, z, totalAcceleration);

            accelXText.setText(String.format("x=%.6f", x));
            accelYText.setText(String.format(" y=%.6f", y));
            accelZText.setText(String.format(" z=%.6f", z));
            accelTotalText.setText(String.format(" ОУ=%.6f", totalAcceleration));

            if (isFFTEnabled) {
                fftBuffer[bufferIndex++] = totalAcceleration;
                if (bufferIndex >= FFT_BUFFER_SIZE) {
                    applyFFT();
                    bufferIndex = 0;
                }
            } else {

                dataRecorder.writeData(x, y, z, totalAcceleration);
            }
        }
    }
    private void applyFFT() {
        // 1. Применяем окно
        for (int i = 0; i < FFT_BUFFER_SIZE; i++) {
            fftBuffer[i] *= window[i];
        }

        // 2. FFT преобразование
        fft.realForward(fftBuffer);

        // 3. Обнуляем высокие частоты (пример: 5 Гц)
        int cutoffBin = (int) (5 * FFT_BUFFER_SIZE / 50); // 50 Гц - частота сенсора
        for (int i = cutoffBin; i < FFT_BUFFER_SIZE/2; i++) {
            fftBuffer[2*i] = 0;   // Real
            fftBuffer[2*i + 1] = 0; // Imaginary
        }

        // 4. Обратное преобразование
        fft.realInverse(fftBuffer, true);

        // 5. Обновляем график
        for (float value : fftBuffer) {
            lineChartView.addDataPoint(0, 0, 0, value); // ОУ
        }

        // После фильтрации:
        for (float value : fftBuffer) {

            dataRecorder.writeData(0, 0, 0, value);
        }
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

        lineChartView.addDataPoint(x, y, z, totalAcceleration);
        accelXText.setText(String.format("x=%.6f", x));
        accelYText.setText(String.format(" y=%.6f", y));
        accelZText.setText(String.format(" z=%.6f", z));
        accelTotalText.setText(String.format(" ОУ=%.6f", totalAcceleration));
    }
}