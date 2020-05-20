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

    MediaCodecCommonWrapper mWrapper[];
    SurfaceView mSurfaceView = null;
    Surface mSurface;

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
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.indexOf("audio") != -1) {
                mWrapper[i] = new MediaCodecAudioWrapper();
                if (mWrapper[i].init(format,null) == false) {
                    Log.e(TAG, "extract: invalid format" + mime );
                    return;
                }
                Log.i(TAG, "create codec: " + mime);
                extractor.selectTrack(i);

            } else if (mime.indexOf("video") != -1) {
                /*
                mWrapper[i] = new MediaCodecVideoWrapper();
                if (mWrapper[i].init(format,mSurface) == false) {
                    Log.e(TAG, "extract: invalid format" + mime );
                    return;
                }
                Log.i(TAG, "create codec: " + mime);
                extractor.selectTrack(i);

                 */
            }

        }

        ByteBuffer inputBuffer = ByteBuffer.allocate(8192); // 1M
        while (extractor.readSampleData(inputBuffer,0) >= 0) {
            int trackIndex = extractor.getSampleTrackIndex();
            long presentationTimeUs = extractor.getSampleTime();
            Log.i(TAG, "extract: " + trackIndex + ":" + presentationTimeUs + " size=" + inputBuffer.remaining());
            boolean writeResult = false;
            while (true) {
                writeResult = mWrapper[trackIndex].write(inputBuffer,presentationTimeUs);
                if (writeResult == false){
                    try {
                        Thread.sleep(100); //3000ミリ秒Sleepする
                    } catch (InterruptedException e) {
                    }
                } else {
                    break;
                }
            }
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
