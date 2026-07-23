package org.kde.necessitas.mucephi.android_xcas;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlotRenderer {

    private static final Pattern PNT_PATTERN = Pattern.compile("pnt\\(pnt\\[curve\\(group\\[(.+?)\\]\\),\\d+\\]\\)");
    private static final Pattern COMPLEX_PATTERN = Pattern.compile("(-?[\\d.eE+]+)([+-][\\d.eE+]+)\\*i");
    private static final Pattern REAL_PATTERN = Pattern.compile("^(-?[\\d.eE+]+)$");

    private static final int[] CURVE_COLORS = {
            Color.rgb(0, 100, 200),
            Color.rgb(200, 50, 50),
            Color.rgb(0, 150, 80),
            Color.rgb(180, 100, 0),
            Color.rgb(130, 0, 180),
            Color.rgb(0, 150, 150),
    };

    public static boolean isPlotResult(String result) {
        if (result == null) return false;
        return result.contains("pnt(pnt[curve(group[");
    }

    public static Bitmap renderPlot(String result, int width, int height) {
        if (!isPlotResult(result)) return null;

        try {
            List<PlotCurve> curves = parseCurves(result);
            if (curves.isEmpty()) return null;

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.WHITE);

            double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
            double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

            for (PlotCurve curve : curves) {
                for (double[] p : curve.points) {
                    minX = Math.min(minX, p[0]);
                    maxX = Math.max(maxX, p[0]);
                    minY = Math.min(minY, p[1]);
                    maxY = Math.max(maxY, p[1]);
                }
            }

            if (minX == maxX) { minX -= 1; maxX += 1; }
            if (minY == maxY) { minY -= 1; maxY += 1; }

            double padX = (maxX - minX) * 0.05;
            double padY = (maxY - minY) * 0.05;
            minX -= padX; maxX += padX;
            minY -= padY; maxY += padY;

            double rangeX = maxX - minX;
            double rangeY = maxY - minY;

            float marginLeft = 55;
            float marginRight = 20;
            float marginTop = 30;
            float marginBottom = 45;
            float plotLeft = marginLeft;
            float plotRight = width - marginRight;
            float plotTop = marginTop;
            float plotBottom = height - marginBottom;
            float plotWidth = plotRight - plotLeft;
            float plotHeight = plotBottom - plotTop;

            Paint borderPaint = new Paint();
            borderPaint.setColor(Color.rgb(180, 180, 180));
            borderPaint.setStrokeWidth(1);
            borderPaint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(plotLeft, plotTop, plotRight, plotBottom, borderPaint);

            Paint gridPaint = new Paint();
            gridPaint.setColor(Color.rgb(230, 230, 230));
            gridPaint.setStrokeWidth(1);

            Paint axisPaint = new Paint();
            axisPaint.setColor(Color.rgb(60, 60, 60));
            axisPaint.setStrokeWidth(2);
            axisPaint.setAntiAlias(true);

            Paint tickPaint = new Paint();
            tickPaint.setColor(Color.rgb(60, 60, 60));
            tickPaint.setStrokeWidth(1);

            Paint labelPaint = new Paint();
            labelPaint.setColor(Color.rgb(80, 80, 80));
            labelPaint.setTextSize(Math.max(10, height / 30f));
            labelPaint.setAntiAlias(true);

            double[] niceX = niceStep(rangeX, 8);
            double stepX = niceX[0];
            double startX = Math.ceil(minX / stepX) * stepX;

            for (double vx = startX; vx <= maxX; vx += stepX) {
                float px = (float) (plotLeft + (vx - minX) / rangeX * plotWidth);
                if (px < plotLeft || px > plotRight) continue;
                canvas.drawLine(px, plotTop, px, plotBottom, gridPaint);
                canvas.drawLine(px, plotBottom, px, plotBottom + 5, tickPaint);
                String lbl = formatNumber(vx);
                labelPaint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(lbl, px, plotBottom + 18, labelPaint);
            }

            double[] niceY = niceStep(rangeY, 6);
            double stepY = niceY[0];
            double startY = Math.ceil(minY / stepY) * stepY;

            for (double vy = startY; vy <= maxY; vy += stepY) {
                float py = (float) (plotTop + (maxY - vy) / rangeY * plotHeight);
                if (py < plotTop || py > plotBottom) continue;
                canvas.drawLine(plotLeft, py, plotRight, py, gridPaint);
                canvas.drawLine(plotLeft - 5, py, plotLeft, py, tickPaint);
                String lbl = formatNumber(vy);
                labelPaint.setTextAlign(Paint.Align.RIGHT);
                canvas.drawText(lbl, plotLeft - 8, py + 4, labelPaint);
            }

            float zeroX = (float) (plotLeft + (0 - minX) / rangeX * plotWidth);
            float zeroY = (float) (plotTop + (maxY - 0) / rangeY * plotHeight);

            if (zeroX >= plotLeft && zeroX <= plotRight) {
                canvas.drawLine(zeroX, plotTop, zeroX, plotBottom, axisPaint);
            }
            if (zeroY >= plotTop && zeroY <= plotBottom) {
                canvas.drawLine(plotLeft, zeroY, plotRight, zeroY, axisPaint);
            }

            for (int ci = 0; ci < curves.size(); ci++) {
                PlotCurve curve = curves.get(ci);
                Paint curvePaint = new Paint();
                curvePaint.setColor(CURVE_COLORS[ci % CURVE_COLORS.length]);
                curvePaint.setStrokeWidth(3);
                curvePaint.setStyle(Paint.Style.STROKE);
                curvePaint.setAntiAlias(true);
                curvePaint.setStrokeJoin(Paint.Join.ROUND);
                curvePaint.setStrokeCap(Paint.Cap.ROUND);

                Path path = new Path();
                boolean penDown = false;
                double prevY = 0;
                double jumpThreshold = rangeY * 0.5;

                for (double[] p : curve.points) {
                    float px = (float) (plotLeft + (p[0] - minX) / rangeX * plotWidth);
                    float py = (float) (plotTop + (maxY - p[1]) / rangeY * plotHeight);

                    boolean discontinuity = penDown && Math.abs(p[1] - prevY) > jumpThreshold;

                    if (!penDown || discontinuity) {
                        path.moveTo(px, py);
                    } else {
                        path.lineTo(px, py);
                    }
                    penDown = true;
                    prevY = p[1];
                }

                canvas.drawPath(path, curvePaint);
            }

            if (curves.size() > 1) {
                Paint legendPaint = new Paint();
                legendPaint.setTextSize(Math.max(10, height / 35f));
                legendPaint.setAntiAlias(true);
                float ly = plotTop + 15;
                for (int ci = 0; ci < curves.size(); ci++) {
                    legendPaint.setColor(CURVE_COLORS[ci % CURVE_COLORS.length]);
                    canvas.drawLine(plotLeft + 10, ly, plotLeft + 30, ly, legendPaint);
                    legendPaint.setColor(Color.rgb(80, 80, 80));
                    canvas.drawText(curves.get(ci).label, plotLeft + 35, ly + 4, legendPaint);
                    ly += 20;
                }
            }

            return bitmap;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static double[] niceStep(double range, int maxTicks) {
        double rawStep = range / maxTicks;
        double magnitude = Math.pow(10, Math.floor(Math.log10(rawStep)));
        double residual = rawStep / magnitude;
        double nice;
        if (residual <= 1.0) nice = 1.0;
        else if (residual <= 2.0) nice = 2.0;
        else if (residual <= 5.0) nice = 5.0;
        else nice = 10.0;
        return new double[]{nice * magnitude};
    }

    private static String formatNumber(double v) {
        if (v == 0) return "0";
        if (Math.abs(v) >= 1000 || (Math.abs(v) < 0.01 && v != 0)) {
            return String.format("%.1e", v);
        }
        if (v == Math.floor(v) && Math.abs(v) < 1e6) {
            return String.valueOf((long) v);
        }
        String s = String.format("%.2f", v);
        while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        return s;
    }

    private static List<PlotCurve> parseCurves(String result) {
        List<PlotCurve> curves = new ArrayList<>();
        Matcher m = PNT_PATTERN.matcher(result);
        while (m.find()) {
            String groupContent = m.group(1);
            PlotCurve curve = parseCurveGroup(groupContent);
            if (curve != null && !curve.points.isEmpty()) {
                curves.add(curve);
            }
        }
        return curves;
    }

    private static PlotCurve parseCurveGroup(String content) {
        int groupIdx = content.indexOf("group[");
        if (groupIdx < 0) return null;

        String header = content.substring(0, groupIdx);
        if (header.endsWith(",")) header = header.substring(0, header.length() - 1);
        header = header.trim();

        String label = header;
        if (header.contains(",")) {
            label = header.substring(0, header.indexOf(","));
        }

        int lastGroupStart = content.lastIndexOf("group[");
        String pointsSection = content.substring(lastGroupStart + 6);
        if (pointsSection.endsWith("]")) {
            pointsSection = pointsSection.substring(0, pointsSection.length() - 1);
        }

        List<double[]> points = new ArrayList<>();
        String[] coords = pointsSection.split(",");
        for (String coord : coords) {
            coord = coord.trim();
            double[] point = parseCoordinate(coord);
            if (point != null) {
                points.add(point);
            }
        }

        PlotCurve curve = new PlotCurve();
        curve.label = label;
        curve.points = points;
        return curve;
    }

    private static double[] parseCoordinate(String coord) {
        coord = coord.trim();
        if (coord.isEmpty()) return null;

        Matcher complexMatcher = COMPLEX_PATTERN.matcher(coord);
        if (complexMatcher.matches()) {
            try {
                double real = Double.parseDouble(complexMatcher.group(1));
                double imag = Double.parseDouble(complexMatcher.group(2));
                return new double[]{real, imag};
            } catch (NumberFormatException e) {
                return null;
            }
        }

        Matcher realMatcher = REAL_PATTERN.matcher(coord);
        if (realMatcher.matches()) {
            try {
                double val = Double.parseDouble(realMatcher.group(1));
                return new double[]{val, 0};
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }

    private static class PlotCurve {
        String label = "";
        List<double[]> points = new ArrayList<>();
    }
}
