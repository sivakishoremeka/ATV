package com.obs.service;

import android.app.IntentService;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.obs.androidiptv.MyApplication;
import com.obs.database.DBHelper;

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
		Log.i("onHandleIntent-req", " : "+taskId);
		switch (taskId){
		case 0:	
			UpdateServices();
			break;
		case 1:
			break;
		case -1:
			//do nothing
			break;
		}
	}

	private void UpdateServices() {
		MyApplication mApplication = (MyApplication)getApplicationContext();
			DBHelper dbHelper = new DBHelper(getBaseContext());
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			mApplication.PullnInsertServices(db);
			mApplication.InsertCategories(db);
			mApplication.InsertSubCategories(db);
			if(db.isOpen())
				db.close();
			stopSelf();
	}

}