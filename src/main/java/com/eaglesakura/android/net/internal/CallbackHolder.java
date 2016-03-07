package com.eaglesakura.android.net.internal;

import com.eaglesakura.android.net.Result;
import com.eaglesakura.android.net.NetworkConnector;

public class CallbackHolder<T> {
    public final NetworkConnector.CancelCallback<T> cancelCallback;

    public final Result<T> mConnection;

    public CallbackHolder(NetworkConnector.CancelCallback<T> cancelCallback, Result<T> mConnection) {
        this.cancelCallback = cancelCallback;
        this.mConnection = mConnection;
    }

    public boolean isCanceled() {
        return cancelCallback.isCanceled(mConnection);
    }
}
