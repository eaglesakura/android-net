package com.eaglesakura.android.net.stream;

import com.eaglesakura.android.net.Connection;
import com.eaglesakura.android.net.HttpHeader;
import com.eaglesakura.android.thread.async.error.TaskException;
import com.eaglesakura.util.IOUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 通信結果を一度ByteArrayに変換するコントローラ
 */
public class ByteArrayStreamController implements IStreamController {

    @Override
    public <T> InputStream wrapStream(Connection<T> connection, HttpHeader respHeader, InputStream originalStream) throws IOException, TaskException {
        byte[] buffer = IOUtil.toByteArray(originalStream, false);
        return new ByteArrayInputStream(buffer);
    }
}
