/*
 *  Copyright (C) 2011 Leonel Hernández Sandoval.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.kde.necessitas.mucephi.android_xcas;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * ImageView that supports pinch-to-zoom, panning and double-tap-to-toggle
 * between fit-screen and a 2.5x zoom focused on the tapped point.
 * <p>
 * Implemented directly with {@link Matrix} so we avoid an extra dependency
 * (PhotoView). Supports super-long images by scaling up to 6x and capping
 * translation so users cannot pan the bitmap off-screen entirely.
 */
public class ZoomableImageView extends AppCompatImageView {

    private static final float MIN_SCALE = 1.0f;
    private static final float MAX_SCALE = 6.0f;
    private static final float DOUBLE_TAP_ZOOM = 2.5f;

    private final Matrix matrix = new Matrix();
    private final float[] matrixValues = new float[9];

    private float currentScale = 1f;
    private float startScale = 1f;

    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;

    private PointF lastTouch = new PointF();
    private boolean isDragging = false;

    public ZoomableImageView(Context context) {
        this(context, null);
    }

    public ZoomableImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZoomableImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        super.setScaleType(ScaleType.MATRIX);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
        setClickable(true);
        setFocusable(true);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        gestureDetector.onTouchEvent(event);

        if (scaleDetector.isInProgress()) {
            isDragging = false;
            return true;
        }

        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                lastTouch.set(event.getX(), event.getY());
                isDragging = true;
                getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_MOVE:
                if (isDragging && currentScale > MIN_SCALE) {
                    float dx = event.getX() - lastTouch.x;
                    float dy = event.getY() - lastTouch.y;
                    panBy(dx, dy);
                    lastTouch.set(event.getX(), event.getY());
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        fitToView();
    }

    @Override
    public void setImageDrawable(android.graphics.drawable.Drawable drawable) {
        super.setImageDrawable(drawable);
        post(this::fitToView);
    }

    @Override
    public void setImageBitmap(android.graphics.Bitmap bm) {
        super.setImageBitmap(bm);
        post(this::fitToView);
    }

    public android.graphics.Bitmap getRenderedBitmap() {
        android.graphics.drawable.Drawable d = getDrawable();
        if (d == null || !(d instanceof android.graphics.drawable.BitmapDrawable)) {
            return null;
        }
        return ((android.graphics.drawable.BitmapDrawable) d).getBitmap();
    }

    public void resetZoom() {
        currentScale = 1f;
        fitToView();
        invalidate();
    }

    private void fitToView() {
        android.graphics.drawable.Drawable d = getDrawable();
        if (d == null || getWidth() == 0 || getHeight() == 0) {
            return;
        }
        int viewW = getWidth();
        int viewH = getHeight();
        int drawW = d.getIntrinsicWidth();
        int drawH = d.getIntrinsicHeight();
        if (drawW <= 0 || drawH <= 0) {
            return;
        }

        float scale = Math.min((float) viewW / drawW, (float) viewH / drawH);
        matrix.reset();
        matrix.postScale(scale, scale);
        float tx = (viewW - drawW * scale) / 2f;
        float ty = (viewH - drawH * scale) / 2f;
        matrix.postTranslate(tx, ty);
        startScale = scale;
        currentScale = 1f;
        clampTranslation();
        setImageMatrix(matrix);
    }

    private void panBy(float dx, float dy) {
        matrix.postTranslate(dx, dy);
        clampTranslation();
        setImageMatrix(matrix);
    }

    private void clampTranslation() {
        if (getDrawable() == null) {
            return;
        }
        matrix.getValues(matrixValues);
        float transX = matrixValues[Matrix.MTRANS_X];
        float transY = matrixValues[Matrix.MTRANS_Y];
        float scale = currentScale * startScale;

        int drawW = (int) (getDrawable().getIntrinsicWidth() * scale);
        int drawH = (int) (getDrawable().getIntrinsicHeight() * scale);
        int viewW = getWidth();
        int viewH = getHeight();

        float fixTransX;
        float fixTransY;
        if (drawW <= viewW) {
            fixTransX = (viewW - drawW) / 2f - matrixValues[Matrix.MTRANS_X];
        } else {
            if (matrixValues[Matrix.MTRANS_X] > 0) {
                fixTransX = -transX;
            } else if (transX < viewW - drawW) {
                fixTransX = viewW - drawW - transX;
            } else {
                fixTransX = 0;
            }
        }
        if (drawH <= viewH) {
            fixTransY = (viewH - drawH) / 2f - matrixValues[Matrix.MTRANS_Y];
        } else {
            if (matrixValues[Matrix.MTRANS_Y] > 0) {
                fixTransY = -transY;
            } else if (transY < viewH - drawH) {
                fixTransY = viewH - drawH - transY;
            } else {
                fixTransY = 0;
            }
        }
        if (fixTransX != 0 || fixTransY != 0) {
            matrix.postTranslate(fixTransX, fixTransY);
        }
    }

    private float getScale() {
        matrix.getValues(matrixValues);
        return matrixValues[Matrix.MSCALE_X];
    }

    private void zoomTo(float targetScale, float focusX, float focusY) {
        float current = getScale();
        float factor = targetScale / current;
        matrix.postScale(factor, factor, focusX, focusY);
        clampTranslation();
        setImageMatrix(matrix);
        currentScale = targetScale / startScale;
    }

    private final class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float current = getScale();
            float factor = detector.getScaleFactor();
            float next = Math.max(MIN_SCALE * startScale,
                    Math.min(current * factor, MAX_SCALE * startScale));
            zoomTo(next, detector.getFocusX(), detector.getFocusY());
            return true;
        }
    }

    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            performClick();
            float current = getScale();
            if (current < DOUBLE_TAP_ZOOM * startScale * 0.9f) {
                zoomTo(DOUBLE_TAP_ZOOM * startScale, e.getX(), e.getY());
            } else {
                zoomTo(startScale, getWidth() / 2f, getHeight() / 2f);
            }
            return true;
        }
    }

    @SuppressWarnings("unused")
    private RectF displayRect() {
        RectF rect = new RectF();
        if (getDrawable() != null) {
            rect.set(0, 0, getDrawable().getIntrinsicWidth(), getDrawable().getIntrinsicHeight());
            matrix.mapRect(rect);
        }
        return rect;
    }
}