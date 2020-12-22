package kr.anymobi.cameraarproject.os;

import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

/**
 * ArAnimationDrawable drawable = new ArAnimationDrawable(this);
 * drawable.addFrame(getResources().getDrawable(R.drawable.shape_radar_bg));
 * drawable.addFrame(getResources().getDrawable(R.drawable.icon));
 * drawable.setDuration(300);
 */
public class ArAnimationDrawable extends AnimationDrawable implements Drawable.Callback {

    private volatile int mDuration;
    private int mCurrentFrame;

    private View mView;

    public ArAnimationDrawable(View view) {
        mView = view;

        mCurrentFrame = 0;
        setCallback(this);
    }

    public void addFrame(Drawable frame) {
        addFrame(frame, 0);
    }

    @Override
    public void run() {
        int n = getNumberOfFrames();
        mCurrentFrame++;
        if (mCurrentFrame >= n) {
            mCurrentFrame = 0;
        }

        selectDrawable(mCurrentFrame);
        scheduleSelf(this, mDuration);
    }

    public void setDuration(int duration) {
        mDuration = duration;
    }

    @Override
    public void start() {
        unscheduleSelf(this);
        selectDrawable(mCurrentFrame);
        scheduleSelf(this, mDuration);
    }

    @Override
    public void invalidateDrawable(Drawable who) {
        mView.invalidate();
    }

    @Override
    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        mView.postDelayed(what, when);
    }

    @Override
    public void unscheduleDrawable(Drawable who, Runnable what) {
        mView.removeCallbacks(what);
    }

}