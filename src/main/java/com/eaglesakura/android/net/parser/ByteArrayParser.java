package com.eaglesakura.android.net.parser;

import com.eaglesakura.android.net.Result;
import com.eaglesakura.util.IOUtil;

import java.io.InputStream;

/**
 * byte[]にパースする
 */
public class ByteArrayParser implements RequestParser<byte[]> {

    private ByteArrayParser() {

    }

    @Override
    public byte[] parse(Result<byte[]> sender, InputStream data) throws Exception {
        return IOUtil.toByteArray(data, false);
    }

    private static final ByteArrayParser instance = new ByteArrayParser();

    public static final ByteArrayParser getInstance() {
        return instance;
    }
}
