package com.example.axel;

import android.content.Context;
import android.util.AttributeSet;
import android.graphics.Color;
import com.github.mikephil.charting.charts.LineChart;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;


public class LineChartView extends LineChart {

    private LineDataSet rawXSet, rawYSet, rawZSet, rawTotalSet;
    private LineDataSet filteredXSet, filteredYSet, filteredZSet, filteredTotalSet;

    public LineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initChart();
    }

    private void initChart() {
        // Настройки графика
        getDescription().setEnabled(false);
        setTouchEnabled(true);
        setDragEnabled(true);
        setScaleEnabled(true);

        // Создаем наборы данных
        createDataSets();
    }

    private void createDataSets() {
        // Исходные данные (полупрозрачные)
        rawXSet = createDataSet("Raw X", Color.argb(100, 0, 0, 255));
        rawYSet = createDataSet("Raw Y", Color.argb(100, 255, 0, 0));
        rawZSet = createDataSet("Raw Z", Color.argb(100, 0, 255, 0));
        rawTotalSet = createDataSet("Raw Total", Color.argb(100, 255, 0, 255));

        // Отфильтрованные данные (яркие)
        filteredXSet = createDataSet("Filtered X", Color.BLUE);
        filteredYSet = createDataSet("Filtered Y", Color.RED);
        filteredZSet = createDataSet("Filtered Z", Color.GREEN);
        filteredTotalSet = createDataSet("Filtered Total", Color.MAGENTA);

        LineData data = new LineData(
                rawXSet, rawYSet, rawZSet, rawTotalSet,
                filteredXSet, filteredYSet, filteredZSet, filteredTotalSet
        );
        setData(data);
    }

    private LineDataSet createDataSet(String label, int color) {
        LineDataSet set = new LineDataSet(null, label);
        set.setColor(color);
        set.setLineWidth(2f);
        set.setDrawCircles(false);
        return set;
    }

    public void addRawData(float x, float y, float z, float total) {
        addEntry(rawXSet, x);
        addEntry(rawYSet, y);
        addEntry(rawZSet, z);
        addEntry(rawTotalSet, total);
    }


    private void addEntry(LineDataSet dataSet, float value) {
        LineData data = getData();
        if (data != null) {
            data.addEntry(new Entry(dataSet.getEntryCount(), value), data.getIndexOfDataSet(dataSet));
            data.notifyDataChanged();
            notifyDataSetChanged();
            setVisibleXRangeMaximum(100);
            moveViewToX(data.getEntryCount());
        }
    }
}