package com.obs.androidiptv;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.obs.data.ClientDatum;
import com.obs.data.RegClientRespDatum;
import com.obs.data.ResponseObj;
import com.obs.data.TemplateDatum;
import com.obs.retrofit.OBSClient;
import com.obs.utils.Utilities;

public class RegisterActivity extends Activity {

	public static String TAG = RegisterActivity.class.getName();
	private final static String NETWORK_ERROR = "Network error.";
	private ProgressDialog mProgressDialog;
	EditText et_MobileNumber;
	EditText et_FirstName;
	EditText et_LastName;
	EditText et_EmailId;
	String mCountry;
	String mState;
	String mCity;

	/** Boolean check for which request is processing */
	boolean mIsClientRegistered = false;
	boolean mIsHWAlocated = false;
	MyApplication mApplication = null;
	OBSClient mOBSClient;
	boolean mIsReqCanceled = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register);

		mApplication = ((MyApplication) getApplicationContext());
		mOBSClient = mApplication.getOBSClient(this);

		et_MobileNumber = (EditText) findViewById(R.id.a_reg_et_mobile_no);
		et_FirstName = (EditText) findViewById(R.id.a_reg_et_first_name);
		et_LastName = (EditText) findViewById(R.id.a_reg_et_last_name);
		et_EmailId = (EditText) findViewById(R.id.a_reg_et_email_id);

		getCountries();
	}

	private void getCountries() {

		Log.d(TAG, "getCountries");

		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
		mProgressDialog = new ProgressDialog(RegisterActivity.this,
				ProgressDialog.THEME_HOLO_DARK);
		mProgressDialog.setMessage("Connecting Server");
		mProgressDialog.setCanceledOnTouchOutside(false);
		mProgressDialog.setOnCancelListener(new OnCancelListener() {
			public void onCancel(DialogInterface arg0) {
				Log.d(TAG, "onCancel");

				if (mProgressDialog.isShowing())
					mProgressDialog.dismiss();
			}
		});
		mProgressDialog.show();
		mOBSClient.getTemplate(templateCallBack);
	}

	final Callback<TemplateDatum> templateCallBack = new Callback<TemplateDatum>() {
		@Override
		public void failure(RetrofitError retrofitError) {

			Log.d(TAG, "templateCallBack-failure");
			if (mProgressDialog != null) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}
			if (retrofitError.isNetworkError()) {
				Toast.makeText(
						RegisterActivity.this,
						getApplicationContext().getString(
								R.string.error_network), Toast.LENGTH_LONG)
						.show();
			} else {
				Toast.makeText(
						RegisterActivity.this,
						"Server Error : "
								+ retrofitError.getResponse().getStatus(),
						Toast.LENGTH_LONG).show();
			}
		}

		@Override
		public void success(TemplateDatum template, Response response) {

			Log.d(TAG, "templateCallBack-success");
			if (mProgressDialog != null) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}
			try {
				mCountry = template.getAddressTemplateData().getCountryData()
						.get(0);
				 mState = template.getAddressTemplateData().getStateData().get(0);
				 mCity = template.getAddressTemplateData().getCityData().get(0);				
			} catch (Exception e) {
				Log.d("templateCallBack-success", e.getMessage());
				Toast.makeText(RegisterActivity.this,
						"Server Error : Country Name not Specified",
						Toast.LENGTH_LONG).show();
			}
		}
	};

	public void btnSubmit_onClick(View v) {

		String email = et_EmailId.getText().toString().trim();
		String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
		if (et_MobileNumber.getText().toString().length() <= 0) {
			Toast.makeText(RegisterActivity.this, "Please enter Mobile Number",
					Toast.LENGTH_LONG).show();
		} else if (et_FirstName.getText().toString().length() <= 0) {
			Toast.makeText(RegisterActivity.this, "Please enter First Name",
					Toast.LENGTH_LONG).show();
		} else if (et_LastName.getText().toString().length() <= 0) {
			Toast.makeText(RegisterActivity.this, "Please enter Last Name",
					Toast.LENGTH_LONG).show();
		} else if (email.matches(emailPattern)) {
			ClientDatum client = new ClientDatum();
			client.setPhone(et_MobileNumber.getText().toString());
			client.setFirstname(et_FirstName.getText().toString());
			client.setLastname(et_LastName.getText().toString());
			client.setCountry(mCountry);
			client.setState(mState);
			client.setCity(mCity);
			client.setEmail(et_EmailId.getText().toString());
			DoOnBackgroundAsyncTask task = new DoOnBackgroundAsyncTask();
			task.execute(client);
		} else {
			Toast.makeText(RegisterActivity.this,
					"Please enter valid Email Id", Toast.LENGTH_LONG).show();
		}
	}

	public void btnCancel_onClick(View v) {
		closeApp();
	}

	private void closeApp() {
		AlertDialog mConfirmDialog = mApplication.getConfirmDialog(this);
		mConfirmDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (mProgressDialog != null) {
							mProgressDialog.dismiss();
							mProgressDialog = null;
						}
						mIsReqCanceled = true;
						RegisterActivity.this.finish();
					}
				});
		mConfirmDialog.show();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK) || keyCode == 4) {
			closeApp();
		} else if (keyCode == 23) {
			View focusedView = getWindow().getCurrentFocus();
			focusedView.performClick();
		}
		return super.onKeyDown(keyCode, event);
	}

	private class DoOnBackgroundAsyncTask extends
			AsyncTask<ClientDatum, Void, ResponseObj> {
		ClientDatum clientData;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			if (mProgressDialog != null) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}
			mProgressDialog = new ProgressDialog(RegisterActivity.this,
					ProgressDialog.THEME_HOLO_DARK);
			mProgressDialog.setMessage("Registering Details");
			mProgressDialog.setCanceledOnTouchOutside(false);
			mProgressDialog.setOnCancelListener(new OnCancelListener() {

				public void onCancel(DialogInterface arg0) {
					if (mProgressDialog.isShowing())
						mProgressDialog.dismiss();
					String msg = "";
					if (mIsClientRegistered && !mIsHWAlocated) {
						msg = "Client Registration Success.Hardware not Allocated.";
						Toast.makeText(RegisterActivity.this, msg,
								Toast.LENGTH_LONG).show();
					}
					if (!mIsClientRegistered) {
						msg = "Client Registration Failed.";
						Toast.makeText(RegisterActivity.this, msg,
								Toast.LENGTH_LONG).show();
					}
					cancel(true);
				}
			});
			mProgressDialog.show();
		}

		@Override
		protected ResponseObj doInBackground(ClientDatum... arg0) {
			ResponseObj resObj = new ResponseObj();
			clientData = (ClientDatum) arg0[0];
			if (mApplication.isNetworkAvailable()) {
				HashMap<String, String> map = new HashMap<String, String>();
				map.put("TagURL", "/clients");
				map.put("officeId", "1");
				map.put("dateFormat", "dd MMMM yyyy");
				map.put("lastname", clientData.getLastname());
				map.put("firstname", clientData.getFirstname());
				map.put("middlename", "");
				map.put("locale", "en");
				map.put("fullname", "");
				map.put("externalId", "");
				map.put("clientCategory", "20");
				map.put("active", "false");
				map.put("flag", "false");
				map.put("activationDate", "");
				map.put("addressNo", "ghcv");
				map.put("street", "#23");
				map.put("city", clientData.getCity());
				map.put("state", clientData.getState());//"ANDHRA PRADESH");// "Akershus");//"Drenth");//
				map.put("country", clientData.getCountry());
				map.put("zipCode", "436346");
				map.put("phone", clientData.getPhone());
				map.put("email", clientData.getEmail());
				resObj = Utilities.callExternalApiPostMethod(
						getApplicationContext(), map);
			} else {
				resObj.setFailResponse(100, NETWORK_ERROR);
			}
			if (resObj.getStatusCode() == 200) {
				mIsClientRegistered = true;
				RegClientRespDatum clientResData = readJsonUser(resObj
						.getsResponse());
				mApplication.setClientId(Long.toString(clientResData
						.getClientId()));
				if (mApplication.isNetworkAvailable()) {
					HashMap<String, String> map = new HashMap<String, String>();
					map.put("TagURL",
							"/ownedhardware/" + mApplication.getClientId());
					map.put("itemType", "1");
					map.put("dateFormat", "dd MMMM yyyy");
					String androidId = Settings.Secure.getString(
							getApplicationContext().getContentResolver(),
							Settings.Secure.ANDROID_ID);
					map.put("serialNumber", androidId);
					map.put("provisioningSerialNumber", "PROVISIONINGDATA");
					Date date = new Date();
					SimpleDateFormat df = new SimpleDateFormat("dd MMMM yyyy",
							new Locale("en"));
					String formattedDate = df.format(date);
					map.put("allocationDate", formattedDate);
					map.put("locale", "en");
					map.put("status", "");
					resObj = Utilities.callExternalApiPostMethod(
							getApplicationContext(), map);
					if (resObj.getStatusCode() == 200)
						mIsHWAlocated = true;
				} else {
					resObj.setFailResponse(100, NETWORK_ERROR);
				}
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
				Intent intent = new Intent(RegisterActivity.this,
						PlanActivity.class);
				RegisterActivity.this.finish();
				startActivity(intent);
			} else {
				Toast.makeText(RegisterActivity.this,
						"Server Error : " + resObj.getsErrorMessage(),
						Toast.LENGTH_LONG).show();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	private RegClientRespDatum readJsonUser(String jsonText) {

		Log.d("readJsonUser", "result is " + jsonText);

		Gson gson = new Gson();
		RegClientRespDatum response = gson.fromJson(jsonText,
				RegClientRespDatum.class);
		return response;
	}
}
