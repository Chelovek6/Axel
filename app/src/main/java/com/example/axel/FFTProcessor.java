package com.example.axel;

import org.jtransforms.fft.FloatFFT_1D;

public class FFTProcessor {
    private final FloatFFT_1D fft;
    private final int fftSize;

    public FFTProcessor(int size) {
        this.fftSize = size;
        this.fft = new FloatFFT_1D(size);
    }

    public float[] getFrequencies(float samplingRate) {
        float[] frequencies = new float[fftSize/2];
        for (int i = 0; i < frequencies.length; i++) {
            frequencies[i] = (i * samplingRate) / fftSize;
        }
        return frequencies;
    }

    public float[] computeFFT(float[] input,float samplingRate) {
        float[] windowed = applyHannWindow(input);
        float[] fftData = new float[fftSize * 2];
        System.arraycopy(windowed, 0, fftData, 0, fftSize);

        fft.realForward(fftData);

        float[] magnitudes = new float[fftSize/2];
        for (int i = 0; i < fftSize/2; i++) {
            float re = fftData[2*i];
            float im = fftData[2*i + 1];
            magnitudes[i] = (float) Math.sqrt(re*re + im*im);
        }
        return magnitudes;
    }


    private float[] applyHannWindow(float[] data) {
        float[] windowed = new float[fftSize];
        for (int i = 0; i < fftSize; i++) {
            float window = 0.5f*(1 - (float)Math.cos(2*Math.PI*i/(fftSize-1)));
            windowed[i] = data[i] * window;
        }
        return windowed;
    }
}
