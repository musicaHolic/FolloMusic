package com.example.stringapp;

import com.example.contactslist.ui.ContactsListActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class HomeActivity extends Activity {

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.activity_home);
	    // TODO Auto-generated method stub
	}
	
	public void onClickFriends(final View view)
	{
		
		Log.v("Magya", "BANDA");
		Intent i = new Intent(this, ContactsListActivity.class);																
		startActivity(i);
		
	}
}
