package com.example.axel;

import org.jtransforms.fft.FloatFFT_1D;

public class FFTProcessor {
    private final int bufferSize;
    private final FloatFFT_1D fft;
    private final float[] window;

    // Буферы для каждой оси
    private final float[] xBuffer;
    private final float[] yBuffer;
    private final float[] zBuffer;
    private int bufferIndex = 0;

    public FFTProcessor(int bufferSize) {
        this.bufferSize = bufferSize;
        this.fft = new FloatFFT_1D(bufferSize);
        this.window = new float[bufferSize];
        this.xBuffer = new float[bufferSize];
        this.yBuffer = new float[bufferSize];
        this.zBuffer = new float[bufferSize];

        initWindow();
    }

    private void initWindow() {
        for (int i = 0; i < bufferSize; i++) {
            window[i] = (float) (0.5 * (1 - Math.cos(2 * Math.PI * i / (bufferSize - 1))));
        }
    }

    public void addData(float x, float y, float z) {
        xBuffer[bufferIndex] = x * window[bufferIndex];
        yBuffer[bufferIndex] = y * window[bufferIndex];
        zBuffer[bufferIndex] = z * window[bufferIndex];
        bufferIndex++;
    }

    public boolean isBufferFull() {
        return bufferIndex >= bufferSize;
    }

    public float[][] processFFT(float cutoffFreq, float sampleRate) {
        // Применяем FFT для каждой оси
        float[][] filtered = new float[3][];
        filtered[0] = processAxis(xBuffer.clone(), cutoffFreq, sampleRate);
        filtered[1] = processAxis(yBuffer.clone(), cutoffFreq, sampleRate);
        filtered[2] = processAxis(zBuffer.clone(), cutoffFreq, sampleRate);

        // Сбрасываем индекс буфера
        bufferIndex = 0;
        return filtered;
    }

    private float[] processAxis(float[] data, float cutoffFreq, float sampleRate) {
        fft.realForward(data);

        // Обнуление высоких частот
        int cutoffBin = (int) (cutoffFreq * bufferSize / sampleRate);
        for (int i = cutoffBin; i < bufferSize / 2; i++) {
            data[2 * i] = 0;     // Real
            data[2 * i + 1] = 0; // Imaginary
        }

        fft.realInverse(data, true);
        return data;
    }
}
