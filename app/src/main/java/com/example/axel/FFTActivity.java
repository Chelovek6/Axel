package com.example.axel;

import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.github.mikephil.charting.data.Entry;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

public class FFTActivity extends BaseActivity {

    private DataRecorder dataRecorder;
    private boolean isRecording = false;
    private SensorDataManager sensorDataManager;
    private TextView samplingRateText;
    // Графики для сырых данных
    private LineChart rawXChart, rawYChart, rawZChart;
    // Графики для FFT
    private LineChart fftXChart, fftYChart, fftZChart;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fft);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        setupNavigation();
        // Инициализация графиков
        rawXChart = findViewById(R.id.raw_x_chart);
        rawYChart = findViewById(R.id.raw_y_chart);
        rawZChart = findViewById(R.id.raw_z_chart);
        fftXChart = findViewById(R.id.fft_x_chart);
        fftYChart = findViewById(R.id.fft_y_chart);
        fftZChart = findViewById(R.id.fft_z_chart);

        samplingRateText = findViewById(R.id.sampling_rate_text);
        ImageButton btnSchedule = findViewById(R.id.schedule_button);
        btnSchedule.setOnClickListener(v -> {
            Intent intent = new Intent(FFTActivity.this, ScheduleActivity.class);
            intent.putExtra("type", "fft");
            startActivity(intent);
        });
        dataRecorder = new DataRecorder(this);
        Button recordButton = findViewById(R.id.record_button);
        recordButton.setOnClickListener(v -> toggleRecording());
        ImageView menuButton = findViewById(R.id.menu_button);
        menuButton.setOnClickListener(v -> {
            DrawerLayout drawer = findViewById(R.id.drawer_layout);
            drawer.openDrawer(GravityCompat.START);
        });

        // Инициализация сенсора
        sensorDataManager = new SensorDataManager(this, new SensorDataManager.SensorDataListener() {
            @Override
            public void onSensorDataUpdated(float x, float y, float z, float total) {
                updateRawChartsWithAveragedData(x, y, z);
            }
            public void onSamplingRateUpdated(float samplingRate) {
                runOnUiThread(() ->
                        samplingRateText.setText(String.format(Locale.US, "Частота: %.1f Гц", samplingRate)));
            }

            @Override
            public void onFFTDataProcessed(float[] fftX, float[] fftY, float[] fftZ) {
                float[] frequencies = sensorDataManager.getCurrentFrequencies();
                updateFFTChart(fftXChart, frequencies, fftX, Color.BLUE, "FFT X");
                updateFFTChart(fftYChart, frequencies, fftY, Color.RED, "FFT Y");
                updateFFTChart(fftZChart, frequencies, fftZ, Color.GREEN, "FFT Z");
            }
        }, 3);

        sensorDataManager.setFFTEnabled(true, 1024);
    }
    @Override
    protected void onResume() {
        super.onResume();
        sensorDataManager.registerListener();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorDataManager.unregisterListener();
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
    @Override
    protected int getLayoutId() {
        return R.layout.activity_fft;
    }
    @Override
    protected void initViews() {



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

    private void updateRawChartsWithAveragedData(float x, float y, float z) {
        float avgX = x; // данные уже усреднены в SensorDataManager
        float avgY = y;
        float avgZ = z;

        updateChart(rawXChart, avgX, Color.BLUE, "Raw X");
        updateChart(rawYChart, avgY, Color.RED, "Raw Y");
        updateChart(rawZChart, avgZ, Color.GREEN, "Raw Z");
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
            entries.add(new Entry(frequencies[i], magnitudes[i]));
        }

        LineDataSet set = new LineDataSet(entries, label);
        set.setColor(color);
        set.setDrawCircles(false);
        set.setLineWidth(1.5f);
        set.setDrawValues(false);
        LineData data = new LineData(set);
        chart.setData(data);
        // Настройка осей
        chart.getXAxis().setAxisMinimum(0f);
        chart.getXAxis().setAxisMaximum(frequencies[frequencies.length-1]);
        chart.getAxisLeft().setAxisMinimum(0f);
        chart.getAxisRight().setEnabled(false);

        chart.invalidate();
        chart.animateX(500); // Анимация для плавного обновления
    }


}
