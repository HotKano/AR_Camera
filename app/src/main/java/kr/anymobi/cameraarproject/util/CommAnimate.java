package kr.anymobi.cameraarproject.util;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

public class CommAnimate {
    // 주소 이동 애니메이션
    public static void leftToRightFlowAnimated(View view) {
        /*Animation left = AnimationUtils.loadAnimation(this, R.anim.view_transition_out_right);
        left.setRepeatCount(Animation.INFINITE);
        left.setRepeatMode(Animation.REVERSE);
        left.setDuration(3000);*/

        TranslateAnimation moveLefttoRight;

        // fade out 으로 부터 오는 것 처리.
        DisplayMetrics matrix = view.getResources().getDisplayMetrics();
        // view 의 최 상위 View width 계산용
        DisplayMetrics parentMatrix = view.getRootView().getResources().getDisplayMetrics();

        moveLefttoRight = new TranslateAnimation(-matrix.widthPixels, parentMatrix.widthPixels, 0, 0);
        moveLefttoRight.setRepeatCount(Animation.INFINITE);
        moveLefttoRight.setDuration(9000);

        view.startAnimation(moveLefttoRight);
    }


}
