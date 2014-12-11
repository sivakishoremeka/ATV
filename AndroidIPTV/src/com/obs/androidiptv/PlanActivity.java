package com.obs.androidiptv;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import retrofit.Callback;
import retrofit.RestAdapter;
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
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.obs.adapter.CustomExpandableListAdapter;
import com.obs.adapter.PaytermAdapter;
import com.obs.androidiptv.MyApplication.DoBGTasks;
import com.obs.data.Paytermdatum;
import com.obs.data.PlanDatum;
import com.obs.data.ResponseObj;
import com.obs.parser.JSONPaytermConverter;
import com.obs.retrofit.CustomUrlConnectionClient;
import com.obs.retrofit.OBSClient;
import com.obs.service.DoBGTasksService;
import com.obs.utils.Utilities;

public class PlanActivity extends Activity {

	// public static String TAG = PlanActivity.class.getName();
	private final static String NETWORK_ERROR = "Network error.";
	private ProgressDialog mProgressDialog;

	MyApplication mApplication = null;
	OBSClient mOBSClient;
	boolean mIsReqCanceled = false;

	List<PlanDatum> mPlans;
	List<Paytermdatum> mPayterms;
	CustomExpandableListAdapter listAdapter;
	PaytermAdapter  mListAdapter;
	ListView mPaytermLv;
	ExpandableListView expListView;
	public static int selectedGroupItem = -1;
	public static int selPaytermId = -1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_plan);

		mApplication = ((MyApplication) getApplicationContext());
		mOBSClient = mApplication.getOBSClient();
		Button btnNext = (Button) findViewById(R.id.a_plan_btn_submit);
		btnNext.setText(R.string.next);
		Button btnCancel = (Button) findViewById(R.id.a_plan_btn_cancel);
		btnCancel.setText(R.string.cancel);
		fetchAndBuildPlanList();
	}

	public void fetchAndBuildPlanList() {
		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
		mProgressDialog = new ProgressDialog(PlanActivity.this,
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
		mOBSClient.getPrepaidPlans(getPlansCallBack);
	}

	final Callback<List<PlanDatum>> getPlansCallBack = new Callback<List<PlanDatum>>() {
		@Override
		public void failure(RetrofitError retrofitError) {
			if (!mIsReqCanceled) {
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
				if (retrofitError.isNetworkError()) {
					Toast.makeText(
							PlanActivity.this,
							getApplicationContext().getString(
									R.string.error_network), Toast.LENGTH_LONG)
							.show();
				} else {
					Toast.makeText(
							PlanActivity.this,
							"Server Error : "
									+ retrofitError.getResponse().getStatus(),
							Toast.LENGTH_LONG).show();
				}
			} else
				mIsReqCanceled = false;
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
					buildPlansList();
				}
			} else
				mIsReqCanceled = false;
		}
	};

	private void buildPlansList() {
		expListView = (ExpandableListView) findViewById(R.id.a_exlv_plans_services);
		listAdapter = new CustomExpandableListAdapter(this, mPlans);
		expListView.setAdapter(listAdapter);
		expListView.setOnGroupClickListener(new OnGroupClickListener() {
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
	
	public void btnSubmit_onClick(View v) {
		if (((Button) v).getText().toString()
				.equalsIgnoreCase(getString(R.string.next))) {
			if(selectedGroupItem!=-1){
				Button btnCancel = (Button) findViewById(R.id.a_plan_btn_cancel);
				btnCancel.setText(R.string.back);
			getPaytermsforSelPlan();
			}
			else{
				Toast.makeText(this, "Choose a Plan to Subscribe", Toast.LENGTH_LONG).show();
			}
		}else if (((Button) v).getText().toString()
				.equalsIgnoreCase(getString(R.string.subscribe))) {
			
			if(selectedGroupItem !=-1 && selPaytermId != -1){
				orderPlans(mPlans.get(selectedGroupItem).toString());
			}
			else{
				Toast.makeText(this, "Choose a Payterm to Subscribe", Toast.LENGTH_LONG).show();
			}
		}
	}
	
	private void getPaytermsforSelPlan(){
		RestAdapter restAdapter = new RestAdapter.Builder()
					.setEndpoint(MyApplication.API_URL)
					.setLogLevel(RestAdapter.LogLevel.NONE)
					.setConverter(new JSONPaytermConverter())
					.setClient(
							new CustomUrlConnectionClient(MyApplication.tenentId,
									MyApplication.basicAuth,
									MyApplication.contentType)).build();
			 OBSClient client = restAdapter.create(OBSClient.class);
			//CLIENT_DATA = mApplication.getResources().getString(
			//		R.string.client_data);
			//mPrefs = mActivity.getSharedPreferences(mApplication.PREFS_FILE, 0);
			//mClinetData = mPrefs.getString(CLIENT_DATA, "");		 
			 if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
				mProgressDialog = new ProgressDialog(this,
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
				client.getPlanPayterms((mPlans.get(selectedGroupItem).getId())+"",getPlanPayterms);	
		}
	
	final Callback<List<Paytermdatum>> getPlanPayterms = new Callback<List<Paytermdatum>>() {
		@Override
		public void failure(RetrofitError retrofitError) {
			if (!mIsReqCanceled) {
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
				if (retrofitError.isNetworkError()) {
					Toast.makeText(
							PlanActivity.this,
							getApplicationContext().getString(
									R.string.error_network), Toast.LENGTH_LONG)
							.show();
				} else {
					Toast.makeText(
							PlanActivity.this,
							"Server Error : "
									+ retrofitError.getResponse().getStatus(),
							Toast.LENGTH_LONG).show();
				}
			} else
				mIsReqCanceled = false;
		}

		@Override
		public void success(List<Paytermdatum> payterms, Response response) {
			if (!mIsReqCanceled) {
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
				if (payterms != null) {
					mPayterms = payterms;
					buildPaytermList();
				}
			} else
				mIsReqCanceled = false;
		}
	};
	
private void buildPaytermList(){
		
		(findViewById(R.id.a_exlv_plans_services)).setVisibility(View.GONE);
		(findViewById(R.id.a_plan_payterm_lv)).setVisibility(View.VISIBLE);
		
		TextView tv_title = (TextView) findViewById(R.id.a_plan_tv_selpkg);
		tv_title.setText(R.string.choose_payterm);
		
		Button btnNext = (Button) findViewById(R.id.a_plan_btn_submit);
		btnNext.setText(R.string.subscribe);
		
		mPaytermLv = (ListView) findViewById(R.id.a_plan_payterm_lv);
		mListAdapter = null;
		if (mPayterms != null && mPayterms.size() > 0) {
			boolean isNewPlan=true;
			mListAdapter = new PaytermAdapter(this, mPayterms,isNewPlan);
			mPaytermLv.setAdapter(mListAdapter);
			
			mPaytermLv.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
				
					// TODO Auto-generated method stub
					RadioButton rb1 = (RadioButton) view
							.findViewById(R.id.a_plan_payterm_row_rb);
					if (null != rb1 && (!rb1.isChecked())) {
						PlanActivity.selPaytermId = position;
					} else {
						PlanActivity.selPaytermId = -1;
					}
					
				}
			});
		} else {
			mPaytermLv.setAdapter(null);
			Toast.makeText(this, "No Payterms for this Plan",
					Toast.LENGTH_LONG).show();
		}		
	}

	/*public void btnSubmit_onClick(View v) {
		if (selectedGroupItem >= 0) {
			orderPlans(mPlans.get(selectedGroupItem).toString());
		} else {
			Toast.makeText(getApplicationContext(), "Select a Plan",
					Toast.LENGTH_SHORT).show();
		}
	}*/

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
						PlanActivity.this.finish();
					}
				});
		mConfirmDialog.show();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK) || keyCode == 4) {
			closeApp();
		} else if (keyCode == 23) {
			Window window = getWindow();
			if (window != null) {
				View focusedView = window.getCurrentFocus();
				if (window != null) {
					focusedView.performClick();
				}
			}

		}
		return super.onKeyDown(keyCode, event);
	}

	public void orderPlans(String planid) {
		new OrderPlansAsyncTask().execute();
	}

	private class OrderPlansAsyncTask extends
			AsyncTask<Void, Void, ResponseObj> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			// Log.d("PlanActivity", "onPreExecute");

			if (mProgressDialog != null) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}
			mProgressDialog = new ProgressDialog(PlanActivity.this,
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

			// Log.d(TAG, "doInBackground");

			PlanDatum plan = mPlans.get(selectedGroupItem);
			ResponseObj resObj = new ResponseObj();
			if (Utilities.isNetworkAvailable(getApplicationContext())) {
				HashMap<String, String> map = new HashMap<String, String>();
				Date date = new Date();
				SimpleDateFormat df = new SimpleDateFormat("dd MMMM yyyy",
						new Locale("en"));
				String formattedDate = df.format(date);

				map.put("TagURL", "/orders/" + mApplication.getClientId());
				map.put("planCode", plan.getId().toString());
				map.put("dateFormat", "dd MMMM yyyy");
				map.put("locale", "en");
				map.put("contractPeriod",  mPayterms.get(selPaytermId).getId()+"");
				map.put("isNewplan", "true");
				map.put("start_date", formattedDate);
				map.put("billAlign", "false");
				map.put("paytermCode", mPayterms.get(selPaytermId).getPaytermtype());

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

			// Log.d(TAG, "onPostExecute");

			if (mProgressDialog.isShowing()) {
				mProgressDialog.dismiss();
			}
			if (resObj.getStatusCode() == 200) {
				// update balance config n Values
				// update balance config n Values
				Intent intent = new Intent(PlanActivity.this,
						DoBGTasksService.class);
				intent.putExtra(DoBGTasksService.TASK_ID,
						DoBGTasks.UPDATESERVICES_CONFIGS.ordinal());
				startService(intent);

				Intent activityIntent = new Intent(PlanActivity.this,
						MainActivity.class);
				PlanActivity.this.finish();
				startActivity(activityIntent);
				//CheckBalancenGetData();
			} else {
				Toast.makeText(PlanActivity.this, resObj.getsErrorMessage(),
						Toast.LENGTH_LONG).show();
			}
		}
	}
}