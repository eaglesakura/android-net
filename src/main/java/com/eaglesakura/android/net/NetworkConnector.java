package com.eaglesakura.android.net;

import com.eaglesakura.android.net.cache.ICacheController;
import com.eaglesakura.android.net.cache.tkvs.TextCacheController;
import com.eaglesakura.android.net.internal.BaseHttpConnection;
import com.eaglesakura.android.net.internal.GoogleHttpClientConnectImpl;
import com.eaglesakura.android.net.parser.RequestParser;
import com.eaglesakura.android.net.request.ConnectRequest;
import com.eaglesakura.android.net.stream.IStreamController;
import com.eaglesakura.android.net.stream.RawStreamController;
import com.eaglesakura.android.rx.RxTask;
import com.eaglesakura.android.rx.RxTaskBuilder;
import com.eaglesakura.android.rx.SubscriptionController;

import android.content.Context;

/**
 * ネットワークの接続制御を行う
 * <p/>
 * 通信そのものは専用スレッドで行われるため、UI/Backgroundどちらからも使用することができる。
 * 同期的に結果を得たい場合はawait()でタスク待ちを行えば良い。
 */
public class NetworkConnector {
    private final Context mContext;

    private IStreamController streamController;

    private ICacheController cacheController;

    private SubscriptionController mSubscriptionController;

    public NetworkConnector(Context context) {
        mContext = context.getApplicationContext();
        streamController = new RawStreamController();
        cacheController = new TextCacheController(mContext);
    }

    public Context getContext() {
        return mContext;
    }

    public void setSubscriptionController(SubscriptionController subscriptionController) {
        mSubscriptionController = subscriptionController;
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
     * 外部接続を行う
     *
     * @param subscription スレッド制御クラス
     * @param request      通信リクエスト
     * @param parser       通信パーサ
     * @param <T>          戻り値の型
     * @return 実行タスク
     */
    public <T> RxTaskBuilder<Connection<T>> connect(SubscriptionController subscription, ConnectRequest request, RequestParser<T> parser) {
        final BaseHttpConnection<T> connection = new GoogleHttpClientConnectImpl<>(this, request, parser);

        return new RxTaskBuilder<Connection<T>>(subscription).async(new RxTask.Async<Connection<T>>() {
            @Override
            public Connection<T> call(RxTask<Connection<T>> task) throws Throwable {
                connection.connect(task);
                return connection;
            }
        });
    }
}
