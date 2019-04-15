package common.modules.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URL;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;

public class IHTTPUtil {

	private static final int SECOND_RETRY_INTERVAL = 2;

	public interface ResponseCallBack {
		public void done(JSONObject json);
	}

	public static JSONObject post(String url, Map<String, String> parameters) {
		return post(url, parameters, 0);
	}

	public static JSONObject post(String url, Map<String, String> parameters, int retryCount) {
		return post(url, null, new JSONObject(parameters).toString(), retryCount);
	}
	
	public static JSONObject post(String url, String jsonString, int retryCount) {
		return post(url, null, jsonString, retryCount);
	}
	
	public static JSONObject post(String url, String jsonString) {
		return post(url, null, jsonString, 0);
	}

	public static JSONObject post(String url, Map<String, Object> headers, String jsonString, int retryCount) {
		JSONObject result = null;
		do {

			try {
				URL urlObj = new URL(url);
				byte[] postDataBytes = jsonString.getBytes("UTF-8");
				long postDataLength = postDataBytes.length;

				HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
				conn.setRequestMethod("POST");
				conn.setRequestProperty("Content-Type", "application/json");
				conn.setRequestProperty("Content-Length", String.valueOf(postDataLength));
				
				if (headers != null) {
					Set<String> keys = headers.keySet();
					for (String key : keys) {
						conn.setRequestProperty(key, headers.get(key).toString());
					}
				}
				
				conn.setDoOutput(true);
				conn.getOutputStream().write(postDataBytes);

				// convert response to json
				BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
				StringBuilder sb = new StringBuilder();
				String str = null;
				while ((str = in.readLine()) != null) {
					sb.append(str);
				}
				String raw = sb.toString();

				result = transferResponseToJsonObject(raw, conn);

				int statusCode = conn.getResponseCode();
				if (statusCode == 200) {
					return result;
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			// retry
			retryCount--;
			if (retryCount > 0) {
				try {
					Thread.sleep(SECOND_RETRY_INTERVAL * 1000);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		} while (retryCount > 0);

		return result;
	}

	public static void postAsync(String url, Map<String, String> parameters, ResponseCallBack handler) {
		postAsync(url, parameters, 0, handler);
	}

	public static void postAsync(String url, Map<String, String> parameters, int retryCount, ResponseCallBack handler) {
		postAsync(url, null, new JSONObject(parameters).toString(), retryCount, handler);
	}

	public static void postAsync(String url, String jsonString, int retryCount, final ResponseCallBack handler) {
		postAsync(url, null, jsonString, retryCount, handler);
	}
	
	public static void postAsync(String url, String jsonString, final ResponseCallBack handler) {
		postAsync(url, null, jsonString, 0, handler);
	}

	public static void postAsync(final String url, final Map<String, Object> headers, final String jsonString, final int retryCount, final ResponseCallBack handler) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				JSONObject object = post(url, headers, jsonString, retryCount);
				handler.done(object);
			}
		}).start();
	}

	public static void getAsync(final String urlStr, final ResponseCallBack handler) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				JSONObject object = get(urlStr);
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
			String raw = sb.toString();

			result = transferResponseToJsonObject(raw, conn);

			int statusCode = conn.getResponseCode();
			if (statusCode == 200) {
				return result;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return result;
	}

	public static void download(String urlStr, String fileName) {
		try {
			URL url = new URL(urlStr);

			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");

			conn.setConnectTimeout(10 * 1000);
			conn.setReadTimeout(10 * 1000);
			conn.connect();

			int statusCode = conn.getResponseCode();
			if (statusCode == 200) {

				File file = new File(fileName);
				File fileParent = file.getParentFile();
				if (!fileParent.exists()) {
					fileParent.mkdirs();
				}
				if (!file.exists()) {
					file.createNewFile();
				}

				InputStream inputStream = conn.getInputStream();
				OutputStream outputStream = new FileOutputStream(file);
				byte[] buffer = new byte[1024];
				int readBytesAmount = 0;
				while ((readBytesAmount = inputStream.read(buffer)) > 0) {
					outputStream.write(buffer, 0, readBytesAmount);
				}
				outputStream.flush();
				outputStream.close();

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static JSONObject transferResponseToJsonObject(String responseString, HttpURLConnection connnection) throws JSONException {
		String raw = responseString;
		
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("__raw_response__", raw);
		jsonObject.put("__connection_obj__", connnection);

		if ((raw.startsWith("{") && raw.endsWith("}")) || (raw.startsWith("[") && raw.endsWith("]"))) {
			jsonObject = new JSONObject(raw);
		}

		return jsonObject;
	}

	/**
	 * Request with proxy
	 */
	@SuppressLint("TrulyRandom")
	public static String justStartRequest(Boolean isUseProxy, String proxy_ip, int proxy_port, String urlStr, int timeout, String referer,
			String user_agent, String cookie) throws Exception {

		URL url = new URL(urlStr);

		SSLContext sslcontext = SSLContext.getInstance("SSL");
		sslcontext.init(null, new TrustManager[] { new TrustAnyTrustManager() }, new java.security.SecureRandom());

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

		String set_cookie = httpsConn.getHeaderField("Set-Cookie");
		return set_cookie;
	}

	private static class TrustAnyTrustManager implements X509TrustManager {
		@Override
		public void checkClientTrusted(java.security.cert.X509Certificate[] arg0, String arg1) throws java.security.cert.CertificateException {
		}

		@Override
		public void checkServerTrusted(java.security.cert.X509Certificate[] arg0, String arg1) throws java.security.cert.CertificateException {
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
