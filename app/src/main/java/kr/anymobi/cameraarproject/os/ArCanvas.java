package kr.anymobi.cameraarproject.os;

import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.os.SystemClock;

import java.util.Stack;

/**
 * 렌더링에 필요한 정보를 저장하는 class 이다.
 */
public class ArCanvas {

    enum RenderMode {
        DRAW,
        PICK
    }

    /** Android Graphics Canvas */
    public Canvas canvas;
    /** Android Graphics Camera */
    public Camera camera;

    public RenderMode mode;
    public long drawingTime;
    public int pickId;

    private Stack<Matrix> matrixStack;
    private Stack<ArVector> rotationStack;
    private Stack<ArVector> positionStack;
    private Stack<Float> alphaStack;

    ArCanvas() {
        this.camera = new Camera();

        this.mode = RenderMode.DRAW;
        this.drawingTime = SystemClock.uptimeMillis();

        this.matrixStack = new Stack<>();
        this.rotationStack = new Stack<>();
        this.positionStack = new Stack<>();
        this.alphaStack = new Stack<>();
    }

    /**
     * matrix stack 에 새로운 matrix를 push한다.
     * @param matrix
     */
    public void push(Matrix matrix) {
        this.matrixStack.push(matrix);
    }

    /**
     * matrix stack 에서 matrix를 pop한다.
     */
    public void pop() {
        try {
            this.matrixStack.pop();
        } catch (Exception e) {
        }
    }

    /**
     * matrix stack 에서 최상위 matrix를 가져온다.
     * @return matrix
     */
    public Matrix peek() {
        Matrix matrix;
        try {
            matrix = new Matrix(this.matrixStack.peek());
        } catch (Exception e) {
            matrix = new Matrix();
        }
        return matrix;
    }

    /**
     * rotation stack에 새로운 ArVector를 push한다.
     * @param vector
     */
    public void rotationPush(ArVector vector) {
        this.rotationStack.push(vector);
    }

    /**
     * rotation stack 에서 ArVector를 pop한다.
     */
    public void rotationPop() {
        try {
            this.rotationStack.pop();
        } catch (Exception e) {
        }
    }

    /**
     * rotation stack 에서 최상위 ArVector를 가져온다.
     * @return ArVector
     */
    public ArVector rotationPeek() {
        ArVector vector;
        try {
            vector = new ArVector(this.rotationStack.peek());
        } catch (Exception e) {
            vector = new ArVector();
        }
        return vector;
    }

    /**
     * position stack에 새로운 ArVector를 push한다.
     * @param vector
     */
    public void positionPush(ArVector vector) {
        this.positionStack.push(vector);
    }

    /**
     * position stack 에서 ArVector를 pop한다.
     */
    public void positionPop() {
        try {
            this.positionStack.pop();
        } catch (Exception e) {
        }
    }

    /**
     * position stack 에서 최상위 ArVector를 가져온다.
     * @return ArVector
     */
    public ArVector positionPeek() {
        ArVector vector;
        try {
            vector = new ArVector(this.positionStack.peek());
        } catch (Exception e) {
            vector = new ArVector();
        }
        return vector;
    }

    /**
     * alpha stack에 새로운 alpha를 push한다.
     * @param alpha
     */
    public void alphaPush(float alpha) {
        this.alphaStack.push(alpha);
    }

    /**
     * alpha stack 에서 alpha를 pop한다.
     */
    public void alphaPop() {
        try {
            this.alphaStack.pop();
        } catch (Exception e) {
        }
    }

    /**
     * alpha stack 에서 최상위 alpha를 가져온다.
     * @return alpha
     */
    public float alphaPeek() {
        float alpha;
        try {
            alpha = this.alphaStack.peek();
        } catch (Exception e) {
            alpha = 1.0f;
        }
        return alpha;
    }
}
