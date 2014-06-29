package com.obs.retrofit;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import retrofit.client.Client;
import retrofit.client.Header;
import retrofit.client.Request;
import retrofit.client.Response;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;
import android.util.Log;

public class CustomUrlConnectionClient implements Client {
	private static final int CHUNK_SIZE = 4096;
	static final int CONNECT_TIMEOUT_MILLIS = 10 * 1000; // 10s
	static final int READ_TIMEOUT_MILLIS = 20 * 1000; // 20s
	public static String tenentId ;
	public static String basicAuth;
	public static String contentType;
	public CustomUrlConnectionClient(String Id,String Auth,String Type) {
		tenentId = Id;
		basicAuth = Auth;
		contentType = Type;
	}

	@Override
	public Response execute(Request request) throws IOException {
		// TurnOff certificate validation
		configureCert();
		HttpsURLConnection connection = openConnection(request);
		prepareRequest(connection, request);
		return readResponse(connection);
	}

	private void configureCert() {
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			@Override
			public void checkClientTrusted(
					java.security.cert.X509Certificate[] arg0, String arg1)
					throws CertificateException {
				// TODO Auto-generated method stub
			}

			@Override
			public void checkServerTrusted(
					java.security.cert.X509Certificate[] chain, String authType)
					throws CertificateException {
				// TODO Auto-generated method stub
			}
		} };
		// Install the all-trusting trust manager
		SSLContext sc = null;
		try {
			sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
		} catch (NoSuchAlgorithmException e) {
			Log.d("Eeception :", e.toString());
			e.printStackTrace();
		} catch (KeyManagementException e) {
			Log.d("Eeception :", e.toString());
			e.printStackTrace();
		}
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		// Create all-trusting host name verifier
		HostnameVerifier allHostsValid = new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};
		// Install the all-trusting host verifier
		HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
	}

	protected HttpsURLConnection openConnection(Request request)
			throws IOException {
		HttpsURLConnection connection = (HttpsURLConnection) new URL(
				request.getUrl()).openConnection();
		connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
		connection.setReadTimeout(READ_TIMEOUT_MILLIS);
		return connection;
	}

	void prepareRequest(HttpURLConnection connection, Request request)
			throws IOException {
		connection.setRequestMethod(request.getMethod());
		connection.setRequestProperty("X-Obs-Platform-TenantId", tenentId);
		connection.setRequestProperty(
				"Authorization",
				basicAuth);
		connection.setRequestProperty("Content-Type", contentType);
		connection.setDoInput(true);

		for (Header header : request.getHeaders()) {
			connection.addRequestProperty(header.getName(), header.getValue());
		}

		TypedOutput body = request.getBody();
		if (body != null) {
			connection.setDoOutput(true);
			connection.addRequestProperty("Content-Type", body.mimeType());
			long length = body.length();
			if (length != -1) {
				connection.setFixedLengthStreamingMode((int) length);
				connection.addRequestProperty("Content-Length",
						String.valueOf(length));
			} else {
				connection.setChunkedStreamingMode(CHUNK_SIZE);
			}
			body.writeTo(connection.getOutputStream());
		}
	}

	Response readResponse(HttpsURLConnection connection) throws IOException {
		int status = connection.getResponseCode();
		String reason = connection.getResponseMessage();
		if (reason == null)
			reason = ""; // HttpURLConnection treats empty reason as null.

		List<Header> headers = new ArrayList<Header>();
		for (Map.Entry<String, List<String>> field : connection
				.getHeaderFields().entrySet()) {
			String name = field.getKey();
			for (String value : field.getValue()) {
				headers.add(new Header(name, value));
			}
		}

		String mimeType = connection.getContentType();
		int length = connection.getContentLength();
		InputStream stream;
		if (status >= 400) {
			stream = connection.getErrorStream();
		} else {
			stream = connection.getInputStream();
		}
		TypedInput responseBody = new TypedInputStream(mimeType, length, stream);
		return new Response(connection.getURL().toString(), status, reason,
				headers, responseBody);
		
	}

	private static class TypedInputStream implements TypedInput {
		private final String mimeType;
		private final long length;
		private final InputStream stream;

		private TypedInputStream(String mimeType, long length,
				InputStream stream) {
			this.mimeType = mimeType;
			this.length = length;
			this.stream = stream;
		}

		@Override
		public String mimeType() {
			return mimeType;
		}

		@Override
		public long length() {
			return length;
		}

		@Override
		public InputStream in() throws IOException {
			return stream;
		}
	}
}