package com.example.smart_air;

import android.graphics.Canvas;


import com.github.mikephil.charting.charts.BarLineChartBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.renderer.XAxisRenderer;
import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.utils.ViewPortHandler;

public class MultiLineXAxisRenderer extends XAxisRenderer {

    public MultiLineXAxisRenderer(ViewPortHandler viewPortHandler, XAxis xAxis, BarLineChartBase<?> chart) {

        super(viewPortHandler, xAxis, chart.getTransformer(YAxis.AxisDependency.LEFT));
    }

    @Override
    public void drawLabel(Canvas c, String label, float x, float y, MPPointF anchor, float angleDegrees) {


        String[] lines = label.split("\n");

        float lineHeight = mAxisLabelPaint.getTextSize() * 1.25f;

        for (int i = 0; i < lines.length; i++) {
            c.drawText(lines[i], x, y + (i * lineHeight), mAxisLabelPaint);
        }
    }
}
