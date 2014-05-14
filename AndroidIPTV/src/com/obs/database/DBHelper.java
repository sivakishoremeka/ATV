package com.obs.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

	// Logcat tag
	private static final String LOG = "DatabaseHelper";

	// Database Version
	public static final int DATABASE_VERSION = 3;

	// Database Name
	public static final String DATABASE_NAME = "obsdatabase.db";

	// Table Names
	public static final String TABLE_SERVICES = "services";
	

	// Common column names
	public static final String SERVICE_KEY_ID = "_id";
	public static final String SERVICE_KEY_SERVICE_ID = "service_id";
	public static final String SERVICE_KEY_CLIENT_ID = "client_id";
	public static final String SERVICE_KEY_CHANNEL_NAME = "channel_name";
	public static final String SERVICE_KEY_CHANNEL_DESC = "channel_desc";
	public static final String SERVICE_KEY_IMAGE = "image";
	public static final String SERVICE_KEY_URL = "url";
	public static final String SERVICE_KEY_FAVOURITE = "is_favourite";
		
	
	// Table Create Statements
	private static final String CREATE_TABLE_SERVICES = "CREATE TABLE "
			+ TABLE_SERVICES + "(" + SERVICE_KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," 
								   + SERVICE_KEY_SERVICE_ID	+ " INTEGER," 
			                       + SERVICE_KEY_CLIENT_ID + " INTEGER," 
			                       + SERVICE_KEY_CHANNEL_NAME + " TEXT,"
			                       + SERVICE_KEY_CHANNEL_DESC + " TEXT,"
								   + SERVICE_KEY_IMAGE + " TEXT,"
			                       + SERVICE_KEY_URL + " TEXT,"
								   + SERVICE_KEY_FAVOURITE + " NUMERIC DEFAULT 0," 
			                       + "UNIQUE("+SERVICE_KEY_SERVICE_ID+") ON CONFLICT REPLACE"+")";


	public DBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {

		// creating required tables
		db.execSQL(CREATE_TABLE_SERVICES);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// on upgrade drop older tables
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_SERVICES);

		// create new tables
		onCreate(db);
	}
}