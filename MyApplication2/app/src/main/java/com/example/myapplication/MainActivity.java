package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
        implements AdapterView.OnItemClickListener, MediaControllerCallback {

    String TAG = "MediaStudyApp";
    private final int EXTERNAL_STORAGE_REQUEST_CODE = 1;
    ProgressBar mbar;

    SurfaceView mSurfaceView = null;
    Surface mSurface;
    MediaExtractorWrapper mExtractor;
    static ArrayAdapter<String> adapter;
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();

        mSurfaceView = (SurfaceView)findViewById(R.id.surfaceView);
        mSurface = mSurfaceView.getHolder().getSurface();

        ArrayList data = new ArrayList<>();
        File dir = new File(Environment.getExternalStorageDirectory() + "/Download");
        File[] list = dir.listFiles();
        for (int i = 0;i < list.length; i++) {
            if (list[i].isFile() == true) {
                Log.i(TAG, "onCreate: " + list[i].toString());
                data.add(list[i].toString());
            }
        }
        adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                data);
        listView = (ListView)findViewById(R.id.contentList);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);

        mbar = (ProgressBar)findViewById(R.id.progressBar);
        mbar.setMax(100);
        mbar.setProgress(50);


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
        Button button3 = (Button)findViewById(R.id.button3);
        button3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mController != null) {
                    mController.pause();
                }
            }
        });
    }
    MediaController mController = null;
    private void extract() {
        mController = new MediaController(this);
        mController.initialize(mSurface);
        mController.prepare(Environment.getExternalStorageDirectory() + "/Download/02.mp4");
        mController.start();
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void checkPermission() {
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Toast.makeText(this, "this application can work if permitted", Toast.LENGTH_SHORT).show();
        }
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, EXTERNAL_STORAGE_REQUEST_CODE);
    }

    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        Log.i(TAG, "onItemClick: " + position);
        Log.i(TAG, "onItemClick: " + parent.getItemAtPosition(position).toString());
        if (mController != null) {
            mController.stop();
        }
        mController = new MediaController(this);
        mController.initialize(mSurface);
        mController.prepare(parent.getItemAtPosition(position).toString());
        mController.start();
        return;
    }

    public void notifyPlayPosition(long pos, long duration) {
        Log.i(TAG, "notifyPlayPosition: " + pos + "/" + duration);
        mbar.setMax((int)duration);
        mbar.setProgress((int)pos);
        return;
    }
}
