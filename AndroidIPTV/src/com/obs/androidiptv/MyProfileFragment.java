package com.obs.androidiptv;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.Date;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.android.MainThreadExecutor;
import retrofit.client.Response;
import retrofit.converter.ConversionException;
import retrofit.converter.Converter;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;
import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.obs.data.ClientDatum;
import com.obs.data.ConfigurationProperty;
import com.obs.retrofit.CustomUrlConnectionClient;
import com.obs.retrofit.OBSClient;

public class MyProfileFragment extends Fragment {
	//public static String TAG = MyProfileFragment.class.getName();
	private ProgressDialog mProgressDialog;
	MyApplication mApplication = null;
	OBSClient mOBSClient;
	boolean mIsReqCanceled = false;
	Activity mActivity;
	View mRootView;
	float mBalance;
	SharedPreferences mPrefs;
	static String CLIENT_DATA;
	String mClinetData;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		mActivity = getActivity();
		mApplication = ((MyApplication) mActivity.getApplicationContext());
		RestAdapter restAdapter = new RestAdapter.Builder()
				.setEndpoint(MyApplication.API_URL)
				.setLogLevel(RestAdapter.LogLevel.NONE)
				.setExecutors(Executors.newCachedThreadPool(),
						new MainThreadExecutor())
				.setConverter(new JSONConverter())
				.setClient(
						new CustomUrlConnectionClient(
								MyApplication.tenentId, MyApplication.basicAuth,
								MyApplication.contentType)).build();
		mOBSClient = restAdapter.create(OBSClient.class);
		CLIENT_DATA = mApplication.getResources().getString(
				R.string.client_data);
		mPrefs = mActivity.getSharedPreferences(mApplication.PREFS_FILE, 0);
		mClinetData = mPrefs.getString(CLIENT_DATA, "");
		/**
		 * In order for onCreateOptionsMenu() and onOptionsItemSelected()
		 * methods to receive calls, however, you must call setHasOptionsMenu()
		 * during onCreate(), to indicate that the fragment would like to add
		 * items to the Options Menu
		 */
		setHasOptionsMenu(true);

		super.onCreate(savedInstanceState);

	}

	private void GetnUpdateFromServer() {

		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
		mProgressDialog = new ProgressDialog(mActivity,
				ProgressDialog.THEME_HOLO_DARK);
		mProgressDialog.setMessage("Connecting Server");
		mProgressDialog.setCanceledOnTouchOutside(false);
		mProgressDialog.setOnCancelListener(new OnCancelListener() {

			public void onCancel(DialogInterface arg0) {
				if (mProgressDialog.isShowing())
					mProgressDialog.dismiss();
				mIsReqCanceled = true;
			}
		});
		mProgressDialog.show();
		mOBSClient.getClinetDetails(mApplication.getClientId(),
				getClientDetailsCallBack);
	}

	final Callback<ClientDatum> getClientDetailsCallBack = new Callback<ClientDatum>() {
		@Override
		public void failure(RetrofitError retrofitError) {
			if (!mIsReqCanceled) {
				//Log.d(TAG, "getClientDetailsCallBack-failure");
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
				if (retrofitError.isNetworkError()) {
					Toast.makeText(mActivity,
							mApplication.getString(R.string.error_network),
							Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(
							mActivity,
							"Server Error : "
									+ retrofitError.getResponse().getStatus(),
							Toast.LENGTH_LONG).show();
				}
			} else
				mIsReqCanceled = false;
		}

		@Override
		public void success(ClientDatum client, Response response) {
			if (!mIsReqCanceled) {
				//Log.d(TAG, "templateCallBack-success");
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
				if (client == null) {
					Toast.makeText(mActivity, "Server Error.",
							Toast.LENGTH_LONG).show();
				} else {
					SharedPreferences.Editor editor = mPrefs.edit();
					editor.putString(CLIENT_DATA, new Gson().toJson(client));
					editor.commit();
					mApplication.setBalance(client.getBalanceAmount());
					mApplication.setCurrency(client.getCurrency());
					mApplication.setBalanceCheck(client.isBalanceCheck());
					boolean isPayPalReq = false;
					if (client.getConfigurationProperty() != null)
						isPayPalReq = client.getConfigurationProperty()
								.getEnabled();
					mApplication.setPayPalReq(isPayPalReq);
					if (isPayPalReq) {
						String value = client.getConfigurationProperty()
								.getValue();
						if (value != null && value.length() > 0) {
							try {
								JSONObject json = new JSONObject(value);
								if (json != null) {
									mApplication.setPayPalClientID(json.get(
											"clientId").toString());
									// mApplication.setPayPalSecret(json.get(
									// "secretCode").toString());
								}
							} catch (JSONException e) {
								Log.e("AuthenticationAcitivity",
										(e.getMessage() == null) ? "Json Exception"
												: e.getMessage());
								Toast.makeText(getActivity(),
										"Invalid Data-Json Exception",
										Toast.LENGTH_LONG).show();
							}
						} else
							Toast.makeText(getActivity(),
									"Invalid Data for PayPal details",
									Toast.LENGTH_LONG).show();
					}
					updateProfile(client);
				}
			} else
				mIsReqCanceled = false;

		}
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mRootView = inflater.inflate(R.layout.fragment_my_profile, container,
				false);

		if ((mBalance = mApplication.getBalance()) == 0 || mClinetData == null
				|| mClinetData.length() == 0) {

			GetnUpdateFromServer();
		} else {
			updateProfile(parseJsonToClient(mClinetData));
		}
		return mRootView;
	}

	protected void updateProfile(ClientDatum client) {
		if (mRootView != null) {

			((TextView) mRootView
					.findViewById(R.id.f_my_profile_account_no_value))
					.setText(":   " + client.getAccountNo());
			((TextView) mRootView
					.findViewById(R.id.f_my_profile_activation_date_value))
					.setText(":   " + client.getActivationDate());
			((TextView) mRootView.findViewById(R.id.f_my_profile_name_value))
					.setText(":   " + client.getFullname());
			((TextView) mRootView.findViewById(R.id.f_my_profile_email_value))
					.setText(":   " + client.getEmail());
			((TextView) mRootView.findViewById(R.id.f_my_profile_phone_value))
					.setText(":   " + client.getPhone());
			((TextView) mRootView.findViewById(R.id.f_my_profile_country_value))
					.setText(":   " + client.getCountry());
			((TextView) mRootView.findViewById(R.id.f_my_profile_serial_value))
					.setText(":   " + client.getHwSerialNumber());
			float bal = client.getBalanceAmount();
			((TextView) mRootView.findViewById(R.id.f_my_profile_balance_value))
					.setText(":   " + Float.toString((bal == 0.0 ? bal : -bal))
							+ " " + client.getCurrency());
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.nav_menu, menu);
		MenuItem homeItem = menu.findItem(R.id.action_home);
		homeItem.setVisible(true);
		MenuItem searchItem = menu.findItem(R.id.action_search);
		searchItem.setVisible(false);
		MenuItem accountItem = menu.findItem(R.id.action_account);
		accountItem.setVisible(false);
		MenuItem refreshItem = menu.findItem(R.id.action_refresh);
		refreshItem.setVisible(true);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_home:
			startActivity(new Intent(getActivity(), MainActivity.class));
			break;
		case R.id.action_refresh:
			GetnUpdateFromServer();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	static class JSONConverter implements Converter {

		@Override
		public ClientDatum fromBody(TypedInput typedInput, Type type)
				throws ConversionException {
			ClientDatum client = null;

			try {
				String json = MyApplication.getJSONfromInputStream(typedInput
						.in());
				client = parseJsonToClient(json);
			} catch (IOException e) {
				Log.i("MyProfileFragment", e.getMessage());
			}
			return client;
		}

		@Override
		public TypedOutput toBody(Object o) {
			return null;
		}

	}

	public static ClientDatum parseJsonToClient(String json) {
		ClientDatum client = null;
		try {
			client = new ClientDatum();

			JSONObject jsonObj = new JSONObject(json);
			client.setAccountNo(jsonObj.getString("accountNo"));
			try {
				JSONArray arrDate = jsonObj.getJSONArray("activationDate");
				Date date = MyApplication.df.parse(arrDate.getString(0) + "-"
						+ arrDate.getString(1) + "-" + arrDate.getString(2));
				client.setActivationDate(MyApplication.df.format(date));
			} catch (JSONException e) {
				client.setActivationDate(jsonObj.getString("activationDate"));
			}

			client.setFirstname(jsonObj.getString("firstname"));
			client.setLastname(jsonObj.getString("lastname"));
			client.setFullname(jsonObj.getString("firstname") + " "
					+ jsonObj.getString("lastname"));
			client.setEmail(jsonObj.getString("email"));
			client.setPhone(jsonObj.getString("phone"));
			client.setCountry(jsonObj.getString("country"));
			client.setBalanceAmount((float) jsonObj.getDouble("balanceAmount"));
			client.setCurrency(jsonObj.getString("currency"));
			client.setBalanceCheck(jsonObj.getBoolean("balanceCheck"));
			client.setHwSerialNumber(MyApplication.androidId);

			// paypal config data
			JSONObject configJson = jsonObj
					.getJSONObject("configurationProperty");
			if (configJson != null) {
				ConfigurationProperty configProperty = new ConfigurationProperty();
				configProperty.setEnabled(configJson.getBoolean("enabled"));
				configProperty.setValue(configJson.getString("value"));
				client.setConfigurationProperty(configProperty);
			}

		} catch (JSONException e) {
			Log.i("MyProfileFragment", e.getMessage());
		} catch (ParseException e) {
			Log.i("MyProfileFragment", e.getMessage());
		}

		return client;
	}

}
