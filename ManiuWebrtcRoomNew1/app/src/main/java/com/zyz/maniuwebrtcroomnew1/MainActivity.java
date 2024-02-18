package com.zyz.maniuwebrtcroomnew1;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.zyz.maniuwebrtcroomnew1.webrtc.WebRTCManager;

public class MainActivity extends AppCompatActivity {

    private EditText mEtRoom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mEtRoom = findViewById(R.id.et_room);
    }

    public void JoinRoom(View view) {
        WebRTCManager.getInstance().connect(this, mEtRoom.getText().toString().trim());
    }

    public void JoinRoomSingleVideo(View view) {

    }
}