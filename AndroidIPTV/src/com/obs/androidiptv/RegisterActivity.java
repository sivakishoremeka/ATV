package com.obs.androidiptv;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.animation.LayoutTransition;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.gson.Gson;
import com.obs.data.ActivePlanDatum;
import com.obs.data.ClientDatum;
import com.obs.data.RegClientRespDatum;
import com.obs.data.ResponseObj;
import com.obs.data.TemplateDatum;
import com.obs.retrofit.OBSClient;
import com.obs.utils.Utilities;

public class RegisterActivity extends Activity {

	// public static String TAG = RegisterActivity.class.getName();
	private final static String NETWORK_ERROR = "Network error.";
	private ProgressDialog mProgressDialog;
	private final static int LOGIN_LAYOUT = 0;
	private final static int REGISTRATION_LAYOUT = 1;
	private final static int LINK_USER_LAYOUT = 2;
	// login
	EditText et_login_EmailId;
	EditText et_Password;
	// register
	EditText et_MobileNumber;
	EditText et_FullName;
	Spinner sp_City;
	EditText et_EmailId;
	String mCountry;
	String mState;
	String mCity;

	int mCurrentLayout;
	MyApplication mApplication = null;
	OBSClient mOBSClient;
	boolean mIsReqCanceled = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register);
		mApplication = ((MyApplication) getApplicationContext());
		mOBSClient = mApplication.getOBSClient();
		mCurrentLayout = LOGIN_LAYOUT;
	}

	public void textRegister_onClick(View v) {
		mCurrentLayout = REGISTRATION_LAYOUT;
		setLayout(mCurrentLayout);
		et_FullName = (EditText) findViewById(R.id.a_reg_et_full_name);
		sp_City = (Spinner) findViewById(R.id.a_reg_sp_city);
		et_MobileNumber = (EditText) findViewById(R.id.a_reg_et_mobile_no);
		et_EmailId = (EditText) findViewById(R.id.a_reg_et_email_id);
		et_Password = (EditText) findViewById(R.id.a_reg_et_pwd);
		getCountries();
	}

	public void textLinkUser_onClick(View v) {
		mCurrentLayout = LINK_USER_LAYOUT;
		setLayout(mCurrentLayout);
	}

	public void textLogin_onClick(View v) {
		mCurrentLayout = LOGIN_LAYOUT;
		setLayout(mCurrentLayout);
	}

	public void btnLogin_onClick(View v) {

		// et_LastName = (EditText) findViewById(R.id.a_reg_et_last_name);
		String sEmailId = ((EditText) findViewById(R.id.a_reg_et_login_email_id))
				.getText().toString().trim();
		String sPassword = ((EditText) findViewById(R.id.a_reg_et_password))
				.getText().toString();

		String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
		if (sPassword.length() <= 0) {
			Toast.makeText(RegisterActivity.this, "Please enter Password",
					Toast.LENGTH_LONG).show();
		} else if (sEmailId.matches(emailPattern)) {
			SelfCareUserDatum data = new SelfCareUserDatum();
			data.email_id = sEmailId;
			data.password = sPassword;
			DoSelfCareLoginAsyncTask task2 = new DoSelfCareLoginAsyncTask();
			task2.execute(data);
		} else {
			Toast.makeText(RegisterActivity.this,
					"Please enter valid Email Id", Toast.LENGTH_LONG).show();
		}
	}

	private void setLayout(int Layout) {
		LinearLayout container = (LinearLayout) findViewById(R.id.a_reg_ll_container);
		LayoutTransition transition = new LayoutTransition();
		container.setLayoutTransition(transition);
		LayoutInflater inflater = this.getLayoutInflater();
		LinearLayout layout;
		if (mCurrentLayout == LINK_USER_LAYOUT) {
			layout = (LinearLayout) inflater.inflate(
					R.layout.a_reg_link_user_layout, null);
		} else if (mCurrentLayout == REGISTRATION_LAYOUT) {
			layout = (LinearLayout) inflater.inflate(
					R.layout.a_reg_registration_layout, null);
		} else {
			layout = (LinearLayout) inflater.inflate(
					R.layout.a_reg_login_layout, null);
		}
		layout.setLayoutParams(new LayoutParams(
				android.view.ViewGroup.LayoutParams.MATCH_PARENT,
				android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
		container.removeAllViews();
		container.addView(layout);
	}

	private void getCountries() {

		// Log.d(TAG, "getCountries");

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

			// Log.d(TAG, "templateCallBack-failure");
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

			// Log.d(TAG, "templateCallBack-success");
			if (mProgressDialog != null) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}
			try {
				mCountry = template.getAddressTemplateData().getCountryData()
						.get(0);
				mState = template.getAddressTemplateData().getStateData()
						.get(0);
				ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(
						RegisterActivity.this,
						R.layout.a_reg_city_spinner_item, template
								.getAddressTemplateData().getCityData());
				dataAdapter
						.setDropDownViewResource(R.layout.a_reg_city_spinner_dropdown_item);
				sp_City.setAdapter(dataAdapter);
			} catch (Exception e) {
				Log.e("templateCallBack-success", e.getMessage());
				Toast.makeText(RegisterActivity.this,
						"Server Error : Country Name not Specified",
						Toast.LENGTH_LONG).show();
			}
		}
	};

	public void btnRegister_onClick(View v) {

		String email = et_EmailId.getText().toString().trim();
		String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
		if (et_MobileNumber.getText().toString().length() <= 0) {
			Toast.makeText(RegisterActivity.this, "Please enter Mobile Number",
					Toast.LENGTH_LONG).show();
		} else if (et_FullName.getText().toString().length() <= 0) {
			Toast.makeText(RegisterActivity.this, "Please enter Full Name",
					Toast.LENGTH_LONG).show();
		}
		if (sp_City.getSelectedItem() == null) {
			Toast.makeText(RegisterActivity.this, "Please select City",
					Toast.LENGTH_LONG).show();
		}
		if (et_Password.getText().toString().length() <= 0) {
			Toast.makeText(RegisterActivity.this, "Please enter Password",
					Toast.LENGTH_LONG).show();
		} else if (email.matches(emailPattern)) {
			mCity = sp_City.getSelectedItem().toString();
			ClientDatum client = new ClientDatum();
			client.setPhone(et_MobileNumber.getText().toString());
			client.setFullname(et_FullName.getText().toString());
			client.setCountry(mCountry);
			client.setState(mState);
			client.setCity(mCity);
			client.setEmail(et_EmailId.getText().toString());
			client.setPassword(et_Password.getText().toString());
			DoRegistrationAsyncTask task = new DoRegistrationAsyncTask();
			task.execute(client);
		} else {
			Toast.makeText(RegisterActivity.this,
					"Please enter valid Email Id", Toast.LENGTH_LONG).show();
		}
	}

	public void btnLinkUser_onClick(View v) {
		mCurrentLayout = LINK_USER_LAYOUT;
		et_EmailId = (EditText) findViewById(R.id.a_reg_et_link_user_email_id);
		String email = et_EmailId.getText().toString().trim();
		String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
		if (email.matches(emailPattern)) {
			DoLinkUserAsyncTask task = new DoLinkUserAsyncTask();
			task.execute(email);
		} else {
			Toast.makeText(RegisterActivity.this,
					"Please enter valid Email Id", Toast.LENGTH_LONG).show();
		}
	}

	public void btnCancel_onClick(View v) {

		if (mCurrentLayout == LOGIN_LAYOUT)
			closeApp();
		else if (mCurrentLayout == REGISTRATION_LAYOUT) {
			mCurrentLayout = LOGIN_LAYOUT;
			setLayout(mCurrentLayout);
		} else if (mCurrentLayout == LINK_USER_LAYOUT) {
			mCurrentLayout = LOGIN_LAYOUT;
			setLayout(mCurrentLayout);
		}
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
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			if (mCurrentLayout == LOGIN_LAYOUT)
				closeApp();
			else if (mCurrentLayout == REGISTRATION_LAYOUT) {
				mCurrentLayout = LOGIN_LAYOUT;
				setLayout(mCurrentLayout);
			} else if (mCurrentLayout == LINK_USER_LAYOUT) {
				mCurrentLayout = LOGIN_LAYOUT;
				setLayout(mCurrentLayout);
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	private class DoRegistrationAsyncTask extends
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
			mProgressDialog.setMessage("Registering Details...");
			mProgressDialog.setCanceledOnTouchOutside(false);
			mProgressDialog.setOnCancelListener(new OnCancelListener() {

				public void onCancel(DialogInterface arg0) {
					if (mProgressDialog.isShowing())
						mProgressDialog.dismiss();
					String msg = "Client Registration Failed.";
					Toast.makeText(RegisterActivity.this, msg,
							Toast.LENGTH_LONG).show();
					cancel(true);
				}
			});
			mProgressDialog.show();
		}

		@Override
		protected ResponseObj doInBackground(ClientDatum... arg0) {
			ResponseObj resObj = new ResponseObj();
			clientData = (ClientDatum) arg0[0];

			String firstName = "";
			String fullName = "";
			String name[] = clientData.getFullname().split(" ", 2);
			if (name[0] != null && name[0].length() != 0) {
				fullName = firstName = name[0];
			}
			if (name.length > 1) {
				fullName = name[1];
			}

			if (mApplication.isNetworkAvailable()) {
				HashMap<String, String> map = new HashMap<String, String>();
				map.put("TagURL", "/activationprocess/selfregistration");// map.put("TagURL","/clients");
				String androidId = Settings.Secure.getString(
						getApplicationContext().getContentResolver(),
						Settings.Secure.ANDROID_ID);
				map.put("device", androidId);
				map.put("firstname", firstName);// clientData.getFullname());
				map.put("address", "none");
				map.put("isMailCheck", "N");
				// map.put("nationalId", "junk");
				map.put("fullname", fullName);// clientData.getFullname());
				map.put("deviceAgreementType", "OWN");
				map.put("city", clientData.getCity());
				map.put("password", clientData.getPassword());
				map.put("zipCode", "436346"); // junk
				map.put("phone", clientData.getPhone());
				map.put("email", clientData.getEmail());
				resObj = Utilities.callExternalApiPostMethod(
						getApplicationContext(), map);
			} else {
				resObj.setFailResponse(100, NETWORK_ERROR);
			}
			return resObj;
		}

		@Override
		protected void onPostExecute(ResponseObj resObj) {

			super.onPostExecute(resObj);
			if (mProgressDialog != null) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}

			if (resObj.getStatusCode() == 200) {
				RegClientRespDatum clientResData = readJsonUser(resObj
						.getsResponse());
				mApplication.setClientId(Long.toString(clientResData
						.getClientId()));
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

	private class DoLinkUserAsyncTask extends
			AsyncTask<String, Void, ResponseObj> {
		String clientEmailId;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			if (mProgressDialog != null) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}
			mProgressDialog = new ProgressDialog(RegisterActivity.this,
					ProgressDialog.THEME_HOLO_DARK);
			mProgressDialog.setMessage("Registering Details...");
			mProgressDialog.setCanceledOnTouchOutside(false);
			mProgressDialog.setOnCancelListener(new OnCancelListener() {

				public void onCancel(DialogInterface arg0) {
					if (mProgressDialog.isShowing())
						mProgressDialog.dismiss();
					String msg = "Client Registration Failed.";
					Toast.makeText(RegisterActivity.this, msg,
							Toast.LENGTH_LONG).show();
					cancel(true);
				}
			});
			mProgressDialog.show();
		}

		@Override
		protected ResponseObj doInBackground(String... arg0) {
			ResponseObj resObj = new ResponseObj();
			clientEmailId = (String) arg0[0];
			if (mApplication.isNetworkAvailable()) {
				HashMap<String, String> map = new HashMap<String, String>();
				map.put("TagURL", "/linkupaccount");// map.put("TagURL","/clients");
				map.put("userName", clientEmailId);
				resObj = Utilities.callExternalApiPostMethod(
						getApplicationContext(), map);
			} else {
				resObj.setFailResponse(100, NETWORK_ERROR);
			}
			return resObj;
		}

		@Override
		protected void onPostExecute(ResponseObj resObj) {

			super.onPostExecute(resObj);
			if (mProgressDialog != null) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}
			if (resObj.getStatusCode() == 200) {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						RegisterActivity.this, AlertDialog.THEME_HOLO_LIGHT);
				builder.setIcon(R.drawable.ic_logo_confirm_dialog);
				builder.setTitle("Information");
				builder.setMessage("Login details sent to your mail.Please check your mail.");
				builder.setCancelable(false);
				AlertDialog dialog = builder.create();
				dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Ok",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {

								// do nothing
							}
						});
				dialog.show();
				mCurrentLayout = LOGIN_LAYOUT;
				setLayout(mCurrentLayout);
			} else {
				Toast.makeText(RegisterActivity.this,
						"Server Error : " + resObj.getsErrorMessage(),
						Toast.LENGTH_LONG).show();
			}
		}
	}


	/**
	 * Async Task For Handling selfcare validation
	 **/

	private class DoSelfCareLoginAsyncTask extends
			AsyncTask<SelfCareUserDatum, Void, ResponseObj> {
		SelfCareUserDatum userData;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			if (mProgressDialog != null) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}
			mProgressDialog = new ProgressDialog(RegisterActivity.this,
					ProgressDialog.THEME_HOLO_DARK);
			mProgressDialog.setMessage("Connecting to Server...");
			mProgressDialog.setCanceledOnTouchOutside(false);
			mProgressDialog.setOnCancelListener(new OnCancelListener() {

				public void onCancel(DialogInterface arg0) {
					if (mProgressDialog.isShowing())
						mProgressDialog.dismiss();
					cancel(true);
				}
			});
			mProgressDialog.show();
		}

		@Override
		protected ResponseObj doInBackground(SelfCareUserDatum... arg0) {
			ResponseObj resObj = new ResponseObj();
			userData = (SelfCareUserDatum) arg0[0];
			if (mApplication.isNetworkAvailable()) {
				HashMap<String, String> map = new HashMap<String, String>();
				// https://192.168.1.104:7070/obsplatform/api/v1/selfcare/login?username="10@gmail.com"&password="wnrodihw"
				map.put("TagURL", "/selfcare/login?username="
						+ userData.email_id + "&password=" + userData.password);
				resObj = Utilities.callExternalApiPostMethod(
						getApplicationContext(), map);
			} else {
				resObj.setFailResponse(100, NETWORK_ERROR);
			}
			if (resObj.getStatusCode() == 200) {

				// gather client details..
				// {"clientData":{"balanceAmount":1440,"currency":"INR","balanceCheck":false}",
				// "paypalConfigData":{"name":"Is_Paypal","enabled":true,"value":"{\"clientId\" :AXND_hChmLdQyk_zr8fBoWc75_h2ixtuc6F6i9BOmIZOaSynNRSToc_2otSR,\"secretCode\" : \"EBUikxDpbaVroBsJV8StIpMpFQAUr5h-RkhFrJKscZJKU2zaSkgQK2KTbMSv\"}","id":17}}
				boolean isPayPalReq = false;
				JSONObject jResObj;
				try {
					jResObj = new JSONObject(resObj.getsResponse());

					JSONObject jClientData = jResObj
							.getJSONObject("clientData");

					mApplication.setClientId(jClientData.getString("id"));
					mApplication.setBalance(Float.parseFloat(jClientData
							.getString("balanceAmount")));
					mApplication.setBalanceCheck(jClientData
							.getBoolean("balanceCheck"));
					mApplication.setCurrency(jClientData.getString("currency"));

					JSONObject jPayPalData = jResObj
							.getJSONObject("paypalConfigData");
					isPayPalReq = jPayPalData.getBoolean("enabled");
					mApplication.setPayPalReq(isPayPalReq);
					if (isPayPalReq) {
						String value = jPayPalData.getString("value");
						if (value != null && value.length() > 0) {
							JSONObject json = new JSONObject(value);
							if (json != null) {
								mApplication.setPayPalClientID(json.get(
										"clientId").toString());
							}
						} else
							Toast.makeText(RegisterActivity.this,
									"Invalid Data for PayPal details",
									Toast.LENGTH_LONG).show();
					}
				} catch (JSONException e) {
					e.printStackTrace();
					if (resObj.getStatusCode() != 200) {
						resObj.setFailResponse(100, "Json Error");
						return resObj;
					}
				}
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
					map.put("provisioningSerialNumber", androidId);
					Date date = new Date();
					SimpleDateFormat df = new SimpleDateFormat("dd MMMM yyyy",
							new Locale("en"));
					String formattedDate = df.format(date);
					map.put("allocationDate", formattedDate);
					map.put("locale", "en");
					map.put("status", "");
					resObj = Utilities.callExternalApiPostMethod(
							getApplicationContext(), map);
					if (resObj.getStatusCode() == 200) {
						
					}
				} else {
					resObj.setFailResponse(100, NETWORK_ERROR);
				}
			}
			return resObj;
		}

		@Override
		protected void onPostExecute(ResponseObj resObj) {

			super.onPostExecute(resObj);
			if (mProgressDialog != null) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}

			if (resObj.getStatusCode() == 200) {

				mOBSClient.getActivePlans(mApplication.getClientId(),
						activePlansCallBack);

				/*
				 * Intent intent = new Intent(RegisterActivity.this,
				 * MainActivity.class); RegisterActivity.this.finish();
				 * startActivity(intent);
				 */
			} else {
				Toast.makeText(RegisterActivity.this,
						"Server Error : " + resObj.getsErrorMessage(),
						Toast.LENGTH_LONG).show();
			}
		}
	}
	
	final Callback<List<ActivePlanDatum>> activePlansCallBack = new Callback<List<ActivePlanDatum>>() {

		@Override
		public void success(List<ActivePlanDatum> list, Response arg1) {
			if (!mIsReqCanceled) {
				/** on success if client has active plans redirect to home page */
				if (list != null && list.size() > 0) {
					Intent intent = new Intent(RegisterActivity.this,
							MainActivity.class);
					RegisterActivity.this.finish();
					startActivity(intent);
				} else {
					Intent intent = new Intent(RegisterActivity.this,
							PlanActivity.class);
					RegisterActivity.this.finish();
					startActivity(intent);
				}
			} else
				mIsReqCanceled = false;
		}

		@Override
		public void failure(RetrofitError retrofitError) {
			if (!mIsReqCanceled) {
				Toast.makeText(
						RegisterActivity.this,
						"Server Error : "
								+ retrofitError.getResponse().getStatus(),
						Toast.LENGTH_LONG).show();
			} else
				mIsReqCanceled = false;
		}
	};

	private class SelfCareUserDatum {

		String email_id;
		String password;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	private RegClientRespDatum readJsonUser(String jsonText) {

		// Log.d("readJsonUser", "result is " + jsonText);

		Gson gson = new Gson();
		RegClientRespDatum response = gson.fromJson(jsonText,
				RegClientRespDatum.class);
		return response;
	}
}
