package com.obs.androidiptv;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONException;
import org.json.JSONObject;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.app.ActionBar;
import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.NavUtils;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.obs.data.DeviceDatum;
import com.obs.data.EPGData;
import com.obs.data.EpgDatum;
import com.obs.data.ServiceDatum;
import com.obs.database.DBHelper;
import com.obs.database.ServiceProvider;
import com.obs.retrofit.OBSClient;

public class ChannelsActivity extends Activity implements
		SurfaceHolder.Callback, MediaPlayer.OnPreparedListener,
		MediaPlayer.OnErrorListener, AdapterView.OnItemSelectedListener,
		AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener,
		LoaderCallbacks<Cursor> {
	private SharedPreferences mPrefs;
	private ProgressDialog mProgressDialog;
	private Editor mPrefsEditor;

	public final static String CHANNEL_EPG = "Channel Epg";
	public final static String IPTV_CHANNELS_DETAILS = "IPTV Channels Details";
	public final static String CHANNELS_UPDATED_AT = "Updated At";
	public final static String CHANNELS_LIST = "Channels";
	public final static String CHANNEL_URL = "URL";
	public static String mDate;
	ListView mListView;

	MyApplication mApplication = null;
	OBSClient mOBSClient;
	ExecutorService mExecutorService;
	boolean mIsReqCanceled = false;

	boolean mIsLiveDataReq = false;
	int mReqType = ServiceProvider.SERVICES;
	boolean mIsBalCheckReq;
	float mBalance;

	String mSearchString;
	String mSelection;
	String[] mSelectionArgs;

	SurfaceView videoSurface;
	MediaPlayer player;
	VideoControllerView controller;
	ServiceDatum mService;

	private SimpleCursorAdapter adapter;
	private int mSelectedIdx =-1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_channels);

		// Log.d("ChannelsActivity","onCreate");

		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {

			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		} else {
			View decorView = getWindow().getDecorView();
			int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_LOW_PROFILE;
			decorView.setSystemUiVisibility(uiOptions);
		}

		mApplication = ((MyApplication) getApplicationContext());
		mExecutorService = Executors.newCachedThreadPool();
		mOBSClient = mApplication.getOBSClient(this, mExecutorService);
		mPrefs = mApplication.getPrefs();
		mIsBalCheckReq = mApplication.isBalCheckReq;

		Calendar c = Calendar.getInstance();
		mDate = mApplication.df.format(c.getTime()); // dt is now the new date

		mListView = (ListView) findViewById(R.id.a_channels_lv);

		videoSurface = (SurfaceView) findViewById(R.id.a_channels_videoSurface);
		SurfaceHolder videoHolder = videoSurface.getHolder();
		videoHolder.addCallback(this);
		player = new MediaPlayer();

		String[] from = new String[] { DBHelper.SERVICE_KEY_CHANNEL_NAME };
		int[] to = new int[] { R.id.ch_lv_item_tv_ch_Name };

		adapter = new SimpleCursorAdapter(this, R.layout.ch_list_item, null,
				from, to, 0);
		mListView.setAdapter(adapter);
		mListView.setOnItemClickListener(this);
		mListView.setOnItemLongClickListener(this);
		mListView.setOnItemSelectedListener(this);
		//mListView.setSelected(true);
		//mListView.setSelection(0);
		// initiallizeUI();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		// Log.d("ChannelsActivity", "onNewIntent");
		if (null != intent && null != intent.getAction()
				&& intent.getAction().equals(Intent.ACTION_SEARCH)) {

			// initiallizing req criteria
			mReqType = ServiceProvider.SERVICES;
			mSearchString = intent.getStringExtra(SearchManager.QUERY);
			mSelection = DBHelper.SERVICE_KEY_CHANNEL_NAME + " LIKE ?";
			mSelectionArgs = new String[] { "%" + mSearchString + "%" };
			CheckBalancenGetData();
		}
	}

	@Override
	protected void onDestroy() {
		// Log.d("ChannelsActivity", "onDestroy");
		if (player != null) {
			if (player.isPlaying())
				player.stop();
			player.release();
		}
		super.onDestroy();
	}

	private void initiallizePlayer() {
		// Log.d("ChannelsActivity","initiallizePlayer");
		if (player == null)
			player = new MediaPlayer();
	}

	private void initiallizeUI() {
		// Log.d("ChannelsActivity","initiallizeUI");
		CheckBalancenGetData();
	}

	private void CheckBalancenGetData() {
		// Log.d("ChannelsActivity","CheckBalancenGetData");
		if (mIsBalCheckReq)
			validateDevice();
		else
			getServices();
	}

	private void getServices() {
		// Log.d("ChannelsActivity","getServices");
		getLoaderManager().restartLoader(mReqType, null, this);
	}

	/** Validating Customer balance */
	private void validateDevice() {
		// Log.d("ChannelsActivity","validateDevice");
		if (mProgressDialog != null && mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}

		mProgressDialog = new ProgressDialog(ChannelsActivity.this,
				ProgressDialog.THEME_HOLO_DARK);
		mProgressDialog.setMessage("Connectiong to Server...");
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

		String androidId = Settings.Secure.getString(getApplicationContext()
				.getContentResolver(), Settings.Secure.ANDROID_ID);
		mOBSClient.getMediaDevice(androidId, deviceCallBack);
	}

	final Callback<DeviceDatum> deviceCallBack = new Callback<DeviceDatum>() {

		@Override
		public void success(DeviceDatum device, Response arg1) {
			// Log.d("ChannelsActivity","success");
			if (!mIsReqCanceled) {
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
				if (device != null) {
					mApplication.setBalance(mBalance = device
							.getBalanceAmount());
					if (mBalance >= 0)
						Toast.makeText(ChannelsActivity.this,
								"Insufficient Balance.Please Make a Payment.",
								Toast.LENGTH_LONG).show();
					else {
						getServices();
					}
				}
			}
		}

		@Override
		public void failure(RetrofitError retrofitError) {
			// Log.d("ChannelsActivity","failure");
			if (!mIsReqCanceled) {
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
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.ch_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			break;
		case R.id.action_refresh:
			// initiallizing req criteria
			mReqType = ServiceProvider.SERVICES_ON_REFRESH;
			mSearchString = null;
			CheckBalancenGetData();
			break;
		case R.id.action_search:
			// The searchbar is initiated programmatically with a call to your
			// Activity’s onSearchRequested method.
			onSearchRequested();
			break;
		case R.id.action_favourite:
			mReqType = ServiceProvider.SERVICES;
			mSelection = DBHelper.SERVICE_KEY_FAVOURITE + "=1";
			mSelectionArgs = null;
			CheckBalancenGetData();
			break;
		case R.id.action_channels:
			mReqType = ServiceProvider.SERVICES;
			mSelection = null;
			mSelectionArgs = null;
			CheckBalancenGetData();
			break;
		default:
			break;
		}
		return true;
	}

	public void CheckCacheForEpgDetails(String channelName) {
		String Epg_Dtls_key = channelName + "_EPG_Details";
		String Epg_Dtls_value = mPrefs.getString(Epg_Dtls_key, "");
		String req_date_Dtls = null;
		boolean getServerData = false;
		getServerData = mIsLiveDataReq;
		if (!mIsLiveDataReq) {
			if (Epg_Dtls_value.length() == 0) {
				getServerData = true;
			} else {
				JSONObject json = null;
				try {
					json = new JSONObject(Epg_Dtls_value);
					req_date_Dtls = json.getString(mDate);
				} catch (JSONException e) {
					req_date_Dtls = "";
					e.printStackTrace();
				}
				if (req_date_Dtls == null || req_date_Dtls.length() == 0) {
					getServerData = true;
				} else {
					getServerData = false;
				}
			}
		}
		if (getServerData) {
			try {
				getEpgDetailsFromServer(channelName);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			updateEPGDetails(getEPGListFromJSON(req_date_Dtls));
		}
	}

	private List<EpgDatum> getEPGListFromJSON(String json) {
		java.lang.reflect.Type t = new TypeToken<List<EpgDatum>>() {
		}.getType();
		List<EpgDatum> EPGList = new Gson().fromJson(json, t);
		return EPGList;
	}

	private void getEpgDetailsFromServer(String channelName) {

		mOBSClient.getEPGDetails(channelName, mDate, getEPGDetailsCallBack);

	}

	final Callback<EPGData> getEPGDetailsCallBack = new Callback<EPGData>() {
		@Override
		public void failure(RetrofitError retrofitError) {
			Log.e("EPGDetails",
					(null != retrofitError.getMessage() ? retrofitError
							.getMessage() : "Error On Epg details"));
		}

		@Override
		public void success(EPGData data, Response response) {
			List<EpgDatum> ProgGuideList = null;
			if (!mIsReqCanceled) {
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}

				if (data != null)
					ProgGuideList = data.getEpgData();
				if (ProgGuideList != null && ProgGuideList.size() > 0) {

					/** saving epg details to preferences */
					mPrefsEditor = mPrefs.edit();
					String Epg_Dtls_key = mService.getChannelName()
							+ "_EPG_Details";
					String Epg_Dtls_value = mPrefs.getString(Epg_Dtls_key, "");
					JSONObject json = null, jsonReq = null;
					try {
						if (Epg_Dtls_value.length() == 0) {
							json = new JSONObject();
						} else {
							json = new JSONObject(Epg_Dtls_value);
							jsonReq = new JSONObject();
							Calendar c = Calendar.getInstance();
							Calendar curr = Calendar.getInstance();
							Date cDate = MyApplication.df
									.parse(MyApplication.df.format(curr
											.getTime()));
							Date keyDate = null;
							Iterator<String> i = json.keys();
							while (i.hasNext()) {
								String key = i.next();
								c.setTime(MyApplication.df.parse(key));
								keyDate = c.getTime();
								if (keyDate.compareTo(cDate) != -1) {
									jsonReq.put(key, json.get(key));
								}
							}
							json = jsonReq;
						}
						json.put(mDate, new Gson().toJson(ProgGuideList));
						Epg_Dtls_value = json.toString();
					} catch (JSONException e) {
						e.printStackTrace();
					} catch (ParseException e) {
						e.printStackTrace();
					}
					mPrefsEditor.putString(Epg_Dtls_key, Epg_Dtls_value);
					mPrefsEditor.commit();

					/** updating fragment **/
					updateEPGDetails(ProgGuideList);
				}/*
				 * else { mErrDialog.setMessage("EPG Data is not Available");
				 * mErrDialog.show(); }
				 */
			}
		}
	};

	public void updateEPGDetails(final List<EpgDatum> mProgGuideList) {

		if (mProgGuideList != null && mProgGuideList.size() > 0) {
			int currProgIdx = -1;
			for (int i = 0; i < mProgGuideList.size(); i++) {

				EpgDatum data = mProgGuideList.get(i);
				if (isCurrentProgramme(data)) {
					currProgIdx = i;
				}
			}

			if (currProgIdx != -1) {
				LinearLayout container = (LinearLayout) findViewById(R.id.a_Epg_container_ll);
				container.removeAllViews();
				for (int i = currProgIdx; i < mProgGuideList.size()
						&& i < currProgIdx + 5; i++) {
					EpgDatum data = mProgGuideList.get(i);

					LayoutInflater inflater = (LayoutInflater) mApplication
							.getApplicationContext().getSystemService(
									Context.LAYOUT_INFLATER_SERVICE);

					View child = inflater.inflate(
							R.layout.ch_epg_details_list_row, null);
					((TextView) child
							.findViewById(R.id.a_ch_epg_details_list_row_tv_start_time))
							.setText(data.getStartTime());
					((TextView) child
							.findViewById(R.id.a_ch_epg_details_list_row_tv_prog_title))
							.setText(data.getProgramTitle());
					container.addView(child);
				}
			}
		}

	}

	private boolean isCurrentProgramme(EpgDatum data) {

		String progStartTime = data.getStartTime();
		String progStopTime = data.getStopTime();
		SimpleDateFormat tf = new SimpleDateFormat("HH:mm:ss");
		Date time1 = null, time2 = null;
		Date time3 = new Date();
		String t3 = tf.format(time3);

		try {
			time1 = tf.parse(progStartTime);
			time2 = tf.parse(progStopTime);
			if (time1.compareTo(time2) > 0)
				time2 = tf.parse("24:00:00");
			time3 = tf.parse(t3);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if ((time3.compareTo(time1) > 0 || time3.compareTo(time1) == 0)
				&& (time3.compareTo(time2) < 0 || time3.compareTo(time2) == 0)) {
			return true;
		}
		return false;
	}

	/*
	 * private void updateChannels(final ArrayList<ServiceDatum> list) { if
	 * (list != null && list.size() > 0) { ChannelListAdapter adapter = new
	 * ChannelListAdapter(list, this); mListView.setAdapter(adapter);
	 * mListView.setSelection(0); mListView.setOnItemClickListener(this);
	 * mListView.setOnItemLongClickListener(this);
	 * mListView.setOnItemSelectedListener(this);
	 * OnChannelSelection(list.get(0));
	 * 
	 * } else mListView.setAdapter(null); }
	 */

	private void OnChannelSelection(ServiceDatum serviceDatum) {
		// Log.d("ChannelsActivity","OnChannelSelection");
		mService = serviceDatum;
		if (player.isPlaying())
			player.stop();
		player.reset();

		player.setAudioStreamType(AudioManager.STREAM_MUSIC);
		player.setVolume(1.0f, 1.0f);
		try {
			player.setDataSource(this, Uri.parse(serviceDatum.getUrl()));
		} catch (Exception e) {
			e.printStackTrace();
		}
		player.setOnPreparedListener(this);
		player.setOnErrorListener(this);
		player.prepareAsync();

		CheckCacheForEpgDetails(serviceDatum.getChannelName());
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
		// Log.d("ChannelsActivity","onPrepared");
		//mListView.setSelection(mSelectedIdx);
		// mListView.getSelectedItem();
		mp.start();
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {

		if (what == MediaPlayer.MEDIA_ERROR_UNKNOWN && extra == -2147483648) {

			Toast.makeText(
					getApplicationContext(),
					"Incorrect URL or Unsupported Media Format.Media player closed.",
					Toast.LENGTH_LONG).show();
		} else if (what == MediaPlayer.MEDIA_ERROR_UNKNOWN && extra == -1004) {

			Toast.makeText(
					getApplicationContext(),
					"Invalid Stream for this channel... Please try other channel",
					Toast.LENGTH_SHORT).show();
		} else {
			if (null != mService && null != mp)
				OnChannelSelection(mService);
		}

		return true;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// Log.d("ChannelsActivity","surfaceCreated");
		initiallizePlayer();
		player.setDisplay(holder);
		if (mSelectedIdx==-1) {
			initiallizeUI();
		} else{
			OnChannelSelection(getServiceFromCursor(((Cursor) mListView
					.getAdapter().getItem(mListView.getSelectedItemPosition()))));
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	}

	@Override
	protected void onPause() {
		if (player != null) {
			if (player.isPlaying())
				player.stop();
			player.release();
			player = null;
		}
		super.onPause();
	}
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// Log.d("ChannelsActivity","surfaceDestroyed");
		// TODO Auto-generated method stub
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		// Log.d("ChannelsActivity","onItemClick");
		if (player != null) {
			if (player.isPlaying())
				player.stop();
			player.release();
			player = null;
		}
		playChannel(getServiceFromCursor(((Cursor) parent.getAdapter().getItem(
				position))));
	}

	private void playChannel(ServiceDatum service) {
		Intent intent = new Intent();
		intent.putExtra("VIDEOTYPE", "LIVETV");
		intent.putExtra(CHANNEL_URL, service.getUrl());
		intent.putExtra("CHANNELID", service.getClientId());
		intent.putExtra("REQTYPE", mReqType);
		intent.putExtra("SELECTION", mSelection);
		intent.putExtra("SEARCHSTRING", mSearchString);
		mApplication.startPlayer(intent, ChannelsActivity.this);
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		// Log.d("ChannelsActivity","onItemSelected");
		if(position!=mSelectedIdx){
			mSelectedIdx = position;
		OnChannelSelection(getServiceFromCursor(((Cursor) parent.getAdapter()
				.getItem(position))));
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// TODO Auto-generated method stub

	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		if (mProgressDialog != null && mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
		mProgressDialog = new ProgressDialog(ChannelsActivity.this,
				ProgressDialog.THEME_HOLO_DARK);
		mProgressDialog.setMessage("Connectiong to Server...");
		mProgressDialog.setCanceledOnTouchOutside(false);
		mProgressDialog.setOnCancelListener(new OnCancelListener() {

			public void onCancel(DialogInterface arg0) {
				if (mProgressDialog.isShowing())
					mProgressDialog.dismiss();
			}
		});
		mProgressDialog.show();

		CursorLoader loader = null;
		if (id == ServiceProvider.SERVICES) {
			loader = new CursorLoader(this, ServiceProvider.SERVICES_URI, null,
					mSelection, mSelectionArgs, null);
		}
		if (id == ServiceProvider.SERVICES_ON_REFRESH) {
			loader = new CursorLoader(this,
					ServiceProvider.SERVICES_ONREFREFRESH_URI, null, null,
					null, null);
		}
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

		// Log.d("ChannelsActivity","onLoadFinished");
		if (mProgressDialog != null && mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
		adapter.swapCursor(cursor);
		mListView.setSelection(0);
		mListView.setSelected(true);
		cursor.moveToFirst();
		OnChannelSelection(getServiceFromCursor(cursor));
		
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {

	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		// Log.d("ChannelsActivity","onItemLongClick");
		mSelectedIdx = position;
		ServiceDatum data = getServiceFromCursor(((Cursor) parent.getAdapter()
				.getItem(position)));
		ContentValues values = new ContentValues();
		values.put(DBHelper.SERVICE_KEY_SERVICE_ID, data.getServiceId());
		values.put(DBHelper.SERVICE_KEY_CLIENT_ID, data.getClientId());
		values.put(DBHelper.SERVICE_KEY_CHANNEL_NAME, data.getChannelName());
		values.put(DBHelper.SERVICE_KEY_IMAGE, data.getImage());
		values.put(DBHelper.SERVICE_KEY_URL, data.getUrl());
		values.put(DBHelper.SERVICE_KEY_FAVOURITE, 1);
		getContentResolver().update(
				ServiceProvider.SERVICES_URI,
				values,
				DBHelper.SERVICE_KEY_SERVICE_ID + "="
						+ data.getServiceId().toString(), null);

		Toast.makeText(this,
				"Channel " + data.getChannelName() + " is added to Favourites",
				Toast.LENGTH_LONG).show();
		return true;
	}

	public static ServiceDatum getServiceFromCursor(Cursor c) {
		ServiceDatum service = new ServiceDatum();
		service.setServiceId(c.getInt(1));
		service.setClientId(c.getInt(2));
		service.setChannelName(c.getString(3));
		service.setImage(c.getString(4));
		service.setUrl(c.getString(5));
		return service;
	}
}
