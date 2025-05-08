package com.example.axel;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import java.util.ArrayList;
import java.util.List;

public class SensorDataManager implements SensorEventListener {
    private static final String TAG = "SensorDataManager";
    private static final int DEFAULT_BUFFER_SIZE = 3;

    private final SensorManager sensorManager;
    private final Sensor accelerometer;
    private final SensorDataListener listener;
    private final int bufferSize;

    // Buffers for sensor data
    private final List<Float> bufferX = new ArrayList<>();
    private final List<Float> bufferY = new ArrayList<>();
    private final List<Float> bufferZ = new ArrayList<>();

    // Sampling rate calculation
    private long previousTimestamp = 0;
    private final ArrayList<Float> samplingRates = new ArrayList<>();
    private static final int SAMPLING_RATE_BUFFER_SIZE = 10;
    // FFT processing
    private FFTProcessor fftProcessor;
    private boolean isFFTEnabled = false;
    private int fftSize = 256;
    private float lastX, lastY, lastZ;
    public float getLastX() { return lastX; }
    public float getLastY() { return lastY; }
    public float getLastZ() { return lastZ; }
    private final Context context;
    //буферы для FFT
    private final List<Float> fftBufferX = new ArrayList<>();
    private final List<Float> fftBufferY = new ArrayList<>();
    private final List<Float> fftBufferZ = new ArrayList<>();
    private DataRecorder dataRecorder;

    public void setDataRecorder(DataRecorder dataRecorder) {
        this.dataRecorder = dataRecorder;
    }

    public SensorDataManager(Context context, SensorDataListener listener, int bufferSize) {
        this.context = context.getApplicationContext();
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.listener = listener;
        this.bufferSize = bufferSize > 0 ? bufferSize : DEFAULT_BUFFER_SIZE;

    }

    public void setFFTEnabled(boolean enabled, int fftSize) {
        this.isFFTEnabled = enabled;
        this.fftSize = fftSize;
        if (enabled) {
            this.fftProcessor = new FFTProcessor(fftSize);
        }
    }

    public void registerListener() {
        SharedPreferences prefs = context.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
        int delay = prefs.getInt("accelerometer_delay", SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.unregisterListener(this);
        sensorManager.registerListener(this, accelerometer, delay);
    }


    public void unregisterListener() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        long currentTimestamp = event.timestamp;
        processSamplingRate(currentTimestamp);
        processSensorData(event.values[0], event.values[1], event.values[2]);
    }

    private void processSamplingRate(long currentTimestamp) {
        if (previousTimestamp != 0) {
            float intervalNs = currentTimestamp - previousTimestamp;
            float frequencyHz = 1_000_000_000.0f / intervalNs;

            samplingRates.add(frequencyHz);
            if (samplingRates.size() > SAMPLING_RATE_BUFFER_SIZE) {
                samplingRates.remove(0);
            }

            if (listener != null) {
                listener.onSamplingRateUpdated(calculateAverage(samplingRates));
            }
        }
        previousTimestamp = currentTimestamp;
    }

    private void processSensorData(float x, float y, float z) {
        bufferX.add(x);
        bufferY.add(y);
        bufferZ.add(z);

        fftBufferX.add(x);
        fftBufferY.add(y);
        fftBufferZ.add(z);

        if (bufferX.size() >= bufferSize) {
            float avgX = calculateAverage(bufferX);
            float avgY = calculateAverage(bufferY);
            float avgZ = calculateAverage(bufferZ);
            float total = (float) Math.sqrt(avgX*avgX + avgY*avgY + avgZ*avgZ);

            if (listener != null) {
                listener.onSensorDataUpdated(
                        avgX / 9.8f,
                        avgY / 9.8f,
                        avgZ / 9.8f,
                        total / 9.8f
                );
            }

            bufferX.clear();
            bufferY.clear();
            bufferZ.clear();

            this.lastX = avgX / 9.8f;
            this.lastY = avgY / 9.8f;
            this.lastZ = avgZ / 9.8f;
        }

        if (isFFTEnabled && fftBufferX.size() >= fftSize) {
            processFFTData();
        }

        if (dataRecorder != null && !isFFTEnabled) {
            float total = (float) Math.sqrt(x*x + y*y + z*z);
            dataRecorder.writeDataToCsv(x/9.8f, y/9.8f, z/9.8f, total/9.8f);
        }
    }

    private void processFFTData() {
        if (fftBufferX.size() >= fftSize && listener != null) { // Исправлено условие
            float[] fftX = fftProcessor.computeFFT(toFloatArray(fftBufferX), calculateAverage(samplingRates));
            float[] fftY = fftProcessor.computeFFT(toFloatArray(fftBufferY), calculateAverage(samplingRates));
            float[] fftZ = fftProcessor.computeFFT(toFloatArray(fftBufferZ), calculateAverage(samplingRates));

            // Очищаем буферы после обработки
            fftBufferX.clear();
            fftBufferY.clear();
            fftBufferZ.clear();

            listener.onFFTDataProcessed(fftX, fftY, fftZ);
        }
    }

    public float[] getCurrentFrequencies() {
        if(fftProcessor != null) {
            return fftProcessor.getFrequencies(calculateAverage(samplingRates));
        }
        return new float[0];
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // Helper methods
    private float calculateAverage(List<Float> list) {
        float sum = 0;
        for (float num : list) sum += num;
        return sum / list.size();
    }

    private float[] toFloatArray(List<Float> list) {
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) array[i] = list.get(i);
        return array;
    }

    private float getMaxAmplitude(float[] data) {
        float max = 0;
        for (float val : data) if (val > max) max = val;
        return max;
    }

    public interface SensorDataListener {
        void onSensorDataUpdated(float x, float y, float z, float total);
        void onFFTDataProcessed(float[] fftX, float[] fftY, float[] fftZ);
        void onSamplingRateUpdated(float samplingRate);
    }
}
