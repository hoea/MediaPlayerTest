package com.example.myapplication;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class VideoSurface extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder holder = null;
    String TAG = "VideoSurface";

    public VideoSurface(Context context) {
        super(context);
        holder = getHolder();
        getHolder().addCallback(this);
        Log.i(TAG, "VideoSurface: constructor called");
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        Log.e(TAG, "surfaceChanged()");
    }
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.e(TAG, "surfaceCreated()");
        draw();
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.e(TAG, "surfaceDestroyed()");
    }

    // SurfaceViewにてタッチイベントが検知されたときに呼び出される
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.e(TAG, event.toString());

        // getAction()メソッドを用いて、eventよりタッチイベントのアクションを取得
        if (event.getAction() == MotionEvent.ACTION_UP) {
            draw();
        }
        return true;
    }
    private void draw() {

    }
}
