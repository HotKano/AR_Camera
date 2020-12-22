package com.netguru.arlocalizerview.test;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.netguru.arlocalizerview.R;

/**
 * ARCamera canvas를 통하여 들어온 data 확인
 * 김종우.
 */
public class TestView extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_result);

        TextView textview = findViewById(R.id.eventResult);

        if (getIntent() != null)
            textview.setText(getIntent().getStringExtra("data"));
        else
            textview.setText("data is null");

    }
}
