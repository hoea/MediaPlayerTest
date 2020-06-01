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

        MediaCodecVideoWrapper video = null;
        // TODO:XXX
        for (int i = 0;i < formats.length;i++) {
            mFormats[i] = new MediaFormatInfo(formats[i], null);
            String mime = formats[i].getString(MediaFormat.KEY_MIME);
            if (mime.indexOf("audio") != -1) {
                mFormats[i].mWrapper = new MediaCodecAudioWrapper(this);
                mClock = (MediaClock)mFormats[i].mWrapper;
                audioduration = formats[i].getLong(MediaFormat.KEY_DURATION);
            } else if (mime.indexOf("video") != -1) {
                mFormats[i].mWrapper = new MediaCodecVideoWrapper(mSurface);
                video = (MediaCodecVideoWrapper)(mFormats[i].mWrapper);
                videoduration = formats[i].getLong(MediaFormat.KEY_DURATION);
            }
            if (mFormats[i].mWrapper != null) {
                if (mFormats[i].mWrapper.init(mFormats[i].mFormat) == false) {
                    Log.e(TAG, "extract: invalid format" + mime);
                    return false;
                }
                mExtractor.selectTrack(i);
            }
        }
        mDuration = Math.max(videoduration,audioduration);
        mDuration = mDuration/1000;
        if (video != null) {
            video.setMediaClock(mClock);
        }
        if (mCallback != null) {
            mCallback.notifyPlayPosition(0, mDuration);
        }
        mState = PlayerState.PREPARED;
        return false;
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
        mExtractor.extract();
        mState = PlayerState.PLAYING;
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
            mCallback.notifyPlayPosition(pos, mDuration);
        }
    }
}