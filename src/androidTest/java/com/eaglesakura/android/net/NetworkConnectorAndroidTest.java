package com.eaglesakura.android.net;

import com.eaglesakura.android.devicetest.DeviceTestCase;
import com.eaglesakura.android.devicetest.ModuleTestCase;
import com.eaglesakura.android.net.error.HttpAccessFailedException;
import com.eaglesakura.android.net.parser.BitmapParser;
import com.eaglesakura.android.net.parser.ByteArrayParser;
import com.eaglesakura.android.net.request.ConnectRequest;
import com.eaglesakura.android.net.request.SimpleHttpRequest;
import com.eaglesakura.android.net.stream.ByteArrayStreamController;
import com.eaglesakura.android.util.AndroidThreadUtil;
import com.eaglesakura.android.util.ImageUtil;
import com.eaglesakura.util.CollectionUtil;
import com.eaglesakura.util.IOUtil;

import org.junit.Test;

import android.graphics.Bitmap;

import java.io.File;
import java.util.Arrays;

import junit.framework.Assert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


public class NetworkConnectorAndroidTest extends DeviceTestCase {


    @Override
    public void onSetup() {
        super.onSetup();
        IOUtil.delete(getCacheDirectory());
    }

    @Test
    public void ポリシー設定よりも大きい場合はキャッシュされない() throws Exception {
        File cacheDirectory = new File(getCacheDirectory(), "no-cache");

        NetworkConnector connector = NetworkConnector.createBinaryApi(getContext(), cacheDirectory);
        SimpleHttpRequest request = new SimpleHttpRequest(ConnectRequest.Method.GET);
        request.setUrl("https://http.cat/200", CollectionUtil.asPairMap(Arrays.asList("ThisIs", "UnitTest"), it -> it));
        request.setReadTimeoutMs(1000 * 30);
        request.setConnectTimeoutMs(1000 * 30);
        request.getCachePolicy().setCacheLimitTimeMs(0);
        request.getCachePolicy().setMaxItemBytes(1024);

        // 初回はキャッシュがないのでダイレクトに取得できる
        {
            Result<Bitmap> connect = connector.connect(request, new BitmapParser(), it -> false);
            Assert.assertNotNull(connect.getResult());

            Bitmap image = connect.getResult();
            assertEquals(image.getWidth(), 750);
            assertEquals(image.getHeight(), 600);
            assertNull(connect.getCacheDigest());
            assertNotNull(connect.getContentDigest());
            assertFalse(connect.isModified());  // 同じ結果が返ってくるべきである

            // キャッシュが生成されていない
            Assert.assertEquals(cacheDirectory.listFiles().length, 0);
        }
    }

    @Test
    public void get操作がキャッシュされる() throws Exception {
        File cacheDirectory = new File(getCacheDirectory(), "get-cached");

        NetworkConnector connector = NetworkConnector.createBinaryApi(getContext(), cacheDirectory);
        connector.setStreamController(new ByteArrayStreamController()); // 途中でabortされないように、一旦byte[]に変換する

        SimpleHttpRequest request = new SimpleHttpRequest(ConnectRequest.Method.GET);
        request.setUrl("https://http.cat/200", null);
        request.setReadTimeoutMs(1000 * 30);
        request.setConnectTimeoutMs(1000 * 30);
        request.getCachePolicy().setCacheLimitTimeMs(1000 * 60);

        // 初回はキャッシュがないのでダイレクトに取得できる
        {
            Result<Bitmap> connect = connector.connect(request, new BitmapParser(), it -> false);
            Assert.assertNotNull(connect.getResult());

            Bitmap image = connect.getResult();
            assertTrue(connect.hasContent());
            assertEquals(image.getWidth(), 750);
            assertEquals(image.getHeight(), 600);
            assertNull(connect.getCacheDigest());
            assertNotNull(connect.getContentDigest());

            // キャッシュが生成されている
            assertEquals(cacheDirectory.listFiles().length, 1);

            // キャッシュが一致している
            String cacheMD5 = IOUtil.genMD5(cacheDirectory.listFiles()[0]);
            Assert.assertEquals(connect.getContentDigest(), cacheMD5);
        }

        // 2回めはキャッシュが働く
        {
            Result<Bitmap> connect = connector.connect(request, new BitmapParser(), it -> false);
            Assert.assertNotNull(connect.getResult());

            Bitmap image = connect.getResult();
            assertEquals(image.getWidth(), 750);
            assertEquals(image.getHeight(), 600);
            assertFalse(connect.isModified());

            // キャッシュからロードされていることを確認する
            assertNotNull(connect.getCacheDigest());
            assertNull(connect.getContentDigest());
        }
    }

    @Test
    public void get操作で200が返却される() throws Exception {
        AndroidThreadUtil.assertBackgroundThread();

        NetworkConnector connector = NetworkConnector.createBinaryApi(getContext(), getCacheDirectory());
        SimpleHttpRequest request = new SimpleHttpRequest(ConnectRequest.Method.GET);
        request.setUrl("https://http.cat/200", null);
        request.setReadTimeoutMs(1000 * 30);
        request.setConnectTimeoutMs(1000 * 30);

        Result<Bitmap> connect = connector.connect(request, new BitmapParser(), it -> false);
        Assert.assertNotNull(connect.getResult());

        Bitmap image = connect.getResult();
        Assert.assertEquals(image.getWidth(), 750);
        Assert.assertEquals(image.getHeight(), 600);
        Assert.assertNotNull(connect.getContentDigest());
    }

    @Test
    public void レスポンスが404の場合にエラーハンドリングが行える() throws Exception {
        NetworkConnector connector = NetworkConnector.createBinaryApi(getContext(), getCacheDirectory());
        SimpleHttpRequest request = new SimpleHttpRequest(ConnectRequest.Method.GET);
        request.setUrl("https://www.google.co.jp/404", null);
        request.getErrorPolicy().setHandleErrorStream(true);

        try {
            connector.connect(request, ByteArrayParser.getInstance(), it -> false);
            fail(); // 404で例外が投げられなければならない
        } catch (HttpAccessFailedException e) {
            assertTrue(e.hasErrorBuffer());
            assertEquals(e.getStatusCode(), 404);

            String html = e.getErrorText();
            assertTrue(html.indexOf("/404") > 0);
            e.printStackTrace();
        }
    }
}
