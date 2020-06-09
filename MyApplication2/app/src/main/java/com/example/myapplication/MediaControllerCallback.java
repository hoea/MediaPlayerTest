package com.example.myapplication;

public interface MediaControllerCallback {
    void notifyPlayPosition(MediaController controller, long pos, long duration);
}
