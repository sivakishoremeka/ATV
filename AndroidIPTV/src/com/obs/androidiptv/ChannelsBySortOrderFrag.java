package com.obs.androidiptv;

import java.util.Calendar;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.app.Activity;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.obs.adapter.ServiceCategoryListAdapter;
import com.obs.androidiptv.MyApplication.SortBy;
import com.obs.data.DeviceDatum;
import com.obs.data.ServiceDatum;
import com.obs.database.ServiceProvider;
import com.obs.retrofit.OBSClient;

public class ChannelsBySortOrderFrag extends Fragment implements
		LoaderManager.LoaderCallbacks<Cursor>,
		ExpandableListView.OnChildClickListener,
		AdapterView.OnItemSelectedListener, AdapterView.OnItemLongClickListener {

	private static final String TAG = ChannelsBySortOrderFrag.class.getName();

	private ProgressDialog mProgressDialog;

	String mSearchString;
	String mSelection;
	String[] mSelectionArgs;

	MyApplication mApplication = null;
	OBSClient mOBSClient;
	boolean mIsReqCanceled = false;

	boolean mIsLiveDataReq = false;
	boolean mIsRefresh = false;
	boolean mIsBalCheckReq;
	float mBalance;

	private Callbacks mCallbacks = sDummyCallbacks;

	private ExpandableListView elv;
	int mSelectionIdx = -1;
	private ServiceCategoryListAdapter adapter;
	int mSortBy = SortBy.CATEGORY.ordinal();

	public interface Callbacks {
		/**
		 * Callback for when an item has been selected.
		 */
		public void onItemSelected(ServiceDatum service, int selIdx);

		public void onItemClick(ServiceDatum data, int selctionIndex,int sortBy,String selection,String searchString);

		public void onItemLongClick(ServiceDatum service, int selIdx);
	}

	/**
	 * A dummy implementation of the {@link Callbacks} interface that does
	 * nothing. Used only when this fragment is not attached to an activity.
	 */
	private static Callbacks sDummyCallbacks = new Callbacks() {
		public void onItemSelected(ServiceDatum service, int selIdx) {
		}

		@Override
		public void onItemClick(ServiceDatum data, int selctionIndex,int sortBy,String selection,String searchString) {
			// TODO Auto-generated method stub
		}

		@Override
		public void onItemLongClick(ServiceDatum service, int selIdx) {
			// TODO Auto-generated method stub
		}
	};

	public ChannelsBySortOrderFrag() {
		Log.d(TAG, "ItemListFragment constructor");
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// Activities containing this fragment must implement its callbacks.
		if (!(activity instanceof Callbacks)) {
			throw new IllegalStateException(
					"Activity must implement fragment's callbacks.");
		}

		mCallbacks = (Callbacks) activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {

		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		mApplication = ((MyApplication) getActivity().getApplicationContext());
		mApplication = (MyApplication) getActivity().getApplicationContext();
		mOBSClient = mApplication.getOBSClient(getActivity());
		mIsBalCheckReq = mApplication.isBalanceCheck();
		Calendar c = Calendar.getInstance();
		setRetainInstance(true);
		setHasOptionsMenu(true);
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_channels_sorted, container,
				false);
		elv = (ExpandableListView) v.findViewById(R.id.elv);
		elv.setChoiceMode(ExpandableListView.CHOICE_MODE_SINGLE);
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		Bundle args = getArguments();
		if (args != null) {
			mSearchString = args.getString("SEARCHSTRING");
			mSelection = args.getString("SELECTION");
			if (mSearchString != null)
				mSelectionArgs = new String[] { "%" + mSearchString + "%" };
			else
				mSelectionArgs = null;
			mSortBy = args.getInt("SORTBY");
			mIsRefresh = args.getBoolean("ISREFRESH",false);
		}
		adapter = new ServiceCategoryListAdapter(null, getActivity(), mSortBy,mSelection,mSelectionArgs);
		elv.setAdapter(adapter);
		elv.setLongClickable(true);
		elv.setOnChildClickListener(this);
		elv.setOnItemSelectedListener(this);
		elv.setOnItemLongClickListener(this);
		// Prepare the loader. Either re-connect with an existing one,
		// or start a new one.

		CheckBalancenGetData();
	}

	@Override
	public void onDetach() {
		super.onDetach();
		// Reset the active callbacks interface to the dummy implementation.
		mCallbacks = sDummyCallbacks;
	}

	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		if (mProgressDialog != null && mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
		mProgressDialog = new ProgressDialog(getActivity(),
				ProgressDialog.THEME_HOLO_DARK);
		mProgressDialog.setMessage("Connectiong to Server...");
		mProgressDialog.setCanceledOnTouchOutside(false);
		mProgressDialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				if (mProgressDialog.isShowing())
					mProgressDialog.dismiss();
				mProgressDialog = null;

			}
		});
		mProgressDialog.show();
		
		
		//using sortOrder arg for passing both mIsRefresh&SortOrder
		String sortOrder = mIsRefresh+"&";
		// This is called when a new Loader needs to be created.
		CursorLoader loader = null;
		if (id == SortBy.CATEGORY.ordinal()) {
			// group cursor
			loader = new CursorLoader(getActivity(),
					ServiceProvider.SERVICE_CATEGORIES_URI, null, null,
					null, sortOrder);
		} else if (id == SortBy.LANGUAGE.ordinal()) {
			// group cursor
			loader = new CursorLoader(getActivity(),
					ServiceProvider.SERVICE_SUB_CATEGORIES_URI, null,
					null, null, sortOrder);
		}
		return loader;
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		// Log.d("ChannelsActivity","onLoadFinished");
				if (mProgressDialog != null && mProgressDialog.isShowing()) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
		if(cursor!=null){		
		adapter.setGroupCursor(cursor);
		expandAllChild();
		}
	}

	private void expandAllChild() {

		for (int i = 0; i < adapter.getGroupCount(); i++) {

			elv.expandGroup(i);
		}

	}

	public void onLoaderReset(Loader<Cursor> loader) {
		// This is called when the last Cursor provided to onLoadFinished()
		// is about to be closed.
		int id = loader.getId();
		if (id != -1) {
			// child cursor
			try {
				adapter.setChildrenCursor(id, null);
			} catch (NullPointerException e) {
				Log.w("TAG", "Adapter expired, try again on the next query: "
						+ e.getMessage());
			}
		} else {
			adapter.setGroupCursor(null);
		}
	}

	@Override
	public boolean onChildClick(ExpandableListView parent, View v,
			int groupPosition, int childPosition, long id) {
		ServiceDatum data = ChannelsActivity
				.getServiceFromCursor(((Cursor) parent
						.getExpandableListAdapter().getChild(groupPosition,
								childPosition)));
		mCallbacks.onItemClick(data,childPosition,mSortBy,mSelection,mSearchString);
		return true;
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		mSelectionIdx = position;
		boolean isChildView = false;
		// checking group or child view
		View v = view.findViewById(R.id.ch_lv_item_tv_ch_Name);
		if (null != v)
			isChildView = true;

		if (isChildView) {
			ServiceDatum data = (ServiceDatum) v.getTag();
			if (data != null) {
				mCallbacks.onItemLongClick(data, mSelectionIdx);
			}
		}
		return true;
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		if (position != mSelectionIdx) {
			mSelectionIdx = position;
			boolean isChildView = false;
			// checking group or child view
			View v = view.findViewById(R.id.ch_lv_item_tv_ch_Name);
			if (null != v)
				isChildView = true;

			if (isChildView) {
				ServiceDatum data = (ServiceDatum) v.getTag();
				if (data != null)
					mCallbacks.onItemSelected(data, mSelectionIdx);
			}
		}

	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// TODO Auto-generated method stub

	}

	private void CheckBalancenGetData() {
		// Log.d("ChannelsActivity","CheckBalancenGetData");
		if (mIsBalCheckReq)
			validateDevice();
		else
			getServices();
	}

	private void validateDevice() {
		if (((ChannelsActivity) getActivity()).isRemoteDeviceValidationReq()) {
			// Log.d("ChannelsActivity","validateDevice");
			if (mProgressDialog != null && mProgressDialog.isShowing()) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}

			mProgressDialog = new ProgressDialog(getActivity(),
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

			String androidId = Settings.Secure.getString(getActivity()
					.getApplicationContext().getContentResolver(),
					Settings.Secure.ANDROID_ID);
			mOBSClient.getMediaDevice(androidId, deviceCallBack);
		} else {
			doValidation();
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
					mApplication.setBalance(mBalance = device
							.getBalanceAmount());
					mApplication.setBalanceCheck(device.isBalanceCheck());
					doValidation();
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
					Toast.makeText(
							getActivity(),
							getActivity().getApplicationContext().getString(
									R.string.error_network), Toast.LENGTH_LONG)
							.show();
				} else if (retrofitError.getResponse().getStatus() == 403) {
					String msg = mApplication
							.getDeveloperMessage(retrofitError);
					msg = (msg != null && msg.length() > 0 ? msg
							: "Internal Server Error");
					Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG)
							.show();
				} else {
					Toast.makeText(
							getActivity(),
							"Server Error : "
									+ retrofitError.getResponse().getStatus(),
							Toast.LENGTH_LONG).show();
				}
			}
			mIsReqCanceled = false;
		}
	};

	private void doValidation() {
		mBalance = mApplication.getBalance();
		if (mBalance >= 0)
			Toast.makeText(getActivity(),
					"Insufficient Balance.Please Make a Payment.",
					Toast.LENGTH_LONG).show();
		else {
			getServices();
		}
	}

	private void getServices() {
		Loader<Cursor> loader = getActivity().getLoaderManager().getLoader(
				mSortBy);
		if (loader != null && !loader.isReset()) {
			getActivity().getLoaderManager()
					.restartLoader(mSortBy, null, this);
		} else {
			getActivity().getLoaderManager().initLoader(mSortBy, null, this);
		}
	}

}
