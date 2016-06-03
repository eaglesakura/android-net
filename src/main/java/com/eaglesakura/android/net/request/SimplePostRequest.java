package com.eaglesakura.android.net.request;

import com.eaglesakura.android.net.RetryPolicy;
import com.eaglesakura.android.net.cache.CachePolicy;
import com.eaglesakura.util.EncodeUtil;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

public class SimplePostRequest extends ConnectRequest {
    private CachePolicy cachePolicy = new CachePolicy();

    private RetryPolicy retryPolicy = new RetryPolicy(10);

    private byte[] buffer;

    private File localFile;

    private String contentType;

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
        this.buffer = buffer;
        this.contentType = contentType;
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

        this.localFile = file;
        this.contentType = contentType;
    }

    @Override
    public CachePolicy getCachePolicy() {
        return cachePolicy;
    }

    @Override
    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    @Override
    public ConnectContent getContent() {
        if (buffer != null) {
            return new ConnectContent() {
                @Override
                public long getLength() {
                    return buffer.length;
                }

                @Override
                public InputStream openStream() throws IOException {
                    return new ByteArrayInputStream(buffer);
                }

                @Override
                public String getContentType() {
                    return contentType;
                }
            };
        } else {
            final long length = localFile.length();
            return new ConnectContent() {
                @Override
                public long getLength() {
                    return length;
                }

                @Override
                public InputStream openStream() throws IOException {
                    return new FileInputStream(localFile);
                }

                @Override
                public String getContentType() {
                    return contentType;
                }
            };
        }
    }
}
