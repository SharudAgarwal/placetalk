package com.placetalk;

import static com.placetalk.CommonUtilities.SENDER_ID;
import static com.placetalk.CommonUtilities.SERVER_URL;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gcm.GCMRegistrar;
import com.placetalk.R;

public class MainActivity extends Activity {
	
	AsyncTask<Void, Void, Void> mRegisterTask;
	static String regId;
	ProgressDialog progressDialog;
	
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
        // Make sure we're running on Honeycomb or higher to use ActionBar APIs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
            getActionBar().hide();
        }  
		
	}
	
	@Override
	protected void onStart() {
		super.onStart();	
		registerDeviceGCM();	
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	public void goToGroups(View view) {
		EditText editText = (EditText) findViewById(R.id.edit_username);
		String s = editText.getText().toString();
		if (s.contains("\n")) {
			Toast toast = Toast.makeText(this, "Illegal username", Toast.LENGTH_SHORT);
        	toast.setGravity(Gravity.CENTER, 0, 5);
        	toast.show();
		}
		else if (s.length() >= 1) {
			Intent intent = new Intent(this, GroupListActivity.class);
	        intent.putExtra("username", s);
//	        Log.i("goToGroups", "regId: "+regId);
	        intent.putExtra("regId", regId);
	        startActivity(intent);
		}
		else {
        	Toast toast = Toast.makeText(this, "No username entered!", Toast.LENGTH_SHORT);
        	toast.setGravity(Gravity.CENTER, 0, 5);
        	toast.show();
        }	
    }
	
	  public String registerDeviceGCM() {
		  
		  checkNotNull(SERVER_URL, "SERVER_URL");
	      checkNotNull(SENDER_ID, "SENDER_ID");
	      // Make sure the device has the proper dependencies.
	      GCMRegistrar.checkDevice(this);
	      // Make sure the manifest was properly set - comment out this line
	      // while developing the app, then uncomment it when it's ready.
	      GCMRegistrar.checkManifest(this);
//	      registerReceiver(mHandleMessageReceiver,
//	              new IntentFilter(DISPLAY_MESSAGE_ACTION));
	      final String regId = GCMRegistrar.getRegistrationId(this);
	      
	      
	      
	      if (regId.equals("")) {
	          // Automatically registers application on startup.
	          GCMRegistrar.register(this, SENDER_ID);
	      } else {
	    	  MainActivity.regId = regId;
	          // Device is already registered on GCM, check server.
	          if (GCMRegistrar.isRegisteredOnServer(this)) {
	              // Skips registration.
	        	  //Toast.makeText(this, "Device already registered on server", Toast.LENGTH_SHORT).show();
	           //   mDisplay.append(getString(R.string.already_registered) + "\n");
	          } else {
	              // Try to register again, but not in the UI thread.
	              // It's also necessary to cancel the thread onDestroy(),
	              // hence the use of AsyncTask instead of a raw thread.
	              final Context context = this;
	              mRegisterTask = new AsyncTask<Void, Void, Void>() {
	            	  
	                  @Override
	                  protected Void doInBackground(Void... params) {
	                      boolean registered = ServerUtilities.register(context, regId);
	                      // At this point all attempts to register with the app
	                      // server failed, so we need to unregister the device
	                      // from GCM - the app will try to register again when
	                      // it is restarted. Note that GCM will send an
	                      // unregistered callback upon completion, but
	                      // GCMIntentService.onUnregistered() will ignore it.
	                      if (!registered) {
	                          GCMRegistrar.unregister(context);
	                      }
	                      return null;
	                  }

	                  @Override
	                  protected void onPostExecute(Void result) {
	                      mRegisterTask = null;
//	                      MainActivity.regId = GCMRegistrar.getRegistrationId(this);
//	                      Toast.makeText(MainActivity.this, "Registration complete", Toast.LENGTH_SHORT).show();
	                      MainActivity.regId = GCMRegistrar.getRegistrationId(MainActivity.this);
	    
	                  }

	              };
	              mRegisterTask.execute(null, null, null);
//	          }
	           
	      }
	      
	      //MainActivity.regId = GCMRegistrar.getRegistrationId(MainActivity.this);
	    	  
//	      Log.i("registerDeviceGCM", "regId: "+MainActivity.regId);
	    	  

	      }
//	      Log.i("MainActivity registering", "regId: " + regId);
	      return regId;
		}
	  
		private void checkNotNull(Object reference, String name) {
	        if (reference == null) {
	            throw new NullPointerException(
	                    getString(R.string.error_config, name));
	        }
	    }
		
		@Override
		protected void onDestroy()
		{	
		    GCMRegistrar.unregister(this);
		    super.onDestroy();
		}
}
