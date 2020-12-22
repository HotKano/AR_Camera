package kr.anymobi.cameraarproject.activity;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.netguru.arlocalizerview.location.LocationData;

import java.util.ArrayList;

import kr.anymobi.cameraarproject.R;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback, SensorEventListener {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback locationCallback;
    private boolean mapFlag = false;
    private final String LOG_TAG = getClass().getSimpleName();

    // fov 객체
    private GroundOverlay mFovOverlay;

    // 센서 관련
    private float[] rotationMatrix;
    private float[] orientation;
    int azimuth;
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

    private final int ACC_VALUE = -105; // 보정치 테스트. -> 폐기 예정.

    //TODO 나중에 DTO 객체로 합칠 예정, 기능 테스트 목적 구현 @김종우
    private ArrayList<LocationData> arrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(2000);
        mLocationRequest.setFastestInterval(1000);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        rotationMatrix = new float[9];
        orientation = new float[3];

        lastAccelerometerSet = false;
        lastMagnetometerSet = false;

        lastAccelerometer = new float[3];
        lastMagnetometer = new float[3];

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);


        //TODO 나중에 DTO 객체로 합칠 예정, 기능 테스트 목적 구현 @김종우
        if (getIntent() != null) {
            arrayList = getIntent().getParcelableArrayListExtra("mapDataList");
        }


    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fusedLocationClient != null)
            fusedLocationClient.removeLocationUpdates(locationCallback);

        stopCompass();
    }

    @Override
    protected void onResume() {
        super.onResume();
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

    //** create the FOV **//
    private void createFov(LatLng userPosition, GoogleMap map, float bearing) {
        GroundOverlayOptions fov = new GroundOverlayOptions();
        fov.position(userPosition, 100f);
        fov.anchor(0.5f, 1f);
        fov.image(BitmapDescriptorFactory.fromResource(R.drawable.btn_arrow_back2));
        fov.bearing(bearing);
        mFovOverlay = map.addGroundOverlay(fov);
    }

    // map 범위 궤도 완성
    // 김종우
    private void createCircleFov(LatLng userPos, GoogleMap map) {
        final int RADIUS_CIRCLE = 500; // diameter
        Bitmap bm = Bitmap.createBitmap(RADIUS_CIRCLE, RADIUS_CIRCLE, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        Paint p = new Paint();
        p.setColor(getResources().getColor(R.color.colorPrimaryDark));
        p.setStyle(Paint.Style.FILL);
        c.drawCircle(RADIUS_CIRCLE / 2, RADIUS_CIRCLE / 2, RADIUS_CIRCLE / 2, p);

        // generate BitmapDescriptor from circle Bitmap
        BitmapDescriptor bmD = BitmapDescriptorFactory.fromBitmap(bm);

        // mapView is the GoogleMap
        map.addGroundOverlay(new GroundOverlayOptions().
                image(bmD).
                position(userPos, RADIUS_CIRCLE, RADIUS_CIRCLE).
                transparency(0.4f));
    }

    //** change the FOV direction **//
    private void changeFovDirection(float bearing) {
        mFovOverlay.setBearing(bearing);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        // W시티 testData.add(new TestData("W시티", 37.40403, 127.1001847));
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);

                Location startLocation = locationResult.getLastLocation();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Log.d(LOG_TAG, startLocation.getBearing() + " :: " + startLocation.getBearingAccuracyDegrees());
                } else {
                    Log.d(LOG_TAG, startLocation.getBearing() + " ");
                }
                LatLng latLng = new LatLng(startLocation.getLatitude(), startLocation.getLongitude());
                if (mMap != null) {
                    // mapFlag 초기 init 용.
                    if (!mapFlag) {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16));
                        mapFlag = true;
                    } /*else {
                        startCompass();
                    }*/

                    mMap.clear();
                    createCircleFov(latLng, mMap);

                    // TODO 나중에 DTO 객체로 합칠 예정, 기능 테스트 목적 구현 @김종우
                    if (arrayList != null && arrayList.size() > 0) {
                        for (LocationData test : arrayList) {
                            LatLng sydney = new LatLng(test.getLatitude(), test.getLongitude());
                            mMap.addMarker(new MarkerOptions().position(sydney).title(test.getPointName()));
                        }
                    }

                    mMap.addMarker(new MarkerOptions().position(latLng).title("user location"));
                }

            }

            @Override
            public void onLocationAvailability(LocationAvailability locationAvailability) {
                super.onLocationAvailability(locationAvailability);
            }
        };

        startLocationUpdates();

    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            azimuth = (int) ((Math.toDegrees((double) (SensorManager.getOrientation(rotationMatrix, orientation)[0]) + 360)) + ACC_VALUE) % 360;
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
            azimuth = (int) ((Math.toDegrees((double) (SensorManager.getOrientation(rotationMatrix, orientation)[0]) + 360)) + ACC_VALUE) % 360;
        }

        azimuth = Math.round((float) azimuth);

        String wayPoint = "not detecting";

        if (azimuth >= 350 || azimuth <= 10) wayPoint = "북";
        if (azimuth < 350 && azimuth > 280) wayPoint = "북서";
        if (azimuth <= 280 && azimuth > 260) wayPoint = "서";
        if (azimuth <= 260 && azimuth > 190) wayPoint = "남서";
        if (azimuth <= 190 && azimuth > 170) wayPoint = "남";
        if (azimuth <= 170 && azimuth > 100) wayPoint = "남동";
        if (azimuth <= 100 && azimuth > 80) wayPoint = "동";
        if (azimuth <= 80 && azimuth > 10) wayPoint = "북동";

        //changeFovDirection(azimuth);

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}