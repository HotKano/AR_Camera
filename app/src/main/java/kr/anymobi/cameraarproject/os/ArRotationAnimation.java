package kr.anymobi.cameraarproject.os;

import android.graphics.Camera;
import android.view.animation.Animation;
import android.view.animation.Transformation;

public class ArRotationAnimation extends Animation {
    private Camera mCamera = new Camera();

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        mCamera.save();
        mCamera.rotateY(interpolatedTime * 360);
        mCamera.getMatrix(t.getMatrix());
        mCamera.restore();
    }
}
