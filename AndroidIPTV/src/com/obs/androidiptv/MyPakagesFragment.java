package com.obs.androidiptv;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.obs.data.OrderDatum;
import com.obs.retrofit.OBSClient;

public class MyPakagesFragment extends Fragment {
	public static String TAG = MyPakagesFragment.class.getName();
	private ProgressDialog mProgressDialog;
	MyApplication mApplication = null;
	OBSClient mOBSClient;
	boolean mIsReqCanceled = false;
	Activity mActivity;
	View mRootView;
	float mBalance;
	SharedPreferences mPrefs;
	static String CLIENT_PACKAGE_DATA;
	String mPkgData;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		mActivity = getActivity();
		mApplication = ((MyApplication) mActivity.getApplicationContext());
		RestAdapter restAdapter = new RestAdapter.Builder()
				.setEndpoint(mApplication.API_URL)
				.setLogLevel(RestAdapter.LogLevel.FULL)
				.setExecutors(Executors.newCachedThreadPool(), new MainThreadExecutor())
				.setConverter(new JSONConverter())
				.setClient(
						new com.obs.retrofit.CustomUrlConnectionClient(
								mApplication.tenentId, mApplication.basicAuth,
								mApplication.contentType)).build();
		mOBSClient = restAdapter.create(OBSClient.class);
		CLIENT_PACKAGE_DATA = mApplication.getResources().getString(
				R.string.client_pkg_data);
		mPrefs = mActivity.getSharedPreferences(mApplication.PREFS_FILE, 0);
		mPkgData = mPrefs.getString(CLIENT_PACKAGE_DATA, "");
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

		mOBSClient.getClinetPackageDetails(mApplication.getClientId(),
				getClientPkgDtlsCallBack);
	}

	Callback<List<OrderDatum>> getClientPkgDtlsCallBack = new Callback<List<OrderDatum>>() {

		@Override
		public void success(List<OrderDatum> orderList, Response arg1) {
			if (!mIsReqCanceled) {
				Log.d(TAG, "templateCallBack-success");
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
				if (orderList == null || orderList.size() == 0) {
					Toast.makeText(mActivity, "Server Error.",
							Toast.LENGTH_LONG).show();
				} else {
					SharedPreferences.Editor editor = mPrefs.edit();
					editor.putString(CLIENT_PACKAGE_DATA,
							new Gson().toJson(orderList));
					editor.commit();
					updatePackages(orderList);
				}
			}

		}

		@Override
		public void failure(RetrofitError retrofitError) {
			if (!mIsReqCanceled) {
				Log.d(TAG, "getClientPkgDtlsCallBack-failure");
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
			}

		}
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mRootView = inflater.inflate(R.layout.fragment_my_packages, container,
				false);

		if ((mBalance = mApplication.getBalance()) == 0 || mPkgData == null
				|| mPkgData.length() == 0) {

			GetnUpdateFromServer();
		} else {
			updatePackages(getOrdersFromJson(mPkgData));
		}
		return mRootView;
	}

	protected void updatePackages(List<OrderDatum> orderList) {
		if (mRootView != null && orderList != null && orderList.size() > 0) {

			LayoutInflater inflater = (LayoutInflater) mApplication
					.getApplicationContext().getSystemService(
							Context.LAYOUT_INFLATER_SERVICE);

			LinearLayout container = (LinearLayout) mRootView
					.findViewById(R.id.f_my_pkgs_container);
			container.removeAllViews();
			for (OrderDatum order : orderList) {
				View child = inflater.inflate(R.layout.f_my_pkgs_item, null);
				((TextView) child.findViewById(R.id.f_my_pkgs_pkg_name))
						.setText(order.getPlanCode());
				((TextView) child.findViewById(R.id.f_my_pkgs_actvtd_on))
						.setText(order.getActiveDate());
				((TextView) child.findViewById(R.id.f_my_pkgs_valid_till))
						.setText(order.getInvoiceTilldate());
				container.addView(child);
			}
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
		public List<OrderDatum> fromBody(TypedInput typedInput, Type type)
				throws ConversionException {
			List<OrderDatum> ordersList = null;

			try {
				String json = MyApplication.getJSONfromInputStream(typedInput
						.in());

				JSONObject jsonObj;
				jsonObj = new JSONObject(json);
				JSONArray arrOrders = jsonObj.getJSONArray("clientOrders");
				ordersList = getOrdersFromJson(arrOrders.toString());

			} catch (IOException e) {
				Log.i(TAG, e.getMessage());
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return ordersList;
		}

		@Override
		public TypedOutput toBody(Object o) {
			return null;
		}

	}

	public static List<OrderDatum> getOrdersFromJson(String json) {
		List<OrderDatum> ordersList = new ArrayList<OrderDatum>();
		try {

			JSONArray arrOrders = new JSONArray(json);
			for (int i = 0; i < arrOrders.length(); i++) {

				JSONObject obj = arrOrders.getJSONObject(i);
				if ("ACTIVE".equalsIgnoreCase(obj.getString("status"))) {
					OrderDatum order = new OrderDatum();
					order.setOrderNo(obj.getString("orderNo"));
					order.setPlanCode(obj.getString("planCode"));
					order.setPdid(obj.getInt("pdid"));
					order.setPrice(obj.getString("price"));
					order.setStatus(obj.getString("status"));
					try {
						JSONArray arrDate = obj.getJSONArray("activeDate");
						Date date = MyApplication.df.parse(arrDate.getString(0)
								+ "-" + arrDate.getString(1) + "-"
								+ arrDate.getString(2));
						order.setActiveDate(MyApplication.df.format(date));
					} catch (JSONException e) {
						order.setActiveDate(obj.getString("activeDate"));
					}
					try {
						JSONArray arrDate = obj.getJSONArray("invoiceTilldate");
						Date date = MyApplication.df.parse(arrDate.getString(0)
								+ "-" + arrDate.getString(1) + "-"
								+ arrDate.getString(2));
						order.setInvoiceTilldate(MyApplication.df.format(date));
					} catch (JSONException e) {
						order.setInvoiceTilldate(obj
								.getString("invoiceTilldate"));
					}
					ordersList.add(order);
				}
			}
		} catch (Exception e) {
			Log.i(TAG, e.getMessage());
		}
		return ordersList;
	}

	public void onFragKeydown(int keyCode, KeyEvent event) {
		/*if (mProgressDialog != null)
			if (mProgressDialog.isShowing()) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}*/
		//mIsReqCanceled = true;
		//mExecutorService.shutdownNow();
	}

}
