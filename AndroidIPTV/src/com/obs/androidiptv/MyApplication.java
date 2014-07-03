package com.obs.androidiptv;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.json.JSONObject;

import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import com.nostra13.universalimageloader.cache.memory.impl.LruMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.obs.data.ServiceDatum;
import com.obs.database.DBHelper;
import com.obs.imagehandler.AuthImageDownloader;
import com.obs.retrofit.CustomUrlConnectionClient;
import com.obs.retrofit.OBSClient;
import com.paypal.android.sdk.payments.PayPalConfiguration;

@ReportsCrashes(formKey = "", // will not be used
mailTo = "shiva@openbillingsystem.com", // my email here
mode = ReportingInteractionMode.TOAST, resToastText = R.string.crash_toast_text)
public class MyApplication extends Application {
	public static String TAG = MyApplication.class.getName();
	public final String PREFS_FILE = "PREFS_FILE";
	public SharedPreferences prefs;
	public Editor editor;
	public static String tenentId;
	public static String basicAuth;
	public static String contentType;
	public static String API_URL;
	public static SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd",
			new Locale("en"));
	private float balance = 0;
	private String currency = null;
	public static String androidId;
	private String clientId = null;
	public boolean balanceCheck = false;
	private boolean isPayPalReq = false;
	private static String payPalClientID = null;
	//private static String payPalSecret = null;
//	public static boolean D = false; // need to delete this variable
	public static Player player = Player.NATIVE_PLAYER;
	public static PayPalConfiguration config = null;
	
	/** PayPal configurations */
	private static final String CONFIG_ENVIRONMENT = PayPalConfiguration.ENVIRONMENT_PRODUCTION;
    // note that these credentials will differ between live & sandbox environments.
	public static final int REQUEST_CODE_PAYMENT = 1;
	public static final int REQUEST_CODE_FUTURE_PAYMENT = 2;
	/**  Paypal configurations*/  
	
	@Override
	public void onCreate() {
		super.onCreate();

		// The following line triggers the initialization of ACRA
		ACRA.init(this);

		/** initializing the ImageLoader instance */
		DisplayImageOptions defaultOptions = new DisplayImageOptions.Builder()
				.showImageOnLoading(R.drawable.ic_default_ch)
				.showImageForEmptyUri(R.drawable.ic_default_ch)
				.showImageOnFail(R.drawable.ic_default_ch).cacheInMemory(true)
				// .displayer(new RoundedBitmapDisplayer(10))
				.cacheOnDisc(true).bitmapConfig(Bitmap.Config.RGB_565).build();

		ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(
				getApplicationContext())
				.imageDownloader(
						new AuthImageDownloader(getApplicationContext(), 3000,
								3000)).threadPoolSize(5)
				.threadPriority(Thread.NORM_PRIORITY)
				.memoryCache(new LruMemoryCache(2 * 1024 * 1024))
				.memoryCacheSize(2 * 1024 * 1024)
				.discCacheSize(50 * 1024 * 1024).discCacheFileCount(100)
				.defaultDisplayImageOptions(defaultOptions).build();

		ImageLoader.getInstance().init(config);

		/** initilizing request headers */
		API_URL = getString(R.string.server_url);
		tenentId = getString(R.string.tenent_id);
		basicAuth = getString(R.string.basic_auth);
		contentType = getString(R.string.content_type);

		prefs = getSharedPreferences(PREFS_FILE, 0);
		editor = prefs.edit();
		androidId = Settings.Secure.getString(getApplicationContext()
				.getContentResolver(), Settings.Secure.ANDROID_ID);
	}

	public boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo wifiNetwork = connectivityManager
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (wifiNetwork != null && wifiNetwork.isConnected()) {
			return true;
		}
		NetworkInfo mobileNetwork = connectivityManager
				.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		if (mobileNetwork != null && mobileNetwork.isConnected()) {
			return true;
		}
		NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
		if (activeNetwork != null && activeNetwork.isConnected()) {
			return true;
		}
		return false;
	}

	public OBSClient getOBSClient() {
		RestAdapter restAdapter = new RestAdapter.Builder()
				.setEndpoint(API_URL)
				.setLogLevel(RestAdapter.LogLevel.NONE)
				.setClient(
						new CustomUrlConnectionClient(
								tenentId, basicAuth, contentType)).build();
		return restAdapter.create(OBSClient.class);
	}

	public void PullnInsertServices(SQLiteDatabase db) {
		OBSClient mOBSClient = getOBSClient();
		prefs = getPrefs();
		editor = getEditor();
		db.delete(DBHelper.TABLE_SERVICES, null, null);
		ArrayList<ServiceDatum> serviceList = mOBSClient
				.getPlanServicesSync(this.getClientId());
		Collections.sort(serviceList);
		if (serviceList != null && serviceList.size() > 0) {
			/** saving channel details to preferences */
			Date date = new Date();
			String formattedDate = df.format(date);
			editor.putString(
					getResources().getString(R.string.channels_updated_at),
					formattedDate);
			editor.commit();
			// Begin the transaction
			db.beginTransaction();
			try {
				for (ServiceDatum service : serviceList) {
					String nullColumnHack = null;
					ContentValues values = new ContentValues();
					values.put(DBHelper.SERVICE_ID, service.getServiceId());
					values.put(DBHelper.CLIENT_ID, service.getClientId());
					values.put(DBHelper.CHANNEL_NAME, service.getChannelName());
					values.put(DBHelper.CHANNEL_DESC,
							service.getChannelDescription());
					values.put(DBHelper.CATEGORY, service.getCategory());
					values.put(DBHelper.SUB_CATEGORY, service.getSubCategory());
					values.put(DBHelper.IMAGE, service.getImage());
					values.put(DBHelper.URL, service.getUrl());
					db.insert(DBHelper.TABLE_SERVICES, nullColumnHack, values);
				}
				// Transaction is successful and all the records have been
				// inserted
				db.setTransactionSuccessful();
			} catch (Exception e) {
				Log.e("Error in transaction", e.toString());
			} finally {
				// End the transaction
				db.endTransaction();

			}
		}
	}

	public void InsertCategories(SQLiteDatabase db) {
		db.delete(DBHelper.TABLE_SERVICE_CATEGORIES, null, null);
		Cursor cursor = db.rawQuery("select DISTINCT " + DBHelper.CATEGORY
				+ " from " + DBHelper.TABLE_SERVICES +" where TRIM( "+DBHelper.CATEGORY+" )!='' AND "
						+DBHelper.CATEGORY+" is NOT NULL " , null);//+  " ORDER BY "+ DBHelper.CATEGORY + " ASC", null);
		if (cursor.getCount() > 0) {
			Date date = new Date();
			String formattedDate = df.format(date);
			editor.putString(
					getResources().getString(
							R.string.channels_category_updated_at),
					formattedDate);
			editor.commit();
			cursor.moveToFirst();
			int category_idx = cursor.getColumnIndexOrThrow(DBHelper.CATEGORY);
			try {
				db.beginTransaction();
				do {
					String category_name = cursor.getString(category_idx);
					ContentValues values = new ContentValues();
					values.put(DBHelper.CATEGORY, category_name);
					db.insert(DBHelper.TABLE_SERVICE_CATEGORIES, null, values);
				} while (cursor.moveToNext());
				db.setTransactionSuccessful();
			} catch (Exception e) {
				Log.e("Error in transaction", e.toString());
			} finally {
				// End the transaction
				db.endTransaction();
			}
		}
	}
	//select * from service_categories where trim(category) != '' and category is not null
	public void InsertSubCategories(SQLiteDatabase db) {
		db.delete(DBHelper.TABLE_SERVICE_SUB_CATEGORIES, null, null);
		Cursor cursor = db.rawQuery("select DISTINCT " + DBHelper.SUB_CATEGORY
				+ " from " + DBHelper.TABLE_SERVICES +" where TRIM( "+DBHelper.SUB_CATEGORY+" )!='' AND "
				+DBHelper.SUB_CATEGORY+" is NOT NULL ", null);// + " ORDER BY "+ DBHelper.SUB_CATEGORY + " ASC", null);
		if (cursor.getCount() > 0) {
			Date date = new Date();
			String formattedDate = df.format(date);
			editor.putString(
					getResources().getString(
							R.string.channels_sub_category_updated_at),
					formattedDate);
			editor.commit();
			cursor.moveToFirst();
			int category_idx = cursor
					.getColumnIndexOrThrow(DBHelper.SUB_CATEGORY);
			try {
				db.beginTransaction();
				do {
					String category_name = cursor.getString(category_idx);
					ContentValues values = new ContentValues();
					values.put(DBHelper.SUB_CATEGORY, category_name);
					db.insert(DBHelper.TABLE_SERVICE_SUB_CATEGORIES, null,
							values);
				} while (cursor.moveToNext());
				db.setTransactionSuccessful();
			} catch (Exception e) {
				Log.e("Error in transaction", e.toString());
			} finally {
				// End the transaction
				db.endTransaction();
			}
		}
	}

	public AlertDialog getConfirmDialog(final Context context) {
		AlertDialog.Builder builder = new AlertDialog.Builder(
				((Activity) context), AlertDialog.THEME_HOLO_LIGHT);
		builder.setIcon(R.drawable.ic_logo_confirm_dialog);
		builder.setTitle("Confirmation");
		builder.setMessage("Do you want to close the app?");
		builder.setCancelable(false);
		AlertDialog dialog = builder.create();
		dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int buttonId) {
					}
				});
		return dialog;
	}

	public enum Player {
		NATIVE_PLAYER, MXPLAYER
	}

	public enum SortBy {
		DEFAULT, CATEGORY, LANGUAGE,
	}
	public enum DoBGTasks {
		UPDATESERVICES,UPDATECLIENT_CONFIGS
	}

	public String getResponseOnSuccess(Response response) {
		try {
			return getJSONfromInputStream(response.getBody().in());
		} catch (Exception e) {
			Log.i(TAG, e.getMessage());
			return "Internal Server Error";
		}
	}

	public static String getJSONfromInputStream(InputStream in) {
		StringBuffer res = new StringBuffer();
		String msg = null;
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			while ((msg = br.readLine()) != null) {
				res.append(msg);
			}
		} catch (Exception e) {
			Log.i(TAG, e.getMessage());
		}
		if (res.length() > 0) {
			return res.toString();
		} else
			return msg;
	}

	public String getDeveloperMessage(RetrofitError retrofitError) {
		String msg = null;
		try {
			msg = getJSONfromInputStream(retrofitError.getResponse().getBody()
					.in());
			msg = new JSONObject(msg).getJSONArray("errors").getJSONObject(0)
					.getString("developerMessage");
		} catch (Exception e) {
			msg = "Internal Server Error";
			Log.i(TAG, e.getMessage());
		}
		return msg;
	}

	public float getBalance() {
		if (balance == 0) {
			balance = getPrefs().getFloat("BALANCE", 0);
		}
		return balance;
	}

	public void setBalance(float balance) {
		getEditor().putFloat("BALANCE", balance);
		Calendar c = Calendar.getInstance();
		String date = df.format(c.getTime()); // dt is now the new date
		getEditor()
				.putString(this.getString(R.string.balance_updated_at), date);
		getEditor().commit();
		this.balance = balance;
	}

	public String getClientId() {
		if (clientId == null || clientId.length() == 0) {
			clientId = getPrefs().getString("CLIENTID", "");
		}
		return clientId;
	}

	public void setClientId(String clientId) {
		getEditor().putString("CLIENTID", clientId);
		getEditor().commit();
		this.clientId = clientId;
	}

	public SharedPreferences getPrefs() {
		if (prefs == null)
			prefs = getSharedPreferences(PREFS_FILE, 0);
		return prefs;
	}

	public Editor getEditor() {
		if (editor == null)
			editor = prefs.edit();
		return editor;
	}

	public boolean isBalanceCheck() {
		return balanceCheck;
	}

	public void setBalanceCheck(boolean balanceCheck) {
		this.balanceCheck = balanceCheck;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}
	public boolean isPayPalReq() {
		return isPayPalReq;
	}

	public void setPayPalReq(boolean isPayPalReq) {
		this.isPayPalReq = isPayPalReq;
	}

	public String getPayPalClientID() {
		return payPalClientID;
	}

	public void setPayPalClientID(String paypalClientID) {
		payPalClientID = paypalClientID;
	}

	/*public String getPayPalSecret() {
		return payPalSecret;
	}

	public void setPayPalSecret(String payPalSecret) {
		this.payPalSecret = payPalSecret;
	}*/
	
public PayPalConfiguration getPaypalConfig(){
	config = new PayPalConfiguration()
    .environment(CONFIG_ENVIRONMENT)
    .clientId(payPalClientID)
    // The following are only used in PayPalFuturePaymentActivity.
    .merchantName(getResources().getString(R.string.app_name))
    .merchantPrivacyPolicyUri(Uri.parse("https://www.example.com/privacy"))
    .merchantUserAgreementUri(Uri.parse("https://www.example.com/legal"));
	
	return config;
	
}

}
