package com.example.provider;

import android.net.Uri;
import android.provider.BaseColumns;

public class AppContactsMetaData {
	
	private AppContactsMetaData(){
		
	}
	
	public static final String AUTHORITY = "com.android.provider.AppContacts";
    public static final Uri CONTENT_URI = Uri.parse(
        "content://" + AUTHORITY + "/contacts"
    );
 
    public static final String DATABASE_NAME = "AppContacts";
    public static final int DATABASE_VERSION = 1;
 
    public static final String CONTENT_TYPE_CONTACTS_LIST = "vnd.android.cursor.dir/vnd.stringapp.contacts";
    public static final String CONTENT_TYPE_CONTACTS_ONE = "vnd.android.cursor.item/vnd.stringapp.contacts";
 
    public class ArticleTable implements BaseColumns {
        private ArticleTable() { }
 
        public static final String TABLE_NAME = "MyContacts";
 
        private static final String CONTACT_ID="_id";
    	//Contacts Display Name Column
    	private static final String DISPLAY_NAME = "name";
    	//Contact Photo Uri Column 
    	private static final String PHOTO_THUMBNAIL_URI = "photo_thumb_uri";
    }

}
