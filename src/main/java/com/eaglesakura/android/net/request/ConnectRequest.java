package com.eaglesakura.android.net.request;

import com.eaglesakura.android.net.ErrorPolicy;
import com.eaglesakura.android.net.HttpHeader;
import com.eaglesakura.android.net.RetryPolicy;
import com.eaglesakura.android.net.cache.CachePolicy;

public abstract class ConnectRequest {
    public enum Method {
        GET {
            @Override
            public boolean hasContent() {
                return false;
            }

            @Override
            public String toString() {
                return "GET";
            }
        },
        POST {
            @Override
            public boolean hasContent() {
                return true;
            }

            @Override
            public String toString() {
                return "POST";
            }
        },
        HEAD {
            @Override
            public boolean hasContent() {
                return false;
            }

            @Override
            public String toString() {
                return "HEAD";
            }
        },
        DELETE {
            @Override
            public boolean hasContent() {
                return false;
            }

            @Override
            public String toString() {
                return "DELETE";
            }
        },
        PUT {
            @Override
            public boolean hasContent() {
                return true;
            }

            @Override
            public String toString() {
                return "PUT";
            }
        };

        public abstract boolean hasContent();
    }

    private final Method method;

    protected String url;

    protected HttpHeader header = new HttpHeader();

    /**
     * 通信タイムアウト時間を指定する
     */
    private long readTimeoutMs = 1000 * 10;

    /**
     * 接続タイムアウト時間を指定する
     */
    private long connectTimeoutMs = 1000 * 10;

    protected ConnectRequest(Method method) {
        this.method = method;
    }

    public Method getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }

    /**
     * 通信時のヘッダを取得する
     */
    public HttpHeader getHeader() {
        return header;
    }

    /**
     * タイムアウト時間を指定する
     */
    public void setReadTimeoutMs(long readTimeoutMs) {
        this.readTimeoutMs = readTimeoutMs;
    }

    public long getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public long getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(long connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    /**
     * キャッシュ制御を取得する
     * nullを返却した場合、キャッシュ制御を行わない
     */
    public abstract CachePolicy getCachePolicy();

    /**
     * リトライ制御を取得する
     * nullを返却した場合、リトライ制御を行わない。
     */
    public abstract RetryPolicy getRetryPolicy();

    /**
     * エラーハンドル制御を取得する
     */
    public abstract ErrorPolicy getErrorPolicy();

    /**
     * POST時のBodyを取得する
     * <p/>
     * nullを返却した場合、POST時に何もデータを付与しない。
     */
    public abstract ConnectContent getContent();
}
