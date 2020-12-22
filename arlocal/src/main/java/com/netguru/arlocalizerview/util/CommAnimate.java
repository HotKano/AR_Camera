package com.netguru.arlocalizerview.util;

import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

public class CommAnimate {

    // 애니메이션 토스트
    public static void leftToastAnimated(final View view) {
        /*Animation left = AnimationUtils.loadAnimation(this, R.anim.view_transition_out_right);
        left.setRepeatCount(Animation.INFINITE);
        left.setRepeatMode(Animation.REVERSE);
        left.setDuration(3000);*/

        final int time = 1000;

        TranslateAnimation leftToastAnimated;

        // fade out 으로 부터 오는 것 처리.
        DisplayMetrics matrix = view.getResources().getDisplayMetrics();
        leftToastAnimated = new TranslateAnimation(-matrix.widthPixels, 0, 0, 0);
        leftToastAnimated.setDuration(time);
        view.setVisibility(View.VISIBLE);
        view.startAnimation(leftToastAnimated);

        leftToastAnimated.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                DisplayMetrics matrix = view.getResources().getDisplayMetrics();
                TranslateAnimation leftToastAnimated = new TranslateAnimation(0, -matrix.widthPixels, 0, 0);
                leftToastAnimated.setStartOffset(time);
                leftToastAnimated.setDuration(time);
                view.startAnimation(leftToastAnimated);
                leftToastAnimated.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        view.setVisibility(View.GONE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });

            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }
}
