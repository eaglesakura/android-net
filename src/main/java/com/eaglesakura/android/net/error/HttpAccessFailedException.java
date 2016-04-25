package com.eaglesakura.android.net.error;

public class HttpAccessFailedException extends HttpStatusException {
    public HttpAccessFailedException(String detailMessage, int statusCode) {
        super(detailMessage, statusCode);
    }

    public HttpAccessFailedException(String message, Throwable cause, int statusCode) {
        super(message, cause, statusCode);
    }

    public HttpAccessFailedException(Throwable cause, int statusCode) {
        super(cause, statusCode);
    }
}
