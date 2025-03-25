package com.example.axel;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.Button;
import com.github.mikephil.charting.data.Entry;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

public class FFTActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private FFTProcessor fftProcessor;
    private static final int FFT_SIZE = 1024;

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
            float x = event.values[0] / 9.8f;
            float y = event.values[1] / 9.8f;
            float z = event.values[2] / 9.8f;

            updateBuffers(x, y, z);
            updateRawCharts(x, y, z);

            if (bufferX.size() >= FFT_SIZE) {
                processFFT();
            }
        }
    }

    private void updateBuffers(float x, float y, float z) {
        bufferX.add(x);
        bufferY.add(y);
        bufferZ.add(z);

        if (bufferX.size() > FFT_SIZE) {
            bufferX.remove(0);
            bufferY.remove(0);
            bufferZ.remove(0);
        }
    }

    private void processFFT() {
        float[] fftX = fftProcessor.computeFFT(toFloatArray(bufferX));
        float[] fftY = fftProcessor.computeFFT(toFloatArray(bufferY));
        float[] fftZ = fftProcessor.computeFFT(toFloatArray(bufferZ));

        updateFFTChart(fftXChart, fftX, Color.BLUE, "FFT X");
        updateFFTChart(fftYChart, fftY, Color.RED, "FFT Y");
        updateFFTChart(fftZChart, fftZ, Color.GREEN, "FFT Z");
    }

    private void updateRawCharts(float x, float y, float z) {
        updateChart(rawXChart, x, Color.BLUE, "Raw X");
        updateChart(rawYChart, y, Color.RED, "Raw Y");
        updateChart(rawZChart, z, Color.GREEN, "Raw Z");
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

    private void updateFFTChart(LineChart chart, float[] fftData, int color, String label) {
        ArrayList<Entry> entries = new ArrayList<>();
        for (int i = 0; i < fftData.length; i++) {
            entries.add(new Entry(i, fftData[i]));
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
