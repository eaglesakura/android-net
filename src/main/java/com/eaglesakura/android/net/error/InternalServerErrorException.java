package com.eaglesakura.android.net.error;

import java.io.IOException;

/**
 * 50xが発生したことを示す例外
 */
public class InternalServerErrorException extends IOException {
    public InternalServerErrorException() {
    }

    public InternalServerErrorException(String detailMessage) {
        super(detailMessage);
    }

    public InternalServerErrorException(String message, Throwable cause) {
        super(message, cause);
    }

    public InternalServerErrorException(Throwable cause) {
        super(cause);
    }
}
