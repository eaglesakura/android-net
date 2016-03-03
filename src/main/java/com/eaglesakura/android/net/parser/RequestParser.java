package com.eaglesakura.android.net.parser;


import com.eaglesakura.android.net.Connection;
import com.eaglesakura.android.rx.RxTask;

import java.io.InputStream;

/**
 * オブジェクトのパースを行う
 */
public interface RequestParser<T> {
    T parse(Connection<T> sender, RxTask task, InputStream data) throws Exception;
}