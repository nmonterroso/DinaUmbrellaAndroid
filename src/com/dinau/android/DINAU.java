package com.dinau.android;

import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.dinau.android.util.DinauAsyncRequest;

public class DINAU extends RoboActivity {
    @InjectView(R.id.main_logo) 		private ImageView logoImage;
    @InjectView(R.id.zip_code_entry) 	private EditText zipCode;
    @InjectView(R.id.dinau_submit)		private Button submitButton;
    @InjectView(R.id.parent)			private LinearLayout root;
    @InjectView(R.id.dinau_messages)	private RelativeLayout messagesContainer;
    
    private DinauAsyncRequest dinauRequest;
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        loadLogo();
        bindSubmit();
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
				submitDinauRequest(v);
			}
		});
    }
    
    private void submitDinauRequest(View v) {
    	((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
    	String zip = getZipText();
    	
		if (zip == null) {
			toast(getString(R.string.submit_empty_zip));
		}
		else {
			doAsyncRequest(zip);
		}
    }
    
    private void doAsyncRequest(String zip) {
    	if (dinauRequest != null) {
    		// cancel current request
    	}
    	
    	dinauRequest = new DinauAsyncRequest(getApplicationContext(), zip, messagesContainer);
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
    }
    
    private String getZipText() {
    	String text = zipCode.getText().toString();
    	
    	return text == null || text.trim().equals("") ? null : text;
    }
    
    private void toast(String message) {
    	toast(message, Toast.LENGTH_LONG);
    }
    
    private void toast(String message, int length) {
    	Toast.makeText(this, message, length).show();
    }
}