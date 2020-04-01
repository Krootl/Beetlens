package com.krootl.beetlens.ui.common.gl;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.FrameLayout;

public class GLFrameLayout extends FrameLayout implements GLRenderable {

    private ViewToGLRenderer mViewToGLRenderer;

    public GLFrameLayout(Context context) {
        super(context);
    }

    public GLFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public GLFrameLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void draw(Canvas canvas) {
        if (mViewToGLRenderer != null) {
            Canvas glAttachedCanvas = mViewToGLRenderer.onDrawViewBegin();
            if (glAttachedCanvas != null) {
                // pre-scale canvas to make sure content fits
                @SuppressLint("CanvasSize") float xScale = glAttachedCanvas.getWidth() / (float) canvas.getWidth();
                glAttachedCanvas.scale(xScale, xScale);
                // draw the view to provided canvas
                super.draw(glAttachedCanvas);
            }
            // notify the canvas is updated
            mViewToGLRenderer.onDrawViewEnd();
        }
        super.draw(canvas);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        return false;
    }

    public void setViewToGLRenderer(ViewToGLRenderer viewToGLRenderer) {
        mViewToGLRenderer = viewToGLRenderer;
        invalidate();
    }
}
