package com.eaglesakura.android.net;

import com.eaglesakura.android.net.error.HttpStatusException;
import com.eaglesakura.android.net.request.ConnectRequest;

import android.support.annotation.FloatRange;

import java.io.IOException;

/**
 * リトライ設定
 */
public class RetryPolicy {
    int mRetryNum = 3;

    float mBackoff = 1.25f;

    float mTimeoutBackoff = 2.0f;

    long mBaseWaitTime = 1000;

    public RetryPolicy(int retryNum) {
        this.mRetryNum = retryNum;
    }

    public RetryPolicy(int retryNum, float backoff, long baseWaitTime) {
        this.mRetryNum = retryNum;
        this.mBackoff = backoff;
        this.mBaseWaitTime = baseWaitTime;
    }

    public int getRetryNum() {
        return mRetryNum;
    }

    public void setRetryNum(int retryNum) {
        if (retryNum < 0) {
            throw new IllegalArgumentException();
        }
        this.mRetryNum = retryNum;
    }

    public float getBackoff() {
        return mBackoff;
    }

    public float getTimeoutBackoff() {
        return mTimeoutBackoff;
    }

    public void setTimeoutBackoff(float timeoutBackoff) {
        mTimeoutBackoff = timeoutBackoff;
    }

    public void setBackoff(@FloatRange(from = 1.0) float backoff) {
        if (backoff < 1.0f) {
            throw new IllegalArgumentException();
        }
        this.mBackoff = backoff;
    }

    public long getBaseWaitTime() {
        return mBaseWaitTime;
    }

    public void setBaseWaitTime(long baseWaitTime) {
        if (baseWaitTime <= 0) {
            throw new IllegalArgumentException();
        }
        this.mBaseWaitTime = baseWaitTime;
    }

    /**
     * 次のバックオフ時間を取得する
     */
    public long nextBackoffTimeMs(int retryNum, long currentBackoff) {
        float result = (float) currentBackoff;
        for (int i = 0; i < retryNum; ++i) {
            result *= mBackoff;
        }
        return (long) result;
    }


    /**
     * エラーリトライを行う場合true
     *
     * @param connector connector
     * @param request   対象リクエスト
     * @param error     エラー内容
     * @return リトライを行う場合true
     */
    public boolean isRetryableError(NetworkConnector connector, ConnectRequest request, IOException error) throws IOException {
        // HTTPステータス異常でないなら、エラーハンドリングを行なう
        return !(error instanceof HttpStatusException);
    }
}
