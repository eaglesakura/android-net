package com.eaglesakura.android.net.internal;

import com.eaglesakura.android.net.NetworkProfile;
import com.eaglesakura.util.Timer;

import android.support.annotation.Nullable;

/**
 * プロファイリング状態を管理する
 */
class NetworkProfileImpl extends NetworkProfile {
    Integer mConnectionTimeMs;

    Integer mTurnaroundTimeMs;

    long mDownloadBytes;

    Timer mTimer = new Timer();

    void onConnectStart() {
        mTimer.start();
    }

    void onConnectionCompleted() {
        mConnectionTimeMs = (int) mTimer.end();
        mTimer.start();
    }

    /**
     * ダウンロードの進捗が進んだ
     */
    void onDownloadStep(int bytes) {
        mDownloadBytes += bytes;
    }

    void onDownloadCompleted() {
        mTurnaroundTimeMs = (int) mTimer.end();
    }

    @Nullable
    @Override
    public Integer getConnectionTimeMs() {
        return mConnectionTimeMs;
    }

    @Nullable
    @Override
    public Integer getTurnaroundTimeMs() {
        return mTurnaroundTimeMs;
    }

    @Override
    public float getNetworkSpeedMbps() {
        final double DOWNLOAD_MBITS = (double) (mDownloadBytes * 8) / 1024.0f / 1024.0f;
        final double TURNAROUND_TIME_SEC;
        if (mConnectionTimeMs == null) {
            return 0.0f;
        } else if (mTurnaroundTimeMs != null) {
            // 既にダウンロード完了しているなら固定計算
            TURNAROUND_TIME_SEC = ((double) (int) (mTurnaroundTimeMs) / 1000.0f);
        } else {
            // ランタイムで計算する
            TURNAROUND_TIME_SEC = mTimer.endSec();
        }

        return (float) (DOWNLOAD_MBITS / TURNAROUND_TIME_SEC);
    }
}
