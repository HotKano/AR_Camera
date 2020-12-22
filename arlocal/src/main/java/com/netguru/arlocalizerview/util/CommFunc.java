package com.netguru.arlocalizerview.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.Window;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.netguru.arlocalizerview.R;

import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

public class CommFunc {
    // 지점간 각도 계산 #2
    public static float betweenBearing(double startLat, double startLng, double endLat, double endLng) {
        double latitude1 = Math.toRadians(startLat);
        double latitude2 = Math.toRadians(endLat);
        double longDiff = Math.toRadians(endLng - startLng);
        double y = sin(longDiff) * cos(latitude2);
        double x = cos(latitude1) * sin(latitude2) - sin(latitude1) * cos(latitude2) * cos(longDiff);

        float headingAngle = (float) Math.toDegrees(atan2(y, x));

        return (headingAngle + 360) % 360;
    }

    // 지점 간 거리 계산
    public static double calcDistance(double lat1, double lon1, double lat2, double lon2) {
        double EARTH_R, Rad, radLat1, radLat2, radDist;
        double distance, ret;
        String dis;

        EARTH_R = 6371000.0;
        Rad = Math.PI / 180;
        radLat1 = Rad * lat1;
        radLat2 = Rad * lat2;
        radDist = Rad * (lon1 - lon2);

        distance = sin(radLat1) * sin(radLat2);
        distance = distance + cos(radLat1) * cos(radLat2) * cos(radDist);
        ret = EARTH_R * Math.acos(distance);

        double rtn = Math.round(Math.round(ret) / 1000);

        if (rtn <= 0) {
            rtn = Math.round(ret);
            dis = rtn + "m";
            return rtn;
        } else {
            //result2.setText(rtn + " km");
            dis = rtn + "Km";
            return rtn;
        }
    }

    //status bar의 높이 계산
    public static int getStatusBarHeight(Context ctx) {
        int result = 0;
        int resourceId = ctx.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0)
            result = ctx.getResources().getDimensionPixelSize(resourceId);

        return result;
    }
}
