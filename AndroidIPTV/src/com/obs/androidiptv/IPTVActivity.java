package com.obs.androidiptv;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONException;
import org.json.JSONObject;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.app.ActionBar;
import android.app.ActionBar.LayoutParams;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.obs.adapter.EPGFragmentPagerAdapter;
import com.obs.androidiptv.EpgFragment.ProgDetails;
import com.obs.data.Reminder;
import com.obs.data.ServiceDatum;
import com.obs.database.DatabaseHandler;
import com.obs.retrofit.OBSClient;
import com.obs.service.ScheduleClient;
import com.obs.utils.Utilities;

public class IPTVActivity extends FragmentActivity implements SearchView.OnQueryTextListener {

	/** This is live/Iptv activity */

	public static String TAG = IPTVActivity.class.getName();
	public final static String CHANNEL_NAME = "CHANNELNAME";
	public final static String CHANNEL_URL = "URL";
	private SharedPreferences mPrefs;
	private SearchView mSearchView;
	private Editor mPrefsEditor;
	EPGFragmentPagerAdapter mEpgPagerAdapter;

	// This is a handle so that we can call methods on our service
	private ScheduleClient scheduleClient;

	private ProgressDialog mProgressDialog;
	private String mChannelURL;
	private int mChannelId;
	private ArrayList<ServiceDatum> mServiceList;
	ViewPager mViewPager;

	MyApplication mApplication = null;
	OBSClient mOBSClient;
	ExecutorService mExecutorService;
	boolean mIsReqCanceled = false;

	boolean requiredLiveData = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_iptv);
		findViewById(R.id.a_iptv_rl_root_layout).setVisibility(View.INVISIBLE);
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		mApplication = ((MyApplication) getApplicationContext());
		mExecutorService = Executors.newCachedThreadPool();
		mOBSClient = mApplication.getOBSClient(this, mExecutorService);

		mPrefs = mApplication.getPrefs();
		// for not refresh data
		mPrefsEditor = mApplication.getEditor();
		mPrefsEditor.putBoolean(EpgFragment.IS_REFRESH, false);
		mPrefsEditor.commit();
		// for not refresh data
		Button btn = (Button) findViewById(R.id.a_iptv_btn_watch_remind);
		btn.setFocusable(true);btn.requestFocus();
		btn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				String label = ((Button) v).getText().toString();
				if (label.trim().equalsIgnoreCase("Watch")) {
					Intent intent = new Intent();
					intent.putExtra("VIDEOTYPE", "LIVETV");
					intent.putExtra(CHANNEL_URL, mChannelURL);
					intent.putExtra("CHANNELID", mChannelId);
					intent.putParcelableArrayListExtra("SERVICELIST",
							mServiceList);

					mApplication.startPlayer(intent, IPTVActivity.this);
				} else {
					/**
					 * This is called to set a new notification
					 */
					ProgDetails progDtls = (ProgDetails) ((Button) v).getTag();
					// Ask our service to set an alarm for that date, this
					// activity talks to the client that talks to the service

					if (progDtls != null) {
						scheduleClient.setAlarmForNotification(
								progDtls.calendar, progDtls.progTitle,
								mChannelId, progDtls.channelName, mChannelURL);
						SimpleDateFormat sdf = new SimpleDateFormat(
								"yyyy-MM-dd HH:mm", new Locale("en"));
						String date = sdf.format(progDtls.calendar.getTime());

						// add to db
						DatabaseHandler dbHandler = new DatabaseHandler(
								IPTVActivity.this);
						dbHandler.deleteOldReminders();
						dbHandler.addReminder(new Reminder(progDtls.progTitle,
								progDtls.calendar.getTimeInMillis(),
								mChannelId, progDtls.channelName, mChannelURL));
						Toast.makeText(IPTVActivity.this,
								"Notification set for: " + date,
								Toast.LENGTH_SHORT).show();
					}
				}
			}
		});

		mViewPager = (ViewPager) findViewById(R.id.a_iptv_pager);

		CheckCacheForChannelList();

		// Create a new service client and bind our activity to this service
		scheduleClient = new ScheduleClient(this);
		scheduleClient.doBindService();
	}

	@Override
	protected void onStop() {
		// When our activity is stopped ensure we also stop the connection to
		// the service
		// this stops us leaking our activity into the system *bad*
		if (scheduleClient != null)
			scheduleClient.doUnbindService();
		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.nav_menu, menu);
		MenuItem searchItem = menu.findItem(R.id.action_search);
		searchItem.setVisible(true);
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
			/* NavUtils.navigateUpFromSameTask(this); */
			startActivity(new Intent(this, MainActivity.class));
			break;
		case R.id.action_refresh:

			mPrefsEditor.remove(ChannelsActivity.IPTV_CHANNELS_DETAILS);
			mPrefsEditor.commit();
			CheckCacheForChannelList();
			break;
		default:
			break;
		}
		return true;
	}
	
	private void setupSearchView(MenuItem searchItem) {
		mSearchView.setOnQueryTextListener(this);
	}

	private void CheckCacheForChannelList() {

		String sChannelDtls = mPrefs.getString(
				ChannelsActivity.IPTV_CHANNELS_DETAILS, "");
		if (sChannelDtls.length() != 0) {
			JSONObject json_ch_dtls = null;
			String channel_details = null;
			try {
				json_ch_dtls = new JSONObject(mPrefs.getString(
						ChannelsActivity.IPTV_CHANNELS_DETAILS, ""));
				channel_details = json_ch_dtls.getString("Channels");
				// channel_details =
				// "[{\"serviceId\":2,\"clientId\":34,\"channelName\":\"BrazCom\",\"image\":\"https://spark.openbillingsystem.com/images/utv.png\",\"url\":\"http://rm-edge-4.cdn2.streamago.tv/streamagoedge/1922/815/playlist.m3u8\"},{\"serviceId\":3,\"clientId\":34,\"channelName\":\"BrazilianC\",\"image\":\"https://spark.openbillingsystem.com/images/stmv.png\",\"url\":\"http://www.wowza.com/_h264/bigbuckbunny_450.mp4\"},{\"serviceId\":4,\"clientId\":34,\"channelName\":\"Barmedas\",\"image\":\"https://spark.openbillingsystem.com/images/wb.png\",\"url\":\"http://rm-edge-4.cdn2.streamago.tv/streamagoedge/1922/815/playlist.m3u8\"},{\"serviceId\":1,\"clientId\":34,\"channelName\":\"Albanian1\",\"image\":\"https://spark.openbillingsystem.com/images/hbo.png\",\"url\":\"http://rm-edge-4.cdn2.streamago.tv/streamagoedge/1922/815/playlist.m3u8\"},{\"serviceId\":2,\"clientId\":34,\"channelName\":\"BrazCom\",\"image\":\"https:/,/spark.openbillingsystem.com/images/utv.png\",\"url\":\"http://rm-edge-4.cdn2.streamago.tv/streamagoedge/1922/815/playlist.m3u8\"},{\"serviceId\":3,\"clientId\":34,\"channelName\":\"BrazilianC\",\"image\":\"https://spark.openbillingsystem.com/images/stmv.png\",\"url\":\"http://www.wowza.com/_h264/bigbuckbunny_450.mp4\"},{\"serviceId\":4,\"clientId\":34,\"channelName\":\"Barmedas\",\"image\":\"https://spark.openbillingsystem.com/images/wb.png\",\"url\":\"http://rm-edge-4.cdn2.streamago.tv/streamagoedge/1922/815/playlist.m3u8\"},{\"serviceId\":1,\"clientId\":34,\"channelName\":\"Albanian1\",\"image\":\"https://spark.openbillingsystem.com/images/hbo.png\",\"url\":\"http://rm-edge-4.cdn2.streamago.tv/streamagoedge/1922/815/playlist.m3u8\"},{\"serviceId\":2,\"clientId\":34,\"channelName\":\"BrazCom\",\"image\":\"https://spark.openbillingsystem.com/images/utv.png\",\"url\":\"http://rm-edge-4.cdn2.streamago.tv/streamagoedge/1922/815/playlist.m3u8\"},{\"serviceId\":3,\"clientId\":34,\"channelName\":\"BrazilianC\",\"image\":\"https://spark.openbillingsystem.com/images/stmv.png\",\"url\":\"http://www.wowza.com/_h264/bigbuckbunny_450.mp4\"},{\"serviceId\":4,\"clientId\":34,\"channelName\":\"Barmedas\",\"image\":\"https://spark.openbillingsystem.com/images/wb.png\",\"url\":\"http://rm-edge-4.cdn2.streamago.tv/streamagoedge/1922/815/playlist.m3u8\"},{\"serviceId\":1,\"clientId\":34,\"channelName\":\"Albanian1\",\"image\":\"https://spark.openbillingsystem.com/images/hbo.png\",\"url\":\"http://rm-edge-4.cdn2.streamago.tv/streamagoedge/1922/815/playlist.m3u8\"},{\"serviceId\":2,\"clientId\":34,\"channelName\":\"BrazCom\",\"image\":\"https://spark.openbillingsystem.com/images/utv.png\",\"url\":\"http://rm-edge-4.cdn2.streamago.tv/streamagoedge/1922/815/playlist.m3u8\"},{\"serviceId\":3,\"clientId\":34,\"channelName\":\"BrazilianC\",\"image\":\"https://spark.openbillingsystem.com/images/stmv.png\",\"url\":\"http://www.wowza.com/_h264/bigbuckbunny_450.mp4\"},{\"serviceId\":4,\"clientId\":34,\"channelName\":\"Barmedas\",\"image\":\"https://spark.openbillingsystem.com/images/wb.png\",\"url\":\"http://rm-edge-4.cdn2.streamago.tv/streamagoedge/1922/815/playlist.m3u8\"},{\"serviceId\":1,\"clientId\":34,\"channelName\":\"Albanian1\",\"image\":\"https://spark.openbillingsystem.com/images/hbo.png\",\"url\":\"http://rm-edge-4.cdn2.streamago.tv/streamagoedge/1922/815/playlist.m3u8\"},{\"serviceId\":2,\"clientId\":34,\"channelName\":\"BrazCom\",\"image\":\"https://spark.openbillingsystem.com/images/utv.png\",\"url\":\"http://rm-edge-4.cdn2.streamago.tv/streamagoedge/1922/815/playlist.m3u8\"},{\"serviceId\":3,\"clientId\":34,\"channelName\":\"BrazilianC\",\"image\":\"https://spark.openbillingsystem.com/images/stmv.png\",\"url\":\"http://www.wowza.com/_h264/bigbuckbunny_450.mp4\"},{\"serviceId\":4,\"clientId\":34,\"channelName\":\"Barmedas\",\"image\":\"https://spark.openbillingsystem.com/images/wb.png\",\"url\":\"http://rm-edge-4.cdn2.streamago.tv/streamagoedge/1922/815/playlist.m3u8\"},{\"serviceId\":1,\"clientId\":34,\"channelName\":\"Albanian1\",\"image\":\"https://spark.openbillingsystem.com/images/hbo.png\",\"url\":\"http://rm-edge-4.cdn2.streamago.tv/streamagoedge/1922/815/playlist.m3u8\"}]";
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
			if (channel_details.length() != 0) {
				String sDate = "";

				try {
					sDate = (String) json_ch_dtls
							.get(ChannelsActivity.CHANNELS_UPDATED_AT);
					SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd",
							new Locale("en"));
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
						updateChannels(getServiceListFromJSON(channel_details));
					} else {
						requiredLiveData = true;
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			} else {
				requiredLiveData = true;
			}
		} else {
			requiredLiveData = true;
		}
		if (requiredLiveData) {

			mPrefsEditor.putBoolean(EpgFragment.IS_REFRESH, true);
			mPrefsEditor.commit();
			GetChannelsFromServer();
		}
	}

	private void GetChannelsFromServer() {

		Log.d(TAG, "GetChannelsFromServer");

		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
		mProgressDialog = new ProgressDialog(IPTVActivity.this,
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
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
				if (retrofitError.isNetworkError()) {
					Toast.makeText(
							IPTVActivity.this,
							getApplicationContext().getString(
									R.string.error_network), Toast.LENGTH_LONG)
							.show();
				} else if (retrofitError.getResponse().getStatus() == 403) {
					String msg = mApplication
							.getDeveloperMessage(retrofitError);
					msg = (msg != null && msg.length() > 0 ? msg
							: "Internal Server Error");
					Toast.makeText(IPTVActivity.this, msg, Toast.LENGTH_LONG)
							.show();
				} else {
					Toast.makeText(
							IPTVActivity.this,
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
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
				if (serviceList != null && serviceList.size() > 0) {

					/** saving channel details to preferences */
					Date date = new Date();
					String formattedDate = Utilities.df.format(date);

					JSONObject json = null;
					try {
						json = new JSONObject();
						json.put(ChannelsActivity.CHANNELS_UPDATED_AT,
								formattedDate);
						json.put(ChannelsActivity.CHANNELS_LIST,
								new Gson().toJson(serviceList));
					} catch (JSONException e) {
						e.printStackTrace();
					}
					mPrefsEditor.putString(
							ChannelsActivity.IPTV_CHANNELS_DETAILS,
							json.toString());
					mPrefsEditor.commit();

					/** updating gridview **/
					updateChannels(serviceList);

				}
			}
		}
	};

	private void updateChannels(ArrayList<ServiceDatum> result) {
		mServiceList = result;
		int imgno = 0;
		LinearLayout channels = (LinearLayout) findViewById(R.id.a_iptv_ll_channels);
		channels.removeAllViews();

		final Editor editor = mPrefs.edit();
		for (final ServiceDatum data : result) {

			editor.putString(data.getChannelName(), data.getUrl());
			editor.commit();
			// imgno += 1;
			ChannelInfo chInfo = new ChannelInfo(data.getChannelName(),
					data.getUrl(), data.getServiceId());
			final ImageButton button = new ImageButton(this);
			LayoutParams params = new LayoutParams(Gravity.CENTER);
			params.height = 96;
			params.width = 96;
			params.setMargins(1, 1, 1, 1);
			button.setLayoutParams(params);
			// button.setId(1000 + imgno);
			button.setTag(chInfo);
			button.setFocusable(true);
			button.setFocusableInTouchMode(false);
			button.setImageDrawable(getResources().getDrawable(
					R.drawable.ic_default_ch));
			if (getIntent().getStringExtra(CHANNEL_NAME) != null) {
				editor.putString(CHANNEL_NAME,
						getIntent().getStringExtra(CHANNEL_NAME));
				editor.commit();
				mChannelURL = getIntent().getStringExtra(CHANNEL_URL);
			}
			ImageLoader.getInstance().displayImage(data.getImage(), button);

			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					ChannelInfo info = (ChannelInfo) v.getTag();
					editor.putString(CHANNEL_NAME, info.channelName);
					// for not refresh data
					editor.putBoolean(EpgFragment.IS_REFRESH, false);
					// for not refresh data
					editor.commit();
					// centerLockHorizontalScrollview.setCenter(v.getId()-1001);
					mChannelURL = info.channelURL;
					mChannelId = info.channelId;
					mEpgPagerAdapter = new EPGFragmentPagerAdapter(
							IPTVActivity.this.getSupportFragmentManager());
					mViewPager.setAdapter(mEpgPagerAdapter);
				}
			});
			channels.addView(button);
		}
		mEpgPagerAdapter = new EPGFragmentPagerAdapter(
				this.getSupportFragmentManager());
		mViewPager.setAdapter(mEpgPagerAdapter);
	}

	private ArrayList<ServiceDatum> getServiceListFromJSON(String json) {
		java.lang.reflect.Type t = new TypeToken<ArrayList<ServiceDatum>>() {
		}.getType();
		ArrayList<ServiceDatum> serviceList = new Gson().fromJson(json, t);
		return serviceList;
	}

	private class ChannelInfo {
		private String channelName;
		private String channelURL;
		private int channelId;

		public ChannelInfo(String channelName, String channelURL, int channelId) {
			this.channelName = channelName;
			this.channelURL = channelURL;
			this.channelId = channelId;
		}
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

	@Override
	public boolean onQueryTextSubmit(String query) {
		
		return false;
	}

	@Override
	public boolean onQueryTextChange(String newText) {
		return false;
	}
}
