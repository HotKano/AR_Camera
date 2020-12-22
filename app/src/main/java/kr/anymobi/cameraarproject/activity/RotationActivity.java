package kr.anymobi.cameraarproject.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import kr.anymobi.cameraarproject.R;

import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static kr.anymobi.cameraarproject.util.CommFunc.getIntScreenXpixels;

public class RotationActivity extends AppCompatActivity implements SensorEventListener {

    private final String LOG_TAG = getClass().getSimpleName();

    // 파노라마 관련 변수
    private ImageView panorama;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rotationview);
        reConnectedWidget();
        initSensor();


    }

    @Override
    protected void onResume() {
        super.onResume();
        startCompass();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopCompass();
    }

    private void reConnectedWidget() {
        panorama = findViewById(R.id.panoramaView);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(getIntScreenXpixels(this) * 3, LinearLayout.LayoutParams.WRAP_CONTENT);
        panorama.setLayoutParams(layoutParams);
        panorama.setBackground(getDrawable(R.drawable.sky));

    }

    int data = 0;

    private void animated(float azimuth) {
        /*Animation left = AnimationUtils.loadAnimation(this, R.anim.view_transition_out_right);
        left.setRepeatCount(Animation.INFINITE);
        left.setRepeatMode(Animation.REVERSE);
        left.setDuration(3000);*/

        int localData = (int) azimuth;
        TranslateAnimation moveLefttoRight;

        if (data >= localData) {
            moveLefttoRight = new TranslateAnimation(0, 4320, 0, 0);
        } else {
            moveLefttoRight = new TranslateAnimation(0, -4320, 0, 0);
        }

        moveLefttoRight.setRepeatCount(Animation.INFINITE);
        moveLefttoRight.setDuration(0);

        panorama.startAnimation(moveLefttoRight);
    }

    private void initSensor() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        rotationMatrix = new float[9];
        orientation = new float[3];

        lastAccelerometerSet = false;
        lastMagnetometerSet = false;

        lastAccelerometer = new float[3];
        lastMagnetometer = new float[3];
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

        Log.d(LOG_TAG, azimuth + "   ");

        if (flag) {
            data = (int) azimuth;
            flag = false;
        }

        animated(azimuth);
    }

    boolean flag = true;

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}