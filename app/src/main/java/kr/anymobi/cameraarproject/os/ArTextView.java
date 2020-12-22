package kr.anymobi.cameraarproject.os;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

/**
 * Text를 표현하는 class 이다.
 */
public class ArTextView extends ArView {

    private String mText;
    private Paint mPaint;

    public ArTextView() {
        mText = "";
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setTextSize(40);
    }

    /**
     * text를 설정한다.
     * @param text
     */
    public void setText(String text) {
        mText = text;
    }

    /**
     * text color를 설정한다.
     * @param color
     */
    public void setTextColor(int color) {
        mPaint.setColor(color);
    }

    /**
     * text typeface를 설정한다.
     * @param typeface
     */
    public void setTypeface(Typeface typeface) {
        mPaint.setTypeface(typeface);
    }

    /**
     * text size를 설정한다.
     * @param textSize
     */
    public void setTextSize(int textSize) {
        mPaint.setTextSize(textSize);
    }

    @Override
    protected void onDraw(ArCanvas arCanvas) {
        super.onDraw(arCanvas);

        Canvas canvas = arCanvas.canvas;
        canvas.drawText(mText, 0, 0, mPaint);
    }
}
