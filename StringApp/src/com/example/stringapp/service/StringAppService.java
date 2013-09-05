package com.example.stringapp.service;


import java.io.File;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.example.stringapp.R;
import com.example.stringapp.RegisterActivity.ContactsQuery;
import com.example.stringapp.utils.ServerUtils;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class StringAppService extends Service {
	
	private static final String TAG = "StringAppService";
	public static final String PROPERTY_USER_NUMBER = "userNumber";
	private static final String PREFS_FILE = "prefs";
	
	boolean mediaChangeReceiverRegistered = false;
	public static String userNumber;
	BroadcastReceiver mReceiver;
	
	AtomicInteger msgId = new AtomicInteger();
	private HashMap<String, Integer> positionMap = new HashMap<String, Integer>();
	
	

	JSONObject contactsList;
	
	/** Data for querying broadcasted song
	 * 
	 */
	public static final String[] PROJECTION = new String[] {MediaStore.Audio.Media.DATA};
    private String[] mFieldKeys = new String[] { MediaStore.Audio.Media.ARTIST_KEY, MediaStore.Audio.Media.TITLE_KEY };
    public static String[] SELECTION;
    
    Cursor musicCursor;
    
    private static StringAppService sInstance = null;
    
    public Handler mHandler;
    
    // Messenger instance of ContactListActvity
    Messenger mContactListMessenger;
    public Messenger mServiceMessenger;
    // Follow flag
    int listIndex;
    
    public int getListIndex() {
		return listIndex;
	}


	public void setListIndex(int listIndex) {
		this.listIndex = listIndex;
	}


	public static StringAppService get(Context context){
    	return sInstance;
    }


	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {

		super.onCreate();

		userNumber = getUserNumber();
		registerMediaChangeReceiver();
		
		//mHandler = new Handler(this);
		mServiceMessenger = new Messenger(new IncomingHandler());
		sInstance = this;
		

	}
		

	

	@SuppressWarnings("unchecked")
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Bundle extras = intent.getExtras();
		if(extras!=null)
			mContactListMessenger = intent.getParcelableExtra("Messenger");
		
		try {
			mContactListMessenger.send(Message.obtain(null, 1));
			//mContactListMessenger.send(Message.obtain(mHandler, 2, mServiceMessenger));
		} catch (RemoteException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
	
		return START_NOT_STICKY;

	}

	public void constructContactsBundle() {
		//Initialize service json instance
		contactsList = new JSONObject();
		
		JSONArray contacts = new JSONArray();
    	JSONObject json = null;
    	try{
    		contactsList.put("sender", userNumber);
    		
        	for(String contactNo : positionMap.keySet()){
        		json = new JSONObject();
        		// Filtering for numbers need not be done again
    			json.put("phoneNumber", contactNo);
    			contacts.put(json);		
        		
        	} 
        	
        	
        	contactsList.put("contacts", contacts);
        	
    	}
    	
    	catch (JSONException e) {
			Log.v(TAG, "JSON exception");
			
		}
		
	}

	@Override
	public void onDestroy() {

		this.unregisterReceiver(mReceiver);
		mediaChangeReceiverRegistered = false;
		super.onDestroy();

	}
	
	
	public void sendString(String musicInfo) {
        new AsyncTask<Object, Object, String>() {
            @Override
            protected String doInBackground(Object... params) {
                String msg = "";
                String musicInfo = (String) params[0];
                try {
                    Bundle data = new Bundle();
                    String id = Integer.toString(msgId.incrementAndGet());
                    //Store musicInfo in JSON object
                    contactsList.put("musicInfo", musicInfo);
                    ServerUtils.broadcastSongInfo(contactsList);
                    msg = "Sent message - " + musicInfo;
                } catch (Exception ex) {
                    msg = "Error :" + ex.getMessage();
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                	//mDisplay.append(msg + "\n");
            }
        }.execute(musicInfo, null, null);
    }
	
	private String getUserNumber(){
		final SharedPreferences prefs = getSharedPreferences(PREFS_FILE, 
	            Context.MODE_PRIVATE);
		return prefs.getString(PROPERTY_USER_NUMBER, "");
		
	}
	
	/**
	 * Update the list item with the song data
	 */
	public void informListView(String broadCasterNo, String artist, String track){
		String textViewInfo = artist + "/" + track;
	   	try {
	   		mContactListMessenger.send(Message.obtain(mHandler, 0, positionMap.get(broadCasterNo), 0, textViewInfo));
	   	} catch (RemoteException e1) {
	   		// TODO Auto-generated catch block
	   		e1.printStackTrace();
	   	}
	   	
	   	// Iterate through position map 
	   	// TODO - optimize
	   	if(listIndex > -1){
	   		for(String number : positionMap.keySet()){
		   		if(number.equals(broadCasterNo)&&(positionMap.get(number)==listIndex))
		   			playBroadcastedSong(getApplicationContext(), artist, track);
		   	}
	   	}
	   	
	}
	
	
	/**Fetches song if it exists in MediaStore DB and plays it via 
     * MediaPlayer
     */
    public void playBroadcastedSong(Context context, String artist, String track){
    	String songInfo = artist + " " + track;
    	   	
    	StringBuilder selection = new StringBuilder();
    	selection.append("is_music!=0");
    	StringBuilder keys = new StringBuilder(20);
		keys.append(mFieldKeys[0]);
		for (int j = 1; j != mFieldKeys.length; ++j) {
			keys.append("||");
			keys.append(mFieldKeys[j]);
		}
    	
    	String needles[];
    			
    	String colKey = MediaStore.Audio.keyFor(songInfo);
		String spaceColKey = DatabaseUtils.getCollationKey(" ");
		needles = colKey.split(spaceColKey);
		
		String[] selectionArgs = new String[needles.length];
		
		for (int j = 0; j != needles.length; ++j) {
			selectionArgs[j] = '%' + needles[j] + '%';

			// If we have something in the selection args (i.e. j > 0), we
			// must have something in the selection, so we can skip the more
			// costly direct check of the selection length.
			if (j != 0 || selection.length() != 0)
				selection.append(" AND ");
			selection.append(keys);
			selection.append(" LIKE ?");
			
		}
    	
    	musicCursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, 
    			PROJECTION, selection.toString(), selectionArgs, null);
    	
    	if(musicCursor.getCount()>0){
    		musicCursor.moveToNext();
    		String filePath = musicCursor.getString(musicCursor.getColumnIndex(MediaStore.Audio.Media.DATA));
    	
    		try {
    			
    			Intent intent = new Intent();  
                intent.setAction(android.content.Intent.ACTION_VIEW);  
                File file = new File(filePath);  
                intent.setDataAndType(Uri.fromFile(file), "audio/*");   
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
          } catch (Exception e) {
        	  	Log.v(TAG, "Error: Media Player could not be instantiated");
          }
    	}
    	
    	else{
    		Log.v(TAG, "Transmitted song not found in MediaStore");
    	}
    	musicCursor.close();
    }
	
	
	public class AppServiceBinder extends Binder {
	    public StringAppService getService() {
	      return StringAppService.this;
	    }
	  }
	
	
	/** Registers receivers for Mediaplayer state changes 
	 *  
	 */
	private void registerMediaChangeReceiver() {
		IntentFilter iF = new IntentFilter();
		 
        // Read action when music player changed current song
        // I just try it with stock music player from android
 
        // stock music player
        iF.addAction("com.android.music.metachanged");
        //iF.addAction("com.android.music.playbackcomplete");
        //iF.addAction("com.android.music.playstatechanged");
        // MIUI music player
        iF.addAction("com.miui.player.metachanged");
 
        // HTC music player
        iF.addAction("com.htc.music.metachanged");
 
        // WinAmp
        iF.addAction("com.nullsoft.winamp.metachanged");
 
        // MyTouch4G
        iF.addAction("com.real.IMP.metachanged");
        
        mReceiver = new BroadcastReceiver() {
   		 
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
 
        registerReceiver(mReceiver, iF);
        mediaChangeReceiverRegistered = true;
		
	}

	class IncomingHandler extends Handler {
		private static final int MSG_UPDATE_CONTACTS_MAP = 1;

		@Override
		public void handleMessage(Message message) {
			switch (message.what) {
	  		case MSG_UPDATE_CONTACTS_MAP:
	  			setPositionMap((HashMap<String, Integer>) message.obj);
	  			constructContactsBundle();
	  			break;
			default:
				super.handleMessage(message);
			}
			
			
			
		}
	}
	
	public void updateContactsMap(HashMap<String, Integer> positionMap){
		setPositionMap(positionMap);
		constructContactsBundle();
	}
	
	
	
	private void setPositionMap(HashMap<String, Integer> positionMap) {
		this.positionMap = positionMap;
	}

}
