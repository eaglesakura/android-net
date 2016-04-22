package com.eaglesakura.android.net.error;

/**
 * 50xが発生したことを示す例外
 */
public class InternalServerErrorException extends HttpStatusException {
    public InternalServerErrorException(String message, Throwable cause, int statusCode) {
        super(message, cause, statusCode);
    }

    public InternalServerErrorException(String detailMessage, int statusCode) {
        super(detailMessage, statusCode);
    }

    public InternalServerErrorException(Throwable cause, int statusCode) {
        super(cause, statusCode);
    }
}
