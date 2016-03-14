package com.eaglesakura.android.net.parser;

import com.eaglesakura.android.net.Result;
import com.eaglesakura.json.JSON;

import java.io.InputStream;

public class JsonParser<T> implements RequestParser<T> {
    Class<T> clazz;

    public JsonParser(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public T parse(Result<T> sender, InputStream data) throws Exception {
        return JSON.decode(data, clazz);
    }
}
