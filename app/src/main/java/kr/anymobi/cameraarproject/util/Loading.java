package kr.anymobi.cameraarproject.util;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;

import kr.anymobi.cameraarproject.R;


public class Loading extends Dialog {

    public Loading(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }
}
