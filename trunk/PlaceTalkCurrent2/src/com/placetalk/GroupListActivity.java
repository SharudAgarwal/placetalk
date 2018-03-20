package com.placetalk;

import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gcm.GCMRegistrar;
import com.google.gson.Gson;
import com.placetalk.R;

public class GroupListActivity extends ListActivity {

    private static final String TAG = "GroupListActivity";
    private static final String EXTRA_JSON = "json";	
	
    private ArrayList<Group> groupList;
//    private PullToRefreshListView mPullRefreshListView;
    private ArrayAdapter<Group> adapter;
    
    
//    Button refreshButton;

    String regId;
    String username;
    boolean fromCreateGroup;
    GetGroups getGroupsTask;
    Thread trd;

	/** Called when the activity is first created. */
    @SuppressLint("NewApi")
	@Override
    public void onCreate(Bundle savedInstanceState) {
//    	Log.i("GroupListActivity", "In onCreate");
        super.onCreate(savedInstanceState);
        
        // Make sure we're running on Honeycomb or higher to use ActionBar APIs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
        	getActionBar().setDisplayHomeAsUpEnabled(true);
        }  
        
        Intent i = getIntent();
        username = i.getStringExtra("username");
        regId = i.getStringExtra("regId");
        
        getGroupsTask = new GetGroups();
        //refreshButton = (Button) findViewById(R.id.button_refresh_groups);
        
        registerReceiver(handleGroupReceiver, new IntentFilter("receiveGroups"));
        
        
        setContentView(R.layout.activity_group_list);
        
    }
    
    @Override
    public void onStart() {
//    	Log.i("ChatActivity", "In onStart");
    	
//    	if (!fromCreateGroup) {
//    		
    		getGroups(getLocation(this));
//    	}
//    	else
//    		fromCreateGroup = false;
    	super.onStart();
    }
    
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
    
    public void createGroup(View view) {
        Intent intent = new Intent(this, CreateGroupActivity.class);
        intent.putExtra("username", username);
        intent.putExtra("regId", regId);
        startActivity(intent);
    }
    
    public void refreshGroups(View view) {
    	getGroups(getLocation(this));
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	super.onListItemClick(l, v, position, id);
    	Intent intent = new Intent(this, ChatActivity.class);
    	Group clickedGroup = (Group) l.getItemAtPosition(position);
//    	clickedGroup.leaveGroup(regId);
    	clickedGroup.leaveGroupInApp();
    	Gson gson = new Gson();
    	String json = gson.toJson(clickedGroup);
    	intent.putExtra("json", json);
    	intent.putExtra("regId", regId);
    	intent.putExtra("username", username);
    	startActivity(intent);
    }
    
	public String getLocation(Context context) {
		GPSTracker gps = new GPSTracker(context);
		if (gps.canGetLocation()) {
			Double lat = gps.getLatitude();
			Double lon = gps.getLongitude();
			String location = lat.toString() + "," + lon.toString();
//			double location = Math.abs(lat) * Math.abs(lon);
//			if (location == 0) { location = "1,1"; }
			return location;
		} else {
			return "1.0,1.0";
		}
	}
	
	public void getGroups(final String location) {
		//refreshButton.setEnabled(false);
		groupList = new ArrayList<Group>();
		
		if(getGroupsTask.getStatus().equals(AsyncTask.Status.FINISHED) || getGroupsTask.getStatus().equals(AsyncTask.Status.PENDING)) {
			getGroupsTask = new GetGroups();
			getGroupsTask.execute(location, null, null);
		}
//        trd = new Thread(new Runnable() {
//        	@Override
//            public void run() {
//        		ServerUtilities.getGroups(location, regId);
//            }
//        });
//        trd.start();
        
//		new GetGroups().execute(location, null, null);
	}
	
	class GetGroups extends AsyncTask<String, Void, Void> {
	    ProgressDialog progressDialog;

	    @Override
	    protected void onPreExecute() {
	        progressDialog = ProgressDialog.show(GroupListActivity.this, "", "Finding groups near you...");
	        super.onPreExecute();
	    }
	    
	    protected Void doInBackground(String... location) {
//	    	Log.i("GetGroups", "in doInBackground()");
	    	ServerUtilities.getGroups(location[0], regId);
	    	return null;
			
	    }

	    protected void onPostExecute(Void result) {
//	    	Log.i("GetGroups", "in onPostExecute()");
	    	//refreshButton.setEnabled(true);
	    	if (progressDialog.isShowing()) {
	    		progressDialog.dismiss();
	    	}
	    	
	    	super.onPostExecute(null);
	        
	    }
	}
	
	
	private final BroadcastReceiver handleGroupReceiver = new BroadcastReceiver() {
		
	@Override
      public void onReceive(Context context, Intent intent) {

//      	Log.i(TAG, "in onReceive");
      	String json = intent.getStringExtra(EXTRA_JSON);
//      	Log.i(TAG, "json = "+json);
      	Gson gson = new Gson();

		@SuppressWarnings("unchecked")
		ArrayList<String> jGroups = gson.fromJson(json, ArrayList.class);
//  		Log.i(TAG, "jGroups = "+jGroups);
  		for (String s : jGroups) {
//  			Log.i(TAG, "In for loop groupList = "+groupList);
  			Group group = gson.fromJson(s, Group.class);
  			groupList.add(group);
  		}
//  		Log.i(TAG, "groupList = "+groupList);

  		adapter = new ArrayAdapter<Group>(GroupListActivity.this, android.R.layout.simple_list_item_1, groupList);
        setListAdapter(adapter);
      }
		
  };
  
	@Override
	protected void onDestroy()
	{	
	    unregisterReceiver(handleGroupReceiver);
	    GCMRegistrar.unregister(GroupListActivity.this);
	    
	    super.onDestroy();
	}
	
}
