package com.eaglesakura.android.net;

import com.eaglesakura.android.devicetest.ModuleTestCase;
import com.eaglesakura.android.net.parser.BitmapParser;
import com.eaglesakura.android.net.request.ConnectRequest;
import com.eaglesakura.android.net.request.SimpleHttpRequest;
import com.eaglesakura.android.net.stream.ByteArrayStreamController;
import com.eaglesakura.android.util.AndroidThreadUtil;
import com.eaglesakura.util.CollectionUtil;
import com.eaglesakura.util.IOUtil;

import android.graphics.Bitmap;

import java.io.File;
import java.util.Arrays;

import junit.framework.Assert;


public class NetworkConnectorAndroidTest extends ModuleTestCase {

    public void test_ポリシー設定よりも大きい場合はキャッシュされない() throws Exception {
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
            Assert.assertEquals(image.getWidth(), 750);
            Assert.assertEquals(image.getHeight(), 600);
            Assert.assertNull(connect.getCacheDigest());
            Assert.assertNotNull(connect.getContentDigest());

            // キャッシュが生成されていない
            Assert.assertEquals(cacheDirectory.listFiles().length, 0);
        }
    }

    public void test_get操作がキャッシュされる() throws Exception {
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
            Assert.assertTrue(connect.hasContent());
            Assert.assertEquals(image.getWidth(), 750);
            Assert.assertEquals(image.getHeight(), 600);
            Assert.assertNull(connect.getCacheDigest());
            Assert.assertNotNull(connect.getContentDigest());

            // キャッシュが生成されている
            Assert.assertEquals(cacheDirectory.listFiles().length, 1);

            // キャッシュが一致している
            String cacheMD5 = IOUtil.genMD5(cacheDirectory.listFiles()[0]);
            Assert.assertEquals(connect.getContentDigest(), cacheMD5);
        }

        // 2回めはキャッシュが働く
        {
            Result<Bitmap> connect = connector.connect(request, new BitmapParser(), it -> false);
            Assert.assertNotNull(connect.getResult());

            Bitmap image = connect.getResult();
            Assert.assertEquals(image.getWidth(), 750);
            Assert.assertEquals(image.getHeight(), 600);

            // キャッシュからロードされていることを確認する
            Assert.assertNotNull(connect.getCacheDigest());
            Assert.assertNull(connect.getContentDigest());
        }
    }

    public void test_get操作で200が返却される() throws Exception {
        AndroidThreadUtil.assertBackgroundThread();

        File cacheDirectory = getCacheDirectory();

        NetworkConnector connector = NetworkConnector.createBinaryApi(getContext(), cacheDirectory);
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
}
