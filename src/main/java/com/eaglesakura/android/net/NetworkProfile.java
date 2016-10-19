package com.eaglesakura.android.net;

import android.support.annotation.FloatRange;
import android.support.annotation.Nullable;

/**
 * ネットワーク状態のプロファイリング
 */
public abstract class NetworkProfile {
    /**
     * サーバーに接続が完了するまでの時間
     * 未接続の場合はnullを返却する
     */
    @Nullable
    public abstract Integer getConnectionTimeMs();

    /**
     * ダウンロード完了までの時間
     * ダウンロード未完了の場合はnullを返却する
     */
    @Nullable
    public abstract Integer getTurnaroundTimeMs();

    /**
     * 平均現在のネットワーク速度を取得する
     * これはレスポンスのダウンロード開始後にカウントされる。
     * それまでは0bpsとなる
     */
    @FloatRange(from = 0)
    public abstract float getNetworkSpeedMbps();
}
