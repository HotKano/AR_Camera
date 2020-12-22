package kr.anymobi.cameraarproject.os;

import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <code>ArView</code>는 Ar 구조의 가장 기본이 되는 뷰이다.
 */
public class ArView {

    private int mId;
    private int mPickId;
    private Map<String, Object> mTag;
    private ArView mParent;

    protected int mWidth;
    protected int mHeight;
    protected ArVector mPosition;
    protected ArVector mRotation;
    protected ArVector mScale;

    protected boolean mVisible;
    protected boolean mPickable;

    protected ArBoolean mFixedRotation;
    protected ArBoolean mFixedPosition;

    private Paint mPaint;
    private Drawable mDrawable;
    private Transformation mAnimationTransformation;
    private Animation mAnimation;

    private boolean mIsDrawingCacheEnabled;
    private Bitmap mDrawingCache;

    private Matrix mCameraMatrix;

    public interface ArOnClickListener {
        void onClick(ArView view);
    }

    private ArOnClickListener mOnClickListener;

    public ArView() {
        mId = -1;
        mTag = new ConcurrentHashMap<>();
        mParent = null;

        mWidth = 1;
        mHeight = 1;
        mPosition = new ArVector(0, 0, 0);
        mRotation = new ArVector(0, 0, 0);
        mScale = new ArVector(1, 1, 1);
        mVisible = true;
        mPickable = false;

        mFixedRotation = new ArBoolean();
        mFixedPosition = new ArBoolean();

        mPaint = new Paint();
        mDrawable = null;
        mAnimationTransformation = new Transformation();

        mCameraMatrix = new Matrix();
    }

    /**
     * 뷰에 사용된 메모리를 명시적으로 제거할때 사용한다.
     */
    public void destroy() {
        dispatchDestroy();
    }

    /**
     * 뷰의 명시적제거를 수행하는 함수이다.
     * 자식뷰에서는 이를 상속받아 원하는 동작을 수행해야한다.
     */
    protected void dispatchDestroy() {
        onDestroy();
    }

    /**
     * 임시로 사용되는 메모리만 제거해야한다.
     */
    protected void onDestroy() {
        destroyDrawingCache();
    }

    /**
     * drawing cache를 제거한다.
     */
    public void destroyDrawingCache() {
        if (mDrawingCache != null) {
            mDrawingCache.recycle();
            mDrawingCache = null;
        }
    }

    /**
     * drawing cache를 생성한다.
     */
    public void buildDrawingCache() {
        if (mIsDrawingCacheEnabled) {
            destroyDrawingCache();

            ArCanvas arCanvas = new ArCanvas();

            mDrawingCache = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            arCanvas.canvas = new Canvas(mDrawingCache);

            arCanvas.canvas.translate(getWidth() * 0.5f, getHeight() * 0.5f);
            dispatchDraw(arCanvas);
        }
    }

    /**
     * drawing cache 상태를 변경한다.
     * drawing cache가 true인 경우, {@link #buildDrawingCache()}를 통해 cache가 생성되고,
     * 이때 생성된 cache를 렌더링하며, 더 이상 자식뷰들은 렌더링이 동작하지 않게된다.
     * @param value
     */
    public void setDrawingCacheEnabled(boolean value) {
        mIsDrawingCacheEnabled = value;
    }

    /**
     * drawing cache 상태를 반환한다.
     * @return
     */
    public boolean isDrawingCacheEnabled() {
        return mIsDrawingCacheEnabled;
    }

    /**
     * 뷰의 id를 설정한다.
     * @param id
     */
    public void setId(int id) {
        mId = id;
    }

    /**
     * 뷰의 id를 반환한다.
     * @return
     */
    public int getId() {
        return mId;
    }

    /**
     * tag를 추가한다. tag는 key, value 쌍으로 여러개 추가할 수 있다.
     * @param key
     * @param value
     */
    public void setTag(String key, Object value) {
        mTag.put(key, value);
    }

    /**
     * key를 통해 등록된 tag를 반환한다.
     * @param key
     * @return
     */
    public Object getTag(String key) {
        return mTag.get(key);
    }

    /**
     * visible 상태를 변경한다.
     * 만약 false이면, 뷰를 더이상 화면에 표시되지 않는다.
     * @param value
     */
    public void setVisible(boolean value) {
        mVisible = value;
    }

    /**
     * pickable 상태를 변경한다.
     * 만약 false이면, 뷰는 더이상 터치대상이 되지 않는다.
     * @param value
     */
    public void setPickable(boolean value) {
        mPickable = value;
    }

    /**
     * pickable 상태를 반환한다.
     * @return
     */
    public boolean getPickable() {
        return mPickable;
    }

    /**
     * 부모뷰를 설정한다.
     * @param view
     */
    public void setParent(ArView view) {
        mParent = view;
    }

    /**
     * 부모뷰를 반환한다.
     * @return
     */
    public ArView getParent() {
        return mParent;
    }

    /**
     * 뷰의 width를 설정한다.
     * @param value
     */
    public void setWidth(int value) {
        mWidth = value;
    }

    /**
     * 뷰의 width를 반환한다.
     * @return
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * 뷰의 height를 설정한다.
     * @param value
     */
    public void setHeight(int value) {
        mHeight = value;
    }

    /**
     * 뷰의 height를 반환한다.
     * @return
     */
    public int getHeight() {
        return mHeight;
    }

    /**
     * 뷰의 position을 설정한다.
     * @param value
     */
    public void setPosition(ArVector value) {
        mPosition = value;
    }

    /**
     * 뷰의 position을 설정한다.
     * @param x
     * @param y
     * @param z
     */
    public void setPosition(float x, float y, float z) {
        setPosition(new ArVector(x, y, z));
    }

    /**
     * 뷰의 position을 반환한다.
     * @return
     */
    public ArVector getPosition() {
        return new ArVector(mPosition);
    }

    /**
     * 뷰의 rotation을 설정한다.
     * @param value
     */
    public void setRotation(ArVector value) {
        mRotation = value;
    }

    /**
     * 뷰의 rotation을 설정한다.
     * @param x
     * @param y
     * @param z
     */
    public void setRotation(float x, float y, float z) {
        setRotation(new ArVector(x, y, z));
    }

    /**
     * 뷰의 rotation을 반환한다.
     * @return
     */
    public ArVector getRotation() {
        return new ArVector(mRotation);
    }

    /**
     * 뷰의 scale을 설정한다.
     * @param x
     * @param y
     */
    public void setScale(float x, float y) {
        mScale = new ArVector(x, y, 1);
    }

    /**
     * 뷰의 scale을 반환한다.
     * @return
     */
    public ArVector getScale() {
        return mScale;
    }

    /**
     * 뷰의 rotation 속성을 기본값으로 고정시키게 설정한다.
     * 만약 true로 설정할 경우, 더이상 부모의 속성값들을 상속받지 않게된다.
     * @param x
     * @param y
     * @param z
     */
    public void setFixedRotation(boolean x, boolean y, boolean z) {
        mFixedRotation = new ArBoolean(x, y, z);
    }

    /**
     * 뷰의 position 속성을 기본값으로 고정시키게 설정한다.
     * 만약 true로 설정할 경우, 더이상 부모의 속성값들을 상속받지 않게된다.
     * @param x
     * @param y
     * @param z
     */
    public void setFixedPosition(boolean x, boolean y, boolean z) {
        mFixedPosition = new ArBoolean(x, y, z);
    }

    /**
     * 렌더링 대상인 drawable을 설정한다.
     * @param drawable
     */
    public void setDrawable(Drawable drawable) {
        mDrawable = drawable;
    }

    /**
     * drawable을 반환한다.
     * @return
     */
    public Drawable getDrawable() {
        return mDrawable;
    }

    /**
     * animation을 설정한다.
     * 설정된 animation은 다음 frame에 자동실행된다.
     * animation이 종료될 경우 설정된 animation은 자동으로 제거된다.
     * 만약 이미 animation이 존재할 경우, cancel된다.
     * @param animation
     */
    public void setAnimation(Animation animation) {
        if (mAnimation != null) {
            mAnimation.cancel();
        }
        mAnimation = animation;
    }

    /**
     * alpha를 설정한다.
     * @param alpha
     */
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    /**
     * color filter를 설정한다.
     * @param filter
     */
    public void setColorFilter(ColorFilter filter) {
        mPaint.setColorFilter(filter);
    }

    /**
     * click listener를 설정한다.
     * @param listener
     */
    public void setOnClickListener(ArOnClickListener listener) {
        mOnClickListener = listener;
        setPickable(true);
    }

    /**
     * click 동작을 수행한다.
     */
    public void performClick() {
        if (mOnClickListener != null) {
            mOnClickListener.onClick(this);
        }
    }

    /**
     * 전달된 id를 통해 자식뷰를 검색한다.
     * @param id
     * @return
     */
    public ArView findViewById(int id) {
        if (id < 0) {
            return null;
        }
        return findViewByIdTraversal(id);
    }

    /**
     * id를 통한 자식뷰를 검색을 수행한다.
     * 자식뷰에서는 이를 상속받아 원하는 동작을 수행해야한다.
     * @param id
     * @return
     */
    protected ArView findViewByIdTraversal(int id) {
        if (id == mId) {
            return this;
        }
        return null;
    }

    /**
     * 전달된 pick id를 통해 자식뷰를 검색한다.
     * @param id
     * @return
     */
    protected ArView findViewByPickId(int id) {
        return findViewByPickIdTraversal(id);
    }

    /**
     * pick id를 통한 자식뷰를 검색을 수행한다.
     * 자식뷰에서는 이를 상속받아 원하는 동작을 수행해야한다.
     * @param id
     * @return
     */
    protected ArView findViewByPickIdTraversal(int id) {
        if (id == mPickId) {
            return this;
        }
        return null;
    }

    /**
     * 뷰의 렌더링 동작을 수행한다.
     * @param arCanvas
     */
    protected void draw(ArCanvas arCanvas) {
        Canvas canvas = arCanvas.canvas;

        if (mVisible) {
            canvas.save();

            bindCameraTransform(arCanvas);
            bindMatrixTransform(arCanvas);

            boolean isInterrupted = false;
            switch (arCanvas.mode) {
                case PICK:
                    drawPick(arCanvas);
                    break;
                case DRAW:
                    onDraw(arCanvas);
                    /** {@link #isDrawingCacheEnabled()}가 true인 경우 cache를 렌더링하고, 자식뷰들은 렌더딩하지 않는다. */
                    if (mIsDrawingCacheEnabled) {
                        drawDrawingCache(arCanvas);
                        isInterrupted = true;
                    }
                    break;
            }

            canvas.restore();

            if (!isInterrupted) {
                dispatchDraw(arCanvas);
            }

            unbindMatrixTransform(arCanvas);
            unbindCameraTransform(arCanvas);
        }
    }

    /**
     * 자식뷰에서는 이를 상속받아 원하는 동작을 수행해야한다.
     * @param arCanvas
     */
    protected void dispatchDraw(ArCanvas arCanvas) {

    }

    /**
     * picking을 위한 draw 동작을 수행한다.
     * 전달된 canvas에 pick id를 color 값으로 변환하여 draw하게 되고,
     * {@link #findViewByPickId(int)}를 통해 picked 여부를 판단한다.
     * @param arCanvas
     */
    private void drawPick(ArCanvas arCanvas) {
        mPickId = ++arCanvas.pickId;

        if (!mPickable) {
            return;
        }

        Canvas canvas = arCanvas.canvas;

        byte[] colors = new byte[4];
        colors[3] = (byte) (mPickId & 0xFF);
        colors[2] = (byte) ((mPickId >> 8) & 0xFF);
        colors[1] = (byte) ((mPickId >> 16) & 0xFF);
        colors[0] = (byte) ((mPickId >> 24) & 0xFF);

        Paint paint = new Paint();
        paint.setARGB(0xFF, colors[1], colors[2], colors[3]);
        canvas.drawRect(getBounds(), paint);
    }

    /**
     * 등록된 drawable의 draw동작을 수행한다.
     * @param arCanvas
     */
    protected void onDraw(ArCanvas arCanvas) {
        Canvas canvas = arCanvas.canvas;

        if (mDrawable != null) {
            mDrawable.setAlpha(mPaint.getAlpha());
            mDrawable.setColorFilter(mPaint.getColorFilter());
            mDrawable.setBounds(getBounds());
            mDrawable.draw(canvas);
        }
    }

    /**
     * drawing cache를 렌더링한다.
     * @param arCanvas
     */
    private void drawDrawingCache(ArCanvas arCanvas) {
        Canvas canvas = arCanvas.canvas;

        if (mDrawingCache == null) {
            buildDrawingCache();
        }
        if (mDrawingCache != null) {
            canvas.drawBitmap(mDrawingCache, -mWidth * 0.5f, -mHeight * 0.5f, mPaint);
        }
    }

    /**
     * 뷰의 bounds를 반환한다.
     * @return
     */
    private Rect getBounds() {
        return new Rect(-mWidth / 2, -mHeight / 2, mWidth / 2, mHeight / 2);
    }

    /**
     * 뷰의 속성을 Camera를 이용하여 transform matrix를 제작한다.
     * 이렇게 제작된 matrix는 Canvas 반영된다.
     * 설정에 사용된 모든 값들은 {@link ArCanvas}의 stack에 저장된다.
     * @param arCanvas
     */
    private void bindCameraTransform(ArCanvas arCanvas) {
        Canvas canvas = arCanvas.canvas;
        Camera camera = arCanvas.camera;

        camera.save();

        ArVector rotation = arCanvas.rotationPeek();
        rotation = rotation.add(mRotation);
        arCanvas.rotationPush(rotation);

        ArVector position = arCanvas.positionPeek();
        position = position.add(mPosition);
        arCanvas.positionPush(position);

        camera.translate(
                mPosition.x - (mFixedPosition.x ? position.x : 0),
                mPosition.y - (mFixedPosition.y ? position.y : 0),
                mPosition.z - (mFixedPosition.z ? position.z : 0));

        camera.rotateX(mRotation.x - (mFixedRotation.x ? rotation.x : 0));
        camera.rotateY(mRotation.y - (mFixedRotation.y ? rotation.y : 0));
        camera.rotateZ(mRotation.z - (mFixedRotation.z ? rotation.z : 0));

        camera.getMatrix(mCameraMatrix);
        canvas.concat(mCameraMatrix);
    }

    /**
     * Scale, Animation 속성을 통해 matrix를 제작한다. 이렇게 제작된 matrix는 Canvas에 반영된다.
     * Alpha 속성을 Paint에 반영한다.
     *
     * @param arCanvas
     */
    private void bindMatrixTransform(ArCanvas arCanvas) {
        Canvas canvas = arCanvas.canvas;

        Matrix matrix = arCanvas.peek();
        matrix.setScale(mScale.x, mScale.y);

        float alpha = arCanvas.alphaPeek();

        /** animation이 존재할 경우, animation의 transformation을 처리한다. */
        if (mAnimation != null) {
            if (!mAnimation.isInitialized()) {
                int pWidth = mWidth;
                int pHeight = mHeight;
                if (mParent != null) {
                    pWidth = mParent.getWidth();
                    pHeight = mParent.getHeight();
                }
                mAnimation.initialize(mWidth, mHeight, pWidth, pHeight);
                mAnimation.start();
            }
            boolean more = mAnimation.getTransformation(arCanvas.drawingTime, mAnimationTransformation);
            if (!more) {
                mAnimation = null;
            }
            matrix.preConcat(mAnimationTransformation.getMatrix());
            alpha *= mAnimationTransformation.getAlpha();
        }

        arCanvas.push(matrix);
        arCanvas.alphaPush(alpha);

        canvas.concat(matrix);
        mPaint.setAlpha((int) (alpha * mPaint.getAlpha()));
    }

    /**
     * Camera, Rotation, Position 속성를 통해 사용된값들을 {@link ArCanvas}의 stack에서 제거한다.
     * @param arCanvas
     */
    private void unbindCameraTransform(ArCanvas arCanvas) {
        Camera camera = arCanvas.camera;

        camera.restore();
        arCanvas.rotationPop();
        arCanvas.positionPop();
    }

    /**
     * Matrix, Alpha 속성를 통해 사용된값을 {@link ArCanvas}의 stack에서 제거한다.
     * @param arCanvas
     */
    private void unbindMatrixTransform(ArCanvas arCanvas) {
        arCanvas.pop();
        arCanvas.alphaPop();
    }
}