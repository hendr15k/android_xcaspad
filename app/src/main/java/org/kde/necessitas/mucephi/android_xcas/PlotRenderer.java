package org.kde.necessitas.mucephi.android_xcas;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlotRenderer {

    private static final Pattern PNT_PATTERN = Pattern.compile("^pnt\\(pnt\\[curve\\(group\\[(.+?)\\]\\),\\d+\\]\\)$");
    private static final Pattern COMPLEX_PATTERN = Pattern.compile("(-?[\\d.]+)([+-][\\d.]+)\\*i");
    private static final Pattern REAL_PATTERN = Pattern.compile("^(-?[\\d.]+)$");

    public static boolean isPlotResult(String result) {
        if (result == null) return false;
        return result.startsWith("pnt(pnt[curve(group[");
    }

    public static Bitmap renderPlot(String result, int width, int height) {
        if (!isPlotResult(result)) return null;

        try {
            List<double[]> points = parsePoints(result);
            if (points.isEmpty()) return null;

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.WHITE);

            double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
            double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

            for (double[] p : points) {
                minX = Math.min(minX, p[0]);
                maxX = Math.max(maxX, p[0]);
                minY = Math.min(minY, p[1]);
                maxY = Math.max(maxY, p[1]);
            }

            double rangeX = maxX - minX;
            double rangeY = maxY - minY;
            if (rangeX == 0) rangeX = 1;
            if (rangeY == 0) rangeY = 1;

            double margin = 40;
            double plotWidth = width - 2 * margin;
            double plotHeight = height - 2 * margin;

            Paint axisPaint = new Paint();
            axisPaint.setColor(Color.BLACK);
            axisPaint.setStrokeWidth(2);
            axisPaint.setStyle(Paint.Style.STROKE);

            Paint gridPaint = new Paint();
            gridPaint.setColor(Color.LTGRAY);
            gridPaint.setStrokeWidth(1);

            Paint curvePaint = new Paint();
            curvePaint.setColor(Color.rgb(0, 100, 200));
            curvePaint.setStrokeWidth(3);
            curvePaint.setStyle(Paint.Style.STROKE);
            curvePaint.setAntiAlias(true);

            double zeroX = margin + (0 - minX) / rangeX * plotWidth;
            double zeroY = margin + (maxY - 0) / rangeY * plotHeight;

            if (zeroX >= margin && zeroX <= width - margin) {
                canvas.drawLine((float) zeroX, (float) margin, (float) zeroX, (float) (height - margin), axisPaint);
            }
            if (zeroY >= margin && zeroY <= height - margin) {
                canvas.drawLine((float) margin, (float) zeroY, (float) (width - margin), (float) zeroY, axisPaint);
            }

            for (int i = 0; i <= 10; i++) {
                double x = margin + i * plotWidth / 10;
                canvas.drawLine((float) x, (float) margin, (float) x, (float) (height - margin), gridPaint);
                double y = margin + i * plotHeight / 10;
                canvas.drawLine((float) margin, (float) y, (float) (width - margin), (float) y, gridPaint);
            }

            Path path = new Path();
            boolean first = true;

            for (double[] p : points) {
                float x = (float) (margin + (p[0] - minX) / rangeX * plotWidth);
                float y = (float) (margin + (maxY - p[1]) / rangeY * plotHeight);

                if (first) {
                    path.moveTo(x, y);
                    first = false;
                } else {
                    path.lineTo(x, y);
                }
            }

            canvas.drawPath(path, curvePaint);

            return bitmap;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static List<double[]> parsePoints(String result) {
        List<double[]> points = new ArrayList<>();

        Matcher m = PNT_PATTERN.matcher(result);
        if (!m.find()) return points;

        String pointsStr = m.group(1);
        String[] parts = pointsStr.split(",(?=group\\[)");

        for (String part : parts) {
            part = part.trim();
            if (part.startsWith("group[")) {
                part = part.substring(6, part.length() - 1);
            }

            String[] coords = part.split(",");
            for (String coord : coords) {
                coord = coord.trim();
                double[] point = parseCoordinate(coord);
                if (point != null) {
                    points.add(point);
                }
            }
        }

        return points;
    }

    private static double[] parseCoordinate(String coord) {
        coord = coord.trim();

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
}
