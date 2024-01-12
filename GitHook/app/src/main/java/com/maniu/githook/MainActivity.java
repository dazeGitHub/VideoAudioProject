package com.maniu.githook;

import androidx.annotation.IntRange;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("111111");
        setContentView(R.layout.activity_main);
//       能 1  不能 2
//        a = 10;
//        setAlpha(100);
        setAlpha(26);
    }

    public void setAlpha(@IntRange(from=0,to=255) int alpha) {
        
        
    }

}