package com.eaglesakura.android.net.error;

import com.eaglesakura.android.net.internal.CallbackHolder;
import com.eaglesakura.android.net.request.ConnectRequest;
import com.eaglesakura.json.JSON;

import android.annotation.SuppressLint;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;

public class HttpStatusException extends IOException {
    final int mStatusCode;

    byte[] mErrorBuffer;

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

    public int getStatusCode() {
        return mStatusCode;
    }

    public boolean hasErrorBuffer() {
        return mErrorBuffer != null;
    }

    public byte[] getErrorBuffer() {
        return mErrorBuffer;
    }

    public String getErrorText() {
        return new String(mErrorBuffer);
    }

    public <T> T getErrorJson(Class<T> clazz) throws IOException {
        return JSON.decode(new ByteArrayInputStream(mErrorBuffer), clazz);
    }

    @SuppressLint("NewApi")
    public HttpStatusException setErrorResponse(HttpURLConnection connection, ConnectRequest request, CallbackHolder holder) throws IOException {
        if (request.getErrorPolicy() == null || !request.getErrorPolicy().isHandleErrorStream()) {
            return this;
        }

        byte[] buffer = new byte[1024];
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (
                InputStream is = connection.getErrorStream()
        ) {
            // バッファを全て読み取る
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);

                // キャンセルされた
                if (holder.isCanceled()) {
                    throw new InterruptedIOException();
                }
            }
        }

        mErrorBuffer = os.toByteArray();
        return this;
    }
}
