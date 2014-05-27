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

	// public static final Uri SERVICES_ON_REFREFRESH_URI =
	// Uri.parse("content://"
	// / + AUTHORITY + "/servicesonrefresh");

	public static final Uri SERVICE_CATEGORIES_URI = Uri.parse("content://"
			+ AUTHORITY + "/categories");
	public static final Uri SERVICE_SUB_CATEGORIES_URI = Uri.parse("content://"
			+ AUTHORITY + "/subcategories");

	public static final int SERVICES = 1;
	// public static final int SERVICES_ON_REFRESH = 2;
	public static final int SERVICE_CATEGORIES = 2;
	public static final int SERVICE_SUB_CATEGORIES = 3;

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
		// uriMatcher.addURI(AUTHORITY, "servicesonrefresh",
		// SERVICES_ON_REFRESH);
		// URI for all service categories
		uriMatcher.addURI(AUTHORITY, "categories", SERVICE_CATEGORIES);
		// URI for all service sub categories
		uriMatcher.addURI(AUTHORITY, "subcategories", SERVICE_SUB_CATEGORIES);
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
		boolean mIsLiveDataReq = false;
		String sDate = "";
		String[] arrSortOrder = null;
		String isRefresh = "false";
		String sortOrder1 = null;
		if (sortOrder != null) {
			arrSortOrder = sortOrder.split("&");
			if (arrSortOrder.length > 0) {
				isRefresh = arrSortOrder[0];
				if (arrSortOrder.length > 1) {
					sortOrder1 = arrSortOrder[1];
				}
			}
		}

		db = dbHelper.getWritableDatabase();
		switch (mUriMatcher.match(uri)) {
		case SERVICES:

			// Table name
			// null gives all columns
			// selection & selectionArgs for where statements
			// groupby having nulls not using so
			// db.query retruns cursor. cursor is an iterator to move over the
			// data.

			// freshness of data (1 day validity)
			if (isRefresh.equalsIgnoreCase("false")) {
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
			}
			// freshness of data (1 day validity)

			if (mIsLiveDataReq || isRefresh.equalsIgnoreCase("true")) {
				doRefreshData(db);
			}
			cursor = db.query(DBHelper.TABLE_SERVICES, projection, selection,
					selectionArgs, null, null, sortOrder1);
			return cursor;

			/*
			 * case SERVICES_ON_REFRESH: // refresh service table
			 * mApplication.PullnInsertServices(db);
			 * mApplication.InsertCategories(db);
			 * mApplication.InsertSubCategories(db); cursor =
			 * db.query(DBHelper.TABLE_SERVICES, projection, selection,
			 * selectionArgs, null, null, sortOrder); return cursor;
			 */

		case SERVICE_CATEGORIES:
			// freshness of data (1 day validity)
			if (isRefresh.equalsIgnoreCase("false")) {
				try {
					sDate = (String) mApplication.getPrefs().getString(
							mApplication.getResources().getString(
									R.string.channels_category_updated_at), "");
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
			}
			// freshness of data (1 day validity)

			if (mIsLiveDataReq || isRefresh.equalsIgnoreCase("true")) {
				doRefreshData(db);
			}
			cursor = db.query(DBHelper.TABLE_SERVICE_CATEGORIES, projection,
					selection, selectionArgs, null, null, sortOrder1);
			if(cursor.getCount() == -1){
				doRefreshData(db);
				cursor = db.query(DBHelper.TABLE_SERVICE_CATEGORIES, projection,
						selection, selectionArgs, null, null, sortOrder1);
			}
			return cursor;

		case SERVICE_SUB_CATEGORIES:
			// refresh service table
			// freshness of data (1 day validity)
			if (isRefresh.equalsIgnoreCase("false")) {
				try {
					sDate = (String) mApplication.getPrefs().getString(
							mApplication.getResources().getString(
									R.string.channels_sub_category_updated_at),
							"");
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
			}
			// freshness of data (1 day validity)

			if (mIsLiveDataReq || isRefresh.equalsIgnoreCase("true")) {
				doRefreshData(db);
			}
			cursor = db.query(DBHelper.TABLE_SERVICE_SUB_CATEGORIES,
					projection, selection, selectionArgs, null, null,
					sortOrder1);
			if(cursor.getCount() == -1){
				doRefreshData(db);
				cursor = db.query(DBHelper.TABLE_SERVICE_SUB_CATEGORIES, projection,
						selection, selectionArgs, null, null, sortOrder1);
			}
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
		db.close();
		return updateCount;
	}

	private void doRefreshData(SQLiteDatabase db) {
		mApplication.PullnInsertServices(db);
		mApplication.InsertCategories(db);
		mApplication.InsertSubCategories(db);
	}

}
