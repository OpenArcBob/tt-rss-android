package org.fox.ttrss;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.google.android.material.textview.MaterialTextView;

/**
 * A TextView that bypasses system-level High Contrast Text (which forcefully overrides
 * TextView font colors to white with black outline in hardware-accelerated HWUI rendering)
 * by rendering its layout into a software bitmap buffer before drawing to the canvas.
 * This guarantees the exact text color (e.g. Samsung High Contrast Yellow #FFFF00) is preserved.
 */
public class HighContrastTextView extends MaterialTextView {

    private Bitmap mBitmap;
    private Canvas mBitmapCanvas;

    public HighContrastTextView(@NonNull Context context) {
        super(context);
    }

    public HighContrastTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public HighContrastTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private boolean isHighTextContrastActive() {
        try {
            AccessibilityManager am = (AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
            if (am != null) {
                java.lang.reflect.Method method = am.getClass().getMethod("isHighTextContrastEnabled");
                Object result = method.invoke(am);
                if (Boolean.TRUE.equals(result)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            if (Settings.Secure.getInt(getContext().getContentResolver(), "high_text_contrast_enabled", 0) == 1) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        try {
            if (Settings.Secure.getInt(getContext().getContentResolver(), "accessibility_high_text_contrast_enabled", 0) == 1) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private boolean isHighContrastThemeActive() {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            String theme = prefs.getString("theme", "");
            return CommonActivity.THEME_YELLOWBLACK.equals(theme) || "yellowblack".equalsIgnoreCase(theme);
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean shouldBypass() {
        return isHighTextContrastActive() || isHighContrastThemeActive();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (!shouldBypass() || !canvas.isHardwareAccelerated() || getWidth() <= 0 || getHeight() <= 0) {
            super.onDraw(canvas);
            return;
        }

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
