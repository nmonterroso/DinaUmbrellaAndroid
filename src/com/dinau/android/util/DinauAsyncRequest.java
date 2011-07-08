package com.dinau.android.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;

import roboguice.util.RoboAsyncTask;

import com.dinau.android.DINAU;
import com.dinau.android.R;

public class DinauAsyncRequest extends RoboAsyncTask<String> {
	private static final String DINAU_BASE_PATH = "http://doineedanumbrella.com/api/";
	
	private DINAU dinau;
	private String zipCode;
	
	public DinauAsyncRequest(DINAU dinau, String zipCode) {
		this.dinau = dinau;
		this.zipCode = zipCode;
	}
	
	public String call() throws Exception {
		String url = DINAU_BASE_PATH+zipCode;
		
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setRequestProperty("User-Agent", System.getProperties().getProperty("http.agent"));
		String response = readInputStream(conn.getInputStream());
		conn.disconnect();
		
		return response;
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
			String description = json.getString("description");
			
			String result = context.getString(R.string.dinau_result,
				temp,
				status.toLowerCase(),
				description
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
		this.dinau.showResults(dinau, result);
	}
	
	private void showError(String error) {
		dinau.showError(error);
	}
}
