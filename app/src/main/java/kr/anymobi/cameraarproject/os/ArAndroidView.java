package kr.anymobi.cameraarproject.os;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * AR이 동작하게되는 Android 기본 View 이다.
 * {@link #setRootView(ArView)} 를 통해 등록된 ArView 가 이 View에서 렌더링되게 된다.
 */
public class ArAndroidView extends View {

    private Bitmap mPickerBitmap;
    private Canvas mPickerCanvas;
    private ArCanvas mArCanvas;

    private ArView mPickedView;

    private ArView mRootView;

    public ArAndroidView(Context context) {
        this(context, null);
    }

    public ArAndroidView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ArAndroidView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        initView();
    }

    private void initView() {
        mPickerBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        mPickerBitmap.setDensity(Bitmap.DENSITY_NONE);
        mPickerCanvas = new Canvas(mPickerBitmap);

        mArCanvas = new ArCanvas();
    }

    /**
     * Root ArView 를 등록한다. 등록된 ArView는 ar engine 에 의해 계속해서 동작하게 된다.
     * @param rootView 기본이 되는 ArView
     */
    public void setRootView(ArView rootView) {
        mRootView = rootView;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = Math.round(event.getX());
        int y = Math.round(event.getY());

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return handleTouchDown(x, y);
            case MotionEvent.ACTION_MOVE:
                return handleTouchMove(x, y);
            case MotionEvent.ACTION_UP:
                return handleTouchUp(x, y);
            case MotionEvent.ACTION_CANCEL:
                return handleTouchCancel(x, y);
        }
        return false;
    }

    private boolean handleTouchDown(int x, int y) {
        if (mRootView == null) {
            return false;
        }

        mPickerCanvas.drawColor(Color.BLACK);

        mArCanvas.mode = ArCanvas.RenderMode.PICK;
        mArCanvas.pickId = 0;

        mArCanvas.canvas = mPickerCanvas;
        update(mArCanvas, x, y);
        mArCanvas.canvas = null;

        int pickId = mPickerBitmap.getPixel(0, 0) - 0xFF000000;
        if (pickId > 0) {
            mPickedView = mRootView.findViewByPickId(pickId);
        }

        return mPickedView != null;
    }

    private boolean handleTouchMove(int x, int y) {
        return mPickedView != null;
    }

    private boolean handleTouchUp(int x, int y) {
        if (mPickedView != null) {
            mPickedView.performClick();
            mPickedView = null;
        }
        return true;
    }

    private boolean handleTouchCancel(int x, int y) {
        mPickedView = null;
        return true;
    }

    @Override
    public void onDraw(Canvas canvas) {
        update(canvas);
        postInvalidate();
    }

    private void update(Canvas canvas) {
        mArCanvas.mode = ArCanvas.RenderMode.DRAW;

        mArCanvas.canvas = canvas;
        update(mArCanvas, 0, 0);
        mArCanvas.canvas = null;
    }

    private void update(ArCanvas arCanvas, int left, int top) {
        if (mRootView == null) {
            return;
        }

        Canvas canvas = arCanvas.canvas;

        int width = mRootView.getWidth();
        int height = mRootView.getHeight();

        int surfaceWidth = getWidth();
        int surfaceHeight = getHeight();

        arCanvas.drawingTime = SystemClock.uptimeMillis();

        int saveCount = canvas.save();

        canvas.scale(((float) surfaceWidth) / width, ((float) surfaceHeight) / height, surfaceWidth * 0.5f - left, surfaceHeight * 0.5f - top);
        canvas.translate(surfaceWidth * 0.5f - left, surfaceHeight * 0.5f - top);

        mRootView.draw(arCanvas);

        canvas.restoreToCount(saveCount);
    }

    public void destroy() {
        if (mRootView != null) {
            mRootView.destroy();
            mRootView = null;
        }

        if (mPickerBitmap != null) {
            mPickerBitmap.recycle();
            mPickerBitmap = null;
        }

        mPickerCanvas = null;
    }
}
