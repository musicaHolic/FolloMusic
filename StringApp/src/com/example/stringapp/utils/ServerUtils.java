package com.example.stringapp.utils;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.Pair;

public class ServerUtils {
	
	private static final String PREFS_FILE = "prefs";
	private static final int TIMEOUT_MILLISEC = 0;
	
	private static  final String SERVER_URL = "http://server.xclaimation11.tk/";
	private static final String REGISTRATION_SCRIPT = "UserReg.php";
	private static final String MESSAGING_SCRIPT = "SendMessage.php";
	private static final String BROADCASTING_SCRIPT = "BroadCastMessage.php";
	private static final String CONTACTS_FETCH_SCRIPT = "ContactsQuery.php";
	
	private static final String TAG = "ServerUtilities";
	
	//Request Param keys
	private static final String REGISTRATION_ID = "regId";
	private static final String USER_NUMBER = "phoneNumber";
	private static final String MESSAGE = "msg";
	private static final String FRIEND_NUMBER = "friendNo";
	
	
	//Request Param values;
	public String pRegId = null;
	public String pUserNumber = null;
	public String pMsg = null;
	public String pFriendNo = null;
	
	
	private  List<Pair<String, String>> paramsPair;
	
	
	public void registerUser(String number, String regId){
		pRegId = regId;
		pUserNumber = number;
		fillAlphaPairList();
        doGet(REGISTRATION_SCRIPT, getConstructedRequestString());
        resetParams();
	}
	
	public  void sendMessage(String sender, String destination, String msg){
		pMsg = msg;
		pFriendNo = destination;
		pUserNumber = sender;
		fillAlphaPairList();
        doGet(MESSAGING_SCRIPT, getConstructedRequestString());
        resetParams();
	}
	
	public  void fillAlphaPairList() {
		//Reinitialize params Pair
		this.paramsPair = new ArrayList<Pair<String,String>>();
				
		if (pRegId != null) {
			paramsPair.add(Pair.create(REGISTRATION_ID, pRegId));
		}
		
		if (pUserNumber != null) {
			paramsPair.add(Pair.create(USER_NUMBER, pUserNumber));
		}
		
		if (pMsg != null) {
			paramsPair.add(Pair.create(MESSAGE, pMsg));
		}
		
		if (pFriendNo != null) {
			paramsPair.add(Pair.create(FRIEND_NUMBER, pFriendNo));
		}
		
		
	}
	
	public void resetParams(){
		pRegId = pUserNumber = pMsg = pFriendNo = null;
	}
	
	public  void doGet(String target, String request){
		// Create URL string
		
		
		HttpClient Client = new DefaultHttpClient();
		
		try
        {
	        String URL = SERVER_URL + target + "?" + request;
            String SetServerString = "";
            // Create Request to server and get response
    
            HttpGet httpget = new HttpGet(URL);
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            SetServerString = Client.execute(httpget, responseHandler);
            // Show response on activity 
            Log.v(TAG, "Server response: " + SetServerString);
	
	         
        }
		
		catch(Exception e){
			Log.v(TAG, "Get Request exception: " + e.toString());
		}
	}
	
	public String getConstructedRequestString(){
		StringBuilder bodyBuilder = new StringBuilder();
		
		
    	//Starting bracket
    	Iterator<Pair<String, String>> iterator = paramsPair.iterator();
        // constructs the POST body using the parameters
        while (iterator.hasNext()) {
        	Pair<String, String> keyValue = iterator.next();
            bodyBuilder.append(keyValue.first).append('=')
                    .append(encodeValue(keyValue.second));
            if (iterator.hasNext()) {
                bodyBuilder.append('&');
            }
        }
        
        return bodyBuilder.toString();
	}
	
	
	public static void broadcastSongInfo(JSONObject json){
		doPOSTJSON(json, BROADCASTING_SCRIPT);
	}
	
	public static JSONObject fetchContacts(JSONObject json){
		return doPOSTJSON(json, CONTACTS_FETCH_SCRIPT);
	}
	
	
	/** Method to send JSON object to server via POST request
	 * @param json JSON object to send to server
	 * @return the JSON object response of server
	 */
	public static JSONObject doPOSTJSON(JSONObject json, String target){
		
		HttpParams httpParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpParams,
                TIMEOUT_MILLISEC);
        HttpConnectionParams.setSoTimeout(httpParams, TIMEOUT_MILLISEC);
        HttpClient client = new DefaultHttpClient(httpParams);
        
        String url = SERVER_URL + target;

        HttpPost request = new HttpPost(url);
        try {
			request.setEntity(new ByteArrayEntity(json.toString().getBytes(
			        "UTF8")));
		
			request.setHeader( "Content-Type", "application/json" );
	        ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String responseBody = client.execute(request,
                    responseHandler);
            // Parse
            JSONObject jsonResponse = new JSONObject(responseBody);
            return jsonResponse;
	        
        } catch (Exception e) {
        	Log.v(TAG, "Server error: " + e.toString());
        	return null;
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
     
	}

	private String encodeValue(String value) {
		try {
			return URLEncoder.encode(value, "UTF-8");
		} 
		
		catch (UnsupportedEncodingException e) {
			return null;
		}
	}
	

}
