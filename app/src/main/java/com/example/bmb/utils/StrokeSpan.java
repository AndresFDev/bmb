package com.example.bmb.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.UpdateAppearance;

public class StrokeSpan extends CharacterStyle implements UpdateAppearance {
    private final int strokeColor;
    private final float strokeWidth;
    private final int fillColor;

    public StrokeSpan(int strokeColor, float strokeWidth, int fillColor) {
        this.strokeColor = strokeColor;
        this.strokeWidth = strokeWidth;
        this.fillColor = fillColor;
    }

    public static StrokeSpan createFromTheme(Context context, int styleResId, float strokeWidth) {
        TypedArray a = context.obtainStyledAttributes(styleResId, new int[]{android.R.attr.strokeColor});
        int strokeColor = a.getColor(0, 0);
        a.recycle();

        int[] attrs = new int[]{android.R.attr.textColor};
        TypedArray ta = context.obtainStyledAttributes(styleResId, attrs);
        int fillColor = ta.getColor(0, 0);
        ta.recycle();

        return new StrokeSpan(strokeColor, strokeWidth, fillColor);
    }

    @Override
    public void updateDrawState(TextPaint textPaint) {
        textPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        textPaint.setStrokeWidth(strokeWidth);
        textPaint.setColor(fillColor);
        textPaint.setFlags(Paint.ANTI_ALIAS_FLAG);

        textPaint.setShadowLayer(strokeWidth, 0, 0, strokeColor);
    }
}