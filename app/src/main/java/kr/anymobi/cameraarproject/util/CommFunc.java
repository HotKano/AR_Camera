package kr.anymobi.cameraarproject.util;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.opengl.Matrix;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import kr.anymobi.cameraarproject.R;

import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

public class CommFunc {
    // 파일 권한
    public static boolean permissionCheck_file(Context ctx) {
        int permissionCheck = ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE);
        int permissionCheck2 = ContextCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        // 권한 없음
        //showCustomDialog_permission(ctx, "파일 권한을 승인해주세요.", 0);
        //Toast.makeText(ctx, "원활한 진행을 위해 파일 권한을 승인해주세요.", Toast.LENGTH_SHORT).show();
        // 권한 있음
        return permissionCheck != PackageManager.PERMISSION_DENIED || permissionCheck2 != PackageManager.PERMISSION_DENIED;
    }

    // 카메라 권한
    public static boolean permissionCheck_camera(Context ctx) {
        int permissionCheck = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA);

        // 권한 없음
        //showCustomDialog_permission(ctx, "카메라 권한을 승인해주세요.", 0);
        //Toast.makeText(ctx, "원활한 진행을 위해 카메라 권한을 승인해주세요.", Toast.LENGTH_SHORT).show();
        //((CameraActivity) ctx).finish();
        // 권한 있음
        return permissionCheck != PackageManager.PERMISSION_DENIED;
    }

    // 문자 및 파일 권한 체크
    public static void checkPermission_common(Context ctx) {
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) ctx, new String[]
                    {
                            Manifest.permission.CAMERA,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    }, CommConst.PERMISSION_ALL_NOT);
        } else {
            ActivityCompat.requestPermissions((Activity) ctx, new String[]
                    {
                            Manifest.permission.CAMERA,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    }, CommConst.PERMISSION_ALL);
        }
    }

    // 사용자가 권한 거부 이후 관련 기능 요청 시 보냄
    public static void settingOpen(final Activity act) {
        AlertDialog.Builder builder = new AlertDialog.Builder(act);

        builder.setMessage(R.string.msg)
                .setTitle(R.string.title);
        // Add the buttons
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", act.getApplicationContext().getPackageName(), null);
                intent.setData(uri);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                act.getApplicationContext().startActivity(intent);
                act.finish();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                dialog.dismiss();
                act.onBackPressed();
            }
        });
        // Set other dialog properties

        // Create the AlertDialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // 지점간 각도 계산 #1
    public static double calculateBearing(Location start, Location end) {
        //Source
        //JSONObject source = step.getJSONObject("start_location");
        //double lat1 = Double.parseDouble(source.getString("lat"));
        //double lng1 = Double.parseDouble(source.getString("lng"));
        double lat1 = start.getLatitude();
        double lng1 = start.getLongitude();

        // destination
        //JSONObject destination = step.getJSONObject("end_location");
        //double lat2 = Double.parseDouble(destination.getString("lat"));
        //double lng2 = Double.parseDouble(destination.getString("lng"));
        double lat2 = end.getLatitude();
        double lng2 = end.getLongitude();

        double dLon = (lng2 - lng1);
        double y = sin(dLon) * cos(lat2);
        double x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon);
        double brng = Math.toDegrees((atan2(y, x)));
        return (360 - ((brng + 360) % 360));
    }

    // 지점간 각도 계산 #2
    public static double betweenBearing(double startLat, double startLng, double endLat, double endLng) {
        double longitude1 = startLng;
        double longitude2 = endLng;
        double latitude1 = Math.toRadians(startLat);
        double latitude2 = Math.toRadians(endLat);
        double longDiff = Math.toRadians(longitude2 - longitude1);
        double y = sin(longDiff) * cos(latitude2);
        double x = cos(latitude1) * sin(latitude2) - sin(latitude1) * cos(latitude2) * cos(longDiff);
        return (Math.toDegrees(atan2(y, x)) + 360) % 360;
    }


    // 지점간 각도 계산 #3
    public static double calculateHeadingAngle(Location currentLocation, Location destinationLocation) {
        final int MAXIMUM_ANGLE = 360;
        double currentLatitudeRadians = Math.toRadians(currentLocation.getLatitude());
        double destinationLatitudeRadians = Math.toRadians(destinationLocation.getLatitude());
        double deltaLongitude = Math.toRadians(destinationLocation.getLongitude() - currentLocation.getLongitude());

        double y = cos(currentLatitudeRadians) * sin(destinationLatitudeRadians) -
                sin(currentLatitudeRadians) * cos(destinationLatitudeRadians) * cos(deltaLongitude);
        double x = sin(deltaLongitude) * cos(destinationLatitudeRadians);
        double headingAngle = Math.toDegrees(atan2(x, y));

        return (headingAngle + MAXIMUM_ANGLE) % MAXIMUM_ANGLE;
    }

    // 지점 간 거리 계산
    public static String calcDistance(double lat1, double lon1, double lat2, double lon2) {
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
            return dis;
        } else {

            //result2.setText(rtn + " km");
            dis = rtn + "Km";
            return dis;
        }
    }

    // 화면 Width 계산
    public static int getIntScreenXpixels(Context context) {
        DisplayMetrics matrix = context.getResources().getDisplayMetrics();
        return matrix.widthPixels;
    }

    // 화면 Height 계산
    public static int getIntScreenYpixels(Context context) {
        DisplayMetrics matrix = context.getResources().getDisplayMetrics();
        return matrix.heightPixels;
    }

    // 객체 width 계산
    public static int getIntXpixels(View view) {
        DisplayMetrics matrix = view.getResources().getDisplayMetrics();
        return matrix.widthPixels;
    }

    // 위도 경도로부터 주소 반환
    // FIXME 주소 값들 geocoder.getFromLocation 에서 일부 값들이 null 로 나오는 경우 확인 됨. -> 달리는 버스에서 그랬는데.. 음..
    public static String findAddress(Context ctx, double lat, double lon) {
        Address addressData;
        List<Address> addressList;
        StringBuilder address = new StringBuilder();
        Geocoder geocoder = new Geocoder(ctx, Locale.KOREA);
        try {
            // 세번째 인수는 최대결과값인데 하나만 리턴받도록 설정했다, MaxResult : 9
            addressList = geocoder.getFromLocation(lat, lon, 2);
            // 설정한 데이터로 주소가 리턴된 데이터가 있으면
            if (addressList != null && addressList.size() > 0) {
                addressData = addressList.get(1);
                address.append(addressData.getAdminArea()).append(" "); // 경기도
                address.append(addressData.getLocality()).append(" "); // 성남시
                address.append(addressData.getSubLocality()).append(" "); // 분당구
                address.append(addressData.getFeatureName()); // 도로명 -> 판교로 // FIXME 숫자 주소로 반환 되는 경우가 있음
            } else {
                // TODO String.xml 만들기
                address.append("현재 위치의 주소를 확인 할 수 없습니다.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return address.toString();
    }


}
