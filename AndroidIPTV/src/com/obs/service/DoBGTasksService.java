package com.obs.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.obs.androidiptv.MyApplication;
import com.obs.data.ResourceIdentifier;
import com.obs.data.StatusReqDatum;
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

	public static String App_State_Req = "app state req";

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
		int req = intent.getIntExtra(App_State_Req, -1);
		Log.i("onHandleIntent-req", " : "+req);
		if (req != -1) {
			setAppState(req);
		}
	}

	private void setAppState(int req) {

		MyApplication appContext = ((MyApplication) getApplicationContext());
		OBSClient mOBSClient = appContext.getOBSClient();
		StatusReqDatum reqData = new StatusReqDatum();
		reqData.setClientId(appContext.getClientId());
		reqData.setStatus(req == MyApplication.SetAppState.SET_ACTIVE.ordinal()?"ACTIVE":"INACTIVE");
		String androidId = Settings.Secure.getString(
				getApplicationContext().getContentResolver(),
				Settings.Secure.ANDROID_ID);
		ResourceIdentifier result = null;
		retrofit.RetrofitError error = null;
		int status =-1;
		try{
			result  = mOBSClient.updateAppStatus(androidId,reqData);
		}
		catch(Exception e)
		{
			error = ((retrofit.RetrofitError) e);
			status = error.getResponse().getStatus();
			
		}
		if (result != null && status == -1 ) {
			if (req == MyApplication.SetAppState.SET_ACTIVE.ordinal())
				MyApplication.isActive = true;
			else if (req == MyApplication.SetAppState.SET_INACTIVE.ordinal()){
				MyApplication.isActive = false;
				MyApplication.startCount = 0;
				MyApplication.stopCount = 0;
			}
			//Log.d("DoBGService", MyApplication.isActive + "");
			// Stop the service when we are finished
			stopSelf();
		}
		else if(error!=null){
			final String toastMsg =(status == 403?appContext.getDeveloperMessage(error):"Server Communication Error");// errMsg;
			 Handler mHandler = new Handler(getMainLooper());
			    mHandler.post(new Runnable() {
			        @Override
			        public void run() {
			            Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_SHORT).show();
			        }
			    });
		}
	}

}