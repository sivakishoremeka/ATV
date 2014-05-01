package com.obs.database;

import java.util.Calendar;
import java.util.Date;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.obs.androidiptv.MyApplication;
import com.obs.androidiptv.R;

public class ServiceProvider extends ContentProvider {

	public static final String AUTHORITY = "com.obs.database.ServiceProvider";

	public static final Uri SERVICES_URI = Uri.parse("content://" + AUTHORITY
			+ "/services");

	public static final Uri SERVICES_ONREFREFRESH_URI = Uri.parse("content://"
			+ AUTHORITY + "/servicesonrefresh");

	public static final int SERVICES = 1;
	public static final int SERVICES_ON_REFRESH = 2;

	// Defines a set of uris allowed with this content provider
	private static final UriMatcher mUriMatcher = buildUriMatcher();
	public MyApplication mApplication;
	DBHelper dbHelper;
	SQLiteDatabase db;

	private static UriMatcher buildUriMatcher() {
		UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		// URI for all services
		uriMatcher.addURI(AUTHORITY, "services", SERVICES);
		// URI for all services
		uriMatcher.addURI(AUTHORITY, "servicesonrefresh", SERVICES_ON_REFRESH);
		return uriMatcher;
	}

	@Override
	public boolean onCreate() {
		this.mApplication = (MyApplication) this.getContext()
				.getApplicationContext();
		this.dbHelper = new DBHelper(getContext());
		return false;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		Cursor cursor = null;
		switch (mUriMatcher.match(uri)) {
		case SERVICES:
			db = dbHelper.getReadableDatabase();
			// Table name
			// null gives all columns
			// selection & selectionArgs for where statements
			// groupby having nulls not using so
			// db.query retruns cursor. cursor is an iterator to move over the
			// data.
			boolean mIsLiveDataReq = false;
			// freshness of data (1 day validity)
			String sDate = "";
			try {

				sDate = (String) mApplication.getPrefs().getString(
						mApplication.getResources().getString(
								R.string.channels_updated_at), "");
				if (sDate.length() != 0) {
					Calendar c = Calendar.getInstance();
					String currDate = mApplication.df.format(c.getTime());
					Date d1 = null, d2 = null;

					d1 = mApplication.df.parse(sDate);
					d2 = mApplication.df.parse(currDate);

					if (d1.compareTo(d2) == 0) {
						mIsLiveDataReq = false;
					} else {
						mIsLiveDataReq = true;
					}
				} else {
					mIsLiveDataReq = true;
				}
			} catch (java.text.ParseException e) {
				e.printStackTrace();
				Log.e("JsonException", e.getMessage());
			}
			// freshness of data (1 day validity)

			if (mIsLiveDataReq) {
				mApplication.PullnInsertServices();
			}
			db = dbHelper.getReadableDatabase();
			cursor = db.query(DBHelper.TABLE_SERVICES, projection, selection,
					selectionArgs, null, null, sortOrder);
			return cursor;

		case SERVICES_ON_REFRESH:
			// refresh service table
			mApplication.PullnInsertServices();
			db = dbHelper.getReadableDatabase();
			cursor = db.query(DBHelper.TABLE_SERVICES, projection, selection,
					selectionArgs, null, null, sortOrder);
			return cursor;
		default:
			return null;
		}
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		db = dbHelper.getReadableDatabase();
		int updateCount = db.update(DBHelper.TABLE_SERVICES, values, selection,
					selectionArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return updateCount;
	}

}
