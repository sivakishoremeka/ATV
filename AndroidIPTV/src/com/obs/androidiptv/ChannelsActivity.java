package com.obs.androidiptv;

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

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.app.ActionBar;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.obs.adapter.ChannelGridViewAdapter;
import com.obs.data.ServiceDatum;
import com.obs.retrofit.OBSClient;

public class ChannelsActivity extends Activity {

	private final String TAG = ChannelsActivity.this.getClass().getName();
	public final static String CHANNEL_EPG = "Channel Epg";
	public final static String IPTV_CHANNELS_DETAILS = "IPTV Channels Details";
	public final static String CHANNELS_UPDATED_AT = "Updated At";
	public final static String CHANNELS_LIST = "Channels";
	private SharedPreferences mPrefs;
	private ProgressDialog mProgressDialog;
	private Editor mPrefsEditor;
	GridView gridView;
	ChannelGridViewAdapter adapter;
	ArrayList<ServiceDatum> mServiceList = new ArrayList<ServiceDatum>();

	MyApplication mApplication = null;
	OBSClient mOBSClient;
	ExecutorService mExecutorService;
	boolean mIsReqCanceled = false;

	boolean isLiveDataReq = false;
	boolean isBalCheckReq;
	Double bal;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_channels);
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		mApplication = ((MyApplication) getApplicationContext());
		mExecutorService = Executors.newCachedThreadPool();
		mOBSClient = mApplication.getOBSClient(this, mExecutorService);
		mPrefs = ChannelsActivity.this.getSharedPreferences(
				mApplication.PREFS_FILE, Activity.MODE_PRIVATE);
		isBalCheckReq = mApplication.isBalCheckReq;
		gridView = (GridView) (findViewById(R.id.a_gv_channels));

		Log.d(TAG, "onCreate");

		// getData();
	}

	@Override
	protected void onPause() {
		if (adapter != null) {
			mServiceList.clear();
			adapter.notifyDataSetChanged();
		}
		super.onPause();
	}

	@Override
	protected void onResume() {
		getData();
		super.onResume();
	}

	private void getData() {
		if (isBalCheckReq) {
			if (isLiveDataReq)
				GetChannelsFromServer();
			else if (mApplication.getBalance() >= 0)
				Toast.makeText(ChannelsActivity.this,
						"Insufficient Balance.Please Make a Payment.",
						Toast.LENGTH_LONG).show();
			else
				CheckCacheForChannelsList();
		} else {
			CheckCacheForChannelsList();
		}

	}

	private void GetChannelsFromServer() {

		Log.d(TAG, "GetChannelsFromServer");

		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
		mProgressDialog = new ProgressDialog(ChannelsActivity.this,
				ProgressDialog.THEME_HOLO_DARK);
		mProgressDialog.setMessage("Retriving Detials");
		mProgressDialog.setCanceledOnTouchOutside(false);
		mProgressDialog.setOnCancelListener(new OnCancelListener() {

			public void onCancel(DialogInterface arg0) {
				if (mProgressDialog.isShowing())
					mProgressDialog.dismiss();
				mIsReqCanceled = true;
				if (null != mExecutorService)
					if (!mExecutorService.isShutdown())
						mExecutorService.shutdownNow();
			}
		});
		mProgressDialog.show();
		mOBSClient.getPlanServices(mApplication.getClientId(),
				getPlanServicesCallBack);
	}

	final Callback<ArrayList<ServiceDatum>> getPlanServicesCallBack = new Callback<ArrayList<ServiceDatum>>() {
		@Override
		public void failure(RetrofitError retrofitError) {
			if (!mIsReqCanceled) {

				Log.d(TAG, "failure");

				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
				if (retrofitError.isNetworkError()) {
					Toast.makeText(
							ChannelsActivity.this,
							getApplicationContext().getString(
									R.string.error_network), Toast.LENGTH_LONG)
							.show();
				} else if (retrofitError.getResponse().getStatus() == 403) {
					String msg = mApplication
							.getDeveloperMessage(retrofitError);
					msg = (msg != null && msg.length() > 0 ? msg
							: "Internal Server Error");
					Toast.makeText(ChannelsActivity.this, msg,
							Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(
							ChannelsActivity.this,
							"Server Error : "
									+ retrofitError.getResponse().getStatus(),
							Toast.LENGTH_LONG).show();
				}
			}
		}

		@Override
		public void success(ArrayList<ServiceDatum> serviceList,
				Response response) {
			if (!mIsReqCanceled) {

				Log.d(TAG, "success");

				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
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

					/** updating gridview **/
					updateChannels(serviceList);

				}
			}
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.nav_menu, menu);
		MenuItem searchItem = menu.findItem(R.id.action_search);
		searchItem.setVisible(false);
		MenuItem refreshItem = menu.findItem(R.id.action_refresh);
		refreshItem.setVisible(true);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			break;
		case R.id.action_home:
			NavUtils.navigateUpFromSameTask(this);
			break;
		case R.id.action_refresh:
			isLiveDataReq = true;
			if (adapter != null) {
				mServiceList.clear();
				adapter.notifyDataSetChanged();
			}
			getData();
			break;
		default:
			break;
		}
		return true;
	}

	private void CheckCacheForChannelsList() {

		Log.d(TAG, "CheckCacheForChannelsList");

		String ch_dtls_res = null;
		if (!isLiveDataReq) {
			String sChannelDtls = mPrefs.getString(IPTV_CHANNELS_DETAILS, "");
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
							isLiveDataReq = false;
						} else {
							isLiveDataReq = true;
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				} else {
					isLiveDataReq = true;
				}
			} else {
				isLiveDataReq = true;
			}
		}
		if (isLiveDataReq) {
			GetChannelsFromServer();
		} else {
			updateChannels(getServiceListFromJSON(ch_dtls_res));
		}
	}

	private List<ServiceDatum> getServiceListFromJSON(String json) {
		java.lang.reflect.Type t = new TypeToken<List<ServiceDatum>>() {
		}.getType();
		List<ServiceDatum> serviceList = new Gson().fromJson(json, t);
		return serviceList;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == 4) {
			if (mProgressDialog != null) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}
			mIsReqCanceled = true;
			mExecutorService.shutdownNow();
			this.finish();
		} else if (keyCode == 23) {
			View focusedView = getWindow().getCurrentFocus();
			focusedView.performClick();
		}
		return super.onKeyDown(keyCode, event);
	}

	private void updateChannels(final List<ServiceDatum> list) {

		Log.d(TAG, "updateChannels :" + list.size());

		if (list != null && list.size() > 0) {
			final GridView gridView = (GridView) (findViewById(R.id.a_gv_channels));
			gridView.setAdapter(new ChannelGridViewAdapter(list,
					ChannelsActivity.this));
			gridView.setDrawSelectorOnTop(true);
			gridView.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View imageVw,
						int position, long arg3) {
					ServiceDatum service = list.get(position);
					startActivity(new Intent(ChannelsActivity.this,
							IPTVActivity.class)
							.putExtra(IPTVActivity.CHANNEL_NAME,
									service.getChannelName()).putExtra(
									IPTVActivity.CHANNEL_URL, service.getUrl()));
				}
			});
		}
	}
}
