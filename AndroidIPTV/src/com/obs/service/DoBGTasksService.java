package com.obs.service;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.IntentService;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.obs.androidiptv.MyApplication;
import com.obs.data.ClientnConfigDatum;
import com.obs.database.DBHelper;
import com.obs.retrofit.OBSClient;

/**
 * This service is started when an Alarm has been raised
 * 
 * We pop a notification into the status bar for the user to click on When the
 * user clicks the notification a new activity is opened
 * 
 * @author paul.blundell
 */

public class DoBGTasksService extends IntentService {

	public static String TASK_ID = "TASK_ID";

	public DoBGTasksService() {
		super("DoBGTasksService");
	}

	@Override
	public void onCreate() {
		//Log.i("DoBGTasksService", "onCreate()");
		super.onCreate();
	}

	/**
	 * Class for clients to access
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		Log.i("onHandleIntent", "onHandleIntent()");
		int taskId = intent.getIntExtra(DoBGTasksService.TASK_ID, -1);
		Log.i("onHandleIntent-req", " : " + taskId);
		switch (taskId) {
		case 0:
			UpdateServices();
			UpdateClientnConfigData();
			stopSelf();
			break;
		case 1:
			UpdateClientnConfigData();
			stopSelf();
			break;
		case -1:
			// do nothing
			break;
		}
	}
	
	private void UpdateClientnConfigData() {
		MyApplication mApplication = ((MyApplication) getApplicationContext());
		OBSClient mOBSClient = mApplication.getOBSClient();
		ClientnConfigDatum result = null;
		retrofit.RetrofitError error = null;
		int status = -1;
		try {
			result = mOBSClient.getClientnConfigDataSync(mApplication
					.getClientId());
		} catch (Exception e) {
			error = ((retrofit.RetrofitError) e);
			status = error.getResponse().getStatus();

		}
		if (result != null && status == -1) {

			try {
				mApplication.setClientId(Long.toString(result.getClientId()));
				mApplication.setBalance(result.getBalanceAmount().floatValue());
				mApplication.setBalanceCheck(result.getBalanceCheck());
				mApplication.setCurrency(result.getCurrency());
				boolean isPayPalReq = result.getPaypalConfigData().getEnabled();
				mApplication.setPayPalReq(isPayPalReq);
				if (isPayPalReq) {
					String value = result.getPaypalConfigData().getValue();
					if (value != null && value.length() > 0) {
						JSONObject json = new JSONObject(value);
						if (json != null) {
							mApplication.setPayPalClientID(json.get("clientId")
									.toString());
						}
					} else {
						Log.e("DoBGTasksService",
								"Invalid Data for PayPal details");
					}
				}
			} catch (JSONException e) {
				Log.e("DoBGTasksService",
						(e.getMessage() == null) ? "Json Exception" : e
								.getMessage());

			} catch (Exception e) {
				Log.e("DoBGTasksService",
						(e.getMessage() == null) ? "Exception" : e.getMessage());

			}
			// Log.d("DoBGService", MyApplication.isActive + "");
			// Stop the service when we are finished
			//stopSelf();
		} else if (error != null) {
			final String toastMsg = (status == 403 ? mApplication
					.getDeveloperMessage(error) : "Server Communication Error");// errMsg;
			Log.e("DoBGTasksService", toastMsg);
			/*
			 * Handler mHandler = new Handler(getMainLooper());
			 * mHandler.post(new Runnable() {
			 * 
			 * @Override public void run() {
			 * Toast.makeText(getApplicationContext(), toastMsg,
			 * Toast.LENGTH_SHORT).show(); } });
			 */
		}

	}

	private void UpdateServices() {
		MyApplication mApplication = (MyApplication) getApplicationContext();
		DBHelper dbHelper = new DBHelper(getBaseContext());
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		mApplication.PullnInsertServices(db);
		mApplication.InsertCategories(db);
		mApplication.InsertSubCategories(db);
		if (db.isOpen())
			db.close();
		//stopSelf();
	}

}