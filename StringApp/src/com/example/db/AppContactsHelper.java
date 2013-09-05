package com.example.db;



import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class AppContactsHelper extends SQLiteOpenHelper {
	private static final int DATABASE_VERSION=1;
	 // Database Name
	private static final String DATABASE_NAME="AppContacts";
	// Table Name
	private static final String TABLE_NAME="MyContacts";
	// Contacts Id column
	private static final String CONTACT_ID="_id";
	// Contact Lookup Key 
	private static final String CONTACT_LOOKUP_KEY = "lookup";
	//Contacts Display Name Column
	private static final String DISPLAY_NAME = "name";
	//Contact Photo Uri Column 
	private static final String PHOTO_THUMBNAIL_URI = "photo_thumb_uri";
	// Contact phone number
	private static final String PHONE_NUMBER = "phoneNumber";

   
	public AppContactsHelper(Context context) {
       super(context, DATABASE_NAME, null, DATABASE_VERSION);
   }
   @Override
   //Creating Tables
   public void onCreate(SQLiteDatabase db) {
       String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_NAME + "("
       		+ CONTACT_ID + " INTEGER PRIMARY KEY," + CONTACT_LOOKUP_KEY + " TEXT," + DISPLAY_NAME + " TEXT," + PHOTO_THUMBNAIL_URI + " TEXT," + PHONE_NUMBER + " TEXT)";
       try{
       	db.execSQL(CREATE_CONTACTS_TABLE);
       }
       
       catch(SQLException e){
       	Log.v("AppContactsHelper", e.toString());
       }
       
   }
   //Upgrading Databases
   @Override
   public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
       // Drop older table if existed
       db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);

       // Create tables again
       onCreate(db);
   }
   
   //New registered contact added to database
   public void addContact(int Id, String lookupKey, String displayName, String photoUri, String phoneNumber){
   	SQLiteDatabase db = this.getWritableDatabase();
   	ContentValues values = new ContentValues();
   	values.put(CONTACT_ID, Id);
   	values.put(CONTACT_LOOKUP_KEY, lookupKey);
   	values.put(DISPLAY_NAME, displayName);
   	values.put(PHOTO_THUMBNAIL_URI, photoUri);
   	values.put(PHONE_NUMBER, phoneNumber);
   	//Inserting Rows
   	try{
   		db.insert(TABLE_NAME,null,values);
   		
       }
       
       catch(SQLException e){
       	Log.v("AppContactsHelper", e.toString());
       }
   	
   	
   }
   
   
 //Getting single song info
   public Cursor getAppContacts(){
   	SQLiteDatabase db = this.getReadableDatabase();
   	return db.query(false, TABLE_NAME, new String[] { CONTACT_ID, CONTACT_LOOKUP_KEY, DISPLAY_NAME, PHOTO_THUMBNAIL_URI, PHONE_NUMBER},
   							null, null,null,null,null,null);
   }
   
   
}
