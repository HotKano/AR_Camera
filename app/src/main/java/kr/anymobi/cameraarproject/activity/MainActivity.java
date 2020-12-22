package kr.anymobi.cameraarproject.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.netguru.arlocalizerview.rxutil.RxEventBusPoint;

import kr.anymobi.cameraarproject.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.moveToAR).setOnClickListener(view -> startActivity(new Intent(MainActivity.this, CameraActivity.class)));
        findViewById(R.id.moveToMap).setOnClickListener(view -> MainActivity.this.startActivity(new Intent(MainActivity.this, ARApiTestKo.class)));
        findViewById(R.id.moveToRotate).setOnClickListener(view -> MainActivity.this.startActivity(new Intent(MainActivity.this, MapActivity.class)));

        //findViewById(R.id.moveToRotate).setOnClickListener(view -> RxEventBusPoint.INSTANCE.sendData("test"));

    }

}