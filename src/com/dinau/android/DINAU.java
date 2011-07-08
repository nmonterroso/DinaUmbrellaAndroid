package com.dinau.android;

import java.util.HashMap;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.dinau.android.util.DinauAsyncRequest;
import com.dinau.android.util.ZipFetcher;
import com.google.inject.Inject;

public class DINAU extends RoboActivity {
	public static final int MESSAGES_KEY_DEFAULT_MESSAGE = 0;
	public static final int MESSAGES_KEY_ERROR_MESSAGE = 1;
	public static final int MESSAGES_KEY_LOADING = 2;
	public static final int MESSAGES_KEY_RESULT = 3;
	
	private static final long LOCATION_DELTA_CUTOFF = 1000*60*15; // 15 minutes
	
	private DinauAsyncRequest dinauRequest;
    private int currentView;
	
    @InjectView(R.id.main_logo) 		private ImageView logoImage;
    @InjectView(R.id.zip_code_entry) 	private EditText zipCode;
    @InjectView(R.id.dinau_submit)		private Button submitButton;
    @InjectView(R.id.parent)			private LinearLayout root;
    @InjectView(R.id.dinau_messages)	private RelativeLayout messagesContainer;
    @Inject								private LocationManager locationManager;
    @Inject								private HashMap<Integer, View> messages;
    @Inject								private InputMethodManager inputMethodManager;
    
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

		messages.put(MESSAGES_KEY_DEFAULT_MESSAGE, messagesContainer.findViewById(R.id.default_message));
		messages.put(MESSAGES_KEY_ERROR_MESSAGE, messagesContainer.findViewById(R.id.error_message));
		messages.put(MESSAGES_KEY_LOADING, messagesContainer.findViewById(R.id.loading));
		messages.put(MESSAGES_KEY_RESULT, messagesContainer.findViewById(R.id.results));
        
        loadLogo();
        bindSubmit();
    }
    
    public void onPause() {
    	super.onPause();
    	if (locationManager != null) {
    		locationManager.removeUpdates(locationListener);
    	}
    }
    
    public void onConfigurationChanged(Configuration config) {
    	super.onConfigurationChanged(config);
    	
    	if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
    		logoImage.setVisibility(View.GONE);
    	}
    	else {
    		logoImage.setVisibility(View.VISIBLE);
    	}
    }
    
    private void bindSubmit() {
    	zipCode.setOnKeyListener(new View.OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
					submitDinauRequest(v);
					return true;
				}
				
				return false;
			}
		});
    	
    	submitButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (getZipText() == null) {
					return;
				}
				
				submitDinauRequest(v);
			}
		});
    }
    
    private void submitDinauRequest(View v) {
    	inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
    	doAsyncRequest(getZipText());
    }
    
    public void doAsyncRequest(String zip) {
    	if (dinauRequest != null) {
    		dinauRequest.cancel(true);
    	}
    	
    	if (currentView != R.id.loading) {
    		showMessage(MESSAGES_KEY_LOADING);
    	}
    	
    	dinauRequest = new DinauAsyncRequest(this, zip);
    	dinauRequest.execute();
    }
    
    private void loadLogo() {
    	Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.logo);
    	
    	int maxWidth = getWindowManager().getDefaultDisplay().getWidth() - (root.getPaddingLeft() + root.getPaddingRight());
    	int bmpWidth = bmp.getWidth();
    	int bmpHeight = bmp.getHeight();
    	
    	if (bmpWidth > maxWidth) {
    		float ratio = (float) maxWidth / bmpWidth;
    		bmpWidth = maxWidth;
    		bmpHeight = (int) (ratio*bmpHeight);
    	}
    	
    	LayoutParams params = logoImage.getLayoutParams();
    	params.height = bmpHeight;
    	params.width = bmpWidth;
    	
    	logoImage.setImageBitmap(bmp);
    	logoImage.setLayoutParams(params);
    	logoImage.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				zipCode.setText("");
				startLocationListener();
			}
		});
    }
    
    private void startLocationListener() {
    	try {
    		showMessage(MESSAGES_KEY_LOADING);
    		Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    		if (isValidLocation(lastLocation)) {
    			locationListener.onLocationChanged(lastLocation);
    		}
    		else {
    			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
    		}
    	}
    	catch (Exception e) {
    		showError(getString(R.string.unable_fetch_zip));
    	}
    }
    
    private LocationListener locationListener = new LocationListener() {
		public void onLocationChanged(Location location) {
			if (isValidLocation(location)) {
				locationManager.removeUpdates(locationListener);
				new ZipFetcher(DINAU.this, location).execute();
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
    
    private String getZipText() {
    	String text = zipCode.getText().toString();
    	return text == null || text.trim().equals("") ? null : text;
    }
    
    public void showError(String error) {
    	TextView errorMessage = (TextView) showMessage(DINAU.MESSAGES_KEY_ERROR_MESSAGE);
		errorMessage.setText(getString(R.string.error_message, error));
    }
    
    public void showResults(String dinau, String result) {
    	ViewGroup group = (ViewGroup) showMessage(DINAU.MESSAGES_KEY_RESULT);
    	TextView summary = (TextView) group.findViewById(R.id.results_summary);
		TextView details = (TextView) group.findViewById(R.id.results_details);
		
		summary.setText(dinau);
		details.setText(result);
    }
    
    public View showMessage(Integer key) {
    	for (View v : messages.values()) {
			v.setVisibility(View.GONE);
		}
		
    	currentView = key;
    	View view = getMessageView(key);
    	view.setVisibility(View.VISIBLE);
    	return view;
    }
    
    public View getMessageView(Integer key) {
    	return messages.get(key);
    }
}