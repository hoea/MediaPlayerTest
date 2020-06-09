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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
        implements AdapterView.OnItemClickListener, MediaControllerCallback {

    String TAG = "MediaStudyApp";
    private final int EXTERNAL_STORAGE_REQUEST_CODE = 1;

    class ContollerTable {
        SurfaceView view;
        Surface surface;
        ProgressBar bar;
        MediaController controller;
    }
    final int TBL_SIZE = 2;
    ContollerTable[] mControllers = null;

    static ArrayAdapter<String> adapter;
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkPermission();

        mControllers = new ContollerTable[TBL_SIZE];
        for (int i = 0;i < TBL_SIZE;i++) {
            mControllers[i] = new ContollerTable();
        }
        mControllers[0].view = (SurfaceView)findViewById(R.id.surfaceView);
        mControllers[0].surface  = mControllers[0].view.getHolder().getSurface();
        mControllers[1].view = (SurfaceView)findViewById(R.id.surfaceView2);
        mControllers[1].surface  = mControllers[1].view.getHolder().getSurface();
        mControllers[0].bar = (ProgressBar)findViewById(R.id.progressBar);
        mControllers[1].bar = (ProgressBar)findViewById(R.id.progressBar2);
        for (int i = 0;i < TBL_SIZE;i++) {
            mControllers[i].bar.setMax(100);
            mControllers[i].bar.setProgress(50);
        }

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
                int index = getDisplayIndex();
                if (mControllers[index].controller != null) {
                    mControllers[index].controller.pause();
                }
            }
        });
    }

    private void extract() {
        int index = getDisplayIndex();
        mControllers[index].controller = new MediaController(this);
        mControllers[index].controller.initialize(mControllers[index].surface);
        mControllers[index].controller.prepare(Environment.getExternalStorageDirectory() + "/Download/02.mp4");
        mControllers[index].controller.start();
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

    public int getDisplayIndex() {
        RadioGroup mdisp = (RadioGroup)findViewById(R.id.radiogroup_id);
        int selected = mdisp.getCheckedRadioButtonId();
        RadioButton button = (RadioButton)findViewById(selected);
        int index = 0;
        if (button.getText().toString().equals("disp1") == true) {
            Log.i(TAG, "onItemClick: disp1");
            index = 0;
        } else if (button.getText().toString().equals("disp2") == true) {
            Log.i(TAG, "onItemClick: disp2");
            index = 1;
        }
        return index;
    }

    //private RadioGroup mdisp;
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        Log.i(TAG, "onItemClick: " + position);
        Log.i(TAG, "onItemClick: " + parent.getItemAtPosition(position).toString());

        //DisplaySelection select = new DisplaySelection();
        //select.show(getSupportFragmentManager(), "missiles");
        //int index = select.getDisplay();

        int index = getDisplayIndex();

        if (mControllers[index].controller != null) {
            mControllers[index].controller.stop();
        }
        mControllers[index].controller = new MediaController(this);
        mControllers[index].controller.initialize(mControllers[index].surface);
        mControllers[index].controller.prepare(parent.getItemAtPosition(position).toString());
        mControllers[index].controller.start();
        return;
    }

    public void notifyPlayPosition(MediaController controller,long pos, long duration) {
        Log.i(TAG, "notifyPlayPosition: " + pos + "/" + duration);
        ProgressBar bar = null;
        for (int i = 0;i<TBL_SIZE;i++) {
            if (controller == mControllers[i].controller) {
                bar =  mControllers[i].bar;
                break;
            }
        }
        if (bar != null) {
            bar.setMax((int) duration);
            bar.setProgress((int) pos);
        }
        return;
    }
}
