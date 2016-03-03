package com.eaglesakura.android.net.parser;

import com.eaglesakura.android.net.Connection;
import com.eaglesakura.android.rx.RxTask;
import com.eaglesakura.util.IOUtil;

import java.io.InputStream;

public class StringParser implements RequestParser<String> {
    private StringParser() {

    }

    @Override
    public String parse(Connection<String> sender, RxTask task, InputStream data) throws Exception {
        return IOUtil.toString(data, false);
    }

    private static final StringParser instance = new StringParser();

    public static final StringParser getInstance() {
        return instance;
    }
}
