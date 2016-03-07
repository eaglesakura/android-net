package com.eaglesakura.android.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.eaglesakura.android.net.NetworkConnector;
import com.eaglesakura.android.net.R;
import com.eaglesakura.android.net.Result;
import com.eaglesakura.android.net.cache.ICacheController;
import com.eaglesakura.android.net.cache.file.FileCacheController;
import com.eaglesakura.android.net.parser.RequestParser;
import com.eaglesakura.android.net.request.ConnectRequest;
import com.eaglesakura.android.net.request.SimpleHttpRequest;
import com.eaglesakura.android.rx.LifecycleState;
import com.eaglesakura.android.rx.RxTask;
import com.eaglesakura.android.rx.SubscriptionController;
import com.eaglesakura.util.LogUtil;

import java.io.File;

import rx.subjects.BehaviorSubject;

/**
 * Support Network ImageView
 */
public class SupportNetworkImageView extends ImageView {
    protected String url;

    protected NetworkConnector mConnector;

    protected RxTask<Result<Bitmap>> imageResult;

    /**
     * ダウンロード失敗時に表示する画像
     */
    protected Drawable errorImage;

    /**
     * 標準では1時間キャッシュ
     */
    protected long cacheTimeoutMs;

    protected OnImageListener onImageListener;

    protected BehaviorSubject<LifecycleState> mSubject = BehaviorSubject.create(LifecycleState.NewObject);

    protected SubscriptionController mSubscription = new SubscriptionController();

    public SupportNetworkImageView(Context context) {
        super(context);
        initialize(context, null, 0, 0);
    }

    public SupportNetworkImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize(context, attrs, 0, 0);
    }

    public SupportNetworkImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize(context, attrs, defStyleAttr, 0);
    }

    @SuppressLint("NewApi")
    public SupportNetworkImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context, attrs, defStyleAttr, defStyleRes);
    }

    private void initialize(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        if (isInEditMode()) {
            return;
        }

        mSubscription.bind(mSubject);
        mConnector = new NetworkConnector(context);
        try {
            FileCacheController ctrl = new FileCacheController(new File(getContext().getCacheDir(), "net-img"));
            ctrl.setExt("img");
            mConnector.setCacheController(ctrl);
        } catch (Exception e) {
            mConnector.setCacheController(null);
        }

        if (attrs != null) {
            LogUtil.log("has attribute");
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SupportNetworkImageView);
            int cacheTimeSec = typedArray.getInteger(R.styleable.SupportNetworkImageView_cacheTimeSec, 0);
            int cacheTimeMin = typedArray.getInteger(R.styleable.SupportNetworkImageView_cacheTimeMin, 0);
            int cacheTimeHour = typedArray.getInteger(R.styleable.SupportNetworkImageView_cacheTimeHour, 0);
            int cacheTimeDay = typedArray.getInteger(R.styleable.SupportNetworkImageView_cacheTimeDay, 0);

            cacheTimeoutMs += (1000 * cacheTimeSec);
            cacheTimeoutMs += ICacheController.CACHE_ONE_MINUTE * cacheTimeMin;
            cacheTimeoutMs += ICacheController.CACHE_ONE_HOUR * cacheTimeHour;
            cacheTimeoutMs += ICacheController.CACHE_ONE_DAY * cacheTimeDay;

            errorImage = typedArray.getDrawable(R.styleable.SupportNetworkImageView_errorImage);
        }

        if (cacheTimeoutMs == 0) {
            cacheTimeoutMs = ICacheController.CACHE_ONE_HOUR;
        }

        LogUtil.log("Cache time(%.2f hour) ErrorImage (%s)", (double) cacheTimeoutMs / (double) ICacheController.CACHE_ONE_HOUR, "" + errorImage);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mSubject.onNext(LifecycleState.OnResumed);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mSubject.onNext(LifecycleState.OnDestroyed);
    }

    /**
     * ネットワークキャッシュが有効な時間を設定する
     */
    public void setCacheTimeoutMs(long cacheTimeoutMs) {
        this.cacheTimeoutMs = cacheTimeoutMs;
    }

    /**
     * 通信エラー時の画像を設定する
     *
     * @param errorImage エラー時に表示する画像
     */
    public void setErrorImage(Drawable errorImage) {
        this.errorImage = errorImage;
    }

    /**
     * ネットワーク経由でgetする
     */
    public void setImageFromNetwork(final String getUrl, RequestParser<Bitmap> parser) {
        this.url = getUrl;

        SimpleHttpRequest request = new SimpleHttpRequest(ConnectRequest.Method.GET);
        request.setUrl(getUrl, null);
        request.getCachePolicy().setCacheLimitTimeMs(cacheTimeoutMs);
        imageResult = mConnector.connect(mSubscription, request, parser)
                .cancelSignal(new RxTask.Signal() {
                    @Override
                    public boolean is(RxTask task) {
                        return task != imageResult;
                    }
                })
                .completed(new RxTask.Action1<Result<Bitmap>>() {
                    @Override
                    public void call(Result<Bitmap> it, RxTask<Result<Bitmap>> task) {
                        if (task == imageResult) {
                            try {
                                onReceivedImage(it.getResult());
                            } catch (Exception e) {
                                onImageLoadError();
                            }
                        }
                    }
                }).canceled(new RxTask.Action0<Result<Bitmap>>() {
                    @Override
                    public void call(RxTask<Result<Bitmap>> task) {
                        onImageLoadError();
                    }
                }).failed(new RxTask.ErrorAction<Result<Bitmap>>() {
                    @Override
                    public void call(Throwable it, RxTask<Result<Bitmap>> task) {
                        onImageLoadError();
                    }
                }).finalized(new RxTask.Action0<Result<Bitmap>>() {
                    @Override
                    public void call(RxTask<Result<Bitmap>> task) {
                        imageResult = null;
                    }
                }).start();
    }

    public void setOnImageListener(OnImageListener onImageListener) {
        this.onImageListener = onImageListener;
    }

    protected void onReceivedImage(Bitmap image) {
        setImageBitmap(image);

        if (onImageListener != null) {
            onImageListener.onImageReceived(this, url, image);
        }
    }

    protected void onImageLoadError() {
        if (errorImage != null) {
            setImageDrawable(errorImage);
        }

        if (onImageListener != null) {
            onImageListener.onImageReceiveFailed(this, url);
        }
    }

    /**
     * 画像受信時のListener
     */
    public interface OnImageListener {
        /**
         * 正常に画像を受信した
         *
         * @param view  this
         * @param url   画像URL
         * @param image 画像
         */
        void onImageReceived(SupportNetworkImageView view, String url, Bitmap image);

        /**
         * 受信に失敗した
         *
         * @param view this
         * @param url  画像URL
         */
        void onImageReceiveFailed(SupportNetworkImageView view, String url);
    }
}
