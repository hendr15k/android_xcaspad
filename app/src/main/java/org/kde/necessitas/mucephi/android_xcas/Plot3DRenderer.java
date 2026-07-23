package org.kde.necessitas.mucephi.android_xcas;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;

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

            Paint bgPaint = new Paint();
            bgPaint.setShader(new LinearGradient(0, 0, 0, height,
                    Color.rgb(248, 250, 255), Color.rgb(225, 232, 245), Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, width, height, bgPaint);

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

            double scale = Math.min(width, height) * 0.32;

            float marginLeft = width * 0.46f;
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

            drawFloorGrid(canvas, minX, maxX, minY, maxY, minZ,
                    cx, cy, cz, rangeX, rangeY, rangeZ, cosZ, sinZ, cosX, sinX, scale, marginLeft, marginTop);

            List<int[]> quads = new ArrayList<>();
            double maxEdgeZ = rangeZ * 3.0;
            for (int i = 0; i < rows - 1; i++) {
                for (int j = 0; j < cols - 1; j++) {
                    int i0 = i * cols + j;
                    int i1 = i * cols + j + 1;
                    int i2 = (i + 1) * cols + j + 1;
                    int i3 = (i + 1) * cols + j;
                    if (i3 >= points.size()) continue;

                    double z0 = points.get(i0)[2], z1 = points.get(i1)[2];
                    double z2 = points.get(i2)[2], z3 = points.get(i3)[2];
                    if (Math.abs(z1 - z0) > maxEdgeZ || Math.abs(z3 - z0) > maxEdgeZ ||
                            Math.abs(z2 - z1) > maxEdgeZ || Math.abs(z2 - z3) > maxEdgeZ) {
                        continue;
                    }

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
            wirePaint.setStrokeWidth(0.4f);
            wirePaint.setAntiAlias(true);

            double lightX = -0.35, lightY = -0.55, lightZ = 0.75;
            double lightLen = Math.sqrt(lightX * lightX + lightY * lightY + lightZ * lightZ);
            lightX /= lightLen;
            lightY /= lightLen;
            lightZ /= lightLen;

            double viewX = 0, viewY = -1, viewZ = 0.5;
            double viewLen = Math.sqrt(viewX * viewX + viewY * viewY + viewZ * viewZ);
            viewX /= viewLen;
            viewY /= viewLen;
            viewZ /= viewLen;

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

                double hx = lightX + viewX, hy = lightY + viewY, hz = lightZ + viewZ;
                double hLen = Math.sqrt(hx * hx + hy * hy + hz * hz);
                double specular = 0;
                if (hLen > 1e-12) {
                    hx /= hLen;
                    hy /= hLen;
                    hz /= hLen;
                    double spec = Math.abs(nx * hx + ny * hy + nz * hz);
                    specular = Math.pow(spec, 32) * 0.4;
                }

                double ambient = 0.25;
                double intensity = ambient + 0.65 * diffuse + specular;
                intensity = Math.max(0, Math.min(1.2, intensity));

                double avgZ = (p0[2] + p1[2] + points.get(quad[2])[2] + p3[2]) / 4.0;
                double zNorm = (avgZ - minZ) / rangeZ;

                int[] rgb = colormap(zNorm);
                int r = clamp((int) (rgb[0] * intensity));
                int g = clamp((int) (rgb[1] * intensity));
                int b = clamp((int) (rgb[2] * intensity));

                if (specular > 0.05) {
                    int specAdd = (int) (specular * 200);
                    r = clamp(r + specAdd);
                    g = clamp(g + specAdd);
                    b = clamp(b + specAdd);
                }

                quadPaint.setColor(Color.rgb(r, g, b));
                wirePaint.setColor(Color.argb(60, 0, 0, 0));

                Path path = new Path();
                path.moveTo(projected[quad[0]][0], projected[quad[0]][1]);
                path.lineTo(projected[quad[1]][0], projected[quad[1]][1]);
                path.lineTo(projected[quad[2]][0], projected[quad[2]][1]);
                path.lineTo(projected[quad[3]][0], projected[quad[3]][1]);
                path.close();

                canvas.drawPath(path, quadPaint);
                canvas.drawPath(path, wirePaint);
            }

            drawAxes(canvas, minX, maxX, minY, maxY, minZ, maxZ,
                    cx, cy, cz, rangeX, rangeY, rangeZ, cosZ, sinZ, cosX, sinX, scale, marginLeft, marginTop, width, height);

            drawColorLegend(canvas, minZ, maxZ, width, height);

            return bitmap;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static int[] colormap(double t) {
        t = Math.max(0, Math.min(1, t));

        int[][] stops = {
                {30, 60, 160},
                {20, 140, 180},
                {30, 190, 130},
                {180, 210, 40},
                {240, 160, 30},
                {220, 50, 40},
        };

        double seg = t * (stops.length - 1);
        int idx = (int) seg;
        if (idx >= stops.length - 1) idx = stops.length - 2;
        double frac = seg - idx;

        int r = (int) (stops[idx][0] + (stops[idx + 1][0] - stops[idx][0]) * frac);
        int g = (int) (stops[idx][1] + (stops[idx + 1][1] - stops[idx][1]) * frac);
        int b = (int) (stops[idx][2] + (stops[idx + 1][2] - stops[idx][2]) * frac);
        return new int[]{r, g, b};
    }

    private static void drawFloorGrid(Canvas canvas,
                                      double minX, double maxX, double minY, double maxY, double minZ,
                                      double cx, double cy, double cz,
                                      double rangeX, double rangeY, double rangeZ,
                                      double cosZ, double sinZ, double cosX, double sinX,
                                      double scale, float marginLeft, float marginTop) {

        Paint gridPaint = new Paint();
        gridPaint.setColor(Color.argb(40, 100, 100, 120));
        gridPaint.setStrokeWidth(0.8f);
        gridPaint.setAntiAlias(true);

        int n = 6;
        for (int i = 0; i <= n; i++) {
            double frac = (double) i / n;
            double xVal = minX + frac * rangeX;
            float[] a = projectPoint(new double[]{xVal, minY, minZ}, cx, cy, cz, rangeX, rangeY, rangeZ, cosZ, sinZ, cosX, sinX, scale, marginLeft, marginTop);
            float[] b = projectPoint(new double[]{xVal, maxY, minZ}, cx, cy, cz, rangeX, rangeY, rangeZ, cosZ, sinZ, cosX, sinX, scale, marginLeft, marginTop);
            canvas.drawLine(a[0], a[1], b[0], b[1], gridPaint);

            double yVal = minY + frac * rangeY;
            float[] c = projectPoint(new double[]{minX, yVal, minZ}, cx, cy, cz, rangeX, rangeY, rangeZ, cosZ, sinZ, cosX, sinX, scale, marginLeft, marginTop);
            float[] d = projectPoint(new double[]{maxX, yVal, minZ}, cx, cy, cz, rangeX, rangeY, rangeZ, cosZ, sinZ, cosX, sinX, scale, marginLeft, marginTop);
            canvas.drawLine(c[0], c[1], d[0], d[1], gridPaint);
        }
    }

    private static void drawAxes(Canvas canvas,
                                 double minX, double maxX, double minY, double maxY, double minZ, double maxZ,
                                 double cx, double cy, double cz,
                                 double rangeX, double rangeY, double rangeZ,
                                 double cosZ, double sinZ, double cosX, double sinX,
                                 double scale, float marginLeft, float marginTop, int width, int height) {

        Paint axisPaint = new Paint();
        axisPaint.setStrokeWidth(2.5f);
        axisPaint.setAntiAlias(true);

        Paint labelPaint = new Paint();
        labelPaint.setColor(Color.rgb(50, 50, 50));
        labelPaint.setTextSize(Math.max(12, height / 26f));
        labelPaint.setAntiAlias(true);
        labelPaint.setFakeBoldText(true);

        Paint tickPaint = new Paint();
        tickPaint.setColor(Color.rgb(90, 90, 90));
        tickPaint.setTextSize(Math.max(9, height / 38f));
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

            float dx = p2[0] - p1[0];
            float dy = p2[1] - p1[1];
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len > 1) {
                dx /= len;
                dy /= len;
                float arrowSize = 8;
                Path arrow = new Path();
                arrow.moveTo(p2[0], p2[1]);
                arrow.lineTo(p2[0] - arrowSize * dx + arrowSize * 0.4f * dy,
                        p2[1] - arrowSize * dy - arrowSize * 0.4f * dx);
                arrow.lineTo(p2[0] - arrowSize * dx - arrowSize * 0.4f * dy,
                        p2[1] - arrowSize * dy + arrowSize * 0.4f * dx);
                arrow.close();
                canvas.drawPath(arrow, axisPaint);
            }

            canvas.drawText(axisLabels[a], p2[0] + 8, p2[1] - 8, labelPaint);
        }

        int numTicks = 4;
        for (int t = 0; t <= numTicks; t++) {
            double frac = (double) t / numTicks;

            double xVal = minX + frac * rangeX;
            float[] tp = projectPoint(new double[]{xVal, minY, minZ}, cx, cy, cz, rangeX, rangeY, rangeZ, cosZ, sinZ, cosX, sinX, scale, marginLeft, marginTop);
            axisPaint.setColor(axisColors[0]);
            canvas.drawLine(tp[0], tp[1] - 4, tp[0], tp[1] + 4, axisPaint);
            canvas.drawText(formatTick(xVal), tp[0] - 10, tp[1] + 16, tickPaint);

            double yVal = minY + frac * rangeY;
            float[] tp2 = projectPoint(new double[]{minX, yVal, minZ}, cx, cy, cz, rangeX, rangeY, rangeZ, cosZ, sinZ, cosX, sinX, scale, marginLeft, marginTop);
            axisPaint.setColor(axisColors[1]);
            canvas.drawLine(tp2[0] - 4, tp2[1], tp2[0] + 4, tp2[1], axisPaint);
            canvas.drawText(formatTick(yVal), tp2[0] - 22, tp2[1] + 14, tickPaint);

            double zVal = minZ + frac * rangeZ;
            float[] tp3 = projectPoint(new double[]{minX, minY, zVal}, cx, cy, cz, rangeX, rangeY, rangeZ, cosZ, sinZ, cosX, sinX, scale, marginLeft, marginTop);
            axisPaint.setColor(axisColors[2]);
            canvas.drawLine(tp3[0] - 4, tp3[1], tp3[0] + 4, tp3[1], axisPaint);
            canvas.drawText(formatTick(zVal), tp3[0] - 34, tp3[1] + 4, tickPaint);
        }
    }

    private static void drawColorLegend(Canvas canvas, double minZ, double maxZ, int width, int height) {
        float legendX = width - 30;
        float legendTop = height * 0.15f;
        float legendBottom = height * 0.85f;
        float legendWidth = 14;

        Paint legendPaint = new Paint();
        legendPaint.setAntiAlias(true);

        int steps = 64;
        float stepH = (legendBottom - legendTop) / steps;
        for (int i = 0; i < steps; i++) {
            double t = 1.0 - (double) i / (steps - 1);
            int[] rgb = colormap(t);
            legendPaint.setColor(Color.rgb(rgb[0], rgb[1], rgb[2]));
            canvas.drawRect(legendX, legendTop + i * stepH, legendX + legendWidth, legendTop + (i + 1) * stepH + 1, legendPaint);
        }

        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.rgb(120, 120, 120));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1);
        canvas.drawRect(legendX, legendTop, legendX + legendWidth, legendBottom, borderPaint);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.rgb(60, 60, 60));
        textPaint.setTextSize(Math.max(9, height / 40f));
        textPaint.setAntiAlias(true);

        canvas.drawText(formatTick(maxZ), legendX - 4, legendTop - 4, textPaint);
        canvas.drawText(formatTick(minZ), legendX - 4, legendBottom + 14, textPaint);
        double midZ = (minZ + maxZ) / 2;
        canvas.drawText(formatTick(midZ), legendX - 4, (legendTop + legendBottom) / 2 + 4, textPaint);
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
