package org.fox.ttrss;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textview.MaterialTextView;

/**
 * A TextView that bypasses system-level High Contrast Text (which forcefully overrides
 * TextView font colors to white with black outline in hardware-accelerated HWUI rendering)
 * by forcing software-layer rendering and offscreen bitmap rasterization.
 * This guarantees the exact text color (e.g. Samsung High Contrast Yellow #FFFF00) is preserved.
 */
public class HighContrastTextView extends MaterialTextView {

    private Bitmap mBitmap;
    private Canvas mBitmapCanvas;

    public HighContrastTextView(@NonNull Context context) {
        super(context);
        init();
    }

    public HighContrastTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HighContrastTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // If rendered via software layer (the default due to setLayerType),
        // canvas.isHardwareAccelerated() is false and Skia CPU preserves pure colors.
        if (!canvas.isHardwareAccelerated() || getWidth() <= 0 || getHeight() <= 0) {
            super.onDraw(canvas);
            return;
        }

        // If hardware acceleration is forced by parent/OEM, draw via offscreen software bitmap
        // because HWUI never alters bitmap pixels with high contrast text overrides.
        try {
            int width = getWidth();
            int height = getHeight();

            if (mBitmap == null || mBitmap.getWidth() != width || mBitmap.getHeight() != height) {
                if (mBitmap != null) {
                    mBitmap.recycle();
                }
                mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                mBitmapCanvas = new Canvas(mBitmap);
            }

            mBitmap.eraseColor(Color.TRANSPARENT);

            int scrollX = getScrollX();
            int scrollY = getScrollY();
            if (scrollX != 0 || scrollY != 0) {
                mBitmapCanvas.save();
                mBitmapCanvas.translate(-scrollX, -scrollY);
                super.onDraw(mBitmapCanvas);
                mBitmapCanvas.restore();
                canvas.drawBitmap(mBitmap, scrollX, scrollY, null);
            } else {
                super.onDraw(mBitmapCanvas);
                canvas.drawBitmap(mBitmap, 0, 0, null);
            }
        } catch (Throwable t) {
            super.onDraw(canvas);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mBitmap != null) {
            mBitmap.recycle();
            mBitmap = null;
            mBitmapCanvas = null;
        }
    }
}
