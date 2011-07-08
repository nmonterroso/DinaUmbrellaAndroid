package com.dinau.android.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import roboguice.util.RoboAsyncTask;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dinau.android.DINAU;
import com.dinau.android.R;

public class DinauAsyncRequest extends RoboAsyncTask<String> {
	private static final int ZIPCODE_USE_LOCATION_SERVICE = -1;
	private static final long LOCATION_DELTA_CUTOFF = 1000*60*5; // 5 minutes
	
	private static final String MESSAGES_KEY_DEFAULT_MESSAGE = "default_message";
	private static final String MESSAGES_KEY_ERROR_MESSAGE = "error_message";
	private static final String MESSAGES_KEY_LOADING = "loading_container";
	private static final String MESSAGES_KEY_RESULT = "results_container";
	
	private static final String DINAU_BASE_PATH = "http://doineedanumbrella.com/api/";
	
	private String zipCode;
	private Context context;
	private HashMap<String, View> messages;
	private LocationManager locationManager;
	private Location currentLocation;
	
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
		int zip = getZipCode();
		if (zip == 0) {
			throw new Exception("Unable to get zip code");
		}
		else if (zip == ZIPCODE_USE_LOCATION_SERVICE) {
			while (currentLocation == null) {}
			locationManager.removeUpdates(locationListener);
			Geocoder geo = new Geocoder(context);
			List<Address> addresses = geo.getFromLocation(
				currentLocation.getLatitude(), 
				currentLocation.getLongitude(), 
				DINAU.GEO_MAX_RESULTS
			);
			
			for (Address address : addresses) {
				if (address.getPostalCode() != null) {
					zipCode = address.getPostalCode();
					zip = getZipCode();
					break;
				}
			}
		}
		
		String url = DINAU_BASE_PATH+zip;
		
		HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
		conn.setRequestProperty("User-Agent", System.getProperties().getProperty("http.agent"));
		String response = readInputStream(conn.getInputStream());
		conn.disconnect();
		
		return response;
	}

	public void onPreExecute() {
		showMessage(MESSAGES_KEY_LOADING);
		if (zipCode == null || zipCode.trim().equals("")) {
			startLocationListener();
		}
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
	
	private int getZipCode() {
		if (zipCode == null || zipCode.trim().equals("")) {
			return ZIPCODE_USE_LOCATION_SERVICE;
		}
		
		try {
			return Integer.parseInt(zipCode);
		}
		catch (NumberFormatException e) {
			return parseZipcodeFromLocation(zipCode);
		}
	}
	
	private int parseZipcodeFromLocation(String location) {
    	Geocoder geo = new Geocoder(context);
    	
    	try {
    		String zip;
    		for (Address address : geo.getFromLocationName(location, DINAU.GEO_MAX_RESULTS)) {
    			zip = address.getPostalCode();
    			if (zip != null) {
    				return Integer.parseInt(zip);
    			}
    		}
    	}
    	catch (Exception e) {}
    	
    	return 0;
    }
	
	private boolean startLocationListener() {
		if (locationManager == null) {
			locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		}
		
		try {
			Location lastLocationNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			locationListener.onLocationChanged(lastLocationNetwork);
			if (!isValidLocation(lastLocationNetwork)) {
				locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
			}
			
			return true;
		}
		catch (Exception e) {
			return false;
		}
	}
	
	private LocationListener locationListener = new LocationListener() {
		public void onLocationChanged(Location location) {
			if (location != null && isValidLocation(location)) {
				currentLocation = location;
			}
		}

		public void onProviderDisabled(String arg0) {}
		public void onProviderEnabled(String arg0) {}
		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {}
		
	};
	
	private boolean isValidLocation(Location location) {
		if (location == null) {
			return false;
		}
		
		
		long timeDelta = System.currentTimeMillis() - location.getTime();
		if (timeDelta <= LOCATION_DELTA_CUTOFF) {
			return true;
		}
		else {
			return false;
		}
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
