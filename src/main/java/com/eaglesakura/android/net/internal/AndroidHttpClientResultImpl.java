package com.eaglesakura.android.net.internal;

import com.eaglesakura.android.db.DBOpenType;
import com.eaglesakura.android.db.TextKeyValueStore;
import com.eaglesakura.android.net.HttpHeader;
import com.eaglesakura.android.net.NetworkConnector;
import com.eaglesakura.android.net.cache.ICacheWriter;
import com.eaglesakura.android.net.error.HttpAccessFailedException;
import com.eaglesakura.android.net.error.HttpStatusException;
import com.eaglesakura.android.net.error.InternalServerErrorException;
import com.eaglesakura.android.net.parser.RequestParser;
import com.eaglesakura.android.net.request.ConnectContent;
import com.eaglesakura.android.net.request.ConnectRequest;
import com.eaglesakura.util.CollectionUtil;
import com.eaglesakura.util.IOUtil;
import com.eaglesakura.util.StringUtil;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.MessageDigest;

/**
 * HttpUrlConnectionで接続試行を行う
 */
public class AndroidHttpClientResultImpl<T> extends HttpResult<T> {

    /**
     * 最後に受信したデータハッシュ
     */
    private final String mLastReceivedDigest;

    private static final String DATABASE_NAME = "es_net_cache.db";
    private static final String DATABASE_TABLE_NAME = "DIGEST_CACHE";

    private final String mDigestCacheKey;

    private long mReadTimeoutMs;

    private long mConnectTimeoutMs;

    public AndroidHttpClientResultImpl(NetworkConnector connector, ConnectRequest request, RequestParser<T> parser) {
        super(connector, request, parser);

        mReadTimeoutMs = request.getReadTimeoutMs();
        mConnectTimeoutMs = request.getConnectTimeoutMs();
        mDigestCacheKey = "dig." + request.getCachePolicy().getCacheKey(request);
        Context context = connector.getContext();
        TextKeyValueStore kvs = new TextKeyValueStore(context, new File(context.getCacheDir(), DATABASE_NAME), DATABASE_TABLE_NAME);
        try {
            kvs.open(DBOpenType.Read);
            mLastReceivedDigest = kvs.getOrNull(mDigestCacheKey);
        } finally {
            kvs.close();
        }
    }

    @Override
    public boolean isModified() {
        if (!StringUtil.isEmpty(mCacheDigest)) {
            // キャッシュから取得したらデータは変動しない
            return false;
        }
        if (StringUtil.isEmpty(mLastReceivedDigest)) {
            return true;
        } else {
            return !mLastReceivedDigest.equals(mNetDigest);
        }
    }

    private void close(HttpURLConnection connection) {
        try {
            if (connection != null) {
                connection.disconnect();
            }
        } catch (Exception e) {

        }
    }

    private void setRequestHeaders(HttpURLConnection connection) throws Throwable {
        CollectionUtil.each(mRequest.getHeader().listHeaderKeyValues(), it -> {
            connection.addRequestProperty(it.first, it.second);
        });
    }

    private void writeContents(CallbackHolder<T> callback, HttpURLConnection connection) throws IOException {
        if (!mRequest.getMethod().hasContent()) {
            // メソッドによっては不要である
            return;
        }

        ConnectContent content = mRequest.getContent();
        long length = content.getLength();
        if (length <= 0) {
            // no content
            return;
        } else {
            connection.addRequestProperty(HttpHeader.HEADER_CONTENT_LENGTH, String.valueOf(length));
        }

        String contentType = content.getContentType();
        if (!StringUtil.isEmpty(contentType)) {
            connection.addRequestProperty(HttpHeader.HEADER_CONTENT_TYPE, contentType);
        }
        connection.setDoOutput(true);

        // データを書き込む
        InputStream is = null;
        OutputStream os = null;
        try {
            is = content.openStream();
            os = connection.getOutputStream();

            byte[] buffer = new byte[1024 * 4];
            int read;
            while (length > 0) {
                // キャンセルチェック
                if (callback.isCanceled()) {
                    throw new InterruptedIOException("task canceled");
                }

                read = is.read(buffer, 0, (int) Math.min(buffer.length, length));
                if (read <= 0) {
                    // コンテンツが必要なのに末端に達したらエラーとなる
                    throw new IOException("Content Length Error");
                }
                os.write(buffer, 0, read);
                length -= read;
            }

            // 書き込みに成功
        } finally {
            IOUtil.close(is);
            IOUtil.close(os);
        }
    }

    /**
     * ヘッダを解析する
     */
    private void parseResponceHeader(HttpURLConnection connection) throws Throwable {
        CollectionUtil.each(connection.getHeaderFields(), (key, value) -> {
            if (CollectionUtil.isEmpty(value)) {
                return;
            }
            for (String it : value) {
                mResponceHeader.put(key, it);
            }
        });
    }

    @Override
    protected T tryNetworkParse(CallbackHolder<T> callback, MessageDigest digest) throws IOException {

        URL url = new URL(mRequest.getUrl());
        HttpURLConnection connection = null;
        InputStream readContent = null;
        ICacheWriter cacheWriter = null;
        T result = null;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(mRequest.getMethod().toString());
            connection.setInstanceFollowRedirects(true);
            connection.setReadTimeout((int) mReadTimeoutMs);
            connection.setConnectTimeout((int) mConnectTimeoutMs);

            // ヘッダを設定する
            setRequestHeaders(connection);
            // コンテンツを書き込む
            writeContents(callback, connection);

            // 戻り値を得る
            final int RESP_CODE = connection.getResponseCode();

            if (callback.isCanceled()) {
                throw new InterruptedIOException("task canceled");
            }

            if ((RESP_CODE / 100) == 4) {
                throw new HttpAccessFailedException("Status Code == " + RESP_CODE + " :: " + mRequest.getUrl(), RESP_CODE).setErrorResponse(connection, mRequest, callback);
            } else if ((RESP_CODE / 100) == 5) {
                throw new InternalServerErrorException("InternalServerError :: " + RESP_CODE + " :: " + mRequest.getUrl(), RESP_CODE).setErrorResponse(connection, mRequest, callback);
            } else if ((RESP_CODE / 100) != 2) {
                // その他、2xx以外のステータスコードはエラーとなる
                throw new HttpStatusException("Resp != 2xx [" + RESP_CODE + "]" + " :: " + mRequest.getUrl(), RESP_CODE).setErrorResponse(connection, mRequest, callback);
            }

            parseResponceHeader(connection);
            readContent = connection.getInputStream();
//            readContent = connection.getErrorStream();

            cacheWriter = newCacheWriter(getResponceHeader());

            // コンテンツのパースを行わせる
            try {
                result = parseFromStream(callback, getResponceHeader(), readContent, cacheWriter, digest);
                return result;
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        } catch (SocketTimeoutException e) {
            // タイムアウト時間が短いようなので、長くする
            mReadTimeoutMs = (long) (mReadTimeoutMs * mRequest.getRetryPolicy().getTimeoutBackoff());
            mConnectTimeoutMs = (long) (mConnectTimeoutMs * mRequest.getRetryPolicy().getTimeoutBackoff());
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Throwable e) {
            throw new IOException(e);
        } finally {
            IOUtil.close(readContent);
            close(connection);
            closeCacheWriter(result, cacheWriter);
        }
    }

    @Override
    public void connect(CallbackHolder<T> callback) throws IOException {
        super.connect(callback);
        if (!StringUtil.isEmpty(mNetDigest)) {
            // digestを保存する
            Context context = mConnector.getContext();
            TextKeyValueStore kvs = new TextKeyValueStore(context, new File(context.getCacheDir(), DATABASE_NAME), DATABASE_TABLE_NAME);
            try {
                kvs.open(DBOpenType.Write);
                kvs.putDirect(mDigestCacheKey, mNetDigest);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                kvs.close();
            }
        }
    }
}
