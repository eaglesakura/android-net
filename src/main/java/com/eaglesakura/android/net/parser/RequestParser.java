package com.eaglesakura.android.net.parser;


import com.eaglesakura.android.net.Result;

import java.io.InputStream;

/**
 * オブジェクトのパースを行う
 * <p>
 * InputStream内でキャンセルチェックを行い、必要に応じて例外を投げるため明示的なcancelチェックを行わない。
 *
 * 入力されたdataのclose処理は外部で行うため、内部で気にする必要はない。
 */
public interface RequestParser<T> {
    T parse(Result<T> sender, InputStream data) throws Exception;
}