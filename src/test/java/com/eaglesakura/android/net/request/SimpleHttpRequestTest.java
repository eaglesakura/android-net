package com.eaglesakura.android.net.request;

import com.eaglesakura.android.net.UnitTestCase;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class SimpleHttpRequestTest extends UnitTestCase {

    @Test
    public void get用URLを構築する() throws Exception {
        SimpleHttpRequest req = new SimpleHttpRequest(ConnectRequest.Method.GET);


        {
            req.setUrl("https://example.com", null);
            assertEquals(req.getUrl(), "https://example.com");
        }

        {
            Map<String, String> values = new HashMap<>();
            values.put("key", "value");
            req.setUrl("https://example.com", values);
            assertEquals(req.getUrl(), "https://example.com?key=value&");
        }

        {
            Map<String, String> values = new HashMap<>();
            values.put("key", "value");
            req.setUrl("https://example.com?def=value", values);
            assertEquals(req.getUrl(), "https://example.com?def=value&key=value&");
        }
    }

    @Test
    public void post用URLを構築する() throws Exception {
        SimpleHttpRequest req = new SimpleHttpRequest(ConnectRequest.Method.POST);

        {
            req.setUrl("https://example.com", null);
            assertEquals(req.getUrl(), "https://example.com");
        }

        {
            Map<String, String> values = new HashMap<>();
            values.put("key", "value");
            req.setUrl("https://example.com", values);
            assertEquals(req.getUrl(), "https://example.com");
        }
    }

}
