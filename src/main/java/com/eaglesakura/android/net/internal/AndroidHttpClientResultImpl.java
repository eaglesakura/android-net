package com.eaglesakura.android.net.internal;

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

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

/**
 * HttpUrlConnectionで接続試行を行う
 */
public class AndroidHttpClientResultImpl<T> extends HttpResult<T> {

    public AndroidHttpClientResultImpl(NetworkConnector connector, ConnectRequest request, RequestParser<T> parser) {
        super(connector, request, parser);
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
            connection.setReadTimeout((int) mRequest.getReadTimeoutMs());
            connection.setConnectTimeout((int) mRequest.getConnectTimeoutMs());

            // ヘッダを設定する
            setRequestHeaders(connection);
            // コンテンツを書き込む
            writeContents(callback, connection);

            // 戻り値を得る
            final int RESP_CODE = connection.getResponseCode();

            if (callback.isCanceled()) {
                throw new InterruptedIOException("task canceled");
            }

            if (RESP_CODE == 404 || RESP_CODE == 403) {
                throw new HttpAccessFailedException("Status Code == " + RESP_CODE, RESP_CODE);
            } else if ((RESP_CODE / 100) == 5) {
                throw new InternalServerErrorException("InternalServerError :: " + RESP_CODE, RESP_CODE);
            } else if ((RESP_CODE / 100) != 2) {
                // その他、2xx以外のステータスコードはエラーとなる
                throw new HttpStatusException("Resp != 2xx [" + RESP_CODE + "]", RESP_CODE);
            }

            parseResponceHeader(connection);
            readContent = connection.getInputStream();

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
}
