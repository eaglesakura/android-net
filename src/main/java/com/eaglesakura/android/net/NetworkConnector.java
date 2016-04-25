package com.eaglesakura.android.net;

import com.eaglesakura.android.net.cache.ICacheController;
import com.eaglesakura.android.net.cache.file.FileCacheController;
import com.eaglesakura.android.net.cache.tkvs.TextCacheController;
import com.eaglesakura.android.net.internal.AndroidHttpClientResultImpl;
import com.eaglesakura.android.net.internal.HttpResult;
import com.eaglesakura.android.net.internal.CallbackHolder;
import com.eaglesakura.android.net.parser.RequestParser;
import com.eaglesakura.android.net.request.ConnectRequest;
import com.eaglesakura.android.net.stream.ByteArrayStreamController;
import com.eaglesakura.android.net.stream.IStreamController;
import com.eaglesakura.android.net.stream.RawStreamController;

import android.content.Context;

import java.io.File;
import java.io.IOException;

/**
 * ネットワークの接続制御を行う
 *
 */
public class NetworkConnector {
    private final Context mContext;

    private IStreamController mStreamController;

    private ICacheController mCacheController;

    public NetworkConnector(Context context) {
        mContext = context.getApplicationContext();
        mStreamController = new RawStreamController();
    }

    public Context getContext() {
        return mContext;
    }

    public void setStreamController(IStreamController streamController) {
        this.mStreamController = streamController;
    }

    public IStreamController getStreamController() {
        return mStreamController;
    }

    public void setCacheController(ICacheController cacheController) {
        this.mCacheController = cacheController;
    }

    public ICacheController getCacheController() {
        return mCacheController;
    }

    /**
     * テキストのREST APIを利用するコネクタを生成する
     */
    public static NetworkConnector createRestful(Context context) {
        NetworkConnector result = new NetworkConnector(context);
        TextCacheController cacheController = new TextCacheController(context);
        cacheController.setEncodeBase64(false);

        result.setCacheController(cacheController);
        result.setStreamController(new ByteArrayStreamController());
        return result;
    }

    /**
     * バイナリを取得するコネクタを生成する
     */
    public static NetworkConnector createBinaryApi(Context context, File cacheDir) throws IOException {
        NetworkConnector result = new NetworkConnector(context);
        result.setCacheController(new FileCacheController(cacheDir));
        result.setStreamController(new RawStreamController());
        return result;
    }

    /**
     * ネットワーク接続クラスを取得する。
     *
     * @param request 通信リクエスト
     * @param parser  通信パーサ
     * @param <T>     戻り値の型
     * @return 実行タスク
     */
    public <T> Result<T> connect(ConnectRequest request, RequestParser<T> parser, CancelCallback<T> cancelCallback) throws IOException {
//        final BaseHttpResult<T> connection = new GoogleHttpClientResultImpl<>(this, mRequest, mParser);
        final HttpResult<T> connection = new AndroidHttpClientResultImpl<>(this, request, parser);
        CallbackHolder<T> holder = new CallbackHolder<>(cancelCallback, connection);
        connection.connect(holder);
        return connection;
    }

    public interface CancelCallback<T> {
        /**
         * タスクをキャンセルさせる場合はtrue
         */
        boolean isCanceled(Result<T> connection);
    }
}
