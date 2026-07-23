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

public class Plot3DRenderer {

    private static final Pattern POINT_PATTERN = Pattern.compile("point\\[(-?[\\d.eE+]+),(-?[\\d.eE+]+),(-?[\\d.eE+]+)\\]");

    public static boolean is3DPlotResult(String result) {
        if (result == null) return false;
        return result.contains("hypersurface(group[");
    }

    public static Bitmap renderPlot3D(String result, int width, int height) {
        if (!is3DPlotResult(result)) return null;

        try {
            List<double[]> points = parsePoints3D(result);
            if (points.isEmpty()) return null;

            int gridSize = detectGridSize(points);
            if (gridSize < 2) return null;

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.WHITE);

            double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
            double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            double minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;

            for (double[] p : points) {
                minX = Math.min(minX, p[0]);
                maxX = Math.max(maxX, p[0]);
                minY = Math.min(minY, p[1]);
                maxY = Math.max(maxY, p[1]);
                minZ = Math.min(minZ, p[2]);
                maxZ = Math.max(maxZ, p[2]);
            }

            double cx = (minX + maxX) / 2;
            double cy = (minY + maxY) / 2;
            double cz = (minZ + maxZ) / 2;
            double rangeX = maxX - minX;
            double rangeY = maxY - minY;
            double rangeZ = maxZ - minZ;
            if (rangeX == 0) rangeX = 1;
            if (rangeY == 0) rangeY = 1;
            if (rangeZ == 0) rangeZ = 1;

            double angleX = Math.toRadians(30);
            double angleZ = Math.toRadians(45);

            double cosZ = Math.cos(angleZ), sinZ = Math.sin(angleZ);
            double cosX = Math.cos(angleX), sinX = Math.sin(angleX);

            double scale = Math.min(width, height) * 0.35;

            float marginLeft = width / 2f;
            float marginTop = height / 2f;

            Paint gridPaint = new Paint();
            gridPaint.setColor(Color.rgb(200, 200, 200));
            gridPaint.setStrokeWidth(1);

            Paint surfacePaint = new Paint();
            surfacePaint.setColor(Color.rgb(0, 100, 200));
            surfacePaint.setStrokeWidth(2);
            surfacePaint.setStyle(Paint.Style.STROKE);
            surfacePaint.setAntiAlias(true);

            Paint axisPaint = new Paint();
            axisPaint.setStrokeWidth(2);
            axisPaint.setAntiAlias(true);

            Paint labelPaint = new Paint();
            labelPaint.setColor(Color.rgb(80, 80, 80));
            labelPaint.setTextSize(Math.max(10, height / 30f));
            labelPaint.setAntiAlias(true);

            int rows = gridSize;
            int cols = points.size() / gridSize;

            for (int i = 0; i < rows; i++) {
                Path rowPath = new Path();
                boolean first = true;
                for (int j = 0; j < cols; j++) {
                    int idx = i * cols + j;
                    if (idx >= points.size()) break;
                    double[] p = points.get(idx);
                    float[] proj = project(p, cx, cy, cz, rangeX, rangeY, rangeZ, cosZ, sinZ, cosX, sinX, scale, marginLeft, marginTop);
                    if (first) {
                        rowPath.moveTo(proj[0], proj[1]);
                        first = false;
                    } else {
                        rowPath.lineTo(proj[0], proj[1]);
                    }
                }
                canvas.drawPath(rowPath, surfacePaint);
            }

            for (int j = 0; j < cols; j++) {
                Path colPath = new Path();
                boolean first = true;
                for (int i = 0; i < rows; i++) {
                    int idx = i * cols + j;
                    if (idx >= points.size()) break;
                    double[] p = points.get(idx);
                    float[] proj = project(p, cx, cy, cz, rangeX, rangeY, rangeZ, cosZ, sinZ, cosX, sinX, scale, marginLeft, marginTop);
                    if (first) {
                        colPath.moveTo(proj[0], proj[1]);
                        first = false;
                    } else {
                        colPath.lineTo(proj[0], proj[1]);
                    }
                }
                canvas.drawPath(colPath, surfacePaint);
            }

            double[][] axes = {
                    {minX, minY, minZ, maxX, minY, minZ},
                    {minX, minY, minZ, minX, maxY, minZ},
                    {minX, minY, minZ, minX, minY, maxZ},
            };
            int[] axisColors = {Color.rgb(200, 50, 50), Color.rgb(50, 150, 50), Color.rgb(50, 50, 200)};
            String[] axisLabels = {"x", "y", "z"};

            for (int a = 0; a < 3; a++) {
                axisPaint.setColor(axisColors[a]);
                float[] p1 = project(axes[a], cx, cy, cz, rangeX, rangeY, rangeZ, cosZ, sinZ, cosX, sinX, scale, marginLeft, marginTop);
                float[] p2 = project(new double[]{axes[a][3], axes[a][4], axes[a][5]}, cx, cy, cz, rangeX, rangeY, rangeZ, cosZ, sinZ, cosX, sinX, scale, marginLeft, marginTop);
                canvas.drawLine(p1[0], p1[1], p2[0], p2[1], axisPaint);
                canvas.drawText(axisLabels[a], p2[0] + 5, p2[1] - 5, labelPaint);
            }

            return bitmap;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static float[] project(double[] p, double cx, double cy, double cz,
                                   double rangeX, double rangeY, double rangeZ,
                                   double cosZ, double sinZ, double cosX, double sinX,
                                   double scale, float marginLeft, float marginTop) {
        double nx = (p[0] - cx) / rangeX;
        double ny = (p[1] - cy) / rangeY;
        double nz = (p[2] - cz) / rangeZ;

        double rx = nx * cosZ - ny * sinZ;
        double ry = nx * sinZ + ny * cosZ;
        double rz = nz;

        double py = ry * cosX - rz * sinX;
        double pz = ry * sinX + rz * cosX;

        float screenX = (float) (marginLeft + rx * scale);
        float screenY = (float) (marginTop - pz * scale);

        return new float[]{screenX, screenY};
    }

    private static int detectGridSize(List<double[]> points) {
        if (points.isEmpty()) return 0;
        double firstX = points.get(0)[0];
        int count = 1;
        for (int i = 1; i < points.size(); i++) {
            if (Math.abs(points.get(i)[0] - firstX) < 1e-9) {
                count++;
            } else {
                break;
            }
        }
        if (count > 1 && points.size() % count == 0) {
            return count;
        }
        return (int) Math.sqrt(points.size());
    }

    private static List<double[]> parsePoints3D(String result) {
        List<double[]> points = new ArrayList<>();
        Matcher m = POINT_PATTERN.matcher(result);
        while (m.find()) {
            try {
                double x = Double.parseDouble(m.group(1));
                double y = Double.parseDouble(m.group(2));
                double z = Double.parseDouble(m.group(3));
                points.add(new double[]{x, y, z});
            } catch (NumberFormatException e) {
                // skip
            }
        }
        return points;
    }
}
