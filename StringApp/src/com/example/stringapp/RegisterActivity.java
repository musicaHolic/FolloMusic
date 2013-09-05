package com.example.stringapp;


import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.example.contactslist.ui.ContactsListActivity;
import com.example.db.AppContactsHelper;
import com.example.stringapp.service.StringAppService;
import com.example.stringapp.service.StringAppService.AppServiceBinder;
import com.example.stringapp.utils.ServerUtils;
import com.example.utils.Utils;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class RegisterActivity extends Activity {
	
	Context context;
	
	private static final String USER_NUMBER = "userNumber";
	private static final String TAG = "RegisterActivity";
	private static final String PREFS_FILE = "prefs";
	
	
	public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    public static final String PROPERTY_USER_NUMBER = "userNumber";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final String PROPERTY_ON_SERVER_EXPIRATION_TIME = "onServerExpirationTimeMs";

    public static final long REGISTRATION_EXPIRY_TIME_MS = 1000 * 3600 * 24 * 7;
    
    public static final String SENDER_ID = "608999186433";
    
    private String registeredUserNumber;
    
    GoogleCloudMessaging gcm;
    String regid;
    
    TextView mDisplay;
    EditText nInput;
    
    AppContactsHelper dbHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register);
		
		context = getApplicationContext();
		
		if(isUserDataSet(context)){
			if(isRegistrationExpired())
				registerBackGround();
			Intent i = new Intent(context, HomeActivity.class);																
			startActivity(i);
			finish();
		}
		else{
			mDisplay = (TextView) findViewById(R.id.textView1);
			nInput = (EditText) findViewById(R.id.editText1);
		}
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.register, menu);
		return true;
	}
	
	
	private boolean isUserDataSet(Context context) {
	    final SharedPreferences prefs = getSharedPreferences(PREFS_FILE, 
	            Context.MODE_PRIVATE);
	    String userNumber = prefs.getString(USER_NUMBER, "");
	    if (userNumber.length() == 0) {
	        Log.v(TAG, "UserNumber not found.");
	        return false;
	    }
	    // check if app was updated; if so, it must clear registration id to
	    // avoid a race condition if GCM sends a message
	    return true;
	}
	
	
	public void onClickRegister(final View view) {
	    if (view == findViewById(R.id.register))
	    		registerBackGround();
	}
	    
	
	public void registerBackGround(){
        new AsyncTask<Object, Object, String>() {
        
        private ProgressDialog dialog = 
                    new ProgressDialog(RegisterActivity.this);
        @Override
            protected void onPreExecute() {
                // TODO i18n
                dialog.setMessage("Please wait while fetching contacts");
                dialog.show();
            }
        	
        @Override
        protected String doInBackground(Object... params) {
            String msg = "";
            try {
                if (gcm == null) {
                    gcm = GoogleCloudMessaging.getInstance(context);
                }
                regid = gcm.register(SENDER_ID);
                msg = "Device registered"; 
           

                // Save the regid - no need to register again.
                // On first run, get list of contacts having app installed
                if(regid.length() > 0){
                	ServerUtils utils = new ServerUtils();
                	utils.registerUser(nInput.getText().toString(), regid);
                	setRegistrationId(context, regid, nInput.getText().toString());
                }
                	
                
            } catch (IOException ex) {
                msg = "Error :" + ex.getMessage();
            }
            return msg;
        }

        @Override
        protected void onPostExecute(String msg) {
        	if (dialog.isShowing()) {
                dialog.dismiss();
            }
        	if(msg.contains("Error"))
    			mDisplay.setText((CharSequence) msg);
    		else{
    			Intent i = new Intent(context, HomeActivity.class);																
    			startActivity(i);
    			finish();
        	}
        	
        		
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
		private void setRegistrationId(Context context, String regId, String userNumber) {
		    final SharedPreferences prefs = getSharedPreferences(PREFS_FILE, 
		            Context.MODE_PRIVATE);
		    int appVersion = getAppVersion(context);
		    Log.v(TAG, "Saving regId on app version " + appVersion);
		    SharedPreferences.Editor editor = prefs.edit();
		    editor.putString(PROPERTY_REG_ID, regId);
		    //Only update userNumber during first run
		    if(userNumber!=null){
		    	editor.putString(PROPERTY_USER_NUMBER, userNumber);
		    	registeredUserNumber = userNumber;
		    	getRegisteredContacts();
		    	
		    }
		    	
		    editor.putInt(PROPERTY_APP_VERSION, appVersion);
		    long expirationTime = System.currentTimeMillis() + REGISTRATION_EXPIRY_TIME_MS;

		    Log.v(TAG, "Setting registration expiry time to " +
		            new Timestamp(expirationTime));
		    editor.putLong(PROPERTY_ON_SERVER_EXPIRATION_TIME, expirationTime);
		    editor.commit();
		}
		
		private boolean isRegistrationExpired() {
		    final SharedPreferences prefs = getSharedPreferences(PREFS_FILE, 
		            Context.MODE_PRIVATE);
		    // checks if the information is not stale
		    long expirationTime =
		            prefs.getLong(PROPERTY_ON_SERVER_EXPIRATION_TIME, -1);
		    return System.currentTimeMillis() > expirationTime;
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
	
		/**Filter out phone contacts which have application installed and save these to custom database
		 * 
		 */
		private void getRegisteredContacts(){
			
            Cursor androidContacts = getContentResolver().query(ContactsQuery.CONTENT_URI, 
											            		ContactsQuery.PROJECTION, 
											            		ContactsQuery.SELECTION, 
											            		null, 
											            		null);
            
            if(androidContacts.getCount()>0){
            	JSONArray contacts = new JSONArray();
            	JSONObject json = null;
            	JSONObject response = null;
            	String filterNum = null;
            	try{
	            	do{
	            		androidContacts.moveToNext();
	            		json = new JSONObject();
	            		//Filter all special characters from phone number, use only last 10 digits
	            		filterNum = androidContacts.getString(ContactsQuery.PHONE_NUMBER).replaceAll("[^0-9]+", "");
	            		if(filterNum.length()>10)
	            			filterNum = filterNum.substring(filterNum.length()-10, filterNum.length());
	            		// Skip adding own number
	            		if(filterNum.equals(registeredUserNumber))
	                      	 json.put("phoneNumber", "0");
	                    else
	                      	 json.put("phoneNumber", filterNum);
            			json.put("status", 0);
            			contacts.put(json);		
	            		
	            	} while (!androidContacts.isLast());
	            	
	            	json = new JSONObject();
	            	json.put("contacts", contacts);
            	}
            	
            	catch (JSONException e) {
					Log.v(TAG, "JSON exception");
					
				}

            	// POST to server and fetch registered contacts
            	response = ServerUtils.fetchContacts(json);
            	if(response!=null){
            		try { 
            			contacts = response.getJSONArray("contacts");
            			
            			
            			if(dbHandler==null)
            				dbHandler = new AppContactsHelper(context);
            	        for (int i = 0; i < contacts.length(); i++) {
            	            JSONObject obj = contacts.getJSONObject(i);
            	            // Server sets status flag as 1, implies save the contact
            	            if(obj.getInt("status")==1){
            	            	androidContacts.moveToPosition(i);
            	            	dbHandler.addContact(androidContacts.getInt(ContactsQuery.ID),
            	            						androidContacts.getString(ContactsQuery.LOOKUP_KEY),
            	            						androidContacts.getString(ContactsQuery.DISPLAY_NAME), 
            	            						androidContacts.getString(ContactsQuery.PHOTO_THUMBNAIL_DATA),
            	            						obj.getString("phoneNumber"));
            	            }
            	        }
            	        
            	         
            	    } catch (JSONException e) {
            	        // handle exception
            	    }
            	}
            	
            	
            	
            	else{
            		mDisplay.setText("Error fetching contacts from server");
            	}
            	
            	//Close cursor 
            	androidContacts.close();
                dbHandler.close();
            }
            
            
            
		}
		
		
		/**
	     * This interface defines constants for the Cursor and CursorLoader, based on constants defined
	     * in the {@link android.provider.ContactsContract.Contacts} class.
	     */
	    public interface ContactsQuery {

	        // An identifier for the loader
	        final static int QUERY_ID = 1;

	        // A content URI for the Contacts table
	        final static Uri CONTENT_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;

	        // The search/filter query Uri
	        final static Uri FILTER_URI = Contacts.CONTENT_FILTER_URI;

	        // The selection clause for the CursorLoader query. The search criteria defined here
	        // restrict results to contacts that have a display name and are linked to visible groups.
	        // Notice that the search on the string provided by the user is implemented by appending
	        // the search string to CONTENT_FILTER_URI.
	        @SuppressLint("InlinedApi")
	        final static String SELECTION =
	                (Utils.hasHoneycomb() ? Contacts.DISPLAY_NAME_PRIMARY : Contacts.DISPLAY_NAME) +
	                "<>''" + " AND " + Contacts.IN_VISIBLE_GROUP + "=1 AND " + Contacts.HAS_PHONE_NUMBER + "=1";

	        // The desired sort order for the returned Cursor. In Android 3.0 and later, the primary
	        // sort key allows for localization. In earlier versions. use the display name as the sort
	        // key.
	        @SuppressLint("InlinedApi")
	        final static String SORT_ORDER =
	                Utils.hasHoneycomb() ? Contacts.DISPLAY_NAME_PRIMARY : Contacts.DISPLAY_NAME;

	        // The projection for the CursorLoader query. This is a list of columns that the Contacts
	        // Provider should return in the Cursor.
	        @SuppressLint("InlinedApi")
	        final static String[] PROJECTION = {

	                // The contact's row id
	        		Contacts._ID,

	                // A pointer to the contact that is guaranteed to be more permanent than _ID. Given
	                // a contact's current _ID value and LOOKUP_KEY, the Contacts Provider can generate
	                // a "permanent" contact URI.
	        		Contacts.LOOKUP_KEY,

	                // In platform version 3.0 and later, the Contacts table contains
	                // DISPLAY_NAME_PRIMARY, which either contains the contact's displayable name or
	                // some other useful identifier such as an email address. This column isn't
	                // available in earlier versions of Android, so you must use Contacts.DISPLAY_NAME
	                // instead.
	                Utils.hasHoneycomb() ? Contacts.DISPLAY_NAME_PRIMARY : Contacts.DISPLAY_NAME,

	                ContactsContract.CommonDataKinds.Phone.NUMBER,

	                Utils.hasHoneycomb() ? Contacts.PHOTO_THUMBNAIL_URI : Contacts._ID,
	                // The sort order column for the returned Cursor, used by the AlphabetIndexer
	                //SORT_ORDER,
	        };

	        // The query column numbers which map to each value in the projection
	        final static int ID = 0;
	        final static int LOOKUP_KEY = 1;
	        final static int DISPLAY_NAME = 2;
	        final static int PHONE_NUMBER = 3;
	        final static int PHOTO_THUMBNAIL_DATA = 4;
	        //final static int SORT_KEY = 4;
	    }
	
	

}
