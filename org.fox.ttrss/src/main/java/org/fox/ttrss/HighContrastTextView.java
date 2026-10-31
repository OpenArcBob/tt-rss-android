package org.fox.ttrss;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.BackgroundColorSpan;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textview.MaterialTextView;

/**
 * A TextView that unconditionally forces Samsung High Contrast Yellow (#FFEB3B)
 * on a solid #0D0D0D background, completely eliminating system-injected black outlines,
 * shadows, or background spans directly under text.
 */
public class HighContrastTextView extends MaterialTextView {

    public static final int HIGH_CONTRAST_YELLOW = 0xFFFFEB3B;
    public static final int HIGH_CONTRAST_BG = 0xFF0D0D0D;

    private final Path mTextPath = new Path();
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
        super.setTextColor(ColorStateList.valueOf(HIGH_CONTRAST_YELLOW));
        super.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL));
        super.setBackgroundColor(HIGH_CONTRAST_BG);
        super.setShadowLayer(0, 0, 0, 0);
        super.setHighlightColor(0x66FFEB3B);
    }

    @Override
    public void setShadowLayer(float radius, float dx, float dy, int color) {
        super.setShadowLayer(0, 0, 0, 0);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        if (text instanceof Spanned) {
            Spannable spannable = (text instanceof Spannable) ? (Spannable) text : new SpannableString(text);
            BackgroundColorSpan[] bgSpans = spannable.getSpans(0, spannable.length(), BackgroundColorSpan.class);
            for (BackgroundColorSpan span : bgSpans) {
                spannable.removeSpan(span);
            }
            text = spannable;
        }
        super.setText(text, type);
    }

    @Override
    public void setBackgroundColor(int color) {
        super.setBackgroundColor(HIGH_CONTRAST_BG);
    }

    @Override
    public void setBackground(Drawable background) {
        super.setBackground(new ColorDrawable(HIGH_CONTRAST_BG));
    }

    @Override
    public void setBackgroundDrawable(Drawable background) {
        super.setBackgroundDrawable(new ColorDrawable(HIGH_CONTRAST_BG));
    }

    @Override
    public void setBackgroundResource(int resid) {
        super.setBackgroundColor(HIGH_CONTRAST_BG);
    }

    @Override
    public void setBackgroundTintList(ColorStateList tint) {
        super.setBackgroundTintList(ColorStateList.valueOf(HIGH_CONTRAST_BG));
    }

    @Override
    public void setTypeface(@Nullable Typeface tf, int style) {
        super.setTypeface(Typeface.create(Typeface.SANS_SERIF, style), style);
    }

    @Override
    public void setTypeface(@Nullable Typeface tf) {
        int style = tf != null ? tf.getStyle() : Typeface.NORMAL;
        super.setTypeface(Typeface.create(Typeface.SANS_SERIF, style));
    }

    @Override
    public void setTextColor(int color) {
        super.setTextColor(HIGH_CONTRAST_YELLOW);
    }

    @Override
    public void setTextColor(ColorStateList colors) {
        super.setTextColor(ColorStateList.valueOf(HIGH_CONTRAST_YELLOW));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(HIGH_CONTRAST_BG);
        super.setTextColor(ColorStateList.valueOf(HIGH_CONTRAST_YELLOW));

        if (getWidth() <= 0 || getHeight() <= 0) {
            super.onDraw(canvas);
            return;
        }

        Layout layout = getLayout();
        if (layout != null) {
            try {
                TextPaint paint = getPaint();
                paint.setColor(HIGH_CONTRAST_YELLOW);
                paint.setStyle(Paint.Style.FILL);
                paint.clearShadowLayer();
                paint.bgColor = HIGH_CONTRAST_BG;
                paint.setShader(null);
                paint.setColorFilter(null);
                paint.setMaskFilter(null);
                paint.setTypeface(Typeface.create(Typeface.SANS_SERIF, paint.getTypeface() != null ? paint.getTypeface().getStyle() : Typeface.NORMAL));

                int saveCount = canvas.save();
                canvas.translate(getCompoundPaddingLeft(), getExtendedPaddingTop());

                CharSequence text = layout.getText();
                String textStr = text != null ? text.toString() : "";
                int lineCount = layout.getLineCount();

                for (int i = 0; i < lineCount; i++) {
                    int start = layout.getLineStart(i);
                    int end = layout.getLineVisibleEnd(i);
                    int ellipsisCount = layout.getEllipsisCount(i);

                    float x = layout.getLineLeft(i);
                    float baseline = layout.getLineBaseline(i);

                    try {
                        if (ellipsisCount > 0) {
                            int ellipsisStart = layout.getEllipsisStart(i);
                            int visibleEnd = start + ellipsisStart;
                            if (visibleEnd > start && visibleEnd <= textStr.length()) {
                                mTextPath.reset();
                                paint.getTextPath(textStr, start, visibleEnd, x, baseline, mTextPath);
                                canvas.drawPath(mTextPath, paint);
                            }
                            float ellipsisX = x + (visibleEnd > start ? layout.getPrimaryHorizontal(visibleEnd) - x : 0);
                            mTextPath.reset();
                            paint.getTextPath("\u2026", 0, 1, ellipsisX, baseline, mTextPath);
                            canvas.drawPath(mTextPath, paint);
                        } else if (end > start && end <= textStr.length()) {
                            mTextPath.reset();
                            paint.getTextPath(textStr, start, end, x, baseline, mTextPath);
                            canvas.drawPath(mTextPath, paint);
                        }
                    } catch (Throwable lineError) {
                        paint.clearShadowLayer();
                        paint.bgColor = HIGH_CONTRAST_BG;
                        if (end > start && end <= textStr.length()) {
                            canvas.drawText(textStr, start, end, x, baseline, paint);
                        }
                    }
                }

                canvas.restoreToCount(saveCount);
                return;
            } catch (Throwable ignored) {
            }
        }

        // Fallback: Offscreen software bitmap rendering
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

            mBitmap.eraseColor(HIGH_CONTRAST_BG);
            getPaint().clearShadowLayer();
            getPaint().bgColor = HIGH_CONTRAST_BG;

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
