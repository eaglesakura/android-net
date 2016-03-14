package com.eaglesakura.android.net.parser;

import com.eaglesakura.android.net.Result;
import com.eaglesakura.util.IOUtil;

import java.io.InputStream;

public class StringParser implements RequestParser<String> {
    private StringParser() {

    }

    @Override
    public String parse(Result<String> sender, InputStream data) throws Exception {
        return IOUtil.toString(data, false);
    }

    private static final StringParser instance = new StringParser();

    public static final StringParser getInstance() {
        return instance;
    }
}
