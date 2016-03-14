package com.eaglesakura.android.net;

import android.content.Context;

import com.eaglesakura.android.net.cache.ICacheController;
import com.eaglesakura.android.net.cache.file.FileCacheController;
import com.eaglesakura.android.net.cache.tkvs.TextCacheController;
import com.eaglesakura.android.net.internal.BaseHttpResult;
import com.eaglesakura.android.net.internal.CallbackHolder;
import com.eaglesakura.android.net.internal.GoogleHttpClientResultImpl;
import com.eaglesakura.android.net.parser.RequestParser;
import com.eaglesakura.android.net.request.ConnectRequest;
import com.eaglesakura.android.net.stream.ByteArrayStreamController;
import com.eaglesakura.android.net.stream.IStreamController;
import com.eaglesakura.android.net.stream.RawStreamController;

import java.io.File;
import java.io.IOException;

/**
 * ネットワークの接続制御を行う
 * <p>
 * 通信そのものは専用スレッドで行われるため、UI/Backgroundどちらからも使用することができる。
 * 同期的に結果を得たい場合はawait()でタスク待ちを行えば良い。
 */
public class NetworkConnector {
    private final Context mContext;

    private IStreamController streamController;

    private ICacheController cacheController;

    public NetworkConnector(Context context) {
        mContext = context.getApplicationContext();
        streamController = new RawStreamController();
        cacheController = new TextCacheController(mContext);
    }

    public Context getContext() {
        return mContext;
    }

    public void setStreamController(IStreamController streamController) {
        this.streamController = streamController;
    }

    public IStreamController getStreamController() {
        return streamController;
    }

    public void setCacheController(ICacheController cacheController) {
        this.cacheController = cacheController;
    }

    public ICacheController getCacheController() {
        return cacheController;
    }

    /**
     * テキストのREST APIを利用するコネクタを生成する
     *
     * @param context
     * @return
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
     *
     * @param context
     * @return
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
        final BaseHttpResult<T> connection = new GoogleHttpClientResultImpl<>(this, request, parser);
        CallbackHolder<T> holder = new CallbackHolder<>(cancelCallback, connection);
        connection.connect(holder);
        return connection;
    }

    public interface CancelCallback<T> {
        /**
         * タスクをキャンセルさせる場合はtrue
         *
         * @return
         */
        boolean isCanceled(Result<T> connection);
    }
}
