package com.eaglesakura.android.net.internal;

import com.eaglesakura.android.net.HttpHeader;
import com.eaglesakura.android.net.NetworkConnector;
import com.eaglesakura.android.net.Result;
import com.eaglesakura.android.net.RetryPolicy;
import com.eaglesakura.android.net.cache.ICacheController;
import com.eaglesakura.android.net.cache.ICacheWriter;
import com.eaglesakura.android.net.parser.RequestParser;
import com.eaglesakura.android.net.request.ConnectRequest;
import com.eaglesakura.android.net.stream.IStreamController;
import com.eaglesakura.util.IOUtil;
import com.eaglesakura.util.LogUtil;
import com.eaglesakura.util.StringUtil;
import com.eaglesakura.util.Timer;
import com.eaglesakura.util.Util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.security.MessageDigest;

/**
 * HTTP接続本体を行う
 */
public abstract class BaseHttpResult<T> extends Result<T> {

    protected final RequestParser<T> parser;

    protected final ConnectRequest request;

    protected final NetworkConnector connector;

    /**
     * キャッシュから生成されたダイジェスト
     */
    protected String cacheDigest;

    /**
     * ネットワークから生成されたダイジェスト
     */
    protected String netDigest;

    /**
     * parseされた戻り値
     */
    protected T mResult;

    public BaseHttpResult(NetworkConnector connector, ConnectRequest request, RequestParser<T> parser) {
        this.parser = parser;
        this.request = request;
        this.connector = connector;
    }

    @Override
    public T getResult() {
        return mResult;
    }

    @Override
    public ConnectRequest getRequest() {
        return request;
    }

    public ICacheController getCacheController() {
        return connector.getCacheController();
    }

    @Override
    public String getCacheDigest() {
        return cacheDigest;
    }

    @Override
    public String getContentDigest() {
        return netDigest;
    }

    protected MessageDigest newMessageDigest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * ネットワーク経由のInputStreamからパースを行う
     * ストリームのcloseは外部に任せる
     */
    protected T parseFromStream(CallbackHolder<T> callback, HttpHeader respHeader, InputStream stream, ICacheWriter cacheWriter, MessageDigest digest) throws Exception {
        // コンテンツをラップする
        // 必要に応じてファイルにキャッシュされたり、メモリに載せたりする。
        IStreamController controller = connector.getStreamController();
        InputStream readStream = null;
        NetworkParseInputStream parseStream = null;
        try {
            parseStream = new NetworkParseInputStream(stream, cacheWriter, digest, callback);
            if (controller != null) {
                readStream = controller.wrapStream(this, respHeader, parseStream);
            } else {
                readStream = stream;
            }
            T parsed = parser.parse(this, readStream);
            return parsed;
        } finally {
            if (readStream != parseStream) {
                IOUtil.close(parseStream);
            }
            IOUtil.close(readStream);
        }
    }

    protected ICacheWriter newCacheWriter(HttpHeader header) {
        try {
            ICacheController controller = connector.getCacheController();
            if (controller != null) {
                return controller.newCacheWriter(request, header);
            }
        } catch (Exception e) {
        }
        return null;
    }

    protected void closeCacheWriter(T result, ICacheWriter writer) {
        if (writer == null) {
            return;
        }

        try {
            if (result != null) {
                writer.commit();
            } else {
                writer.abort();
            }

        } catch (Exception e) {

        }

        IOUtil.close(writer);
    }

    /**
     * キャッシュからデータをパースする
     */
    private T tryCacheParse(CallbackHolder<T> taskResult) {
        ICacheController controller = connector.getCacheController();
        if (controller == null) {
            return null;
        }

        MessageDigest digest = newMessageDigest();
        InputStream stream = null;
        try {
            stream = controller.openCache(request);
            if (stream == null) {
                // キャッシュが無いので何もできない
                return null;
            }
            T parsed = parseFromStream(taskResult, null, stream, null, digest);
            if (parsed != null) {
                // パースに成功したら指紋を残す
                cacheDigest = StringUtil.toHexString(digest.digest());
            }
            return parsed;
        } catch (Exception e) {
            // キャッシュ読み込み失敗は無視する
            LogUtil.log(e);
            return null;
        } finally {
            IOUtil.close(stream);
        }
    }

    /**
     * 接続を行う
     */
    protected abstract T tryNetworkParse(CallbackHolder<T> callback, MessageDigest digest) throws IOException;

    /**
     * streamから戻り値のパースを行う
     */
    private T parseFromStream(CallbackHolder<T> callback) throws IOException {
        RetryPolicy retryPolicy = request.getRetryPolicy();
        int tryCount = 0;
        final int MAX_RETRY;
        long waitTime = 0;
        if (retryPolicy != null) {
            MAX_RETRY = retryPolicy.getRetryNum();
            waitTime = retryPolicy.getBaseWaitTime();
        } else {
            MAX_RETRY = 0;
        }

        Timer waitTimer = new Timer();
        // 施行回数が残っていたら通信を行う
        while ((++tryCount) <= (MAX_RETRY + 1)) {
            try {
                MessageDigest digest = newMessageDigest();
                T parsed = tryNetworkParse(callback, digest);
                if (parsed != null) {
                    netDigest = StringUtil.toHexString(digest.digest());
                    return parsed;
                }
            } catch (FileNotFoundException e) {
                // この例外はリトライしても無駄
                throw e;
            } catch (IOException e) {
                // その他のIO例外はひとまずリトライくらいはできる
                e.printStackTrace();
            }

            // 必要時間だけウェイトをかける
            {
                waitTimer.start();
                // キャンセルされてない、かつウェイト時間が残っていたら眠らせる
                while (!callback.isCanceled() && (waitTimer.end() < waitTime)) {
                    Util.sleep(1);
                }
                if (callback.isCanceled()) {
                    throw new InterruptedIOException("task canceled");
                }
            }


            waitTime = retryPolicy.nextBackoffTimeMs(tryCount, waitTime);
        }

        throw new IOException("Connection Failed try : " + tryCount);
    }


    /**
     * ネットワーク接続を行い、結果を返す
     */
    public void connect(CallbackHolder<T> callback) throws IOException {
        mResult = tryCacheParse(callback);
        if (mResult != null) {
            return;
        }

        if (callback.isCanceled()) {
            throw new InterruptedIOException();
        }

        mResult = parseFromStream(callback);
    }
}
