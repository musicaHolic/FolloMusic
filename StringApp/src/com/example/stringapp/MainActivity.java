package com.example.stringapp;

import java.io.IOException;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import com.example.contactslist.ui.ContactsListActivity;
import com.example.stringapp.service.StringAppService;
import com.example.stringapp.utils.ServerUtils;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private static final String PREFS_FILE = "prefs";
	
	public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    public static final String PROPERTY_USER_NUMBER = "userNumber";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final String PROPERTY_ON_SERVER_EXPIRATION_TIME = "onServerExpirationTimeMs";

    public static final long REGISTRATION_EXPIRY_TIME_MS = 1000 * 3600 * 24 * 7;
    
    String SENDER_ID = "608999186433";
    
    static final String TAG = "StringAppTag";
    
    GoogleCloudMessaging gcm;
    SharedPreferences prefs;
    TelephonyManager tm;
    
    Context context;

    String regid;
    String userNumber;
    
    TextView mDisplay;
    EditText destinationNo;
    
    AtomicInteger msgId = new AtomicInteger();
    


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main2);
		
		mDisplay = (TextView) findViewById(R.id.display);
		destinationNo = (EditText) findViewById(R.id.editText1);

		
		context = getApplicationContext();
		userNumber = getUserNumber(context);
        gcm = GoogleCloudMessaging.getInstance(this);
        
		IntentFilter iF = new IntentFilter();
		 
        // Read action when music player changed current song
        // I just try it with stock music player from android
 
        // stock music player
        iF.addAction("com.android.music.metachanged");
 
        // MIUI music player
        iF.addAction("com.miui.player.metachanged");
 
        // HTC music player
        iF.addAction("com.htc.music.metachanged");
 
        // WinAmp
        iF.addAction("com.nullsoft.winamp.metachanged");
 
        // MyTouch4G
        iF.addAction("com.real.IMP.metachanged");
 
        registerReceiver(mReceiver, iF);
	}

	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	
	BroadcastReceiver mReceiver = new BroadcastReceiver() {
		 
        @Override
        public void onReceive(Context arg0, Intent intent) {
 
            String action = intent.getAction();
            String cmd = intent.getStringExtra("command");
            //Log.d("mIntentReceiver.onReceive ", action + " / " + cmd);
            String artist = intent.getStringExtra("artist");
            String album = intent.getStringExtra("album");
            String track = intent.getStringExtra("track");
            String msg = userNumber + "/" + artist + "/" + track;
            //Log.d("Music", artist + ": " + album + ": " + track);
            Log.d("Music", msg);
            if(!msg.contains("unknown")){
            	sendString(msg);
            }
            
            //Toast.makeText(MainActivity.this, "Now Playing track:" + track + " artist:" + artist,
            //        Toast.LENGTH_SHORT).show();
            
            
        }
	};
	
	
	
	/**
	 * Gets the current registration id for application on GCM service.
	 * If result is empty, the registration has failed.
	 *
	 * @return registration id, or empty string if the registration is not
	 *         complete.
	 */
	private String getRegistrationId(Context context) {
	    final SharedPreferences prefs = getGCMPreferences(context);
	    String registrationId = prefs.getString(PROPERTY_REG_ID, "");
	    if (registrationId.length() == 0) {
	        Log.v(TAG, "Registration not found.");
	        return "";
	    }
	    // check if app was updated; if so, it must clear registration id to
	    // avoid a race condition if GCM sends a message
	    int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
	    int currentVersion = getAppVersion(context);
	    if (registeredVersion != currentVersion || isRegistrationExpired()) {
	        Log.v(TAG, "App version changed or registration expired.");
	        return "";
	    }
	    return registrationId;
	}
	
	private String getUserNumber(Context context){
		final SharedPreferences prefs = getSharedPreferences(PREFS_FILE, 
	            Context.MODE_PRIVATE);
		String registrationId = prefs.getString(PROPERTY_USER_NUMBER, "");
		return registrationId;
	}
	
	
	/**
	 * @return Application's {@code SharedPreferences}.
	 */
	private SharedPreferences getGCMPreferences(Context context) {
	    return getSharedPreferences(MainActivity.class.getSimpleName(), 
	            Context.MODE_PRIVATE);
	}
	
	
	
	/**
	 * @return Application's version code from the {@code PackageManager}.
	 */
	private static int getAppVersion(Context context) {
	    try {
	        PackageInfo packageInfo = context.getPackageManager()
	                .getPackageInfo(context.getPackageName(), 0);
	        return packageInfo.versionCode;
	    } catch (NameNotFoundException e) {
	        // should never happen
	        throw new RuntimeException("Could not get package name: " + e);
	    }
	}

	/**
	 * Checks if the registration has expired.
	 *
	 * <p>To avoid the scenario where the device sends the registration to the
	 * server but the server loses it, the app developer may choose to re-register
	 * after REGISTRATION_EXPIRY_TIME_MS.
	 *
	 * @return true if the registration has expired.
	 */
	private boolean isRegistrationExpired() {
	    final SharedPreferences prefs = getGCMPreferences(context);
	    // checks if the information is not stale
	    long expirationTime =
	            prefs.getLong(PROPERTY_ON_SERVER_EXPIRATION_TIME, -1);
	    return System.currentTimeMillis() > expirationTime;
	}
	
	
	
	
	
	/**
	 * Registers the application with GCM servers asynchronously.
	 * <p>
	 * Stores the registration id, app versionCode, and expiration time in the 
	 * application's shared preferences.
	 */
	private void registerBackground() {
	    new AsyncTask() {
	        @Override
	        protected String doInBackground(Object... params) {
	            String msg = "";
	            try {
	                if (gcm == null) {
	                    gcm = GoogleCloudMessaging.getInstance(context);
	                }
	                regid = gcm.register(SENDER_ID);
	                msg = "Device registered"; 
	                //, registration id=" + regid;

	                // You should send the registration ID to your server over HTTP,
	                // so it can use GCM/HTTP or CCS to send messages to your app.

	                // For this demo: we don't need to send it because the device
	                // will send upstream messages to a server that echo back the message
	                // using the 'from' address in the message.

	                // Save the regid - no need to register again.
	                if(regid.length() > 0){
	                	ServerUtils utils = new ServerUtils();
	                	utils.registerUser(tm.getSubscriberId(), regid);
	                }
	                	
	                setRegistrationId(context, regid);
	            } catch (IOException ex) {
	                msg = "Error :" + ex.getMessage();
	            }
	            return msg;
	        }

	        @Override
	        protected void onPostExecute(Object msg) {
	        	if(msg instanceof String)
	        		mDisplay.append(msg + "\n");
	        }
	        
	    }.execute(null, null, null);
	}
	
	
	/**
	 * Stores the registration id, app versionCode, and expiration time in the
	 * application's {@code SharedPreferences}.
	 *
	 * @param context application's context.
	 * @param regId registration id
	 */
	private void setRegistrationId(Context context, String regId) {
	    final SharedPreferences prefs = getGCMPreferences(context);
	    int appVersion = getAppVersion(context);
	    Log.v(TAG, "Saving regId on app version " + appVersion);
	    SharedPreferences.Editor editor = prefs.edit();
	    editor.putString(PROPERTY_REG_ID, regId);
	    editor.putInt(PROPERTY_APP_VERSION, appVersion);
	    long expirationTime = System.currentTimeMillis() + REGISTRATION_EXPIRY_TIME_MS;

	    Log.v(TAG, "Setting registration expiry time to " +
	            new Timestamp(expirationTime));
	    editor.putLong(PROPERTY_ON_SERVER_EXPIRATION_TIME, expirationTime);
	    editor.commit();
	}
	
	
	
	public void onClick(final View view) {
	    if (view == findViewById(R.id.send)) {
	    	Intent i = new Intent(this, ContactsListActivity.class);
	    	startActivity(i);
	        /*new AsyncTask() {
	            @Override
	            protected String doInBackground(Object... params) {
	                String msg = "";
	                try {
	                    Bundle data = new Bundle();
	                    data.putString("hello", "World");
	                    String id = Integer.toString(msgId.incrementAndGet());
	                    //gcm.send(SENDER_ID + "@gcm.googleapis.com", id, data);
	                    ServerUtils utils = new ServerUtils();
	                    utils.sendMessage(userNumber, destinationNo.getText().toString(), "Check123");
	                    msg = "Sent message";
	                } catch (Exception ex) {
	                    msg = "Error :" + ex.getMessage();
	                }
	                return msg;
	            }

	            @Override
	            protected void onPostExecute(Object msg) {
	                if(msg instanceof String)
	                	mDisplay.append(msg + "\n");
	            }
	        }.execute(null, null, null);*/
	    } else if (view == findViewById(R.id.clear)) {
	        mDisplay.setText("");
	    } 
	}
	
	
	public void sendString(String musicInfo) {
	        new AsyncTask() {
	            @Override
	            protected String doInBackground(Object... params) {
	                String msg = "";
	                String musicInfo = (String) params[0];
	                try {
	                    Bundle data = new Bundle();
	                    data.putString("hello", "World");
	                    String id = Integer.toString(msgId.incrementAndGet());
	                    //gcm.send(SENDER_ID + "@gcm.googleapis.com", id, data);
	                    ServerUtils utils = new ServerUtils();
	                    utils.sendMessage(userNumber, destinationNo.getText().toString(), musicInfo);
	                    msg = "Sent message - " + musicInfo;
	                } catch (Exception ex) {
	                    msg = "Error :" + ex.getMessage();
	                }
	                return msg;
	            }

	            @Override
	            protected void onPostExecute(Object msg) {
	                if(msg instanceof String)
	                	mDisplay.append(msg + "\n");
	            }
	        }.execute(musicInfo, null, null);
	    } 
	

}
