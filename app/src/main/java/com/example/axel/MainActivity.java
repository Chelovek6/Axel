package com.example.axel;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import android.widget.EditText;
import java.util.Date;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private LineChartView lineChartView;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private boolean isRecording = false;
    private FileWriter csvWriter;
    private File tempCsvFile;

    private TextView accelXText, accelYText, accelZText, accelTotalText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        lineChartView = findViewById(R.id.line_chart_view);
        accelXText = findViewById(R.id.accel_x);
        accelYText = findViewById(R.id.accel_y);
        accelZText = findViewById(R.id.accel_z);
        accelTotalText = findViewById(R.id.accel_total);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
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
                recordButton.setText("▶"); // Треугольник
            } else {
                startRecording();
                recordButton.setText("■"); // Квадрат
            }
        });
    }

    private void startRecording() {
        isRecording = true;

        String fileName = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date()) + ".csv";
        tempCsvFile = new File(getCacheDir(), fileName); // Создаем временный файл

        try {
            csvWriter = new FileWriter(tempCsvFile);
            csvWriter.append("time;x;y;z;totalAcceleration\n"); // Заголовки
        } catch (IOException e) {
            e.printStackTrace();
        }

        Toast.makeText(this, "Запись началась", Toast.LENGTH_SHORT).show();
    }

    private void stopRecording() {
        isRecording = false;

        try {
            if (csvWriter != null) {
                csvWriter.flush();
                csvWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        showSaveDialog();
    }





    private void saveToFile(String fileName, List<String> data) {
        try {

            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            if (!downloadDir.exists()) {
                downloadDir.mkdirs();
            }

            File file = new File(downloadDir, fileName + ".csv");
            try (FileWriter writer = new FileWriter(file)) {
                for (String line : data) {
                    writer.write(line + "\n");
                }
            }


            Toast.makeText(this, "Файл успешно сохранён в " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Ошибка при сохранении файла: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showSaveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Сохранить запись");

        EditText input = new EditText(this);
        input.setHint("Введите название файла");
        builder.setView(input);

        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String userFileName = input.getText().toString().trim();
            if (userFileName.isEmpty()) {
                userFileName = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date());
            }
            File finalFile = new File(getCacheDir(), userFileName + ".csv");
            shareSavedFile(finalFile);

            if (tempCsvFile.renameTo(finalFile)) {
                shareSavedFile(finalFile);
            } else {
                Toast.makeText(this, "Ошибка сохранения", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Отмена", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void shareFile(File file) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_STREAM, android.net.Uri.fromFile(file));
        startActivity(Intent.createChooser(intent, "Отправить файл"));
    }

    private void shareSavedFile(File file) {
        Uri fileUri = FileProvider.getUriForFile(this, "com.example.axel.provider", file);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/csv");
        shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Открываем стандартное окно выбора приложения
        startActivity(Intent.createChooser(shareIntent, "Отправить файл через"));
    }


    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }


    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            float x = event.values[0] / 9.8f;
            float y = event.values[1] / 9.8f;
            float z = event.values[2] / 9.8f;


            float totalAcceleration = (float) Math.sqrt(x * x + y * y + z * z);


            lineChartView.addDataPoint(x, y, z, totalAcceleration);


            accelXText.setText(String.format("x=%.2f", x));
            accelYText.setText(String.format(" y=%.2f", y));
            accelZText.setText(String.format(" z=%.2f", z));
            accelTotalText.setText(String.format(" ОУ=%.2f", totalAcceleration));
            double currentTime = System.currentTimeMillis() / 1000.0;


            writeDataToCsv(currentTime, x, y, z, totalAcceleration);
        }
    }
    private void writeDataToCsv(double time, float x, float y, float z, float totalAcceleration) {
        if (csvWriter != null) {
            try {
                csvWriter.append(String.format(Locale.getDefault(), "%.3f;%.2f;%.2f;%.2f;%.2f\n",
                        time, x, y, z, totalAcceleration));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

}