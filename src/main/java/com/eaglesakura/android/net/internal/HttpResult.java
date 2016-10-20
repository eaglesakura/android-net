package com.eaglesakura.android.net.internal;

import com.eaglesakura.android.net.HttpHeader;
import com.eaglesakura.android.net.NetworkConnector;
import com.eaglesakura.android.net.NetworkProfile;
import com.eaglesakura.android.net.Result;
import com.eaglesakura.android.net.RetryPolicy;
import com.eaglesakura.android.net.cache.ICacheController;
import com.eaglesakura.android.net.cache.ICacheWriter;
import com.eaglesakura.android.net.error.HttpAccessFailedException;
import com.eaglesakura.android.net.parser.RequestParser;
import com.eaglesakura.android.net.request.ConnectRequest;
import com.eaglesakura.android.net.stream.IStreamController;
import com.eaglesakura.util.IOUtil;
import com.eaglesakura.util.StringUtil;
import com.eaglesakura.util.Timer;
import com.eaglesakura.util.Util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.security.MessageDigest;

/**
 * HTTP接続本体を行う
 */
public abstract class HttpResult<T> extends Result<T> {

    protected final RequestParser<T> mParser;

    protected final ConnectRequest mRequest;

    protected final NetworkConnector mConnector;

    /**
     * キャッシュから生成されたダイジェスト
     */
    protected String mCacheDigest;

    /**
     * ネットワークから生成されたダイジェスト
     */
    protected String mNetDigest;

    /**
     * ヘッダ戻り値
     */
    protected HttpHeader mResponseHeader;

    protected NetworkProfileImpl mProfile = new NetworkProfileImpl();

    /**
     * parseされた戻り値
     */
    protected T mResult;

    public HttpResult(NetworkConnector connector, ConnectRequest request, RequestParser<T> parser) {
        this.mParser = parser;
        this.mRequest = request;
        this.mConnector = connector;
    }

    @Override
    public T getResult() {
        return mResult;
    }

    @Override
    public ConnectRequest getRequest() {
        return mRequest;
    }

    public ICacheController getCacheController() {
        return mConnector.getCacheController();
    }

    @NonNull
    @Override
    public NetworkProfile getProfile() {
        return mProfile;
    }

    @Override
    public String getCacheDigest() {
        return mCacheDigest;
    }

    @Override
    public String getContentDigest() {
        return mNetDigest;
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
        IStreamController controller = mConnector.getStreamController();
        InputStream readStream = null;
        NetworkParseInputStream parseStream = null;
        try {
            parseStream = new NetworkParseInputStream(stream, mProfile, cacheWriter, digest, callback);
            if (controller != null) {
                readStream = controller.wrapStream(this, respHeader, parseStream);
            } else {
                readStream = stream;
            }
            T parsed = mParser.parse(this, readStream);
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
            ICacheController controller = mConnector.getCacheController();
            if (controller != null) {
                return controller.newCacheWriter(mRequest, header);
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
        ICacheController controller = mConnector.getCacheController();
        if (controller == null) {
            return null;
        }

        MessageDigest digest = newMessageDigest();
        InputStream stream = null;
        try {
            stream = controller.openCache(mRequest);
            if (stream == null) {
                // キャッシュが無いので何もできない
                return null;
            }
            T parsed = parseFromStream(taskResult, null, stream, null, digest);
            if (parsed != null) {
                // パースに成功したら指紋を残す
                mCacheDigest = StringUtil.toHexString(digest.digest());
            }
            return parsed;
        } catch (FileNotFoundException e) {
            // キャッシュが見つからない場合はログも吐かない
            return null;
        } catch (Exception e) {
            // キャッシュ読み込み失敗は無視する
            e.printStackTrace();
            return null;
        } finally {
            IOUtil.close(stream);
        }
    }

    /**
     * 接続を行う
     */
    protected abstract T tryNetworkParse(CallbackHolder<T> callback, MessageDigest digest) throws IOException;

    @Nullable
    @Override
    public HttpHeader getResponseHeader() {
        return mResponseHeader;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    /**
     * streamから戻り値のパースを行う
     */
    private T parseFromStream(CallbackHolder<T> callback) throws IOException {
        RetryPolicy retryPolicy = mRequest.getRetryPolicy();
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
                mResponseHeader = new HttpHeader();
                MessageDigest digest = newMessageDigest();
                T parsed = tryNetworkParse(callback, digest);
                if (parsed != null) {
                    mNetDigest = StringUtil.toHexString(digest.digest());
                    return parsed;
                }
            } catch (HttpAccessFailedException e) {
                // この例外はリトライしても無駄
                throw e;
            } catch (IOException e) {
                // その他のIO例外はひとまずリトライくらいはできる
                if (!mRequest.getRetryPolicy().isRetryableError(mConnector, mRequest, e)) {
                    e.printStackTrace();
                    // リトライ対象の例外ではない
                    throw e;
                }
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

        throw new IOException("Connection Failed try : " + (tryCount - 1) + " : " + getRequest().getUrl());
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
