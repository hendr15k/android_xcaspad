package org.kde.necessitas.mucephi.android_xcas;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Interactive 3D surface plot. Drag with one finger to rotate the surface and
 * pinch with two fingers to zoom. The rendered bitmap is cached and only
 * recomputed when the view size or the viewing parameters change.
 */
public class Plot3DView extends View {

    private static final float MIN_ANGLE_X = 5f;
    private static final float MAX_ANGLE_X = 85f;
    private static final float MIN_ZOOM = 0.5f;
    private static final float MAX_ZOOM = 4f;

    private Plot3DRenderer.Plot3DData data;

    private float angleX = 25f;
    private float angleZ = 40f;
    private float zoom = 1f;

    private Bitmap cachedBitmap;
    private int cacheW = -1;
    private int cacheH = -1;
    private float cacheAngleX = Float.NaN;
    private float cacheAngleZ = Float.NaN;
    private float cacheZoom = Float.NaN;

    private float lastX;
    private float lastY;
    private float startDist;
    private float startZoom;
    private float startAngleX;
    private float startAngleZ;

    public Plot3DView(Context context) {
        this(context, null);
    }

    public Plot3DView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Plot3DView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setFocusable(true);
    }

    public void setPlotData(Plot3DRenderer.Plot3DData data) {
        this.data = data;
        angleX = 25f;
        angleZ = 40f;
        zoom = 1f;
        invalidatePlot();
    }

    public Plot3DRenderer.Plot3DData getPlotData() {
        return data;
    }

    public void resetView() {
        angleX = 25f;
        angleZ = 40f;
        zoom = 1f;
        invalidatePlot();
    }

    private void invalidatePlot() {
        cachedBitmap = null;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        if (width == 0) {
            width = getSuggestedMinimumWidth();
        }
        setMeasuredDimension(width, width);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (data == null) {
            return;
        }
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }

        if (cachedBitmap == null || cacheW != w || cacheH != h
                || cacheAngleX != angleX || cacheAngleZ != angleZ || cacheZoom != zoom) {
            cachedBitmap = Plot3DRenderer.render(data, w, h, angleX, angleZ, zoom);
            cacheW = w;
            cacheH = h;
            cacheAngleX = angleX;
            cacheAngleZ = angleZ;
            cacheZoom = zoom;
        }

        if (cachedBitmap != null) {
            canvas.drawBitmap(cachedBitmap, 0, 0, null);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (data == null) {
            return super.onTouchEvent(event);
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = event.getX();
                lastY = event.getY();
                return true;

            case MotionEvent.ACTION_POINTER_DOWN:
                startDist = distance(event);
                startZoom = zoom;
                startAngleX = angleX;
                startAngleZ = angleZ;
                return true;

            case MotionEvent.ACTION_MOVE:
                if (event.getPointerCount() >= 2) {
                    float dist = distance(event);
                    if (startDist > 0) {
                        zoom = clamp(startZoom * dist / startDist, MIN_ZOOM, MAX_ZOOM);
                    }
                } else {
                    float dx = event.getX() - lastX;
                    float dy = event.getY() - lastY;
                    angleZ -= dx * 0.5f;
                    angleX = clamp(angleX + dy * 0.5f, MIN_ANGLE_X, MAX_ANGLE_X);
                }
                lastX = event.getX();
                lastY = event.getY();
                invalidatePlot();
                return true;

            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_UP:
                lastX = event.getX();
                lastY = event.getY();
                return true;

            default:
                return super.onTouchEvent(event);
        }
    }

    private static float distance(MotionEvent event) {
        if (event.getPointerCount() < 2) {
            return 0f;
        }
        float dx = event.getX(0) - event.getX(1);
        float dy = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
