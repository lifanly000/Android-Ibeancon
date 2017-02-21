package com.zsq.ibeacondemo;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import android.util.Log;


public class Http_ED {
	private final static String TAG = "TAG";

	public static void sendHttpRequestForPost(final String path,
											  final String paramsValue, final HttpCallbackListener listener) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				HttpURLConnection connection = null;
				try {
					StringBuilder response = new StringBuilder();
					URL url = new URL(path);
					connection = (HttpURLConnection) url.openConnection();
					connection.setRequestMethod("POST");
					connection.setConnectTimeout(100000);
					connection.setReadTimeout(100000);
					connection.setDoOutput(true);
					connection.setDoInput(true);
					connection.setRequestProperty("Content-Type",
							"application/json;charset=UTF-8");
					DataOutputStream out = new DataOutputStream(connection
							.getOutputStream());
					out.writeBytes(paramsValue);
					// 成功
					if (connection.getResponseCode() == 200) {
						Log.d(TAG, "success");
						InputStream in = connection.getInputStream();
						BufferedReader reader = new BufferedReader(
								new InputStreamReader(in, "utf-8"));
						String line;
						while ((line = reader.readLine()) != null) {
							response.append(line);
						}
						listener.onFinish(response.toString());
					} else {
						Log.e(TAG,
								Integer.toString(connection.getResponseCode()));
						Log.e(TAG, "fail");
					}
				} catch (Exception e) {
					if (listener != null) {
						Log.d(TAG, e.getMessage());
						listener.onError(e);
					}
				} finally {
					if (connection != null) {
						connection.disconnect();
					}
				}
			}
		}).start();
	}
	public interface HttpCallbackListener {
		void onFinish(String response);

		void onError(Exception e);
	}
}
