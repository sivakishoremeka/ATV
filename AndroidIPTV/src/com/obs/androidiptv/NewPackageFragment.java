package com.obs.androidiptv;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.obs.adapter.NewPackageAdapter;
import com.obs.data.OrderDatum;
import com.obs.data.PlanDatum;
import com.obs.data.ResponseObj;
import com.obs.retrofit.OBSClient;
import com.obs.utils.Utilities;

public class NewPackageFragment extends Fragment {
	public static String TAG = NewPackageFragment.class.getName();

	private final static String NETWORK_ERROR = "Network error.";
	private final static String PREPAID_PLANS = "Prepaid plans";
	private final static String MY_PLANS = "My plans";
	private static String NEW_PAKAGE_DATA;
	static String CLIENT_PACKAGE_DATA;
	private ProgressDialog mProgressDialog;
	MyApplication mApplication = null;
	boolean mIsReqCanceled = false;
	boolean mIsPlanSubscribed = false;
	List<PlanDatum> mPlans;
	List<PlanDatum> mNewPlans;
	List<OrderDatum> mMyOrders;
	NewPackageAdapter listAdapter;
	ExpandableListView expListView;
	public static int selectedGroupItem = -1;
	Activity mActivity;
	View mRootView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mActivity = getActivity();
		mApplication = ((MyApplication) mActivity.getApplicationContext());
		CLIENT_PACKAGE_DATA = mApplication.getResources().getString(
				R.string.client_pkg_data);
		/**
		 * In order for onCreateOptionsMenu() and onOptionsItemSelected()
		 * methods to receive calls, however, you must call setHasOptionsMenu()
		 * during onCreate(), to indicate that the fragment would like to add
		 * items to the Options Menu
		 */

		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mRootView = inflater.inflate(R.layout.fragment_new_package, container,
				false);
		NEW_PAKAGE_DATA = mApplication.getResources().getString(
				R.string.new_pkg_data);
		String newPkgJson = mApplication.getPrefs().getString(NEW_PAKAGE_DATA,
				"");
		if (newPkgJson != null && newPkgJson.length() != 0) {
			mNewPlans = getPlanListFromJSON(newPkgJson);
			buildPlansList();
		} else {
			getPlansFromServer();
		}
		return mRootView;
	}

	private void CheckForNewPlansnUpdate() {
		mNewPlans = new ArrayList<PlanDatum>();
		if (null != mPlans && null != mMyOrders && mPlans.size() > 0
				&& mMyOrders.size() > 0) {
			for (PlanDatum plan : mPlans) {
				int planId = plan.getId();
				boolean isNew = true;
				for (int i = 0; i < mMyOrders.size(); i++) {
					if (mMyOrders.get(i).getPdid() == planId) {
						isNew = false;
					}
				}
				if (isNew) {
					mNewPlans.add(plan);
				}
			}
		}
		if ((null != mNewPlans && mNewPlans.size() != 0)
				|| (mPlans.size() == mMyOrders.size())) {
			Editor editor = mApplication.getEditor();
			editor.putString(NEW_PAKAGE_DATA, new Gson().toJson(mNewPlans));
			editor.commit();
			buildPlansList();
		}
	}

	private void getMyPlansFromServer() {
		getPlans(MY_PLANS);
	}

	private void getPlansFromServer() {
		getPlans(PREPAID_PLANS);
	}

	public void getPlans(String planType) {
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
		if (PREPAID_PLANS.equalsIgnoreCase(planType)) {
			OBSClient mOBSClient = mApplication.getOBSClient(mActivity);
			mOBSClient.getPrepaidPlans(getPlansCallBack);
		} else if (MY_PLANS.equalsIgnoreCase(planType)) {
			RestAdapter restAdapter = new RestAdapter.Builder()
					.setEndpoint(mApplication.API_URL)
					.setLogLevel(RestAdapter.LogLevel.FULL)
					.setExecutors(Executors.newCachedThreadPool(),
							new MainThreadExecutor())
					.setConverter(new JSONConverter())
					.setClient(
							new com.obs.retrofit.CustomUrlConnectionClient(
									mApplication.tenentId,
									mApplication.basicAuth,
									mApplication.contentType)).build();
			OBSClient mOBSClient = restAdapter.create(OBSClient.class);
			mOBSClient.getClinetPackageDetails(mApplication.getClientId(),
					getClientPkgDtlsCallBack);
		}
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
					SharedPreferences.Editor editor = mApplication.getPrefs()
							.edit();
					editor.putString(CLIENT_PACKAGE_DATA,
							new Gson().toJson(orderList));
					editor.commit();
					mMyOrders = orderList;
					CheckForNewPlansnUpdate();
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

	final Callback<List<PlanDatum>> getPlansCallBack = new Callback<List<PlanDatum>>() {
		@Override
		public void failure(RetrofitError retrofitError) {
			if (!mIsReqCanceled) {
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

		@Override
		public void success(List<PlanDatum> planList, Response response) {
			if (!mIsReqCanceled) {
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
				if (planList != null) {
					mPlans = planList;
					getMyPlansFromServer();
				}
			}
		}
	};

	private void buildPlansList() {

		expListView = (ExpandableListView) mRootView
				.findViewById(R.id.a_exlv_plans_services);
		listAdapter = new NewPackageAdapter(mActivity, mNewPlans, this);
		expListView.setAdapter(listAdapter);
		if (mIsPlanSubscribed)
			Toast.makeText(mActivity, "Plan Subscription Success.",
					Toast.LENGTH_LONG).show();
	}

	public void btnSubmit_onClick() {
		if (selectedGroupItem >= 0) {
			orderPlans();
		} else {
			Toast.makeText(mActivity, "Select a Plan", Toast.LENGTH_SHORT)
					.show();
		}
	}

	public void btnCancel_onClick(View v) {

	}

	public void orderPlans() {
		new OrderPlansAsyncTask().execute();
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
			getPlansFromServer();
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private class OrderPlansAsyncTask extends
			AsyncTask<Void, Void, ResponseObj> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			Log.d(TAG, "onPreExecute");

			if (mProgressDialog != null) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}
			mProgressDialog = new ProgressDialog(mActivity,
					ProgressDialog.THEME_HOLO_DARK);
			mProgressDialog.setMessage("Processing Order");
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
		protected ResponseObj doInBackground(Void... params) {
			Log.d(TAG, "doInBackground");
			PlanDatum plan = mNewPlans.get(selectedGroupItem);
			ResponseObj resObj = new ResponseObj();
			if (Utilities.isNetworkAvailable(mApplication)) {
				HashMap<String, String> map = new HashMap<String, String>();
				Date date = new Date();
				SimpleDateFormat df = new SimpleDateFormat("dd MMMM yyyy",
						new Locale("en"));
				String formattedDate = df.format(date);

				map.put("TagURL", "/orders/" + mApplication.getClientId());
				map.put("planCode", plan.getId().toString());
				map.put("dateFormat", "dd MMMM yyyy");
				map.put("locale", "en");
				map.put("contractPeriod", plan.getContractId().toString());
				map.put("isNewplan", "true");
				map.put("start_date", formattedDate);
				map.put("billAlign", "true");
				map.put("paytermCode", plan.getServices().get(0)
						.getChargeCode());
				resObj = Utilities.callExternalApiPostMethod(mApplication, map);
			} else {
				resObj.setFailResponse(100, NETWORK_ERROR);
			}
			return resObj;
		}

		@Override
		protected void onPostExecute(ResponseObj resObj) {
			super.onPostExecute(resObj);
			Log.d(TAG, "onPostExecute");
			if (mProgressDialog.isShowing()) {
				mProgressDialog.dismiss();
			}
			if (resObj.getStatusCode() == 200) {
				mIsPlanSubscribed = true;
				getPlansFromServer();

			} else {
				Toast.makeText(mActivity, resObj.getsErrorMessage(),
						Toast.LENGTH_LONG).show();
			}
		}
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

	private List<PlanDatum> getPlanListFromJSON(String json) {
		java.lang.reflect.Type t = new TypeToken<List<PlanDatum>>() {
		}.getType();
		return new Gson().fromJson(json, t);
	}

	public void onFragKeydown(int keyCode, KeyEvent event) {
		/*
		 * if (mProgressDialog != null) if (mProgressDialog.isShowing()) {
		 * mProgressDialog.dismiss(); mProgressDialog = null; }
		 */
		// mIsReqCanceled = true;
		// mExecutorService.shutdownNow();
	}

}
