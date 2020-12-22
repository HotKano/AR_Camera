package kr.anymobi.cameraarproject.arview;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;

import kr.anymobi.cameraarproject.activity.EventResultActivity;
import kr.anymobi.cameraarproject.util.TestData;

import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static kr.anymobi.cameraarproject.util.CommFunc.betweenBearing;
import static kr.anymobi.cameraarproject.util.CommFunc.calcDistance;

public class ARCanvas extends RelativeLayout implements SensorEventListener {

    private final String LOG_TAG = getClass().getSimpleName();
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest mLocationRequest;

    private TextView dataView;

    // 나침반 센서 관련 변수
    private float[] rotationMatrix;
    private float[] orientation;
    float azimuth;
    private boolean lastAccelerometerSet;
    private boolean lastMagnetometerSet;

    SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;

    private Sensor rotationVector;

    private boolean haveSensorVec;
    private boolean haveSensorAcc;
    private boolean haveSensorMag;

    private float[] lastAccelerometer;
    private float[] lastMagnetometer;
    private Button button;

    private final int ACCVALUE = -105; // 보정치 테스트. -105

    public ARCanvas(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
        locationInit(context);

        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        startCompass();
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        fusedLocationClient.requestLocationUpdates(mLocationRequest,
                locationCallback,
                Looper.getMainLooper());

    }

    // 위, 경도 테스트 목적
    private void locationInit(Context context) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        } else {

            ArrayList<TestData> testData = new ArrayList<>();

            // 도로쪽
            testData.add(new TestData("도로쪽", 37.4026336019775, 127.09885135140361));

            // 포스코 ICT
            testData.add(new TestData("포스코 ICT", 37.40436371944314, 127.1024025958136));

            // W시티
            testData.add(new TestData("W시티", 37.40403, 127.1001847));

            // PDCC 타워(?)
            testData.add(new TestData("PDCC 타워(?)", 37.40240348482312, 127.1012116950863));

            Location endLocation = new Location("");
            endLocation.setLatitude(testData.get(2).getLat());
            endLocation.setLongitude(testData.get(2).getLon());

            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);

                    Location startLocation = locationResult.getLastLocation();

                    float[] dist = new float[3];

                    double test2 = betweenBearing(startLocation.getLatitude(), startLocation.getLongitude(), endLocation.getLatitude(), endLocation.getLongitude());
                    String dis = calcDistance(startLocation.getLatitude(), startLocation.getLongitude(), endLocation.getLatitude(), endLocation.getLongitude());
                    Location.distanceBetween(startLocation.getLatitude(), startLocation.getLongitude(), endLocation.getLatitude(), endLocation.getLongitude(), dist);

                    //String data1 = convertLocationToString(startLocation);
                    //Log.d(LOG_TAG, data1);
                    String data = "Dis : " + dis + "\nBetween bearing " + test2 /*+ "\nUser Compass : " + -azimuth*/;

                    if (dataView != null)
                        dataView.setText(data);

                }

                @Override
                public void onLocationAvailability(LocationAvailability locationAvailability) {
                    super.onLocationAvailability(locationAvailability);
                    Log.d(LOG_TAG, "onLocationAvailability");
                }
            };

            startLocationUpdates();
        }
    }

    private void init(Context context) {

        rotationMatrix = new float[9];
        orientation = new float[3];

        lastAccelerometerSet = false;
        lastMagnetometerSet = false;

        lastAccelerometer = new float[3];
        lastMagnetometer = new float[3];

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);

        button = new Button(context);
        button.setLayoutParams(layoutParams);
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                context.startActivity(new Intent(context, EventResultActivity.class));
            }
        });

        RelativeLayout.LayoutParams layoutParamsText = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

        dataView = new TextView(context);
        dataView.setLayoutParams(layoutParamsText);
        dataView.setBackgroundColor(Color.WHITE);
        dataView.setTextColor(Color.BLACK);

        layoutParams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        setLayoutParams(layoutParams);

        addView(button);
        addView(dataView);

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(2000);
        mLocationRequest.setFastestInterval(1000);
    }

    // 커스텀 카메라 종료 처리 시 호출 센서 관련하여 처리할 예정
    public void destroyView() {
        Log.d(LOG_TAG, "destroyView");
        if (fusedLocationClient != null)
            fusedLocationClient.removeLocationUpdates(locationCallback);

        if (sensorManager != null)
            stopCompass();
    }

    private String convertLocationToString(Location location) {
        StringBuilder st = new StringBuilder();
        double lat = location.getLatitude();
        double lon = location.getLongitude();

        if (lat < 0)
            st.append("S ");
        else
            st.append("N ");

        String latDegrees = Location.convert(Math.abs(lat), Location.FORMAT_SECONDS);
        String[] latSplit = latDegrees.split(":");
        st.append(latSplit[0]);
        st.append("˚");
        st.append(latSplit[1]);
        st.append("`");
        st.append("\"");
        st.append("\n");

        if (lon < 0)
            st.append("W ");
        else
            st.append("E ");

        String lonDegrees = Location.convert(Math.abs(lon), Location.FORMAT_SECONDS);
        String[] lonSplit = lonDegrees.split(":");
        st.append(lonSplit[0]);
        st.append("˚");
        st.append(lonSplit[1]);
        st.append("`");
        st.append("\"");

        return st.toString();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            //azimuth = (int) ((Math.toDegrees((double) (SensorManager.getOrientation(rotationMatrix, orientation)[0]) + 360)) + ACCVALUE) % 360;
            azimuth = lowPassDegreesFilter(orientation[0]);
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.length);
            lastAccelerometerSet = true;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.length);
            lastMagnetometerSet = true;
        }

        if (lastAccelerometerSet && lastMagnetometerSet) {
            SensorManager.getRotationMatrix(rotationMatrix, null, lastAccelerometer, lastMagnetometer);
            //SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            SensorManager.getOrientation(rotationMatrix, orientation);
            azimuth = lowPassDegreesFilter(orientation[0]);
        }

        azimuth = Math.round(azimuth);

        String wayPoint = "not detecting";

        if (azimuth >= 350 || azimuth <= 10) wayPoint = "북";
        if (azimuth < 350 && azimuth > 280) wayPoint = "북서";
        if (azimuth <= 280 && azimuth > 260) wayPoint = "서";
        if (azimuth <= 260 && azimuth > 190) wayPoint = "남서";
        if (azimuth <= 190 && azimuth > 170) wayPoint = "남";
        if (azimuth <= 170 && azimuth > 100) wayPoint = "남동";
        if (azimuth <= 100 && azimuth > 80) wayPoint = "동";
        if (azimuth <= 80 && azimuth > 10) wayPoint = "북동";

        button.setText(wayPoint + " " + azimuth);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private Float lowPassDegreesFilter(Float azimuthRadians) {

        float alpha = 0f;
        double lastCos = 0f;
        double lastSin = 0f;

        lastSin = alpha * lastSin + (1 - alpha) * sin(azimuthRadians);
        lastCos = alpha * lastCos + (1 - alpha) * cos(azimuthRadians);

        return (float) ((Math.toDegrees((float) atan2(lastSin, lastCos)) + 360) % 360);
    }

    private void startCompass() {
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        haveSensorAcc = sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        haveSensorMag = sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
        haveSensorVec = sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_GAME);
    }

    private void stopCompass() {
        sensorManager.unregisterListener(this, accelerometer);
        sensorManager.unregisterListener(this, magnetometer);
        sensorManager.unregisterListener(this, rotationVector);
    }


}
