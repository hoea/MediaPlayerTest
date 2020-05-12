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
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.nio.ByteBuffer;

import static android.media.MediaCodecList.ALL_CODECS;

public class MainActivity extends AppCompatActivity {

    String TAG = "hoge";
    private final int EXTERNAL_STORAGE_REQUEST_CODE = 1;

    MediaCodecCommonWrapper mWrapper[];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();
        Button button1 = (Button)findViewById(R.id.button);
        button1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                extract();
            }
        });
        Button button2 = (Button)findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getcodecs();
            }
        });
    }
    private void getcodecs() {
        Log.i(TAG, "getcodecs: called");
        MediaCodecList codeclist = new MediaCodecList(ALL_CODECS);
        MediaCodecInfo[] codecInfos = codeclist.getCodecInfos();
        Log.i(TAG, "getcodecs: " + codecInfos.length);
        for (int i=0;i < codecInfos.length;i++) {
            Log.i(TAG, "getcodecs: " + codecInfos[i]);
            String types[] = codecInfos[i].getSupportedTypes();
            for (int j=0;j < types.length;j++) {
                Log.i(TAG, "getcodecs: name:" + codecInfos[i].getName() + " type:" + types[j] );
            }
        }
    }

    private void extract() {
        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(Environment.getExternalStorageDirectory() + "/Download/01.mp4");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        int numTracks = extractor.getTrackCount();
        mWrapper = new MediaCodecCommonWrapper[numTracks];
        Log.i(TAG, "extract: " + numTracks);
        for (int i = 0; i < numTracks; ++i) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            mWrapper[i] = new MediaCodecCommonWrapper();
            mWrapper[i].init(format);
            Log.i(TAG, "extract: " + mime);
            boolean weAreInterestedInThisTrack = true;
            if (weAreInterestedInThisTrack) {
                extractor.selectTrack(i);
            }
        }

        ByteBuffer inputBuffer = ByteBuffer.allocate(1024*1024); // 1M
        while (extractor.readSampleData(inputBuffer,0) >= 0) {
            int trackIndex = extractor.getSampleTrackIndex();
            long presentationTimeUs = extractor.getSampleTime();
            Log.i(TAG, "extract: " + trackIndex + ":" + presentationTimeUs);
            mWrapper[trackIndex].write(inputBuffer,presentationTimeUs);
            extractor.advance();
        }

        extractor.release();
        extractor = null;
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
