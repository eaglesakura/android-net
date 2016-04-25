package com.eaglesakura.android.net;

import com.eaglesakura.util.StringUtil;

import android.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HttpHeader {
    Map<String, String> mValues = new HashMap<>();
    Set<String> mCookies = new HashSet<>();

    public static final String HEADER_ETAG = "ETag";
    public static final String HEADER_CONTENT_LENGTH = "Content-Length";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_CONTENT_RANGE = "Content-Range";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_RANGE = "Range";
    public static final String HEADER_SET_COOKIE = "Set-Cookie";
    public static final String HEADER_COOKIE = "Cookie";

    public HttpHeader() {
    }

    public HttpHeader put(String key, String value) {
        if (HEADER_SET_COOKIE.equals(key) || HEADER_COOKIE.equals(key)) {
            mCookies.add(value);
        } else {
            mValues.put(key, value);
        }

        return this;
    }

    /**
     * Cookie一覧を取得する
     */
    public List<String> getCookies() {
        return new ArrayList<>(mCookies);
    }

    /**
     * コンテンツのRangeを指定してダウンロードする。
     */
    public HttpHeader range(long offset, long length) {
        String result = String.format("bytes=%d-%d", offset, (offset + length - 1));
        mValues.put(HEADER_RANGE, result);
        return this;
    }

    /**
     * ダウンロードするコンテンツの最大サイズを取得する。
     */
    public long getContentFullSize() {
        String range = mValues.get(HEADER_CONTENT_RANGE);
        String length = mValues.get(HEADER_CONTENT_LENGTH);
        try {
            if (!StringUtil.isEmpty(range)) {
                String[] split = range.split("/");
                return Long.parseLong(split[1]);
            }

            if (!StringUtil.isEmpty(length)) {
                return Long.parseLong(length);
            }
        } catch (Exception e) {
        }
        return -1;
    }

    public String getContentType() {
        return mValues.get(HEADER_CONTENT_TYPE);
    }

    /**
     * ヘッダを取得する
     */
    public String get(String key) {
        return mValues.get(key);
    }

    /**
     * Key-Valueペアを全て列挙する。
     */
    public List<Pair<String, String>> listHeaderKeyValues() {
        List<Pair<String, String>> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : mValues.entrySet()) {
            result.add(new Pair<>(entry.getKey(), entry.getValue()));
        }
        for (String cookie : mCookies) {
            result.add(new Pair<>(HEADER_COOKIE, cookie));
        }
        return result;
    }

    public Map<String, String> getValues() {
        return mValues;
    }
}
