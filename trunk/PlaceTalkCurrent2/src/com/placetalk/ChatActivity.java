package com.placetalk;

import android.annotation.SuppressLint; 
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.placetalk.R;

public class ChatActivity extends Activity {
	public ListView msgView;
	public ArrayAdapter<String> msgList;
	TextView mDisplay;
    AsyncTask<Void, Void, Void> mRegisterTask;
    private static final String EXTRA_JSON = "json";
    private static final String TAG = "GCMIntentService";
    private Group myGroup;
    String regId;
    String username;
	 
	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
//		Log.i("ChatActivity", "In onCreate");
		super.onCreate(savedInstanceState);
        // Make sure we're running on Honeycomb or higher to use ActionBar APIs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
        	getActionBar().setDisplayHomeAsUpEnabled(true);
        }  
		
		Intent intent = getIntent();
		String json = intent.getStringExtra("json");
		username = intent.getStringExtra("username");
//		Log.i("JSON", "in ChatActivity, received group json: " + json);
		
		myGroup = (new Gson()).fromJson(json, Group.class);
//		Log.i("Group", "In ChatActivity, converted json to Group: " + myGroup);
		regId = intent.getStringExtra("regId");
		myGroup.joinGroup(regId);
		
		registerReceiver(mHandleMessageReceiver, new IntentFilter("receiveMessage"));
		
		setContentView(R.layout.activity_chat);
		
		msgView = (ListView) findViewById(R.id.msgListView);
		msgList = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		msgView.setAdapter(msgList);		 
		
		sendMessageToServer(username + " has joined group");
		
		TextView text = (TextView) findViewById(R.id.text_chatGroupName);
		text.setText(myGroup.getGroupName());
		Button btn = (Button) findViewById(R.id.button_sendMsg);
		final EditText editText = (EditText) findViewById(R.id.edit_typeMsg);
		
		btn.setOnClickListener(new View.OnClickListener() {
		    @Override
		    public void onClick(View v) {
		        final String message = editText.getText().toString();
		        if (message.length() >= 1) {
		        	editText.setText("");    
		        	sendMessageToServer(username+": "+message);
		        }
		        else {
		        	Toast toast = Toast.makeText(ChatActivity.this, "Can't send empty message", Toast.LENGTH_SHORT);
		        	toast.setGravity(Gravity.CENTER, 0, 5);
		        	toast.show();
		        }
		    }
		});
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.chat, menu);
		return true;
	}
	
	private final BroadcastReceiver mHandleMessageReceiver = new BroadcastReceiver() {
		
		@Override
        public void onReceive(Context context, Intent intent) {
//        	Log.i(TAG, "in onReceive");
        	String json = intent.getStringExtra(EXTRA_JSON);
        	Gson gson = new Gson();
        	try {
        		Group gotGroup = gson.fromJson(json, Group.class);
//        		Log.i("TAG", "gotGroupName = " + gotGroup.getGroupName() + "myGroupName = " + myGroup.getGroupName() + "gotGroupID = " + gotGroup.getLocationID() + "myGroupID = " + myGroup.getLocationID());
		        if ((gotGroup.getGroupName().equals(myGroup.getGroupName())) && (gotGroup.getLocationID().equals(myGroup.getLocationID()))) {
		        	String message = gotGroup.getLatestMessage();
		        	myGroup.addMessage(message);
//		        	myGroup.setDevices(gotGroup.getDevices());
		        	String output = "Group: " + myGroup.getGroupName() + "/n" + "message: " + message;
//		        	Log.i(TAG, output);
		        	displayMessage(message);
	        	} else {
//	        		ServerUtilities.unregisterOldId();
	        	}
        	} catch (JsonSyntaxException e) {
        		
        	}
        }
		 
    };

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
	
	
	public void sendMessage(String message, String group) throws Exception {

	}
	
	public void displayMessage(String message) {
		msgList.add(message);
		msgList.notifyDataSetChanged();
		msgView.smoothScrollToPosition(msgList.getCount() - 1);
	}
	
	public void sendMessageToServer(String message) {
//		Log.i("SendMessageToServer", "sending message: " + message);
		
		myGroup.addMessage(message);
		
        Thread trd = new Thread(new Runnable() {
        	@Override
            public void run() {
           	ServerUtilities.sendMessage(myGroup);  
            }
        });
        trd.start();
	}
	
	
	@Override
	protected void onStop()
	{	
//		Log.i("onStop","onStop");
	    super.onStop(); 
	}
	
	@Override
	protected void onDestroy()
	{	
//		Log.i("onDestroy","onDestroy");
//		myGroup.leaveGroup(regId);
		myGroup.leaveGroupInApp();
		sendMessageToServer(username + " has left group");
	    unregisterReceiver(mHandleMessageReceiver);
	    super.onDestroy();
	}
	@Override
	protected void onPause()
	{	
//		Log.i("onPause","onPause");
	    super.onPause();
	}
	@Override
	protected void onRestart()
	{	
//		Log.i("onRestart","onRestart");
	    super.onRestart();
	}
	@Override
	protected void onResume()
	{	
//		Log.i("onResume","onResume");
	    super.onResume();
	}
}
