package com.eaglesakura.android.net;

/**
 * エラーハンドリングを行う
 */
public class ErrorPolicy {
    boolean mHandleErrorStream;

    public boolean isHandleErrorStream() {
        return mHandleErrorStream;
    }

    public void setHandleErrorStream(boolean handleErrorStream) {
        mHandleErrorStream = handleErrorStream;
    }
}
