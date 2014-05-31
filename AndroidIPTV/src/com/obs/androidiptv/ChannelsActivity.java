package com.obs.androidiptv;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.obs.androidiptv.MyApplication.SortBy;
import com.obs.data.DeviceDatum;
import com.obs.data.EPGData;
import com.obs.data.EpgDatum;
import com.obs.data.ResponseObj;
import com.obs.data.ServiceDatum;
import com.obs.database.DBHelper;
import com.obs.database.ServiceProvider;
import com.obs.retrofit.OBSClient;
import com.obs.utils.Utilities;
import com.paypal.android.sdk.payments.PayPalPayment;
import com.paypal.android.sdk.payments.PayPalService;
import com.paypal.android.sdk.payments.PaymentActivity;
import com.paypal.android.sdk.payments.PaymentConfirmation;

public class ChannelsActivity extends Activity implements
		SurfaceHolder.Callback, MediaPlayer.OnPreparedListener,
		MediaPlayer.OnErrorListener, ChannelsByDefOrderFrag.Callbacks,
		ChannelsBySortOrderFrag.Callbacks {
	public final static String CHANNEL_URL = "URL";
	private SharedPreferences mPrefs;
	private ProgressDialog mProgressDialog;
	private Editor mPrefsEditor;

	public static String mDate;
	MyApplication mApplication = null;
	OBSClient mOBSClient;
	boolean mIsReqCanceled = false;

	boolean mIsLiveDataReq = false;
	boolean mIsBalCheckReq;
	boolean mIsPayPalReq = true;
	float mBalance;
	String mPaymentStatus = null;

	String mSearchString;
	String mSelection;
	String[] mSelectionArgs;
	boolean mIsRefresh = false;

	SurfaceView videoSurface;
	MediaPlayer player;
	VideoControllerView controller;
	String mChannelName;
	String mChannelUrl;
	int mSelectionIdx = -1;
	ServiceDatum mService = null;
	ListView lv = null;
	AlertDialog mConfirmDialog;

	public static int mSortBy = SortBy.CATEGORY.ordinal();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_channels);

		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		videoSurface = (SurfaceView) findViewById(R.id.a_channels_videoSurface);
		SurfaceHolder videoHolder = videoSurface.getHolder();
		videoHolder.addCallback(this);
		player = new MediaPlayer();

		mApplication = ((MyApplication) getApplicationContext());
		mOBSClient = mApplication.getOBSClient(this);
		mPrefs = mApplication.getPrefs();
		mIsBalCheckReq = mApplication.isBalanceCheck();
		mIsPayPalReq = mApplication.isPayPalReq();

		Calendar c = Calendar.getInstance();
		mDate = mApplication.df.format(c.getTime()); // dt is now the new date

	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		// Log.d("ChannelsActivity", "onNewIntent");
		if (null != intent && null != intent.getAction()
				&& intent.getAction().equals(Intent.ACTION_SEARCH)) {

			// initiallizing req criteria
			mSearchString = intent.getStringExtra(SearchManager.QUERY);
			mSelection = DBHelper.CHANNEL_DESC + " LIKE ?";
			mSelectionArgs = new String[] { "%" + mSearchString + "%" };

			// initializing player
			initiallizePlayer();
			SurfaceHolder videoHolder = videoSurface.getHolder();
			videoHolder.addCallback(this);
			player.setDisplay(videoHolder);

			CheckBalancenGetData();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.ch_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		mSelection = null;
		mSearchString = null;
		mSelectionArgs = null;
		mIsRefresh = false;
		updateEPGDetails(null);
		if (null != player)
			player.reset();
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			break;
		case R.id.action_refresh:
			mIsRefresh = true;
			CheckBalancenGetData();
			break;
		case R.id.action_search:
			onSearchRequested();
			break;
		case R.id.action_favourite:
			mSelection = DBHelper.IS_FAVOURITE + "=1";
			CheckBalancenGetData();
			break;
		case R.id.action_channels:
			CheckBalancenGetData();
			break;
		case R.id.action_sort_by_default:
			mSortBy = SortBy.DEFAULT.ordinal();
			CheckBalancenGetData();
			break;
		case R.id.action_sort_by_categ:
			mSortBy = SortBy.CATEGORY.ordinal();
			CheckBalancenGetData();
			break;
		case R.id.action_sort_by_lang:
			mSortBy = SortBy.LANGUAGE.ordinal();
			CheckBalancenGetData();
			break;
		default:
			break;
		}
		return true;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		initiallizePlayer();
		player.setDisplay(holder);

		if (mSelectionIdx == -1) {
			CheckBalancenGetData();
		} else {
			if (mService != null && mConfirmDialog == null)
				OnChannelSelection(mService.getUrl(), mService.getChannelName());
		}
	}

	private void CheckBalancenGetData() {
		// Log.d("ChannelsActivity","CheckBalancenGetData");
		if (mIsBalCheckReq)
			validateDevice();
		else
			initiallizeUI();
	}

	private void validateDevice() {

		if (isRemoteDeviceValidationReq()) {
			// Log.d("ChannelsActivity","validateDevice");
			if (mProgressDialog != null && mProgressDialog.isShowing()) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}

			mProgressDialog = new ProgressDialog(this,
					ProgressDialog.THEME_HOLO_DARK);
			mProgressDialog.setMessage("Connectiong to Server...");
			mProgressDialog.setCanceledOnTouchOutside(false);
			mProgressDialog.setOnCancelListener(new OnCancelListener() {

				public void onCancel(DialogInterface arg0) {
					if (mProgressDialog.isShowing())
						mProgressDialog.dismiss();
					mProgressDialog = null;
					mIsReqCanceled = true;
				}
			});
			mProgressDialog.show();

			String androidId = Settings.Secure.getString(this
					.getApplicationContext().getContentResolver(),
					Settings.Secure.ANDROID_ID);
			mOBSClient.getMediaDevice(androidId, deviceCallBack);
		} else {
			initiallizeUI();
		}
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
					try {
						mApplication.setClientId(Long.toString(device
								.getClientId()));
						mApplication.setBalance(device.getBalanceAmount());
						mApplication.setBalanceCheck(device.isBalanceCheck());
						mApplication.setCurrency(device.getCurrency());
						boolean isPayPalReq = device.getPaypalConfigData()
								.getEnabled();
						mApplication.setPayPalReq(isPayPalReq);
						if (isPayPalReq) {
							String value = device.getPaypalConfigData()
									.getValue();
							try {
								JSONObject json = new JSONObject(value);
								if (json != null) {
									mApplication.setPayPalClientID(json.get(
											"clientId").toString());
									mApplication.setPayPalSecret(json.get(
											"secretCode").toString());
								}
							} catch (JSONException e) {
								Log.e("ChannelsActivity",
										(e.getMessage() == null) ? "Json Exception"
												: e.getMessage());
								Toast.makeText(ChannelsActivity.this,
										"Invalid Data-Json Exception",
										Toast.LENGTH_LONG).show();
							}

						}
					} catch (NullPointerException npe) {
						Log.e("ChannelsActivity",
								(npe.getMessage() == null) ? "NPE Exception"
										: npe.getMessage());
						Toast.makeText(ChannelsActivity.this,
								"Invalid Data-NPE Error", Toast.LENGTH_LONG)
								.show();
					} catch (Exception e) {
						Log.e("ChannelsActivity",
								(e.getMessage() == null) ? "Exception" : e
										.getMessage());
						Toast.makeText(ChannelsActivity.this,
								"Invalid Data-Error", Toast.LENGTH_LONG).show();
					}
					initiallizeUI();
				}
			}
			mIsReqCanceled = false;
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
					Toast.makeText(ChannelsActivity.this,
							getString(R.string.error_network),
							Toast.LENGTH_LONG).show();
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
			mIsReqCanceled = false;
		}
	};

	private void initiallizeUI() {
		// get balance
		mBalance = mApplication.getBalance();

		// Log.d("ChannelsActivity","initiallizeUI");
		Bundle b = new Bundle();
		b.putString("SEARCHSTRING", mSearchString);
		b.putString("SELECTION", mSelection);
		b.putInt("SORTBY", mSortBy);
		b.putBoolean("ISREFRESH", mIsRefresh);
		FragmentTransaction transaction = getFragmentManager()
				.beginTransaction();
		Fragment oldDefFrag = getFragmentManager().findFragmentByTag(
				"SORT_BY_DEFAULT");
		if (null != oldDefFrag)
			transaction.remove(oldDefFrag);
		Fragment oldSortedFrag = getFragmentManager().findFragmentByTag(
				"SORT_BY");
		if (null != oldSortedFrag)
			transaction.remove(oldSortedFrag);
		if (mSortBy == SortBy.DEFAULT.ordinal()) {
			Fragment defFrag = new ChannelsByDefOrderFrag();
			defFrag.setArguments(b);
			transaction.add(R.id.ch_frag_container, defFrag, "SORT_BY_DEFAULT");
		} else {
			Fragment frag = new ChannelsBySortOrderFrag();
			frag.setArguments(b);
			transaction.add(R.id.ch_frag_container, frag, "SORT_BY");
		}
		transaction.commit();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
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
					String Epg_Dtls_key = mChannelName + "_EPG_Details";
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
				} else {
					updateEPGDetails(null);
				}

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
		} else {
			LinearLayout container = (LinearLayout) findViewById(R.id.a_Epg_container_ll);
			container.removeAllViews();
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

	private void OnChannelSelection(String url, String channelName) {
		mChannelName = channelName;
		mChannelUrl = url;
		if (mIsBalCheckReq == true && (mBalance > 0)) {
			AlertDialog.Builder builder = new AlertDialog.Builder((this),
					AlertDialog.THEME_HOLO_LIGHT);
			builder.setIcon(R.drawable.ic_logo_confirm_dialog);
			builder.setTitle("Confirmation");
			String msg = "Insufficient Balance."
					+ (mIsPayPalReq == true ? "Go to PayPal ??"
							: "Please do Payment.");
			builder.setMessage(msg);
			builder.setCancelable(true);
			mConfirmDialog = builder.create();
			mConfirmDialog.setButton(AlertDialog.BUTTON_NEGATIVE,
					(mIsPayPalReq == true ? "No" : ""),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int buttonId) {
						}
					});
			mConfirmDialog.setButton(AlertDialog.BUTTON_POSITIVE,
					(mIsPayPalReq == true ? "Yes" : "Ok"),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (mIsPayPalReq == true) {
								Intent svcIntent = new Intent(
										ChannelsActivity.this,
										PayPalService.class);

								svcIntent
										.putExtra(
												PayPalService.EXTRA_PAYPAL_CONFIGURATION,
												mApplication.config);

								startService(svcIntent);

								PayPalPayment paymentData = new PayPalPayment(
										new BigDecimal(mBalance), mApplication
												.getCurrency(),
										"AndroidIPTV-Payment",
										PayPalPayment.PAYMENT_INTENT_SALE);

								Intent actviIntent = new Intent(
										ChannelsActivity.this,
										PaymentActivity.class);

								actviIntent.putExtra(
										PaymentActivity.EXTRA_PAYMENT,
										paymentData);

								startActivityForResult(actviIntent,
										mApplication.REQUEST_CODE_PAYMENT);
							}
						}
					});
			mConfirmDialog.show();
		} else {
			// Log.d("ChannelsActivity","OnChannelSelection");

			if (player.isPlaying())
				player.stop();
			player.reset();

			player.setAudioStreamType(AudioManager.STREAM_MUSIC);
			player.setVolume(1.0f, 1.0f);
			try {
				player.setDataSource(this, Uri.parse(url));
			} catch (Exception e) {
				e.printStackTrace();
			}
			player.setOnPreparedListener(this);
			player.setOnErrorListener(this);
			player.prepareAsync();

			CheckCacheForEpgDetails(channelName);
		}
	}

	@Override
	public void onPrepared(MediaPlayer mp) {
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
			if (null != mChannelUrl && null != mp)
				OnChannelSelection(mChannelUrl, mChannelName);
		}

		return true;
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
	}

	private void playChannel(ServiceDatum data, int sortBy, String selection,
			String searchString) {
		Intent intent = new Intent();
		intent.putExtra("VIDEOTYPE", "LIVETV");
		intent.putExtra(CHANNEL_URL, data.getUrl());
		intent.putExtra("CHANNELID", data.getServiceId()); // int
		intent.putExtra("SORTBY", sortBy); // int
		intent.putExtra("SELECTION", selection);
		intent.putExtra("SEARCHSTRING", searchString);
		// for player.
		switch (MyApplication.player) {
		case NATIVE_PLAYER:
			intent.setClass(getApplicationContext(), VideoPlayerActivity.class);
			startActivity(intent);
			break;
		case MXPLAYER:
			intent.setClass(getApplicationContext(), MXPlayerActivity.class);
			startActivity(intent);
			break;
		default:
			intent.setClass(getApplicationContext(), VideoPlayerActivity.class);
			startActivity(intent);
			break;
		}
	}

	@Override
	public void onItemSelected(ServiceDatum data, int selIdx) {
		mSelectionIdx = selIdx;
		mService = data;
		mChannelName = data.getChannelName();
		OnChannelSelection(data.getUrl(), data.getChannelName());
	}

	@Override
	public void onItemClick(ServiceDatum data, int selIdx, int sortBy,
			String selection, String searchString) {
		if (mIsBalCheckReq == true && (mBalance > 0)) {
			AlertDialog.Builder builder = new AlertDialog.Builder((this),
					AlertDialog.THEME_HOLO_LIGHT);
			builder.setIcon(R.drawable.ic_logo_confirm_dialog);
			builder.setTitle("Confirmation");
			String msg = "Insufficient Balance."
					+ (mIsPayPalReq == true ? "Go to PayPal ??"
							: "Please do Payment.");
			builder.setMessage(msg);
			builder.setCancelable(true);
			mConfirmDialog = builder.create();
			mConfirmDialog.setCancelable(true);
			mConfirmDialog.setButton(AlertDialog.BUTTON_NEGATIVE,
					(mIsPayPalReq == true ? "No" : ""),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int buttonId) {
						}
					});
			mConfirmDialog.setButton(AlertDialog.BUTTON_POSITIVE,
					(mIsPayPalReq == true ? "Yes" : "Ok"),
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (mIsPayPalReq == true) {
								Intent svcIntent = new Intent(
										ChannelsActivity.this,
										PayPalService.class);

								svcIntent
										.putExtra(
												PayPalService.EXTRA_PAYPAL_CONFIGURATION,
												mApplication.config);

								startService(svcIntent);

								PayPalPayment paymentData = new PayPalPayment(
										new BigDecimal(mBalance), mApplication
												.getCurrency(),
										"AndroidIPTV-Payment",
										PayPalPayment.PAYMENT_INTENT_SALE);

								Intent actviIntent = new Intent(
										ChannelsActivity.this,
										PaymentActivity.class);

								actviIntent.putExtra(
										PaymentActivity.EXTRA_PAYMENT,
										paymentData);

								startActivityForResult(actviIntent,
										mApplication.REQUEST_CODE_PAYMENT);
							}
						}
					});
			mConfirmDialog.show();
		} else {
			mSelectionIdx = selIdx;
			mService = data;
			if (player != null) {
				if (player.isPlaying())
					player.stop();
				player.release();
				player = null;
			}
			playChannel(data, sortBy, selection, searchString);
		}
	}

	@Override
	public void onItemLongClick(ServiceDatum data, int selIdx) {
		// Log.d("ChannelsActivity","onItemLongClick");
		mSelectionIdx = selIdx;
		mService = data;
		ContentValues values = new ContentValues();
		values.put(DBHelper.SERVICE_ID, data.getServiceId());
		values.put(DBHelper.CLIENT_ID, data.getClientId());
		values.put(DBHelper.CHANNEL_NAME, data.getChannelName());
		values.put(DBHelper.CHANNEL_DESC, data.getChannelDescription());
		values.put(DBHelper.CATEGORY, data.getCategory());
		values.put(DBHelper.SUB_CATEGORY, data.getSubCategory());
		values.put(DBHelper.IMAGE, data.getImage());
		values.put(DBHelper.URL, data.getUrl());
		values.put(DBHelper.IS_FAVOURITE, 1);
		getContentResolver().update(ServiceProvider.SERVICES_URI, values,
				DBHelper.SERVICE_ID + "=" + data.getServiceId().toString(),
				null);
		Toast.makeText(
				this,
				"Channel " + data.getChannelDescription()
						+ " is added to Favourites", Toast.LENGTH_LONG).show();

	}

	public static ServiceDatum getServiceFromCursor(Cursor c) {
		int Id_idx = c.getColumnIndex(DBHelper.SERVICE_ID);
		int ClientId_idx = c.getColumnIndex(DBHelper.CLIENT_ID);
		int ChanName_idx = c.getColumnIndex(DBHelper.CHANNEL_NAME);
		int ChanDesc_idx = c.getColumnIndex(DBHelper.CHANNEL_DESC);
		int Img_idx = c.getColumnIndex(DBHelper.IMAGE);
		int Url_idx = c.getColumnIndex(DBHelper.URL);
		int Categ_idx = c.getColumnIndex(DBHelper.CATEGORY);
		int SubCateg_idx = c.getColumnIndex(DBHelper.SUB_CATEGORY);

		ServiceDatum service = new ServiceDatum();
		service.setServiceId(c.getInt(Id_idx));
		service.setClientId(c.getInt(ClientId_idx));
		service.setChannelName(c.getString(ChanName_idx));
		service.setChannelDescription(c.getString(ChanDesc_idx));
		service.setImage(c.getString(Img_idx));
		service.setUrl(c.getString(Url_idx));
		service.setCategory(c.getString(Categ_idx));
		service.setSubCategory(c.getString(SubCateg_idx));
		return service;
	}

	public boolean isRemoteDeviceValidationReq() {
		Calendar cal = Calendar.getInstance();
		String sCurrDate = mApplication.df.format(cal.getTime());

		String sUpdatedAt = mApplication.getPrefs().getString(
				getString(R.string.balance_updated_at), "");
		if (sUpdatedAt != null && sUpdatedAt.length() > 0) {
			try {
				Date dCurrDate = mApplication.df.parse(sCurrDate);
				Date dUpdatedAt = mApplication.df.parse(sUpdatedAt);

				if (dUpdatedAt.compareTo(dCurrDate) < 0) {
					return true;
				}
			} catch (ParseException e) {
				e.printStackTrace();
				return true;
			}
		} else
			return true;
		return false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		/** Stop PayPalIntent Service... */
		stopService(new Intent(this, PayPalService.class));
		if (mConfirmDialog != null && mConfirmDialog.isShowing()) {
			mConfirmDialog.dismiss();
		}
		if (resultCode == Activity.RESULT_OK) {
			PaymentConfirmation confirm = data
					.getParcelableExtra(PaymentActivity.EXTRA_RESULT_CONFIRMATION);
			if (confirm != null) {
				try {
					Log.i("OBSPayment", confirm.toJSONObject().toString(4));
					/** Call OBS API for verification and payment record. */
					OBSPaymentAsyncTask task = new OBSPaymentAsyncTask();
					task.execute(confirm.toJSONObject().toString(4));
				} catch (JSONException e) {
					Log.e("OBSPayment",
							"an extremely unlikely failure occurred: ", e);
				}
			}
		} else if (resultCode == Activity.RESULT_CANCELED) {
			Log.i("OBSPayment", "The user canceled.");
			Toast.makeText(this, "The user canceled.", Toast.LENGTH_LONG)
					.show();
		} else if (resultCode == PaymentActivity.RESULT_EXTRAS_INVALID) {
			Log.i("OBSPayment",
					"An invalid Payment or PayPalConfiguration was submitted. Please see the docs.");
			Toast.makeText(this,
					"An invalid Payment or PayPalConfiguration was submitted",
					Toast.LENGTH_LONG).show();
		}
	}

	private class OBSPaymentAsyncTask extends
			AsyncTask<String, Void, ResponseObj> {
		JSONObject reqJson = null;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			if (mProgressDialog != null) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}
			mProgressDialog = new ProgressDialog(ChannelsActivity.this,
					ProgressDialog.THEME_HOLO_DARK);
			mProgressDialog.setMessage("Registering Details");
			mProgressDialog.setCanceledOnTouchOutside(false);
			mProgressDialog.setOnCancelListener(new OnCancelListener() {

				public void onCancel(DialogInterface arg0) {
					if (mProgressDialog.isShowing())
						mProgressDialog.dismiss();

					Toast.makeText(ChannelsActivity.this,
							"Payment verification Failed.", Toast.LENGTH_LONG)
							.show();
					cancel(true);
				}
			});
			mProgressDialog.show();
		}

		@Override
		protected ResponseObj doInBackground(String... arg) {
			ResponseObj resObj = new ResponseObj();
			try {
				reqJson = new JSONObject(arg[0]);

				if (mApplication.isNetworkAvailable()) {
					resObj = Utilities.callExternalApiPostMethod(
							getApplicationContext(),
							"/payments/paypalEnquirey/"
									+ mApplication.getClientId(), reqJson);
				} else {
					resObj.setFailResponse(100, "Network error.");
				}
			} catch (JSONException e) {
				Log.e("ChannelsActivity-ObsPaymentCheck",
						(e.getMessage() == null) ? "Json Exception" : e
								.getMessage());
				e.printStackTrace();
				Toast.makeText(ChannelsActivity.this,
						"Invalid data: On PayPal Payment ", Toast.LENGTH_LONG)
						.show();
			}
			if (mConfirmDialog != null && mConfirmDialog.isShowing()) {
				mConfirmDialog.dismiss();
			}
			return resObj;
		}

		@Override
		protected void onPostExecute(ResponseObj resObj) {

			super.onPostExecute(resObj);
			if (mProgressDialog.isShowing()) {
				mProgressDialog.dismiss();
			}

			if (resObj.getStatusCode() == 200) {
				if (resObj.getsResponse().length() > 0) {

					JSONObject json;
					try {
						json = new JSONObject(resObj.getsResponse());

						json = json.getJSONObject("changes");
						if (json != null) {
							mPaymentStatus = json.getString("paymentStatus");
							if (mPaymentStatus.equalsIgnoreCase("Success")) {
								mBalance = (float) json.getLong("totalBalance");
								mApplication.setBalance(mBalance);
								Toast.makeText(ChannelsActivity.this,
										"Payment Verification Success",
										Toast.LENGTH_LONG).show();
								OnChannelSelection(mChannelUrl, mChannelName);

							} else if (mPaymentStatus.equalsIgnoreCase("Fail")) {
								Toast.makeText(ChannelsActivity.this,
										"Payment Verification Failed",
										Toast.LENGTH_LONG).show();
							}
						}

					} catch (JSONException e) {
						Toast.makeText(ChannelsActivity.this, "Server Error",
								Toast.LENGTH_LONG).show();
						Log.i("ChannelsActivity",
								"JsonEXception at payment verification");
					} catch (NullPointerException e) {
						Toast.makeText(ChannelsActivity.this, "Server Error  ",
								Toast.LENGTH_LONG).show();
						Log.i("ChannelsActivity",
								"Null PointerEXception at payment verification");
					}

				}
			} else {
				Toast.makeText(ChannelsActivity.this, "Server Error",
						Toast.LENGTH_LONG).show();
			}
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		ListView lv = null;
		if (mSortBy == SortBy.DEFAULT.ordinal()) {
			lv = (ListView) findViewById(R.id.f_channels_lv);
		} else
			lv = (ExpandableListView) findViewById(R.id.elv);
		if (null != lv) {
			int position = lv.getSelectedItemPosition();
			if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
				if (position < lv.getCount() - 1) {
					position++;
					lv.requestFocus();
					lv.setSelection(position);
					if (position - 2 > 0)
						lv.smoothScrollToPosition(position - 2);
					return true;
				}
			} else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
				if (position > 0) {
					lv.requestFocus();
					position--;
					lv.setSelection(position);
					if (position + 2 <= lv.getCount())
						lv.smoothScrollToPosition(position + 2);
					return true;
				}
			}
		}
		return super.onKeyDown(keyCode, event);
	}
}
