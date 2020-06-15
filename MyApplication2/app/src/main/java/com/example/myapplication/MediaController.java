package com.example.myapplication;

import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.nio.ByteBuffer;

public class MediaController implements MediaExtractorWrapperCallback ,MediaClockCallback{
    final private String TAG = "MediaController";
    enum PlayerState {
        IDLE,
        PREPARED,
        PLAYING,
        PAUSED,
        ERROR
    };
    private PlayerState mState = PlayerState.IDLE;
    Surface mSurface = null;
    MediaExtractorWrapper mExtractor = null;
    MediaFormatInfo mFormats[] = null;
    MediaClock mClock = null;
    MediaControllerCallback mCallback;

    class MediaFormatInfo {
        public MediaFormat mFormat = null;
        public MediaCodecCommonWrapper mWrapper = null;

        MediaFormatInfo(MediaFormat format, MediaCodecCommonWrapper wrapper) {
            mFormat = format;
            mWrapper = wrapper;
        }
    }
    public MediaController(MediaControllerCallback callback) {
        mCallback = callback;
    }
    public boolean initialize(Surface surface) {
        mSurface = surface;
        return false;
    }
    long mDuration;
    public boolean prepare(String filename) {
        if (mState != PlayerState.IDLE) {
            Log.e(TAG, "prepare: invalid state" + mState);
            return false;
        }
        Log.i(TAG, "prepare: called " + filename);
        mExtractor = new MediaExtractorWrapper(this);
        MediaFormat[] formats = mExtractor.getContentsInfos(filename);
        mFormats = new MediaFormatInfo[formats.length];

        mDuration = 0;
        long videoduration = 0;
        long audioduration = 0;
        MediaCodecAudioWrapper audioDec = null;
        MediaCodecVideoWrapper videoDec = null;

        // TODO:XXX
        for (int i = 0;i < formats.length;i++) {
            mFormats[i] = new MediaFormatInfo(formats[i], null);
            String mime = formats[i].getString(MediaFormat.KEY_MIME);
            if (mime.indexOf("audio") != -1 && audioDec == null) {
                audioDec = new MediaCodecAudioWrapper(this);
                if (audioDec.init(mFormats[i].mFormat) == false) {
                    audioDec = null;
                }
                mFormats[i].mWrapper = audioDec;
                mClock = (MediaClock)mFormats[i].mWrapper;
                audioduration = formats[i].getLong(MediaFormat.KEY_DURATION);
                mExtractor.selectTrack(i);
            } else if (mime.indexOf("video") != -1 && videoDec == null) {
                videoDec = new MediaCodecVideoWrapper(mSurface);
                if (videoDec.init(mFormats[i].mFormat) == false) {
                    videoDec = null;
                }
                mFormats[i].mWrapper = videoDec;
                videoduration = formats[i].getLong(MediaFormat.KEY_DURATION);
                mExtractor.selectTrack(i);
            }
        }
        if (videoDec == null && audioDec == null) {
            return false;
        }
        mDuration = Math.max(videoduration,audioduration);
        mDuration = mDuration/1000;
        if (mClock == null) {
            mClock = new DefaultMediaClock();
        }
        if (videoDec != null) {
            videoDec.setMediaClock(mClock);
        }
        if (mCallback != null) {
            mCallback.notifyPlayPosition(this,0, mDuration);
        }
        mState = PlayerState.PREPARED;
        return true;
    }
    public boolean start() {
        Log.i(TAG, "start: called");
        if (mState != PlayerState.PREPARED) {
            Log.e(TAG, "prepare: invalid state" + mState);
            return false;
        }
        for (int i = 0;i < mFormats.length;i++) {
            if (mFormats[i].mWrapper != null) {
                mFormats[i].mWrapper.start();
            }
        }
        mExtractor.start();
        mState = PlayerState.PLAYING;
        return true;
    }
    boolean seek(int position) {
        Log.i(TAG, "seek: called:" + position);

        mExtractor.stopThread();
        for (int i = 0;i < mFormats.length;i++) {
            if (mFormats[i].mWrapper != null) {
                mFormats[i].mWrapper.flush();
            }
        }
        Log.i(TAG, "seek: all threads stopped.");
        mClock.setBaseTime(position);
        mExtractor.seek(position);
        mExtractor.start();
        return true;
    }
    public boolean pause(){
        boolean pauseOn = true;
        if (mState == PlayerState.PLAYING) {
            mState = PlayerState.PAUSED;
        } else if (mState == PlayerState.PAUSED){
            pauseOn = false;
            mState = PlayerState.PLAYING;
        } else {
            Log.e(TAG, "pause: invalid state " + mState);
            return false;
        }
        Log.i(TAG, "pause: called " + pauseOn);
        for (int i = 0;i < mFormats.length;i++) {
            if (mFormats[i].mWrapper != null) {
                mFormats[i].mWrapper.pause(pauseOn);
            }
        }
        return true;
    }
    public boolean stop() {
        Log.i(TAG, "stop: called");
        for (int i = 0;i < mFormats.length;i++) {
            if (mFormats[i].mWrapper != null) {
                mFormats[i].mWrapper.stop();
            }
        }
        if (mExtractor != null)
            mExtractor.stop();
        return true;
    }
    @Override
    public boolean writeCallback(ByteBuffer buffer, long pts, int index) {
        return mFormats[index].mWrapper.write(buffer,pts);
    }

    public void notifyPlayPosition(long pos) {
        if (mCallback != null) {
            mCallback.notifyPlayPosition(this, pos, mDuration);
        }
    }
}
