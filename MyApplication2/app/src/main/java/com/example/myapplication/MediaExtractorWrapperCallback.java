package com.example.myapplication;

import java.nio.ByteBuffer;

public interface MediaExtractorWrapperCallback {
    boolean  writeCallback(ByteBuffer buffer, long pts, int index);
}
