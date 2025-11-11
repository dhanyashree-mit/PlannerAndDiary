package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WeeklyProgressChart extends View {

    private Paint linePaint, circlePaint, textPaint, bgPaint;
    private Map<String, Integer> data;
    private boolean isDarkMode = false;

    public WeeklyProgressChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        linePaint = new Paint();
        linePaint.setColor(Color.parseColor("#2196F3")); // default blue
        linePaint.setStrokeWidth(6f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);

        circlePaint = new Paint();
        circlePaint.setColor(Color.parseColor("#2196F3"));
        circlePaint.setStyle(Paint.Style.FILL);
        circlePaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(28f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        bgPaint = new Paint();
        bgPaint.setColor(Color.WHITE);
    }

    public void setData(Map<String, Integer> data) {
        this.data = data;
        invalidate();
    }

    public void setDarkMode(boolean darkMode) {
        isDarkMode = darkMode;
        if (isDarkMode) {
            bgPaint.setColor(Color.BLACK);
            textPaint.setColor(Color.WHITE);
            linePaint.setColor(Color.parseColor("#64B5F6")); // brighter blue
            circlePaint.setColor(Color.parseColor("#64B5F6"));
        } else {
            bgPaint.setColor(Color.WHITE);
            textPaint.setColor(Color.BLACK);
            linePaint.setColor(Color.parseColor("#2196F3"));
            circlePaint.setColor(Color.parseColor("#2196F3"));
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Fill background
        canvas.drawRect(0, 0, getWidth(), getHeight(), bgPaint);

        if (data == null || data.isEmpty()) return;

        float width = getWidth();
        float height = getHeight();
        int max = 1;
        for (int value : data.values()) if (value > max) max = value;

        List<Float> xPoints = new ArrayList<>();
        List<Float> yPoints = new ArrayList<>();

        int i = 0;
        float spacing = width / (data.size() + 1);

        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            float x = spacing * (i + 1);
            float y = height - ((entry.getValue() / (float) max) * (height - 50)) - 40;

            xPoints.add(x);
            yPoints.add(y);

            i++;
        }

        // Draw lines
        for (i = 0; i < xPoints.size() - 1; i++) {
            canvas.drawLine(xPoints.get(i), yPoints.get(i), xPoints.get(i + 1), yPoints.get(i + 1), linePaint);
        }

        // Draw points
        for (i = 0; i < xPoints.size(); i++) {
            canvas.drawCircle(xPoints.get(i), yPoints.get(i), 10, circlePaint);
            String label = data.keySet().toArray(new String[0])[i];
            canvas.drawText(label, xPoints.get(i), height - 10, textPaint);
        }
    }
}
