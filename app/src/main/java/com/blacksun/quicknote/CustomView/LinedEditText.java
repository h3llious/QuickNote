package com.blacksun.quicknote.CustomView;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.ContextCompat;

import com.blacksun.quicknote.R;

public class LinedEditText extends AppCompatEditText {
    private Rect mRect;
    private Paint mPaint;

    public LinedEditText(Context context) {
        super(context);
        init(context);
    }


    public LinedEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LinedEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mRect = new Rect();
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        int mColor = ContextCompat.getColor(context, R.color.gray);
        mPaint.setColor(mColor);
    }

    @Override
    protected void onDraw(Canvas canvas) {
//        int count = getLineCount();
        int lineHeight = getLineHeight();
        int height = getHeight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();
        int count = (height - paddingBottom - paddingTop) / lineHeight;

        int baseRect = getLineBounds(0, mRect);
        for (int i = 0; i< count; i++) {
            int baseline = lineHeight * (i+1) + paddingTop;
            canvas.drawLine(mRect.left, baseline + 2, mRect.right, baseline + 2, mPaint);
        }

        super.onDraw(canvas);
    }
}
