package com.eaglesakura.android.net.request;

import com.eaglesakura.android.net.ErrorPolicy;
import com.eaglesakura.android.net.RetryPolicy;
import com.eaglesakura.android.net.cache.CachePolicy;
import com.eaglesakura.util.EncodeUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class SimplePostRequest extends ConnectRequest {
    private CachePolicy mCachePolicy = new CachePolicy();

    private RetryPolicy mRetryPolicy = new RetryPolicy(10);

    private ErrorPolicy mErrorPolicy = new ErrorPolicy();

    private byte[] mPostBuffer;

    private File mPostFile;

    private String mContentType;

    public SimplePostRequest() {
        super(Method.POST);
    }

    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * オンメモリのバッファをPOSTする
     */
    public void setPostBuffer(String contentType, byte[] buffer) {
        this.mPostBuffer = buffer;
        this.mContentType = contentType;
    }

    /**
     * FORM形式でkey-valueのセットを送信する
     *
     * UTF-8エンコーディングを使用する
     */
    public void setPostFormParameters(Map<String, String> values) {
        StringBuffer buffer = new StringBuffer();

        for (Map.Entry<String, String> entry : values.entrySet()) {
            buffer.append(EncodeUtil.toUrl(entry.getKey()));
            buffer.append("=");
            buffer.append(EncodeUtil.toUrl(entry.getValue()));
            buffer.append("&");
        }

        setPostBuffer("application/x-www-form-urlencoded", buffer.toString().getBytes());
    }

    /**
     * ローカルにあるファイルをPOSTする
     */
    public void setPostFile(String contentType, File file) throws IOException {
        if (!file.isFile() || file.length() <= 0) {
            throw new IOException("file access failed :: " + file.getAbsolutePath());
        }

        this.mPostFile = file;
        this.mContentType = contentType;
    }

    @Override
    public CachePolicy getCachePolicy() {
        return mCachePolicy;
    }

    @Override
    public RetryPolicy getRetryPolicy() {
        return mRetryPolicy;
    }

    @Override
    public ErrorPolicy getErrorPolicy() {
        return mErrorPolicy;
    }

    @Override
    public ConnectContent getContent() {
        if (mPostBuffer != null) {
            return new ConnectContent() {
                @Override
                public long getLength() {
                    return mPostBuffer.length;
                }

                @Override
                public InputStream openStream() throws IOException {
                    return new ByteArrayInputStream(mPostBuffer);
                }

                @Override
                public String getContentType() {
                    return mContentType;
                }
            };
        } else {
            final long length = mPostFile.length();
            return new ConnectContent() {
                @Override
                public long getLength() {
                    return length;
                }

                @Override
                public InputStream openStream() throws IOException {
                    return new FileInputStream(mPostFile);
                }

                @Override
                public String getContentType() {
                    return mContentType;
                }
            };
        }
    }
}
