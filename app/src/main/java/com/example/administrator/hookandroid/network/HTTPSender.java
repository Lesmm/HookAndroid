package com.example.administrator.hookandroid.network;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URL;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


public class HTTPSender {

    private static final int SECOND_RETRY_INTERVAL = 2;

    public interface ResponseCallBack {
        public void done(JSONObject json);
    }

    public static JSONObject post(String url, Map<String, String> parameters) {
        return post(url, parameters, 0);
    }

    public static JSONObject post(String url, Map<String, String> parameters, int retryCount) {
        return post(url, new JSONObject(parameters).toString(), retryCount);
    }

    public static JSONObject post(String url, String jsonString) {
        return post(url, jsonString, 0);
    }

    public static JSONObject post(String url, String jsonString, int retryCount) {
        JSONObject result = null;
        try {

            do {

                URL urlObj = new URL(url);
                byte[] postDataBytes = jsonString.getBytes("UTF-8");
                long postDataLength = postDataBytes.length;

                HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Content-Length", String.valueOf(postDataLength));
                conn.setDoOutput(true);
                conn.getOutputStream().write(postDataBytes);

                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String str = null;
                while((str = in.readLine()) != null){
                    sb.append(str);
                }
                String responseContent = sb.toString();

                JSONObject jsonObject = null;
                if ((responseContent.startsWith("{") && responseContent.endsWith("}")) || (responseContent.startsWith("[") && responseContent.endsWith("]"))) {
                    jsonObject = new JSONObject(responseContent);
                }
                if (jsonObject == null) {
                    jsonObject = new JSONObject();
                    jsonObject.put("STRING_CONTENTS", responseContent);
                }
                result = jsonObject;

                int statusCode = conn.getResponseCode();
                if (statusCode == 200) {
                    return result;
                }

                retryCount--;
                if (retryCount > 0) {
                    try {
                        Thread.sleep(SECOND_RETRY_INTERVAL * 1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            } while (retryCount > 0);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static void postAsync(String url, Map<String, String> parameters, ResponseCallBack handler) {
        postAsync(url, parameters, 0, handler);
    }

    public static void postAsync(String url, Map<String, String> parameters, int retryCount, ResponseCallBack handler) {
        postAsync(url, new JSONObject(parameters).toString(), retryCount, handler);
    }

    public static void postAsync(String url, String jsonString, final ResponseCallBack handler) {
        postAsync(url, jsonString, 0, handler);
    }

    public static void postAsync(final String url, final String jsonString, final int retryCount, final ResponseCallBack handler) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                JSONObject object = post(url, jsonString, retryCount);
                handler.done(object);
            }
        }).start();
    }

    public static JSONObject get(String urlStr) {
        JSONObject result = null;

        try {
            URL url = new URL(urlStr);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            conn.setConnectTimeout(10 * 1000);
            conn.setReadTimeout(10 * 1000);
            conn.connect();

            // convert response to json
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String str = null;
            while ((str = in.readLine()) != null) {
                sb.append(str);
            }
            String responseContent = sb.toString();

            JSONObject jsonObject = null;
            if ((responseContent.startsWith("{") && responseContent.endsWith("}"))
                    || (responseContent.startsWith("[") && responseContent.endsWith("]"))) {
                jsonObject = new JSONObject(responseContent);
            }
            if (jsonObject == null) {
                jsonObject = new JSONObject();
                jsonObject.put("STRING_CONTENTS", responseContent);
            }
            result = jsonObject;

            int statusCode = conn.getResponseCode();
            if (statusCode == 200) {
                return result;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public static String startRequest(Boolean isUseProxy, String proxy_ip, int proxy_port, String urlStr, int timeout, String referer, String user_agent, String cookie) throws Exception {

        URL url = new URL(urlStr);

        SSLContext sslcontext = SSLContext.getInstance("SSL");
        sslcontext.init(null, new TrustManager[]{new TrustAnyTrustManager()}, new java.security.SecureRandom());

        HttpsURLConnection httpsConn = null;
        if (isUseProxy) {
            httpsConn = (HttpsURLConnection) url.openConnection(new Proxy(Type.HTTP, new InetSocketAddress(proxy_ip, proxy_port)));
        } else {
            httpsConn = (HttpsURLConnection) url.openConnection();
        }

        httpsConn.setSSLSocketFactory(sslcontext.getSocketFactory());
        httpsConn.setHostnameVerifier(new TrustAnyHostnameVerifier());

        httpsConn.setRequestMethod("GET");
        httpsConn.setRequestProperty("Referer", referer);
        httpsConn.setRequestProperty("User-Agent", user_agent);
        if (cookie != null && cookie.length() != 0) {
            httpsConn.setRequestProperty("Cookie", cookie);
        }

        httpsConn.setConnectTimeout(timeout * 1000);
        httpsConn.setReadTimeout(timeout * 1000);

        httpsConn.connect();
		
		/*
		Reader in = new BufferedReader(new InputStreamReader(httpsConn.getInputStream(), "UTF-8"));
		StringBuilder sb = new StringBuilder();
		for (int c; (c = in.read()) >= 0;)
			sb.append((char) c);
		String response = sb.toString();
		*/

        String set_cookie = httpsConn.getHeaderField("Set-Cookie");
        return set_cookie;
    }

    private static class TrustAnyTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] arg0, String arg1)
                throws java.security.cert.CertificateException {
        }

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] arg0, String arg1)
                throws java.security.cert.CertificateException {
        }

        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }

    private static class TrustAnyHostnameVerifier implements HostnameVerifier {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

}
