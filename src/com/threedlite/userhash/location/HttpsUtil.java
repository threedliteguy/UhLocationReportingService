package com.threedlite.userhash.location;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Base64;

/*

Copyright 2013 Dan Meany

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

public class HttpsUtil {


	public static void send(StatusCallback cb, String command, Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String encodedString = null;
		try {
			encodedString = URLEncoder.encode(command, "UTF-8");
		} catch (UnsupportedEncodingException e) {
		}
		String data = "command=" + encodedString;
		HttpsUtil https = getHttpsUtil(cb, prefs);
		https.doPostAsync(data);
	}


	private synchronized static HttpsUtil getHttpsUtil(StatusCallback cb, SharedPreferences prefs) {

		String prefCert = prefs.getString(Constants.PREF_CERT, "");

		if (oldPrefCert == null || !oldPrefCert.equals(prefCert)){
			oldPrefCert = prefCert;
			try {
				currcert = formatCert(prefCert);
			} catch (Exception e) {
				if (cb!= null) cb.status("Cert format error: "+e.getMessage());
			}
		}

		return new HttpsUtil(cb,  
				prefs.getString(Constants.PREF_URL, ""),
				prefs.getString(Constants.PREF_USERNAME, ""),
				prefs.getString(Constants.PREF_PWD, ""),
				currcert 
				);
	}

	private static String oldPrefCert;
	private static String currcert;
	private static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
	private static final String END_CERT = "-----END CERTIFICATE-----";

	private static String formatCert(String s) {

		s = s.trim();
		s = s.replace("\r", "").replace("\n", "");
		s = s.substring(s.indexOf(BEGIN_CERT)+BEGIN_CERT.length());
		s = s.substring(0, s.indexOf(END_CERT));
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (i % 64 == 0) {
				sb.append('\n');
			}
			sb.append(c);
		}
		s = BEGIN_CERT + sb.toString();
		if (!s.endsWith("\n")) {
			s = s + "\n";
		}
		s = s + END_CERT;
		return s;

	}




	public static interface StatusCallback {
		public void status(String msg);
		public void result(String msg);
	}

	private StatusCallback cb;
	private String serverurl;
	private String username;
	private String password;
	private String cert;


	public HttpsUtil(StatusCallback cb, String serverurl, String username, String password, String cert) {
		if (cb == null) cb = new StatusCallback() {
			public void status(String msg) {
			}
			public void result(String msg) {
			}
		};
		this.cb=cb;
		this.serverurl=serverurl;
		this.username=username;
		this.password=password;
		this.cert=cert;
	}


	public void doPostAsync(String s) {

		cb.status("Posting: "+s);

		new AsyncTask<String, Void, String> () {
			@Override
			protected String doInBackground(String... params) {
				return post(params[0]);
			}        
			@Override
			protected void onPostExecute(String result) { 
				cb.result(result);
			}
			@Override
			protected void onPreExecute() {
			}
			@Override
			protected void onProgressUpdate(Void... values) {
			}
		}.execute(s);

	}

	private String post(String data) {

		StringBuffer result = new StringBuffer();

		String surl = serverurl;
		try {

			URL url = new URL(surl);

			URLConnection connection = url.openConnection();
			connection.setConnectTimeout(2*60*1000); // 2 min

			if (connection instanceof HttpURLConnection) {
				((HttpURLConnection)connection).setRequestMethod("POST");
			}

			if (connection instanceof HttpsURLConnection) {
				HttpsURLConnection huc = (HttpsURLConnection) connection;
				// IP address need no host verifier
				if (Character.isDigit(url.getHost().charAt(0))) {
					huc.setHostnameVerifier(new HostnameVerifier() {
						public boolean verify(String hostname, SSLSession session) {
							return true;
						}
					});
				}
				if (cert != null && cert.trim().length() > 0) {
					huc.setSSLSocketFactory(getSSLContext().getSocketFactory());
				}
			}


			String auth = username+":"+password;
			connection.setRequestProperty("Authorization", "Basic "
					+ new String(Base64.encode(auth.getBytes(), Base64.DEFAULT)));
			connection.setConnectTimeout(10*1000);
			connection.setDoOutput(true);


			OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
			try {
				out.write(data);
			} finally{
				out.close();
			}

			BufferedReader in = new BufferedReader(
					new InputStreamReader(
							connection.getInputStream()));
			try {
				String decodedString;
				while ((decodedString = in.readLine()) != null) {
					result.append(decodedString).append("\n");
				}
			} finally{
				in.close();
			}

		} catch (Exception e) {
			return "Error: Unable to access server: "+e.getMessage(); // +": "+e.getStackTrace()[0];
		}

		return result.toString();
	}

	private SSLContext getSSLContext() throws Exception {

		ByteArrayInputStream in = new ByteArrayInputStream(cert.getBytes());
		CertificateFactory factory = CertificateFactory.getInstance("X.509");
		Certificate certificate = factory.generateCertificate(in);

		KeyStore ts = KeyStore.getInstance(KeyStore.getDefaultType());
		ts.load(null, null);
		ts.setCertificateEntry("a", certificate);

		TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		tmf.init(ts);
		SSLContext sslCtx = SSLContext.getInstance("TLS");
		sslCtx.init(null, tmf.getTrustManagers(), null);

		return sslCtx;
	}




}
