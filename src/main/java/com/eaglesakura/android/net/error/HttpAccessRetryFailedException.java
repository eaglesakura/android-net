package com.eaglesakura.android.net.error;

import java.io.IOException;

/**
 * HTTPアクセスのリトライ上限を超えた
 */
public class HttpAccessRetryFailedException extends IOException {
    public HttpAccessRetryFailedException() {
    }

    public HttpAccessRetryFailedException(String message) {
        super(message);
    }

    public HttpAccessRetryFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public HttpAccessRetryFailedException(Throwable cause) {
        super(cause);
    }
}
