package com.eaglesakura.android.net.error;

import java.io.IOException;
import java.util.List;

/**
 * HTTPアクセスのリトライ上限を超えた
 */
public class HttpAccessRetryFailedException extends IOException {

    final int mTryCount;

    final List<IOException> mErrorList;

    public HttpAccessRetryFailedException(String message, int tryCount, List<IOException> errorList) {
        super(message);
        mTryCount = tryCount;
        mErrorList = errorList;
    }

    public int getTryCount() {
        return mTryCount;
    }

    public List<IOException> getErrorList() {
        return mErrorList;
    }
}
