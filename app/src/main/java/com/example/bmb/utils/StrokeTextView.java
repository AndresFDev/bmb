package com.example.bmb.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.UpdateAppearance;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.example.bmb.R;

public class StrokeTextView extends AppCompatTextView {
    private final int strokeColor;
    private final float strokeWidth;
    private final int fillColor;
    private Typeface typeface;

    public StrokeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.StrokeTextView, 0, 0);
        try {
            strokeColor = a.getColor(R.styleable.StrokeTextView_strokeColor, getColorFromTheme(context, com.google.android.material.R.attr.colorSurface));
            strokeWidth = a.getDimension(R.styleable.StrokeTextView_strokeWidth, 8f);
            fillColor = a.getColor(R.styleable.StrokeTextView_fillColor, getColorFromTheme(context, androidx.appcompat.R.attr.colorPrimary));

            typeface = ResourcesCompat.getFont(context, R.font.product_sans_black);
            setTypeface(typeface);
        } finally {
            a.recycle();
        }
    }

    private int getColorFromTheme(Context context, int attr) {
        TypedArray a = context.getTheme().obtainStyledAttributes(new int[] { attr });
        int color = a.getColor(0, 0);
        a.recycle();
        return color;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Paint strokePaint = new Paint(getPaint());
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(strokeWidth);
        strokePaint.setColor(strokeColor);
        strokePaint.setAntiAlias(true);
        strokePaint.setTypeface(typeface);

        drawTextWithPaint(canvas, strokePaint);

        Paint fillPaint = new Paint(getPaint());
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(fillColor);
        fillPaint.setAntiAlias(true);
        strokePaint.setTypeface(typeface);

        drawTextWithPaint(canvas, fillPaint);
    }

    private void drawTextWithPaint(Canvas canvas, Paint paint) {
        String text = getText().toString();
        paint.setTextSize(getTextSize());

        float textWidth = paint.measureText(text);
        float textHeight = paint.descent() - paint.ascent();
        float x = (getWidth() - textWidth) / 2;
        float y = (getHeight() - textHeight) / 2 - paint.ascent();

        canvas.drawText(text, x, y, paint);
    }

    @Override
    public void setTypeface(Typeface tf) {
        super.setTypeface(tf != null ? tf : typeface);
    }
}