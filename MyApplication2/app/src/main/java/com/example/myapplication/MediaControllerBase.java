package com.example.myapplication;

import android.view.SurfaceHolder;

public interface MediaControllerBase {
    public boolean initialize(SurfaceHolder surface);
    public boolean prepare(String filename);
    public boolean start();
    public boolean seek(int position);
    public boolean pause();
    public boolean stop();
}
