package com.placetalk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.placetalk.R;

public class CreateGroupActivity extends Activity {
	
	String username, regId;
	
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
        // Make sure we're running on Honeycomb or higher to use ActionBar APIs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
        	getActionBar().setDisplayHomeAsUpEnabled(true);
        }  
	    
	    Intent intent = getIntent();
	    username = intent.getStringExtra("username");
	    regId = intent.getStringExtra("regId");
	    
	    setContentView(R.layout.activity_create_group);
	    
//	    EditText textGroupRange = (EditText) findViewById(R.id.edit_group_range);
//	    textGroupRange.setText("50");
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Intent upIntent = new Intent(this, GroupListActivity.class);
			upIntent.putExtra("regId", regId);
			upIntent.putExtra("username", username);
			NavUtils.navigateUpTo(this, upIntent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void addGroup(View view) {	
        EditText textGroupName = (EditText) findViewById(R.id.edit_group_name);
        String groupName = textGroupName.getText().toString();
        EditText textGroupRange = (EditText) findViewById(R.id.edit_group_range);
        String stringGroupRange = textGroupRange.getText().toString();
        int groupRange;
        
        try {
        	groupRange = Integer.parseInt(stringGroupRange);
        }
        catch (NumberFormatException e) {
        	groupRange = 50;
        }
        
        if (groupName.contains("\n")) {
			Toast toast = Toast.makeText(this, "Illegal group name", Toast.LENGTH_SHORT);
        	toast.setGravity(Gravity.CENTER, 0, 5);
        	toast.show();
		}
        if (groupName.length() >= 1) {
        	if (groupName.length() < 25) {
	        	if (groupRange >= 10) {
	        		if (groupRange < 200) {
			        	Intent intent = new Intent(this, ChatActivity.class);
			        	Group newGroup = new Group(groupName, getLocation(this), groupRange);
			        	String json = (new Gson()).toJson(newGroup);
			        	intent.putExtra("json", json);
			        	intent.putExtra("regId", regId);
			        	intent.putExtra("username", username);
			        	startActivity(intent);
	            	}
	        		else {
	                	Toast toast = Toast.makeText(this, "Range must be less than 200 meters", Toast.LENGTH_SHORT);
	                	toast.setGravity(Gravity.CENTER, 0, 5);
	                	toast.show();
	                }
	        	}
	        	else {
	            	Toast toast = Toast.makeText(this, "Range must be at least 10 meters", Toast.LENGTH_SHORT);
	            	toast.setGravity(Gravity.CENTER, 0, 5);
	            	toast.show();
	            }
        	}
        	else {
            	Toast toast = Toast.makeText(this, "Name must be less than 25 characters", Toast.LENGTH_SHORT);
            	toast.setGravity(Gravity.CENTER, 0, 5);
            	toast.show();
            }
        	
        }
        else {
        	Toast toast = Toast.makeText(this, "No group name entered!", Toast.LENGTH_SHORT);
        	toast.setGravity(Gravity.CENTER, 0, 5);
        	toast.show();
        }
        
    }
	
	public String getLocation(Context context) {
		GPSTracker gps = new GPSTracker(context);
		if (gps.canGetLocation()) {
			Double lat = gps.getLatitude();
			Double lon = gps.getLongitude();
			String location = lat.toString() + "," + lon.toString();
			return location;
		} else {
			return "1.0,1.0";
		}
	}

}
