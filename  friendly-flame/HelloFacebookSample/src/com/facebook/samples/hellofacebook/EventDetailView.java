package com.facebook.samples.hellofacebook;

import org.json.JSONArray;
import org.json.JSONObject;

import com.android.future.usb.UsbManager;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.android.FacebookError;
import com.facebook.model.GraphObject;
import com.facebook.samples.hellofacebook.R;
import com.facebook.samples.hellofacebook.HelloFacebookSampleActivity.RunnableForArduinoService;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

//detailview for events
public class EventDetailView extends Activity implements OnClickListener {

	
	public String eid;
	public String[] userAllEventsResult;
	public String[][] userAllEvents; 
	public String[][] userRSVP;
	public String[] userRSVPResult;
    private int userAttributes = 0; 
	private int all_events = 0;
    public String[] returnStringResult;
    
    public TextView nameText;
    public TextView dateText;
    public TextView locationText;
    public TextView rsvpText;
    public TextView allMemCount;
    public TextView acceptMemCount;
    public TextView declinedMemCount;
    
    private Button detailButton; 
    
    
    public String query_allEvents;
    public String query_eventStatus;
//    private Flame flame = new Flame();
		
    
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			 if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
			    unbindService(arduinoConnection);
			    Log.d("MainActivity","Service unbound");
				finish();
			}
		}
	}; 
	
    private Handler messageHandler = new Handler();
    private ArduinoService.MyServiceBinder arduinoBinder = null;
	private ServiceConnection arduinoConnection = 
		new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			arduinoBinder = (ArduinoService.MyServiceBinder) arg1;
			//arduinoBinder.setRunnable(new RunnableForArduinoService());
			arduinoBinder.setActivityCallbackHandler(messageHandler);
			Log.d("MainActivity","Arduino Service is connected!");				
		}

		@Override
		public void onServiceDisconnected(ComponentName arg0) {		
			Log.d("MainActivity","Arduino Service is disconnected!");	
		}

	};
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_event_detail_view);
		
		Log.d("EventDetailView", "test");
		
		Bundle extras = getIntent().getExtras();
		eid = extras.getString("eid");
		Log.d("EventDetailView", eid);
		
		query_allEvents = "SELECT eid, name, start_time, location, all_members_count, attending_count, declined_count  FROM event WHERE eid =  " + eid;
	    query_eventStatus = "SELECT eid, rsvp_status FROM event_member WHERE uid = me() and start_time > 0 and eid =  " + eid;
		
		//setup text fields for GUI
		nameText = (TextView) findViewById(R.id.eventName);
		dateText = (TextView) findViewById(R.id.eventDate);
		locationText = (TextView)findViewById(R.id.location);
		rsvpText = (TextView)findViewById(R.id.rsvp);
		allMemCount = (TextView)findViewById(R.id.all_members_count); 
		acceptMemCount = (TextView)findViewById(R.id.attending_count);
		declinedMemCount = (TextView)findViewById(R.id.declined_count);
		detailButton = (Button)findViewById(R.id.button_detail);
		detailButton.setOnClickListener(this);
		
		
		Log.d("query", query_allEvents);
 		Log.d("query", query_eventStatus);
 		
		//get details for current event
		getAllEvents(query_allEvents);
		getAllEvents(query_eventStatus);
		
		IntentFilter filter = new IntentFilter("com.google.android.BeyondTheDesktop.action.USB_PERMISSION");
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(mUsbReceiver, filter);	
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_event_detail_view, menu);
		return true;
	}
	
		//method to execute FQL query, get the event with the given eid
	 	public void getAllEvents(final String query){
	 		
	 		
	 		//send FQL request and retrieve informations from JSON object
	         Bundle params = new Bundle();
	         params.putString("q", query);
	         Session session = Session.getActiveSession();
	         Request request = new Request(session,
	             "/fql",                         
	             params,                         
	             HttpMethod.GET,                 
	             new Request.Callback(){         
	                 public void onCompleted(Response response) {
	                     Log.i("TAG", "Result: " + response.toString());
	                     
	                     //parse output from FQL query
	                     
	                     if (query.equals(query_allEvents)) {
	                    	 parseUserFromFQLResponse(response); // 
	                     }
	                     else  {
	                    	 parseUserDetailsFromFQLResponse(response);
	                     }
	                 }                  
	         }); 
	         Request.executeBatchAsync(request);    
	 	}
	 	
	 	
	 	//method to filter needed informations from JSON Object
	    protected void parseUserFromFQLResponse(Response response) {
			try {
				//this will deliver all events where a user took some part in it,
				//attending, declined, not_replied or maybe
				GraphObject go = response.getGraphObject();
				JSONObject jso = go.getInnerJSONObject();
				JSONArray arr = jso.getJSONArray("data");
				
				//get all events by the length of the data array
				// userAttributes = eventname, date, eid and location
				all_events = arr.length();
				
				userAttributes = 7;  	
				
				//array to find all informations of event
				userAllEvents = new String[all_events][userAttributes];
				
				//array for string concatenation
				userAllEventsResult = new String[all_events];
				
				//array for final results
				returnStringResult = new String[all_events];
				JSONObject json_obj = arr.getJSONObject(0);

				//save attributes in multidimensional arrays
				userAllEvents[0][0] = json_obj.getString("eid");
				userAllEvents[0][1] = json_obj.getString("name");
				userAllEvents[0][2] = json_obj.getString("start_time");
				userAllEvents[0][3] = json_obj.getString("location");
				userAllEvents[0][4] = json_obj.getString("all_members_count");
				userAllEvents[0][5] = json_obj.getString("attending_count");
				userAllEvents[0][6] = json_obj.getString("declined_count");
				
				
				userAllEvents[0][0] = json_obj.getString("eid");
				/**
				Log.d("eid", userAllEvents[0][0]);
				Log.d("name", userAllEvents[0][1]);
				Log.d("start_time", userAllEvents[0][2]);
				Log.d("location", userAllEvents[0][3]);
				**/

				//set textviews with results from FQL query to certain event
				nameText.setText("Event: " + userAllEvents[0][1]);
				dateText.setText("Datum: " + userAllEvents[0][2]);
				allMemCount.setText("Eingeladen: " + userAllEvents[0][4]); 
				acceptMemCount.setText("Zugesagt: " + userAllEvents[0][5]);
				declinedMemCount.setText("Abgesagt: " + userAllEvents[0][6]);
				
				//hide location if no location was entered
				if (!userAllEvents[0][3].equals("null")) {
					locationText.setText("Ort: " + userAllEvents[0][3]);
				}	
				else {
					locationText.setText("Ort: nicht bekannt");
				}
	
				
			} catch(Throwable t) {
				t.printStackTrace();
			}
		}
	    
	  //method to filter needed informations from JSON Object
	    protected void parseUserDetailsFromFQLResponse(Response response) {
			try {
				//this will deliver all events where a user took some part in it,
				//attending, declined, not_replied or maybe
				GraphObject go = response.getGraphObject();
				JSONObject jso = go.getInnerJSONObject();
				JSONArray arr = jso.getJSONArray("data");
				
				//get all events by the length of the data array
				// userAttributes = eventname, date, eid and location
				all_events = arr.length();
				
				userAttributes = 2;  	
				
				//array to find all informations of event
				userRSVP = new String[all_events][userAttributes];
				
				//array for string concatenation
				userRSVPResult = new String[all_events];
				
				//array for final results
				JSONObject json_obj = arr.getJSONObject(0);

				//save attributes in multidimensional arrays
				userRSVP[0][0] = json_obj.getString("rsvp_status");
				String status = "";
				byte[] flamecolor = new byte[1];
				
				if (userRSVP[0][0].equals("attending")) {
					status = "zugesagt";
					//green
					flamecolor[0]=(byte)1;
				} else if (userRSVP[0][0].equals("declined")) {
					status = "abgesagt";
					//red
					flamecolor[0]=(byte)0;
				} else if (userRSVP[0][0].equals("unsure")) {
					status = "unsicher";
					//pink
					flamecolor[0]=(byte)2;
				} else {
					status = "noch nicht beantwortet";
					//blue
					flamecolor[0]=(byte)3;
				}
				
				
				Log.d("rsvp", userRSVP[0][0]);
				
				rsvpText.setText("Status: " + status);
				Log.d("Flamecolor: ", Byte.valueOf(flamecolor[0]).toString());
				arduinoBinder.sendMessageToArduino(flamecolor);
//				flame.flameConnector(color);
				//set textviews with results from FQL query to certain event
//				nameText.setText("Event: " + userAllEvents[0][1]);
//				dateText.setText("Datum: " + userAllEvents[0][2]);					
	
				
			} catch(Throwable t) {
				t.printStackTrace();
			}
		}
	    
	    @Override
	    protected void onResume() {
	    	
	    	final Intent netzwerkIntent = new Intent(getApplicationContext(), ArduinoService.class);
			bindService(netzwerkIntent, arduinoConnection, Context.BIND_AUTO_CREATE);
			    
	        super.onResume();
	      
	    }
	    
	    @Override
	    public void onPause() {
	        super.onPause();
	    }

	    @Override
	    public void onDestroy() {
	    	unregisterReceiver(mUsbReceiver);
	        super.onDestroy();
	    }
	    
	    @Override
		public void onClick(View arg0) {
			switch (arg0.getId()) {
			case R.id.button_detail:
				//rsvpEventStatus("attending");
				
				String uri = "fb://page/" + eid;
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
				
				/*
				String facebookScheme = "fb://https://graph.facebook.com/" +"324439851003129";
				Intent facebookIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(facebookScheme)); 
				*/
				startActivity(intent);	
				
				
				
				break;
				
			
			
			}
		}
	    
	    public void rsvpEventStatus(String status) {
	    	/*
	    	Bundle params = new Bundle();
            params.putString("url",
                    "http://graph.facebook.com/"+eid+"/attending");
            
            Utility.mAsyncRunner.request("me/events", params,
                    "POST", null, null);
            
            */
            
	    }
	    
	    
	    
	    

} //end of class