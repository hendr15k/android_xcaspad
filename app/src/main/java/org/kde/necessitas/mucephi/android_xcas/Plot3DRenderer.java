package org.kde.necessitas.mucephi.android_xcas;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

            int[] grid = detectGrid(points);
            int rows = grid[0];
            int cols = grid[1];
            if (rows < 2 || cols < 2) return null;

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

            double angleX = Math.toRadians(25);
            double angleZ = Math.toRadians(40);

            double cosZ = Math.cos(angleZ), sinZ = Math.sin(angleZ);
            double cosX = Math.cos(angleX), sinX = Math.sin(angleX);

            double scale = Math.min(width, height) * 0.34;

            float marginLeft = width / 2f;
            float marginTop = height / 2f;

            float[][] projected = new float[points.size()][2];
            double[] depths = new double[points.size()];
            for (int i = 0; i < points.size(); i++) {
                double[] p = points.get(i);
                double nx = (p[0] - cx) / rangeX;
                double ny = (p[1] - cy) / rangeY;
                double nz = (p[2] - cz) / rangeZ;
                double rx = nx * cosZ - ny * sinZ;
                double ry = nx * sinZ + ny * cosZ;
                double rz = nz;
                double py = ry * cosX - rz * sinX;
                double pz = ry * sinX + rz * cosX;
                projected[i][0] = (float) (marginLeft + rx * scale);
                projected[i][1] = (float) (marginTop - pz * scale);
                depths[i] = py;
            }

            List<int[]> quads = new ArrayList<>();
            for (int i = 0; i < rows - 1; i++) {
                for (int j = 0; j < cols - 1; j++) {
                    int i0 = i * cols + j;
                    int i1 = i * cols + j + 1;
                    int i2 = (i + 1) * cols + j + 1;
                    int i3 = (i + 1) * cols + j;
                    if (i3 >= points.size()) continue;
                    quads.add(new int[]{i0, i1, i2, i3});
                }
            }

            Collections.sort(quads, new Comparator<int[]>() {
                @Override
                public int compare(int[] a, int[] b) {
                    double da = (depths[a[0]] + depths[a[1]] + depths[a[2]] + depths[a[3]]) / 4.0;
                    double db = (depths[b[0]] + depths[b[1]] + depths[b[2]] + depths[b[3]]) / 4.0;
                    return Double.compare(da, db);
                }
            });

            Paint quadPaint = new Paint();
            quadPaint.setStyle(Paint.Style.FILL);
            quadPaint.setAntiAlias(true);

            Paint wirePaint = new Paint();
            wirePaint.setStyle(Paint.Style.STROKE);
            wirePaint.setStrokeWidth(0.5f);
            wirePaint.setAntiAlias(true);

            double lightX = -0.4, lightY = -0.6, lightZ = 0.7;
            double lightLen = Math.sqrt(lightX * lightX + lightY * lightY + lightZ * lightZ);
            lightX /= lightLen;
            lightY /= lightLen;
            lightZ /= lightLen;

            for (int[] quad : quads) {
                double[] p0 = points.get(quad[0]);
                double[] p1 = points.get(quad[1]);
                double[] p3 = points.get(quad[3]);

                double ux = p1[0] - p0[0], uy = p1[1] - p0[1], uz = p1[2] - p0[2];
                double vx = p3[0] - p0[0], vy = p3[1] - p0[1], vz = p3[2] - p0[2];
                double nx = uy * vz - uz * vy;
                double ny = uz * vx - ux * vz;
                double nz = ux * vy - uy * vx;
                double nLen = Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (nLen > 1e-12) {
                    nx /= nLen;
                    ny /= nLen;
                    nz /= nLen;
                }

                double diffuse = Math.abs(nx * lightX + ny * lightY + nz * lightZ);
                double ambient = 0.3;
                double intensity = ambient + 0.7 * diffuse;
                intensity = Math.max(0, Math.min(1, intensity));

                double avgZ = (p0[2] + p1[2] + points.get(quad[2])[2] + p3[2]) / 4.0;
                double zNorm = (avgZ - minZ) / rangeZ;

                int r = (int) (30 + 180 * zNorm * intensity);
                int g = (int) (80 + 120 * (1 - zNorm) * intensity);
                int b = (int) (160 + 90 * intensity);
                r = clamp(r);
                g = clamp(g);
                b = clamp(b);

                quadPaint.setColor(Color.rgb(r, g, b));
                wirePaint.setColor(Color.rgb(Math.max(0, r - 40), Math.max(0, g - 40), Math.max(0, b - 40)));

                Path path = new Path();
                path.moveTo(projected[quad[0]][0], projected[quad[0]][1]);
                path.lineTo(projected[quad[1]][0], projected[quad[1]][1]);
                path.lineTo(projected[quad[2]][0], projected[quad[2]][1]);
                path.lineTo(projected[quad[3]][0], projected[quad[3]][1]);
                path.close();

                canvas.drawPath(path, quadPaint);
                canvas.drawPath(path, wirePaint);
            }

            drawAxes(canvas, projected, points, rows, cols, minX, maxX, minY, maxY, minZ, maxZ,
                    cx, cy, cz, rangeX, rangeY, rangeZ, cosZ, sinZ, cosX, sinX, scale, marginLeft, marginTop, width, height);

            return bitmap;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void drawAxes(Canvas canvas, float[][] projected, List<double[]> points,
                                 int rows, int cols,
                                 double minX, double maxX, double minY, double maxY, double minZ, double maxZ,
                                 double cx, double cy, double cz,
                                 double rangeX, double rangeY, double rangeZ,
                                 double cosZ, double sinZ, double cosX, double sinX,
                                 double scale, float marginLeft, float marginTop, int width, int height) {

        Paint axisPaint = new Paint();
        axisPaint.setStrokeWidth(2.5f);
        axisPaint.setAntiAlias(true);

        Paint labelPaint = new Paint();
        labelPaint.setColor(Color.rgb(60, 60, 60));
        labelPaint.setTextSize(Math.max(11, height / 28f));
        labelPaint.setAntiAlias(true);
        labelPaint.setFakeBoldText(true);

        Paint tickPaint = new Paint();
        tickPaint.setColor(Color.rgb(100, 100, 100));
        tickPaint.setTextSize(Math.max(9, height / 36f));
        tickPaint.setAntiAlias(true);

        int[] axisColors = {Color.rgb(200, 40, 40), Color.rgb(40, 150, 40), Color.rgb(40, 40, 200)};
        String[] axisLabels = {"x", "y", "z"};

        double[][] axisStart = {
                {minX, minY, minZ},
                {minX, minY, minZ},
                {minX, minY, minZ},
        };
        double[][] axisEnd = {
                {maxX, minY, minZ},
                {minX, maxY, minZ},
                {minX, minY, maxZ},
        };

        for (int a = 0; a < 3; a++) {
            axisPaint.setColor(axisColors[a]);
            float[] p1 = projectPoint(axisStart[a], cx, cy, cz, rangeX, rangeY, rangeZ, cosZ, sinZ, cosX, sinX, scale, marginLeft, marginTop);
            float[] p2 = projectPoint(axisEnd[a], cx, cy, cz, rangeX, rangeY, rangeZ, cosZ, sinZ, cosX, sinX, scale, marginLeft, marginTop);
            canvas.drawLine(p1[0], p1[1], p2[0], p2[1], axisPaint);
            canvas.drawText(axisLabels[a], p2[0] + 6, p2[1] - 6, labelPaint);
        }

        int numTicks = 4;
        for (int t = 0; t <= numTicks; t++) {
            double frac = (double) t / numTicks;

            double xVal = minX + frac * rangeX;
            float[] tp = projectPoint(new double[]{xVal, minY, minZ}, cx, cy, cz, rangeX, rangeY, rangeZ, cosZ, sinZ, cosX, sinX, scale, marginLeft, marginTop);
            canvas.drawLine(tp[0], tp[1] - 4, tp[0], tp[1] + 4, axisPaint);
            canvas.drawText(formatTick(xVal), tp[0] - 10, tp[1] + 16, tickPaint);

            double yVal = minY + frac * rangeY;
            float[] tp2 = projectPoint(new double[]{minX, yVal, minZ}, cx, cy, cz, rangeX, rangeY, rangeZ, cosZ, sinZ, cosX, sinX, scale, marginLeft, marginTop);
            canvas.drawLine(tp2[0] - 4, tp2[1], tp2[0] + 4, tp2[1], axisPaint);
            canvas.drawText(formatTick(yVal), tp2[0] - 20, tp2[1] + 14, tickPaint);

            double zVal = minZ + frac * rangeZ;
            float[] tp3 = projectPoint(new double[]{minX, minY, zVal}, cx, cy, cz, rangeX, rangeY, rangeZ, cosZ, sinZ, cosX, sinX, scale, marginLeft, marginTop);
            canvas.drawLine(tp3[0] - 4, tp3[1], tp3[0] + 4, tp3[1], axisPaint);
            canvas.drawText(formatTick(zVal), tp3[0] - 30, tp3[1] + 4, tickPaint);
        }
    }

    private static String formatTick(double val) {
        if (Math.abs(val) < 1e-10) return "0";
        if (Math.abs(val) >= 1000 || (Math.abs(val) < 0.01 && val != 0)) {
            return String.format("%.1e", val);
        }
        if (val == Math.floor(val)) return String.valueOf((int) val);
        return String.format("%.2f", val);
    }

    private static float[] projectPoint(double[] p, double cx, double cy, double cz,
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

    private static int[] detectGrid(List<double[]> points) {
        if (points.isEmpty()) return new int[]{0, 0};

        int n = points.size();

        double firstX = points.get(0)[0];
        double firstY = points.get(0)[1];

        int sameX = 1;
        for (int i = 1; i < n; i++) {
            if (Math.abs(points.get(i)[0] - firstX) < 1e-9) {
                sameX++;
            } else {
                break;
            }
        }

        int sameY = 1;
        for (int i = 1; i < n; i++) {
            if (Math.abs(points.get(i)[1] - firstY) < 1e-9) {
                sameY++;
            } else {
                break;
            }
        }

        if (sameX > 1 && n % sameX == 0) {
            return new int[]{n / sameX, sameX};
        }
        if (sameY > 1 && n % sameY == 0) {
            return new int[]{sameY, n / sameY};
        }

        int sq = (int) Math.sqrt(n);
        if (sq * sq == n) return new int[]{sq, sq};

        for (int r = sq; r >= 2; r--) {
            if (n % r == 0) return new int[]{r, n / r};
        }

        return new int[]{0, 0};
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
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
