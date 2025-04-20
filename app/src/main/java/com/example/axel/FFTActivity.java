package com.example.axel;

import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.github.mikephil.charting.data.Entry;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FFTActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private FFTProcessor fftProcessor;
    private static final int FFT_SIZE = 1024;
    private static final int AVERAGE_BUFFER_SIZE = 3; // Размер буфера для усреднения
    private final List<Float> averagedBufferX = new ArrayList<>();
    private final List<Float> averagedBufferY = new ArrayList<>();
    private final List<Float> averagedBufferZ = new ArrayList<>();
    private DataRecorder dataRecorder;
    private boolean isRecording = false;

    private TextView samplingRateText;
    private long previousTimestamp = 0;
    private final ArrayList<Float> samplingRates = new ArrayList<>();
    private static final int SAMPLING_RATE_BUFFER_SIZE = 10;

    // Графики для сырых данных
    private LineChart rawXChart, rawYChart, rawZChart;
    // Графики для FFT
    private LineChart fftXChart, fftYChart, fftZChart;

    // Буферы данных
    private final List<Float> bufferX = new ArrayList<>();
    private final List<Float> bufferY = new ArrayList<>();
    private final List<Float> bufferZ = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fft);

        // Инициализация графиков
        rawXChart = findViewById(R.id.raw_x_chart);
        rawYChart = findViewById(R.id.raw_y_chart);
        rawZChart = findViewById(R.id.raw_z_chart);
        fftXChart = findViewById(R.id.fft_x_chart);
        fftYChart = findViewById(R.id.fft_y_chart);
        fftZChart = findViewById(R.id.fft_z_chart);

        samplingRateText = findViewById(R.id.sampling_rate_text);

        dataRecorder = new DataRecorder(this, null);
        Button recordButton = findViewById(R.id.record_button);
        recordButton.setOnClickListener(v -> toggleRecording());
        // Кнопка возврата
        Button backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());

        // Настройка FFT
        fftProcessor = new FFTProcessor(FFT_SIZE);

        // Инициализация сенсора
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            long currentTimestamp = event.timestamp;
            if (previousTimestamp != 0) {
                float intervalNs = currentTimestamp - previousTimestamp;
                float frequencyHz = 1_000_000_000f / intervalNs;

                samplingRates.add(frequencyHz);
                if (samplingRates.size() > SAMPLING_RATE_BUFFER_SIZE) {
                    samplingRates.remove(0);
                }

                runOnUiThread(() -> {
                    float avg = calculateAverageFloat(samplingRates);
                    samplingRateText.setText(String.format(Locale.US, "Частота: %.1f Гц", avg));
                });
            }
            previousTimestamp = currentTimestamp;

            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // Обновляем буферы для FFT
            updateFFTBuffers(x, y, z);

            // Обновляем буферы для усреднения
            updateAverageBuffers(x, y, z);

            // Обновляем графики усредненных данных
            updateRawChartsWithAveragedData();

            // Обработка FFT
            if (bufferX.size() >= FFT_SIZE) {
                processFFT();
            }
        }
    }

    private void toggleRecording() {
        if (isRecording) {
            stopRecording();
        } else {
            startRecording();
        }
        isRecording = !isRecording;
        ((Button) findViewById(R.id.record_button)).setText(isRecording ? "■" : "▶");
    }

    private void startRecording() {
        dataRecorder.startRecording(true);
        Intent serviceIntent = new Intent(this, RecordingService.class);
        serviceIntent.putExtra("isFFT", true);
        startService(serviceIntent);
    }

    private void stopRecording() {
        dataRecorder.stopRecording();
        File tempFile = dataRecorder.getTempCsvFile();
        if (tempFile != null) {
            FileUtils.showSaveDialog(this, tempFile, true);
        }
        stopService(new Intent(this, RecordingService.class));
    }
    private float calculateAverageFloat(List<Float> list) {
        float sum = 0;
        for (float val : list) sum += val;
        return sum / list.size();
    }

    private void updateFFTBuffers(float x, float y, float z) {
        bufferX.add(x);
        bufferY.add(y);
        bufferZ.add(z);

        // Ограничиваем размер буферов для FFT
        if (bufferX.size() > FFT_SIZE) {
            bufferX.remove(0);
            bufferY.remove(0);
            bufferZ.remove(0);
        }
    }

    private void updateAverageBuffers(float x, float y, float z) {
        averagedBufferX.add(x);
        averagedBufferY.add(y);
        averagedBufferZ.add(z);

        if (averagedBufferX.size() > AVERAGE_BUFFER_SIZE) {
            averagedBufferX.remove(0);
            averagedBufferY.remove(0);
            averagedBufferZ.remove(0);
        }
    }


    private void processFFT() {
        // Получаем среднюю частоту дискретизации
        float avgFrequency = calculateAverageFloat(samplingRates);

        // Вычисляем FFT с учетом частоты
        float[] fftX = fftProcessor.computeFFT(toFloatArray(bufferX), avgFrequency);
        float[] fftY = fftProcessor.computeFFT(toFloatArray(bufferY), avgFrequency);
        float[] fftZ = fftProcessor.computeFFT(toFloatArray(bufferZ), avgFrequency);

        // Получаем массив частот для оси X
        float[] frequencies = fftProcessor.getFrequencies(avgFrequency);

        // Обновляем графики
        updateFFTChart(fftXChart, frequencies, fftX, Color.BLUE, "FFT X");
        updateFFTChart(fftYChart, frequencies, fftY, Color.RED, "FFT Y");
        updateFFTChart(fftZChart, frequencies, fftZ, Color.GREEN, "FFT Z");

        float avgX = calculateAverage(bufferX) / 9.8f;
        float avgY = calculateAverage(bufferY) / 9.8f;
        float avgZ = calculateAverage(bufferZ) / 9.8f;
        float xfft = getMaxAmplitude(fftX);
        float yfft = getMaxAmplitude(fftY);
        float zfft = getMaxAmplitude(fftZ);
        dataRecorder.writeFFTData(avgX, avgY, avgZ, xfft, yfft, zfft);
    }

    private float getMaxAmplitude(float[] data) {
        float max = 0;
        for (float val : data) if (val > max) max = val;
        return max;
    }

    private void updateRawChartsWithAveragedData() {
        if (averagedBufferX.size() >= AVERAGE_BUFFER_SIZE) {
            float avgX = calculateAverage(averagedBufferX) / 9.8f;
            float avgY = calculateAverage(averagedBufferY) / 9.8f;
            float avgZ = calculateAverage(averagedBufferZ) / 9.8f;

            updateChart(rawXChart, avgX, Color.BLUE, "Raw X");
            updateChart(rawYChart, avgY, Color.RED, "Raw Y");
            updateChart(rawZChart, avgZ, Color.GREEN, "Raw Z");
        }
    }

    private float calculateAverage(List<Float> list) {
        float sum = 0;
        for (float num : list) {
            sum += num;
        }
        return sum / list.size();
    }

    private void updateChart(LineChart chart, float value, int color, String label) {
        LineData data = chart.getData();
        if (data == null) {
            data = new LineData();
            chart.setData(data);
        }

        LineDataSet set = (LineDataSet) data.getDataSetByLabel(label, true);
        if (set == null) {
            set = new LineDataSet(new ArrayList<>(), label);
            set.setColor(color);
            set.setDrawCircles(false);
            data.addDataSet(set);
        }

        data.addEntry(new Entry(set.getEntryCount(), value), data.getIndexOfDataSet(set));
        data.notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.setVisibleXRangeMaximum(100);
        chart.moveViewToX(data.getEntryCount());
    }

    private void updateFFTChart(LineChart chart, float[] frequencies, float[] magnitudes, int color, String label) {
        ArrayList<Entry> entries = new ArrayList<>();
        for (int i = 0; i < magnitudes.length; i++) {
            entries.add(new Entry(frequencies[i], magnitudes[i])); // X - частота, Y - амплитуда
        }

        LineDataSet set = new LineDataSet(entries, label);
        set.setColor(color);
        set.setDrawCircles(false);

        chart.setData(new LineData(set));
        chart.invalidate();
    }

    private float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i);
        }
        return array;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
