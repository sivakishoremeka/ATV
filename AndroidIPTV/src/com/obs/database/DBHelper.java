package com.obs.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

	// Logcat tag
	private static final String LOG = "DatabaseHelper";

	// Database Version
	public static final int DATABASE_VERSION = 4;

	// Database Name
	public static final String DATABASE_NAME = "obsdatabase.db";

	// Table Names
	public static final String TABLE_SERVICES = "services";
	public static final String TABLE_SERVICE_CATEGORIES = "service_categories";
	public static final String TABLE_SERVICE_SUB_CATEGORIES = "service_sub_categories";
	
	// Common column names
	public static final String KEY_ID = "_id";
	public static final String CATEGORY = "category";
	public static final String SUB_CATEGORY = "sub_category";
	
	//Service column names
	public static final String SERVICE_ID = "service_id";
	public static final String CLIENT_ID = "client_id";
	public static final String CHANNEL_NAME = "channel_name";
	public static final String CHANNEL_DESC = "channel_desc";
	public static final String IMAGE = "image";
	public static final String URL = "url";
	public static final String IS_FAVOURITE = "is_favourite";
		
	
	// Table Services Create Statements
	private static final String CREATE_TABLE_SERVICES = "CREATE TABLE "
			+ TABLE_SERVICES + "(" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," 
								   + SERVICE_ID	+ " INTEGER," 
			                       + CLIENT_ID + " INTEGER," 
			                       + CHANNEL_NAME + " TEXT,"
			                       + CHANNEL_DESC + " TEXT,"
			                       + CATEGORY + " TEXT,"
			                       + SUB_CATEGORY + " TEXT," 
								   + IMAGE + " TEXT,"
			                       + URL + " TEXT,"
								   + IS_FAVOURITE + " NUMERIC DEFAULT 0," 
			                       + "UNIQUE("+SERVICE_ID+") ON CONFLICT REPLACE"+")";

	// Table Categories Create Statements
		private static final String CREATE_TABLE_SERVICE_CATEGORIES = "CREATE TABLE "
				+ TABLE_SERVICE_CATEGORIES + "(" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," 
				                       + CATEGORY + " TEXT,"
				                       + "UNIQUE("+CATEGORY+") ON CONFLICT REPLACE"+")";
		// Table Services Create Statements
		private static final String CREATE_TABLE_SERVICE_SUB_CATEGORIES = "CREATE TABLE "
				+ TABLE_SERVICE_SUB_CATEGORIES + "(" + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," 
				                       + SUB_CATEGORY + " TEXT," 
				                       + "UNIQUE("+SUB_CATEGORY+") ON CONFLICT REPLACE"+")";

	public DBHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		// creating required tables
		db.execSQL(CREATE_TABLE_SERVICES);
		db.execSQL(CREATE_TABLE_SERVICE_CATEGORIES);
		db.execSQL(CREATE_TABLE_SERVICE_SUB_CATEGORIES);	
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// on upgrade drop older tables
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_SERVICES);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_SERVICE_CATEGORIES);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_SERVICE_SUB_CATEGORIES);
		// create new tables
		onCreate(db);
	}
}