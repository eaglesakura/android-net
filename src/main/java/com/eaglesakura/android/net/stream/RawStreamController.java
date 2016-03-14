package com.eaglesakura.android.net.stream;

import com.eaglesakura.android.net.HttpHeader;
import com.eaglesakura.android.net.Result;

import java.io.IOException;
import java.io.InputStream;

public class RawStreamController implements IStreamController {
    @Override
    public <T> InputStream wrapStream(Result<T> connection, HttpHeader respHeader, InputStream originalStream) throws IOException {
        return originalStream;
    }
}
