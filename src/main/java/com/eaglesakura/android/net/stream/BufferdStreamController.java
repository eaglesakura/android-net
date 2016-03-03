package com.eaglesakura.android.net.stream;

import com.eaglesakura.android.net.Connection;
import com.eaglesakura.android.net.HttpHeader;
import com.eaglesakura.android.rx.error.RxTaskException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BufferdStreamController implements IStreamController {
    int bufferBytes = 1024 * 32;

    public BufferdStreamController() {
    }

    public BufferdStreamController(int bufferBytes) {
        this.bufferBytes = bufferBytes;
    }

    @Override
    public <T> InputStream wrapStream(Connection<T> connection, HttpHeader respHeader, InputStream originalStream) throws IOException, RxTaskException {
        return new BufferedInputStream(originalStream, bufferBytes);
    }
}
