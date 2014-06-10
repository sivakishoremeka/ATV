package com.obs.androidiptv;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.app.ActionBar;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.obs.adapter.MyFragmentPagerAdapter;
import com.obs.adapter.VodCategoryAdapter;
import com.obs.data.MediaDetailRes;
import com.obs.retrofit.OBSClient;

public class VodActivity extends FragmentActivity 
//implements
//		SearchView.OnQueryTextListener
{
	private static final String TAG = VodActivity.class.getName();
	public static int ITEMS;
	private final static String CATEGORY = "CATEGORY";
	MyFragmentPagerAdapter mAdapter;
	ViewPager mPager;
	private SharedPreferences mPrefs;
	private Editor mPrefsEditor;

	ListView listView;
	private ProgressDialog mProgressDialog;

	MyApplication mApplication = null;
	OBSClient mOBSClient;
	boolean mIsReqCanceled = false;
	
	String mSearchString;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_vod);
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		mApplication = ((MyApplication) getApplicationContext());
		mOBSClient = mApplication.getOBSClient(this);

		mPrefs = getSharedPreferences(mApplication.PREFS_FILE, 0);
		mPrefsEditor = mPrefs.edit();
		mPrefsEditor.putString(CATEGORY, "RELEASE");
		mPrefsEditor.commit();
		listView = (ListView) findViewById(R.id.a_vod_lv_category);
		String[] arrMovCategNames = getResources().getStringArray(
				R.array.arrMovCategNames);
		VodCategoryAdapter categAdapter = new VodCategoryAdapter(this,
				arrMovCategNames);
		listView.setAdapter(categAdapter);
		listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		listView.setItemChecked(0, true);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				((AbsListView) arg0).setItemChecked(arg2, true);
				String[] arrMovCategValues = getResources().getStringArray(
						R.array.arrMovCategValues);
				mPrefsEditor.putString(CATEGORY, arrMovCategValues[arg2]);
				mPrefsEditor.commit();
				setPageCountAndGetDetails();
			}
		});
		setPageCountAndGetDetails();
		mPager = (ViewPager) findViewById(R.id.a_vod_pager);
		mPager.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageScrollStateChanged(int arg0) {
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
				Button button = (Button) findViewById(R.id.a_vod_btn_pgno);
				button.setText("" + (arg0 + 1));
			}

			@Override
			public void onPageSelected(int arg0) {
			}
		});
		Button button = (Button) findViewById(R.id.a_vod_btn_first);
		button.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mPager.setCurrentItem(0);
			}
		});
		button = (Button) findViewById(R.id.a_vod_btn_last);
		button.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				mPager.setCurrentItem(ITEMS - 1);
			}
		});
	}

	@Override
	protected void onNewIntent(Intent intent) {		
		super.onNewIntent(intent);
		Log.d("onNewIntent", "onNewIntent");
		if(null!=intent.getAction()&&intent.getAction().equals(Intent.ACTION_SEARCH)){
			Log.d(intent.getStringExtra(SearchManager.QUERY),"onNewIntent");
			mSearchString = intent.getStringExtra(SearchManager.QUERY);
			listView.clearChoices();
			mPrefs = getSharedPreferences(mApplication.PREFS_FILE, 0);
			mPrefsEditor = mPrefs.edit();
			mPrefsEditor.putString(CATEGORY, mSearchString);
			mPrefsEditor.commit();
			setPageCountAndGetDetails();
		}
	}
	
	protected void setPageCountAndGetDetails() {
		mPrefs = getSharedPreferences(mApplication.PREFS_FILE, 0);
		String category = mPrefs.getString(CATEGORY, "");

		String deviceId = Settings.Secure.getString(getApplicationContext()
				.getContentResolver(), Settings.Secure.ANDROID_ID);

		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
		mProgressDialog = new ProgressDialog(VodActivity.this,
				ProgressDialog.THEME_HOLO_DARK);
		mProgressDialog.setMessage("Retriving Detials");
		mProgressDialog.setCanceledOnTouchOutside(false);
		mProgressDialog.setOnCancelListener(new OnCancelListener() {

			public void onCancel(DialogInterface arg0) {
				if (mProgressDialog.isShowing())
					mProgressDialog.dismiss();
				mIsReqCanceled = true;
				//if (null != mExecutorService)
				//	if (!mExecutorService.isShutdown())
				//		mExecutorService.shutdownNow();
			}
		});
		mProgressDialog.show();

		mOBSClient.getPageCountAndMediaDetails(category.equals("") ? "RELEASE"
				: category, "0", deviceId, getPageCountAndDetailsCallBack);
	}

	final Callback<MediaDetailRes> getPageCountAndDetailsCallBack = new Callback<MediaDetailRes>() {
		@Override
		public void failure(RetrofitError retrofitError) {
			if (!mIsReqCanceled) {
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
				if (retrofitError.isNetworkError()) {
					Toast.makeText(
							VodActivity.this,
							getApplicationContext().getString(
									R.string.error_network), Toast.LENGTH_LONG)
							.show();
				} else {
					Toast.makeText(
							VodActivity.this,
							"Server Error : "
									+ retrofitError.getResponse().getStatus(),
							Toast.LENGTH_LONG).show();
				}
			}
			else
				mIsReqCanceled = false;
		}

		@Override
		public void success(MediaDetailRes objDetails, Response response) {
			if (!mIsReqCanceled) {
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
				if (objDetails != null) {
					ITEMS = objDetails.getNoOfPages();
					mAdapter = new MyFragmentPagerAdapter(
							getSupportFragmentManager());
					mPager.setAdapter(mAdapter);
				}
			}
			else
				mIsReqCanceled = false;
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.nav_menu, menu);
		MenuItem searchItem = menu.findItem(R.id.action_search);
	    searchItem.setVisible(true);
	    MenuItem refreshItem = menu.findItem(R.id.action_refresh);
	    refreshItem.setVisible(true);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			break;
		case R.id.action_home:
			NavUtils.navigateUpFromSameTask(this);
			break;
		case R.id.action_account:
			startActivity(new Intent(this, MyAccountActivity.class));
			break;
		case R.id.action_search:
			onSearchRequested();
		break;
		case R.id.action_refresh:
			setPageCountAndGetDetails();
			break;
		default:
			break;
		}
		return true;
	}
}