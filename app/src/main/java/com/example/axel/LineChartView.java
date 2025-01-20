package com.example.axel;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;

public class LineChartView extends View {

    private Paint xLinePaint;
    private Paint yLinePaint;
    private Paint zLinePaint;
    private Paint totalLinePaint;
    private Paint axisPaint;
    private Paint gridPaint;

    private ArrayList<Float> xDataPoints = new ArrayList<>();
    private ArrayList<Float> yDataPoints = new ArrayList<>();
    private ArrayList<Float> zDataPoints = new ArrayList<>();
    private ArrayList<Float> totalDataPoints = new ArrayList<>();

    private int visibleDataCount = 50;

    public LineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);


        xLinePaint = new Paint();
        xLinePaint.setColor(Color.BLUE);
        xLinePaint.setStrokeWidth(5f);

        yLinePaint = new Paint();
        yLinePaint.setColor(Color.RED);
        yLinePaint.setStrokeWidth(5f);

        zLinePaint = new Paint();
        zLinePaint.setColor(Color.GREEN);
        zLinePaint.setStrokeWidth(5f);

        totalLinePaint = new Paint();
        totalLinePaint.setColor(Color.MAGENTA);
        totalLinePaint.setStrokeWidth(5f);

        axisPaint = new Paint();
        axisPaint.setColor(Color.BLACK);
        axisPaint.setStrokeWidth(3f);

        gridPaint = new Paint();
        gridPaint.setColor(Color.LTGRAY);
        gridPaint.setStrokeWidth(2f);
    }

    public void addDataPoint(float x, float y, float z, float total) {
        if (xDataPoints.size() >= visibleDataCount) {
            xDataPoints.remove(0);
            yDataPoints.remove(0);
            zDataPoints.remove(0);
            totalDataPoints.remove(0);
        }

        xDataPoints.add(x);
        yDataPoints.add(y);
        zDataPoints.add(z);
        totalDataPoints.add(total);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        float zeroLevel = height / 2f;
        float scaleY = zeroLevel / 10;


        canvas.drawLine(50, zeroLevel, width - 50, zeroLevel, axisPaint);
        canvas.drawLine(50, 0, 50, height, axisPaint);

        if (xDataPoints.isEmpty()) return;

        float scaleX = (width - 100) / (float) (visibleDataCount - 1);
        drawLine(canvas, xDataPoints, zeroLevel, scaleX, scaleY, xLinePaint);
        drawLine(canvas, yDataPoints, zeroLevel, scaleX, scaleY, yLinePaint);
        drawLine(canvas, zDataPoints, zeroLevel, scaleX, scaleY, zLinePaint);
        drawLine(canvas, totalDataPoints, zeroLevel, scaleX, scaleY, totalLinePaint);
    }

    private void drawLine(Canvas canvas, ArrayList<Float> dataPoints, float zeroLevel, float scaleX, float scaleY, Paint paint) {
        Float lastX = null, lastY = null;
        for (int i = 0; i < dataPoints.size(); i++) {
            float value = dataPoints.get(i);
            float x = 50 + i * scaleX;
            float y = zeroLevel - value * scaleY;

            if (lastX != null && lastY != null) {
                canvas.drawLine(lastX, lastY, x, y, paint);
            }
            lastX = x;
            lastY = y;
        }
    }
}
