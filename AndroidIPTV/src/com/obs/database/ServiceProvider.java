package com.obs.database;

import java.util.ArrayList;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import com.obs.androidiptv.MyApplication;
import com.obs.data.ServiceDatum;
import com.obs.utils.MyServices;

public class ServiceProvider extends ContentProvider {

	public static final String AUTHORITY = "com.obs.database.ServiceProvider";

	public static final Uri ALLSERVICES_URI = Uri.parse("content://"
			+ AUTHORITY + "/allservices");

	public static final Uri ALLSERVICES_ONREFREFRESH_URI = Uri
			.parse("content://" + AUTHORITY + "/allservicesonrefresh");

	public static final Uri SEARCH_URI = Uri.parse("content://" + AUTHORITY
			+ "/search");

	public static final String SERVICE_ID = "serviceId";
	public static final String CLIENT_ID = "clientId";
	public static final String CHANNEL_NAME = "channelName";
	public static final String IMAGE = "image";
	public static final String URL = "url";

	public static final int ALLSERVICES = 1;
	public static final int ALLSERVICES_ON_REFRESH = 2;
	public static final int SEARCH = 3;

	// Defines a set of uris allowed with this content provider
	private static final UriMatcher mUriMatcher = buildUriMatcher();

	public MyApplication mApplication;

	private static UriMatcher buildUriMatcher() {
		UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		// URI for all services
		uriMatcher.addURI(AUTHORITY, "allservices", ALLSERVICES);
		// URI for all services
		uriMatcher.addURI(AUTHORITY, "allservicesonrefresh",
				ALLSERVICES_ON_REFRESH);
		// URI for search services
		uriMatcher.addURI(AUTHORITY, "search", SEARCH);
		return uriMatcher;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {

		Cursor c = null;

		MyServices svcObj = null;

		ArrayList<ServiceDatum> serviceList = null;

		MatrixCursor mCursor = null;

		switch (mUriMatcher.match(uri)) {
		case ALLSERVICES:
			// Defining a cursor object with columns description, lat and lng
			mCursor = new MatrixCursor(new String[] { SERVICE_ID, CLIENT_ID,
					CHANNEL_NAME, IMAGE, URL });

			// Create a parser object to parse places in JSON format
			svcObj = new MyServices(mApplication, false);

			serviceList = svcObj.GetChannelsList();

			// Adding services cursor
			for (int i = 0; i < serviceList.size(); i++) {
				ServiceDatum service = serviceList.get(i);
				mCursor.addRow(new String[] {
						service.getServiceId().toString(),
						service.getClientId().toString(),
						service.getChannelName(), service.getImage(),
						service.getUrl() });
			}
			c = mCursor;
			break;

		case ALLSERVICES_ON_REFRESH:
			// Defining a cursor object with columns description, lat and lng
			mCursor = new MatrixCursor(new String[] { SERVICE_ID, CLIENT_ID,
					CHANNEL_NAME, IMAGE, URL });

			// Create a parser object to parse places in JSON format
			svcObj = new MyServices(mApplication, true);

			serviceList = svcObj.GetChannelsList();

			// Adding services cursor
			for (int i = 0; i < serviceList.size(); i++) {
				ServiceDatum service = serviceList.get(i);
				mCursor.addRow(new String[] {
						service.getServiceId().toString(),
						service.getClientId().toString(),
						service.getChannelName(), service.getImage(),
						service.getUrl() });
			}
			c = mCursor;
			break;

		case SEARCH:

			mCursor = new MatrixCursor(new String[] { SERVICE_ID, CLIENT_ID,
					CHANNEL_NAME, IMAGE, URL });

			svcObj = new MyServices(mApplication, false);

			serviceList = svcObj.GetChannelsList();
			// Adding services cursor
			for (int i = 0; i < serviceList.size(); i++) {
				ServiceDatum service = serviceList.get(i);
				if (service.getChannelName().toLowerCase()
						.contains(selectionArgs[0].toLowerCase()))
					mCursor.addRow(new String[] {
							service.getServiceId().toString(),
							service.getClientId().toString(),
							service.getChannelName(), service.getImage(),
							service.getUrl() });
			}

			c = mCursor;
			break;
		}

		return c;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
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
	public boolean onCreate() {
		this.mApplication = (MyApplication) this.getContext()
				.getApplicationContext();
		return false;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO Auto-generated method stub
		return 0;
	}
}