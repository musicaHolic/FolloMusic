package com.example.stringapp;
import java.io.File;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.example.stringapp.service.StringAppService;
import com.google.android.gms.gcm.GoogleCloudMessaging;


public class MyBroadcastReceiver extends BroadcastReceiver{

	static final String TAG = "StringAppReceiver";
    public static final int NOTIFICATION_ID = 1;
    
    /** Key to retrieve message from GCM bundle
     * 
     */
    public static final String BUNDLE_MSG_KEY = "message";
    
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;
    Context ctx;
    
    private StringAppService mAppService = null;
    

	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
        ctx = context;
        String messageType = gcm.getMessageType(intent);
        if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
            sendNotification("Send error: " + intent.getExtras().toString());
        } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
            sendNotification("Deleted messages on server: " +
                    intent.getExtras().toString());
        } else {
            //sendNotification("Received: " + intent.getExtras().toString());
        	
        	//If message sent via GCMPushMessage script
        	if(intent.getExtras().getString(BUNDLE_MSG_KEY)!=null){
        		String[] musicInfo = intent.getExtras().getString(BUNDLE_MSG_KEY).split("/");
            	String displayInfo = "User " + musicInfo[0] + " Now Playing " + musicInfo[1] + " - " + musicInfo[2];
            	
            	//Log.v(TAG, displayInfo);
            	Toast.makeText(context.getApplicationContext(), displayInfo,
                        Toast.LENGTH_LONG).show();
            	
            	if(mAppService == null)
            		mAppService = StringAppService.get(context);
            	if(mAppService!=null){
            		mAppService.informListView(musicInfo[0], musicInfo[1], musicInfo[2]);
            	}
            	else
            		Log.v(TAG, "Service not running");
            	
        	}
        	
        	

        }
        setResultCode(Activity.RESULT_OK);


	}
	
	// Put the GCM message into a notification and post it.
    private void sendNotification(String msg) {
        mNotificationManager = (NotificationManager)
                ctx.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0,
                new Intent(ctx, MainActivity.class), 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(ctx)
        .setSmallIcon(R.drawable.ic_launcher)
        .setContentTitle("GCM Notification")
        .setStyle(new NotificationCompat.BigTextStyle()
        .bigText(msg))
        .setContentText(msg);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
    
    
    
    



}
