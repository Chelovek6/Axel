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

//    private Paint linePaint, gridPaint, textPaint;
//    private List<Float> xValues = new ArrayList<>();
//    private List<Float> yValues = new ArrayList<>();
//    private List<Float> zValues = new ArrayList<>();
//    private List<Float> totalValues = new ArrayList<>();
//    private LineDataSet rawXSet, rawYSet, rawZSet, rawTotalSet;
//    private LineDataSet filteredXSet, filteredYSet, filteredZSet, filteredTotalSet;
//    private static final float AXIS_MIN = -0.5f;
//    private static final float AXIS_MAX = 1.5f;
//    private static final float STEP = 0.5f;
//    private static final int MAX_POINTS = 100;
//
//    public LineChartView(Context context, AttributeSet attrs) {
//        super(context, attrs);
//        init();
//    }
//
//    private void init() {
//
//        linePaint = new Paint();
//        linePaint.setStrokeWidth(5f);
//        linePaint.setStyle(Paint.Style.STROKE);
//
//
//        gridPaint = new Paint();
//        gridPaint.setColor(Color.LTGRAY);
//        gridPaint.setStrokeWidth(1f);
//
//
//        textPaint = new Paint();
//        textPaint.setColor(Color.BLACK);
//        textPaint.setTextSize(30f);
//        textPaint.setAntiAlias(true);
//    }
//
//    public void addDataPoint(float x, float y, float z, float total) {
//
//        xValues.add(x);
//        yValues.add(y);
//        zValues.add(z);
//        totalValues.add(total);
//
//
//        if (xValues.size() > MAX_POINTS) xValues.remove(0);
//        if (yValues.size() > MAX_POINTS) yValues.remove(0);
//        if (zValues.size() > MAX_POINTS) zValues.remove(0);
//        if (totalValues.size() > MAX_POINTS) totalValues.remove(0);
//
//        invalidate();
//    }
//
//    @Override
//    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
//
//        int width = getWidth();
//        int height = getHeight();
//        int padding = 50;
//
//
//        drawGrid(canvas, width, height, padding);
//
//
//        drawLineChart(canvas, xValues, Color.BLUE, width, height, padding);
//        drawLineChart(canvas, yValues, Color.RED, width, height, padding);
//        drawLineChart(canvas, zValues, Color.GREEN, width, height, padding);
//        drawLineChart(canvas, totalValues, Color.MAGENTA, width, height, padding);
//    }
//
//    private void drawGrid(Canvas canvas, int width, int height, int padding) {
//        float graphHeight = height - 2 * padding;
//        float graphWidth = width - 2 * padding;
//
//
//        for (float i = AXIS_MIN; i <= AXIS_MAX; i += STEP) {
//            float y = padding + graphHeight * (1 - (i - AXIS_MIN) / (AXIS_MAX - AXIS_MIN));
//            canvas.drawLine(padding, y, width - padding, y, gridPaint);
//            canvas.drawText(String.format("%.1f", i), 10, y, textPaint);
//        }
//
//
//        int horizontalSteps = 10;
//        for (int i = 0; i <= horizontalSteps; i++) {
//            float x = padding + i * (graphWidth / horizontalSteps);
//            canvas.drawLine(x, padding, x, height - padding, gridPaint);
//        }
//    }
//
//    private void drawLineChart(Canvas canvas, List<Float> values, int color, int width, int height, int padding) {
//        if (values.size() < 2) return;
//
//        float graphHeight = height - 2 * padding;
//        float graphWidth = width - 2 * padding;
//
//        linePaint.setColor(color);
//
//        int maxVisiblePoints = Math.min(values.size(), MAX_POINTS);
//        float pointSpacing = graphWidth / (MAX_POINTS - 1);
//        for (int i = 1; i < maxVisiblePoints; i++) {
//            float startX = padding + (i - 1) * pointSpacing;
//            float startY = padding + graphHeight * (1 - (values.get(i - 1) - AXIS_MIN) / (AXIS_MAX - AXIS_MIN));
//            float stopX = padding + i * pointSpacing;
//            float stopY = padding + graphHeight * (1 - (values.get(i) - AXIS_MIN) / (AXIS_MAX - AXIS_MIN));
//
//            canvas.drawLine(startX, startY, stopX, stopY, linePaint);
//        }
//    }


}