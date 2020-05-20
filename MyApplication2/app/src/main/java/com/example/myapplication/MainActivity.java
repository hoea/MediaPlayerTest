package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.nio.ByteBuffer;

import static android.media.MediaCodecList.ALL_CODECS;

public class MainActivity extends AppCompatActivity {

    String TAG = "MediaStudyApp";
    private final int EXTERNAL_STORAGE_REQUEST_CODE = 1;


    SurfaceView mSurfaceView = null;
    Surface mSurface;
    MediaExtractorWrapper mExtractor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();

        mSurfaceView = (SurfaceView)findViewById(R.id.surfaceView);
        mSurface = mSurfaceView.getHolder().getSurface();

        Button button1 = (Button)findViewById(R.id.button);
        button1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                extract();
            }
        });
        Button button2 = (Button)findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                MediaCodecCommonWrapper.getcodecs();
            }
        });
    }

    private void extract() {
        mExtractor = new MediaExtractorWrapper();
        mExtractor.extract(Environment.getExternalStorageDirectory() + "/Download/02.mp4",mSurface);
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void checkPermission() {
        // 既に許可している
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Toast.makeText(this, "許可されないとアプリが実行できません", Toast.LENGTH_SHORT).show();
        }
        // パーミッションの取得を依頼
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, EXTERNAL_STORAGE_REQUEST_CODE);
    }
}
