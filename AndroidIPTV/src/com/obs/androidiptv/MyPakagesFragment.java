package com.obs.androidiptv;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
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
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.obs.adapter.PackageAdapter;
import com.obs.androidiptv.MyApplication.DoBGTasks;
import com.obs.data.DeviceDatum;
import com.obs.data.OrderDatum;
import com.obs.data.PlanDatum;
import com.obs.data.ResponseObj;
import com.obs.retrofit.CustomUrlConnectionClient;
import com.obs.retrofit.OBSClient;
import com.obs.service.DoBGTasksService;
import com.obs.utils.Utilities;

public class MyPakagesFragment extends Fragment {
	// public static String TAG = MyPakagesFragment.class.getName();
	private final static String NETWORK_ERROR = "Network error.";
	private static String NEW_PLANS_DATA;
	private static String MY_PLANS_DATA;
	public static int selectedGroupItem = -1;
	public static int orderId = -1;
	private final static String PREPAID_PLANS = "Prepaid plans";
	private final static String MY_PLANS = "My plans";
	private ProgressDialog mProgressDialog;
	MyApplication mApplication = null;
	boolean mIsReqCanceled = false;
	Activity mActivity;
	View mRootView;
	List<PlanDatum> mPrepaidPlans;
	List<PlanDatum> mMyPlans;
	List<PlanDatum> mNewPlans;
	List<OrderDatum> mMyOrders;
	PackageAdapter listAdapter;
	ExpandableListView mExpListView;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		mActivity = getActivity();
		mApplication = ((MyApplication) mActivity.getApplicationContext());
		// mOBSClient = mApplication.getOBSClient(mActivity);
		setHasOptionsMenu(true);
		selectedGroupItem = -1;
		orderId = -1;
		super.onCreate(savedInstanceState);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		mRootView = inflater.inflate(R.layout.fragment_my_packages, container,
				false);

		TextView tv_title = (TextView) mRootView
				.findViewById(R.id.a_plan_tv_selpkg);
		tv_title.setText(R.string.choose_plan_change);

		Button btnNext = (Button) mRootView
				.findViewById(R.id.a_plan_btn_submit);
		btnNext.setText(R.string.next);

		NEW_PLANS_DATA = mApplication.getResources().getString(
				R.string.new_plans_data);
		MY_PLANS_DATA = mApplication.getResources().getString(
				R.string.my_plans_data);
		String newPlansJson = mApplication.getPrefs().getString(NEW_PLANS_DATA,
				"");
		String myPlansJson = mApplication.getPrefs().getString(MY_PLANS_DATA,
				"");
		if (newPlansJson != null && newPlansJson.length() != 0) {
			mNewPlans = getPlanListFromJSON(newPlansJson);
		}
		if (myPlansJson != null && myPlansJson.length() != 0) {
			mMyPlans = getPlanListFromJSON(myPlansJson);
			buildPlansList();
		} else {
			getPlansFromServer();
		}
		return mRootView;
	}

	private void buildPlansList() {
		if (mMyPlans != null && mMyPlans.size() > 0) {
			mExpListView = (ExpandableListView) mRootView
					.findViewById(R.id.f_my_pkg_exlv_my_plans);
			listAdapter = new PackageAdapter(mActivity, mMyPlans);
			mExpListView.setAdapter(listAdapter);
			mExpListView.setOnGroupClickListener(new OnGroupClickListener() {
				@Override
				public boolean onGroupClick(ExpandableListView parent, View v,
						int groupPosition, long id) {

					RadioButton rb1 = (RadioButton) v
							.findViewById(R.id.plan_list_plan_rb);
					if (null != rb1 && (!rb1.isChecked())) {
						PlanActivity.selectedGroupItem = groupPosition;
					} else {
						PlanActivity.selectedGroupItem = -1;
					}
					return false;
				}
			});
		}
	}

	private void getPlansFromServer() {
		getPlans(PREPAID_PLANS);
	}

	private void getMyPlansFromServer() {
		getPlans(MY_PLANS);
	}

	public void getPlans(String planType) {
		boolean showProgressDialog = false;
		if (mProgressDialog != null)
			if (mProgressDialog.isShowing()) {
				// do nothing
			} else {
				showProgressDialog = true;
			}
		else {
			showProgressDialog = true;
		}
		if (showProgressDialog) {
			mProgressDialog = null;
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
		}

		if (PREPAID_PLANS.equalsIgnoreCase(planType)) {
			OBSClient mOBSClient = mApplication.getOBSClient();
			mOBSClient.getPrepaidPlans(getPrepaidPlansCallBack);
		} else if (MY_PLANS.equalsIgnoreCase(planType)) {
			OBSClient mOBSClient = null;
			RestAdapter restAdapter = new RestAdapter.Builder()
					.setEndpoint(MyApplication.API_URL)
					.setLogLevel(RestAdapter.LogLevel.NONE)
					/** Need to change Log level to NONe */
					.setConverter(new JSONConverter())
					.setClient(
							new CustomUrlConnectionClient(
									MyApplication.tenentId,
									MyApplication.basicAuth,
									MyApplication.contentType)).build();
			mOBSClient = restAdapter.create(OBSClient.class);
			mOBSClient.getClinetPackageDetails(mApplication.getClientId(),
					getMyPlansCallBack);
		}
	}

	final Callback<List<PlanDatum>> getPrepaidPlansCallBack = new Callback<List<PlanDatum>>() {
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
			} else
				mIsReqCanceled = true;
		}

		@Override
		public void success(List<PlanDatum> planList, Response response) {
			if (!mIsReqCanceled) {
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
				if (planList != null) {
					mPrepaidPlans = planList;
					getMyPlansFromServer();
				}
			} else
				mIsReqCanceled = true;
		}
	};

	Callback<List<OrderDatum>> getMyPlansCallBack = new Callback<List<OrderDatum>>() {

		@Override
		public void success(List<OrderDatum> orderList, Response response) {
			if (!mIsReqCanceled) {
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
				if (orderList == null || orderList.size() == 0) {
					Toast.makeText(mActivity, "Server Error.",
							Toast.LENGTH_LONG).show();
				} else {
					mMyOrders = orderList;
					CheckPlansnUpdate();
				}

			} else
				mIsReqCanceled = false;

		}

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
			} else
				mIsReqCanceled = false;

		}
	};

	private void CheckPlansnUpdate() {
		mNewPlans = new ArrayList<PlanDatum>();
		mMyPlans = new ArrayList<PlanDatum>();
		if (null != mPrepaidPlans && null != mMyOrders
				&& mPrepaidPlans.size() > 0 && mMyOrders.size() > 0) {
			for (PlanDatum plan : mPrepaidPlans) {
				int planId = plan.getId();
				boolean isNew = true;
				String sOrderId = null;
				for (int i = 0; i < mMyOrders.size(); i++) {
					if (mMyOrders.get(i).getPdid() == planId
							&& mMyOrders.get(i).status
									.equalsIgnoreCase("ACTIVE")) {
						isNew = false;
						sOrderId = mMyOrders.get(i).orderId;
					}
				}
				if (isNew) {
					mNewPlans.add(plan);
				} else {
					plan.orderId = sOrderId;
					mMyPlans.add(plan);
				}
			}
		}

		boolean savePlans = false;
		if (null != mNewPlans && mNewPlans.size() != 0) {
			mApplication.getEditor().putString(NEW_PLANS_DATA,
					new Gson().toJson(mNewPlans));
			savePlans = true;
		}
		if (null != mMyPlans && mMyPlans.size() != 0) {
			mApplication.getEditor().putString(MY_PLANS_DATA,
					new Gson().toJson(mMyPlans));
			savePlans = true;
		}
		if (savePlans)
			mApplication.getEditor().commit();
		buildPlansList();

	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		inflater.inflate(R.menu.nav_menu, menu);
		MenuItem refreshItem = menu.findItem(R.id.action_refresh);
		refreshItem.setVisible(true);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_home:
			startActivity(new Intent(getActivity(), MainActivity.class)
					.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
			break;
		case R.id.action_refresh:
			Button btn = (Button) mRootView
					.findViewById(R.id.a_plan_btn_submit);
			if (btn.getText().toString()
					.equalsIgnoreCase(getString(R.string.subscribe))) {
				selectedGroupItem = -1;
				TextView tv_title = (TextView) mRootView
						.findViewById(R.id.a_plan_tv_selpkg);
				tv_title.setText(R.string.choose_plan_change);

				btn.setText(R.string.next);
			}
			getPlansFromServer();
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
				Log.i("MyPakagesFragment", e.getMessage());
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
					order.setOrderId(obj.getString("id"));
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
						try {
							order.setInvoiceTilldate(obj
									.getString("invoiceTilldate"));
						} catch (JSONException ex) {
							// no invoice till date.
						}
					}
					ordersList.add(order);
				}
			}
		} catch (Exception e) {
			Log.i("MyPakagesFragment", e.getMessage());
		}
		return ordersList;
	}

	public void btnSubmit_onClick(View v) {

		if (((Button) v).getText().toString()
				.equalsIgnoreCase(getString(R.string.next))) {

			if (selectedGroupItem == -1) {
				Toast.makeText(getActivity(), "Select a Plan to Change",
						Toast.LENGTH_LONG).show();
			} else {
				orderId = Integer
						.parseInt(mMyPlans.get(selectedGroupItem).orderId);
				selectedGroupItem = -1;
				TextView tv_title = (TextView) mRootView
						.findViewById(R.id.a_plan_tv_selpkg);
				tv_title.setText(R.string.choose_plan_sub);

				((Button) v).setText(R.string.subscribe);
				mExpListView = (ExpandableListView) mRootView
						.findViewById(R.id.f_my_pkg_exlv_my_plans);
				listAdapter = null;
				if (mNewPlans != null && mNewPlans.size() > 0) {
					listAdapter = new PackageAdapter(mActivity, mNewPlans);
					mExpListView.setAdapter(listAdapter);
					mExpListView
							.setOnGroupClickListener(new OnGroupClickListener() {
								@Override
								public boolean onGroupClick(
										ExpandableListView parent, View v,
										int groupPosition, long id) {

									RadioButton rb1 = (RadioButton) v
											.findViewById(R.id.plan_list_plan_rb);
									if (null != rb1 && (!rb1.isChecked())) {
										PlanActivity.selectedGroupItem = groupPosition;
									} else {
										PlanActivity.selectedGroupItem = -1;
									}
									return false;
								}
							});
				} else {
					mExpListView.setAdapter(listAdapter);
					Toast.makeText(getActivity(), "No new Plans",
							Toast.LENGTH_LONG).show();
				}
			}
		} else if (((Button) v).getText().toString()
				.equalsIgnoreCase(getString(R.string.subscribe))) {
			if (selectedGroupItem >= 0) {
				changePlan(mNewPlans.get(selectedGroupItem).toString());
			} else {
				Toast.makeText(getActivity().getApplicationContext(),
						"Select a Plan", Toast.LENGTH_SHORT).show();
			}
		}
	}

	public void changePlan(String planid) {
		new ChangePlansAsyncTask().execute();
	}

	private class ChangePlansAsyncTask extends
			AsyncTask<Void, Void, ResponseObj> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			boolean showProgressDialog = false;
			if (mProgressDialog != null)
				if (mProgressDialog.isShowing()) {
					// do nothing
				} else {
					showProgressDialog = true;
				}
			else {
				showProgressDialog = true;
			}
			if (showProgressDialog) {
				mProgressDialog = new ProgressDialog(getActivity(),
						ProgressDialog.THEME_HOLO_DARK);
				mProgressDialog.setMessage("Processing Order...");
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
		}

		@Override
		protected ResponseObj doInBackground(Void... params) {
			PlanDatum plan = mNewPlans.get(selectedGroupItem);
			ResponseObj resObj = new ResponseObj();
			if (Utilities.isNetworkAvailable(getActivity()
					.getApplicationContext())) {
				HashMap<String, String> map = new HashMap<String, String>();
				Object[] arrStartDate = plan.getStartDate().toArray();
				Date discDate = new Date();
				SimpleDateFormat df = new SimpleDateFormat("dd MMMM yyyy",
						new Locale("en"));
				Date startDate = new Date((Integer) arrStartDate[0],
						(Integer) arrStartDate[1], (Integer) arrStartDate[2]);
				String strStartDate = df.format(startDate);
				String strDiscDate = df.format(discDate);

				map.put("TagURL", "/orders/changePlan/" + orderId);
				map.put("planCode", plan.getId().toString());
				map.put("dateFormat", "dd MMMM yyyy");
				map.put("locale", "en");
				map.put("contractPeriod", plan.getContractId().toString());
				map.put("isNewplan", "false");
				map.put("start_date", strStartDate);
				map.put("disconnectionDate", strDiscDate);
				map.put("disconnectReason", "Not Interested");
				map.put("billAlign", "false");
				map.put("paytermCode", plan.getServices().get(0)
						.getChargeCode());

				resObj = Utilities.callExternalApiPutMethod(getActivity()
						.getApplicationContext(), map);
			} else {
				resObj.setFailResponse(100, NETWORK_ERROR);
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
				// update balance config n Values
				CheckBalancenGetData();
			} else {
				Toast.makeText(getActivity(), resObj.getsErrorMessage(),
						Toast.LENGTH_LONG).show();
			}
		}
	}

	private void CheckBalancenGetData() {
		// Log.d("PlanActivity","CheckBalancenGetData");
		validateDevice();
	}

	private void validateDevice() {
		// Log.d("PlanActivity","validateDevice");
		boolean showProgressDialog = false;
		if (mProgressDialog != null)
			if (mProgressDialog.isShowing()) {
				// do nothing
			} else {
				showProgressDialog = true;
			}
		else {
			showProgressDialog = true;
		}
		if (showProgressDialog) {
			mProgressDialog = new ProgressDialog(getActivity(),
					ProgressDialog.THEME_HOLO_DARK);
			mProgressDialog.setMessage("Connecting Server...");
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
		}
		String androidId = Settings.Secure.getString(getActivity()
				.getApplicationContext().getContentResolver(),
				Settings.Secure.ANDROID_ID);
		OBSClient mOBSClient = mApplication.getOBSClient();
		mOBSClient.getMediaDevice(androidId, deviceCallBack);
	}

	final Callback<DeviceDatum> deviceCallBack = new Callback<DeviceDatum>() {

		@Override
		public void success(DeviceDatum device, Response arg1) {
			// Log.d("PlanActivity","success");
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
							if (value != null && value.length() > 0) {
								JSONObject json = new JSONObject(value);
								if (json != null) {
									mApplication.setPayPalClientID(json.get(
											"clientId").toString());
								}
							} else
								Toast.makeText(getActivity(),
										"Invalid Data for PayPal details",
										Toast.LENGTH_LONG).show();
						}
					} catch (JSONException e) {
						Log.e("PlanActivity",
								(e.getMessage() == null) ? "Json Exception" : e
										.getMessage());
						Toast.makeText(getActivity(),
								"Invalid Data-Json Error", Toast.LENGTH_LONG)
								.show();
					} catch (Exception e) {
						Log.e("PlanActivity",
								(e.getMessage() == null) ? "Exception" : e
										.getMessage());
						Toast.makeText(getActivity(), "Invalid Data-Error",
								Toast.LENGTH_LONG).show();
					}
				}

				Toast.makeText(mActivity, "Plan Change Success",
						Toast.LENGTH_LONG).show();
				Intent intent = new Intent(mActivity, DoBGTasksService.class);
				intent.putExtra(DoBGTasksService.TASK_ID,
						DoBGTasks.UPDATESERVICES.ordinal());
				mActivity.startService(intent);
				UpdateUI();
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
					Toast.makeText(mActivity,
							getString(R.string.error_network),
							Toast.LENGTH_LONG).show();
				} else if (retrofitError.getResponse().getStatus() == 403) {
					String msg = mApplication
							.getDeveloperMessage(retrofitError);
					msg = (msg != null && msg.length() > 0 ? msg
							: "Internal Server Error");
					Toast.makeText(mActivity, msg, Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(
							mActivity,
							"Server Error : "
									+ retrofitError.getResponse().getStatus(),
							Toast.LENGTH_LONG).show();
				}
				Toast.makeText(mActivity, "Plan Change Failed",
						Toast.LENGTH_LONG).show();
				mIsReqCanceled = false;
			}

		}
	};

	public void onBackPressed() {
		Button btn = (Button) mRootView.findViewById(R.id.a_plan_btn_submit);
		if (btn.getText().toString()
				.equalsIgnoreCase(getString(R.string.subscribe))) {
			selectedGroupItem = -1;
			TextView tv_title = (TextView) mRootView
					.findViewById(R.id.a_plan_tv_selpkg);
			tv_title.setText(R.string.choose_plan_change);

			btn.setText(R.string.next);
			mExpListView = (ExpandableListView) mRootView
					.findViewById(R.id.f_my_pkg_exlv_my_plans);

			if (mMyPlans != null && mMyPlans.size() > 0) {
				mExpListView = (ExpandableListView) mRootView
						.findViewById(R.id.f_my_pkg_exlv_my_plans);
				listAdapter = new PackageAdapter(mActivity, mMyPlans);
				mExpListView.setAdapter(listAdapter);
				mExpListView
						.setOnGroupClickListener(new OnGroupClickListener() {
							@Override
							public boolean onGroupClick(
									ExpandableListView parent, View v,
									int groupPosition, long id) {

								RadioButton rb1 = (RadioButton) v
										.findViewById(R.id.plan_list_plan_rb);
								if (null != rb1 && (!rb1.isChecked())) {
									PlanActivity.selectedGroupItem = groupPosition;
								} else {
									PlanActivity.selectedGroupItem = -1;
								}
								return false;
							}
						});
			}
		} else
			getActivity().finish();
	}

	protected void UpdateUI() {
		selectedGroupItem = -1;
		TextView tv_title = (TextView) mRootView
				.findViewById(R.id.a_plan_tv_selpkg);
		tv_title.setText(R.string.choose_plan_change);
		Button btn = (Button) mRootView.findViewById(R.id.a_plan_btn_submit);
		btn.setText(R.string.next);
		getPlansFromServer();
	}

	private List<PlanDatum> getPlanListFromJSON(String json) {
		java.lang.reflect.Type t = new TypeToken<List<PlanDatum>>() {
		}.getType();
		return new Gson().fromJson(json, t);
	}

}
