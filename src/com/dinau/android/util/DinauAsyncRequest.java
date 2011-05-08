package com.dinau.android.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import org.json.JSONObject;

import roboguice.util.RoboAsyncTask;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dinau.android.R;

public class DinauAsyncRequest extends RoboAsyncTask<String> {
	private static final String MESSAGES_KEY_DEFAULT_MESSAGE = "default_message";
	private static final String MESSAGES_KEY_ERROR_MESSAGE = "error_message";
	private static final String MESSAGES_KEY_LOADING = "loading_container";
	private static final String MESSAGES_KEY_RESULT = "results_container";
	
	private static final String DINAU_BASE_PATH = "http://doineedanumbrella.com/api/";
	
	private String zipCode;
	private Context context;
	private HashMap<String, View> messages;
	
	public DinauAsyncRequest(Context context, String zipCode, ViewGroup messagesContainer) {
		this.context = context;
		this.zipCode = zipCode;
		this.messages = new HashMap<String, View>();
		
		messages.put(MESSAGES_KEY_DEFAULT_MESSAGE, messagesContainer.findViewById(R.id.default_message));
		messages.put(MESSAGES_KEY_ERROR_MESSAGE, messagesContainer.findViewById(R.id.error_message));
		messages.put(MESSAGES_KEY_LOADING, messagesContainer.findViewById(R.id.loading));
		messages.put(MESSAGES_KEY_RESULT, messagesContainer.findViewById(R.id.results));
	}
	
	public String call() throws Exception {
		String url = DINAU_BASE_PATH+zipCode;
		
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setRequestProperty("User-Agent", System.getProperties().getProperty("http.agent"));
		String response = readInputStream(conn.getInputStream());
		conn.disconnect();
		
		return response;
	}

	public void onPreExecute() {
		showMessage(MESSAGES_KEY_LOADING);
	}
	
	public void onSuccess(String response) {
		try {
			JSONObject json = new JSONObject(response);
			
			if (json.has("error")) {
				throw new Exception(json.getString("error"));
			}
			
			String dinau = json.getString("need");
			String temp = json.getString("temp");
			String status = json.getString("status");
			String period = json.getString("period");
			String description = json.getString("description");
			String percent = json.getInt("percent")+"%";
			
			String result = context.getString(R.string.dinau_result,
				temp,
				status.toLowerCase(),
				description,
				period,
				percent
			);
			
			showResults(dinau.toUpperCase(), result);
		}
		catch (Exception e) {
			onException(e);
		}
	}
	
	public void onException(Exception e) {
		showError(e.getMessage());
	}
	
	private String readInputStream(InputStream is) throws Exception {
		StringBuilder sb = new StringBuilder();
		BufferedReader r = new BufferedReader(new InputStreamReader(is), 1000);
		
		for (String line = r.readLine(); line != null; line = r.readLine()) {
			sb.append(line);
		}
		
		is.close();
		return sb.toString();
	}
	
	private void showResults(String dinau, String result) {
		showMessage(MESSAGES_KEY_RESULT);
		ViewGroup group = (ViewGroup) messages.get(MESSAGES_KEY_RESULT);
		TextView summary = (TextView) group.findViewById(R.id.results_summary);
		TextView details = (TextView) group.findViewById(R.id.results_details);
		
		summary.setText(dinau);
		details.setText(result);
	}
	
	private void showError(String error) {
		showMessage(MESSAGES_KEY_ERROR_MESSAGE);
		TextView errorMessage = (TextView) messages.get(MESSAGES_KEY_ERROR_MESSAGE);
		errorMessage.setText(context.getString(R.string.error_message, error));
	}
	
	private void showMessage(String key) {
		for (View v : messages.values()) {
			v.setVisibility(View.GONE);
		}
		
		messages.get(key).setVisibility(View.VISIBLE);
	}
}
