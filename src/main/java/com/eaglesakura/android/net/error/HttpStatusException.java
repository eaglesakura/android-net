package com.eaglesakura.android.net.error;

import java.io.IOException;

public class HttpStatusException extends IOException {
    final int mStatusCode;

    public HttpStatusException(String detailMessage, int statusCode) {
        super(detailMessage);
        mStatusCode = statusCode;
    }

    public HttpStatusException(String message, Throwable cause, int statusCode) {
        super(message, cause);
        mStatusCode = statusCode;
    }

    public HttpStatusException(Throwable cause, int statusCode) {
        super(cause);
        mStatusCode = statusCode;
    }
}
