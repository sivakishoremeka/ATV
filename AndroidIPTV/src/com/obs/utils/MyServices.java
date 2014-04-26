package com.obs.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.obs.androidiptv.MyApplication;
import com.obs.data.ServiceDatum;
import com.obs.retrofit.OBSClient;

public class MyServices {
	public final static String IPTV_CHANNELS_DETAILS = "IPTV Channels Details";
	public final static String CHANNELS_UPDATED_AT = "Updated At";
	public final static String CHANNELS_LIST = "Channels";
	boolean mIsLiveDataReq = false;
	//ArrayList<ServiceDatum> mServiceList = new ArrayList<ServiceDatum>();
	MyApplication mApplication = null;
	OBSClient mOBSClient;
	ExecutorService mExecutorService;
	private SharedPreferences mPrefs;
	private Editor mPrefsEditor;
	
	public MyServices(MyApplication myApplcn,boolean isLiveDataReq) {
		this.mApplication = myApplcn;
		this.mIsLiveDataReq = isLiveDataReq;
		mExecutorService = Executors.newCachedThreadPool();
		mOBSClient = mApplication.getOBSClient(mApplication, mExecutorService);
		mPrefs = mApplication.getPrefs();
		mPrefsEditor = mApplication.getEditor();
	}

	public ArrayList<ServiceDatum> GetChannelsList() {
		String ch_dtls_res = null;
		if (!mIsLiveDataReq) {
			String sChannelDtls = mApplication.getPrefs().getString(IPTV_CHANNELS_DETAILS, "");
			if (sChannelDtls.length() != 0) {
				JSONObject json_ch_dtls = null;
				try {
					json_ch_dtls = new JSONObject(sChannelDtls);
					ch_dtls_res = json_ch_dtls.getString(CHANNELS_LIST);
				} catch (JSONException e1) {
					e1.printStackTrace();
				}
				if (ch_dtls_res != null && ch_dtls_res.length() != 0) {
					String sDate = "";

					try {
						sDate = (String) json_ch_dtls.get(CHANNELS_UPDATED_AT);
						SimpleDateFormat df = new SimpleDateFormat(
								"yyyy-MM-dd", new Locale("en"));
						Calendar c = Calendar.getInstance();
						String currDate = df.format(c.getTime());
						Date d1 = null, d2 = null;
						try {
							d1 = df.parse(sDate);
							d2 = df.parse(currDate);
						} catch (ParseException e) {
							e.printStackTrace();
						}
						if ((sDate.length() != 0) && (d1.compareTo(d2) == 0)) {
							mIsLiveDataReq = false;
						} else {
							mIsLiveDataReq = true;
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				} else {
					mIsLiveDataReq = true;
				}
			} else {
				mIsLiveDataReq = true;
			}
		}
		if (mIsLiveDataReq) {
			return getServiceListFromServer();
		} else {
			 return getServiceListFromJSON(ch_dtls_res);
		}
	}

	private ArrayList<ServiceDatum> getServiceListFromJSON(String json) {
		java.lang.reflect.Type t = new TypeToken<List<ServiceDatum>>() {
		}.getType();
		ArrayList<ServiceDatum> serviceList = new Gson().fromJson(json, t);
		return serviceList;
	}

	private ArrayList<ServiceDatum> getServiceListFromServer() {
		ArrayList<ServiceDatum> serviceList=  mOBSClient.getPlanServicesSync(mApplication.getClientId());
		if (serviceList != null && serviceList.size() > 0) {

			/** saving channel details to preferences */

			mPrefsEditor = mPrefs.edit();
			Date date = new Date();
			String formattedDate = mApplication.df.format(date);
			JSONObject json = null;
			try {
				json = new JSONObject();
				json.put(CHANNELS_UPDATED_AT, formattedDate);
				json.put(CHANNELS_LIST, new Gson().toJson(serviceList));
			} catch (JSONException e) {
				e.printStackTrace();
			}
			mPrefsEditor.putString(IPTV_CHANNELS_DETAILS,
					json.toString());
			mPrefsEditor.commit();
		}
		return serviceList;
	}
}
