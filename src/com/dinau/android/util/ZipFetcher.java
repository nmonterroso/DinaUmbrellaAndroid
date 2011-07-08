package com.dinau.android.util;

import java.util.List;

import roboguice.util.RoboAsyncTask;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;

import com.dinau.android.DINAU;
import com.dinau.android.R;

public class ZipFetcher extends RoboAsyncTask<String> {
	private static final int GEO_MAX_RESULTS = 20;
	
	private DINAU dinau;
	private Location location;
	private boolean retry = true;
	
	public ZipFetcher(DINAU dinau, Location location) {
		this.dinau = dinau;
		this.location = location;
	}
	
	public String call() throws Exception {
		Geocoder geo = new Geocoder(dinau);
		List<Address> addresses = geo.getFromLocation(
			location.getLatitude(), 
			location.getLongitude(), 
			GEO_MAX_RESULTS
		);
		
		for (Address address : addresses) {
			if (address.getPostalCode() != null) {
				return address.getPostalCode();
			}
		}
		
		if (retry) {
			retry = false;
			return call();
		}
		else {
			throw new Exception(dinau.getString(R.string.unable_fetch_zip));
		}
		
	}
	
	public void onSuccess(String zipcode) {
		dinau.doAsyncRequest(zipcode);
	}
	
	public void onException(Exception e) {
		showError(e.getMessage());
	}
	
	private void showError(String error) {
		dinau.showError(error);
	}
}
