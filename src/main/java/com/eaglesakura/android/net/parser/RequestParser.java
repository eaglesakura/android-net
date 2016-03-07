package com.eaglesakura.android.net.parser;


import com.eaglesakura.android.net.Result;

import java.io.InputStream;

/**
 * オブジェクトのパースを行う
 * <p>
 * InputStream内でキャンセルチェックを行い、必要に応じて例外を投げるため明示的なcancelチェックを行わない。
 */
public interface RequestParser<T> {
    T parse(Result<T> sender, InputStream data) throws Exception;
}