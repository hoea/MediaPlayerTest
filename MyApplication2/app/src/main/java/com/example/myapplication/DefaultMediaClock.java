package com.example.myapplication;

public class DefaultMediaClock implements MediaClock {
    @Override
    public int getPosition() {
        return 0;
    }

    private int mBaseTime = 0;
    @Override
    public boolean setBaseTime(int position) {
        mBaseTime = position;
        return true;
    }
}
