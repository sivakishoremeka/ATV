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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.obs.androidiptv.MyApplication.SortBy;
import com.obs.data.DeviceDatum;
import com.obs.data.ServiceDatum;
import com.obs.database.DBHelper;
import com.obs.database.ServiceProvider;
import com.obs.retrofit.OBSClient;

public class ChannelsByDefOrderFrag extends Fragment implements
		LoaderManager.LoaderCallbacks<Cursor>,
		AdapterView.OnItemSelectedListener,
		AdapterView.OnItemLongClickListener, AdapterView.OnItemClickListener {

	private static final String TAG = ChannelsByDefOrderFrag.class.getName();
	private Callbacks mCallbacks = sDummyCallbacks;

	private ListView lv;
	private SimpleCursorAdapter adapter;
	private int mSelectedIdx = -1;

	private ProgressDialog mProgressDialog;

	String mSearchString;
	String mSelection;
	String[] mSelectionArgs;

	MyApplication mApplication = null;
	OBSClient mOBSClient;
	boolean mIsReqCanceled = false;

	//int mReqType = ServiceProvider.SERVICES;
	boolean mIsLiveDataReq = false;
	boolean mIsRefresh = false;
	boolean mIsBalCheckReq;
	float mBalance;
	int mSortBy = SortBy.DEFAULT.ordinal();

	public interface Callbacks {
		/**
		 * Callback for when an item has been selected.
		 */
		public void onItemSelected(ServiceDatum service, int selctionIndex);

		public void onItemClick(ServiceDatum data, int selctionIndex,int sortBy,String selection,String searchString);

		public void onItemLongClick(ServiceDatum service, int selctionIndex);
	}

	/**
	 * A dummy implementation of the {@link Callbacks} interface that does
	 * nothing. Used only when this fragment is not attached to an activity.
	 */
	private static Callbacks sDummyCallbacks = new Callbacks() {
		public void onItemSelected(ServiceDatum service, int selctionIndex) {
		}

		@Override
		public void onItemClick(ServiceDatum data, int selctionIndex,int sortBy,String selection,String searchString) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onItemLongClick(ServiceDatum service, int selctionIndex) {
			// TODO Auto-generated method stub

		}
	};

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public ChannelsByDefOrderFrag() {

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

		mApplication = (MyApplication) getActivity().getApplicationContext();
		mOBSClient = mApplication.getOBSClient(getActivity());
		mIsBalCheckReq = mApplication.isBalanceCheck();
		Calendar c = Calendar.getInstance();
		setRetainInstance(true);
		setHasOptionsMenu(true);
	}

	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_channels_default_order,
				container, false);
		lv = (ListView) v.findViewById(R.id.f_channels_lv);
		lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
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
			//mReqType = args.getInt("REQTYPE");
			mSortBy = args.getInt("SORTBY");
			mIsRefresh = args.getBoolean("ISREFRESH",false);
		}
		String[] from = new String[] { DBHelper.CHANNEL_DESC };
		int[] to = new int[] { R.id.ch_lv_item_tv_ch_Name };
		adapter = new SimpleCursorAdapter(getActivity(),
				R.layout.f_list_channel_item, null, from, to, 0);
		lv.setAdapter(adapter);
		lv.setLongClickable(true);
		lv.setOnItemSelectedListener(this);
		lv.setOnItemLongClickListener(this);
		lv.setOnItemClickListener(this);
		
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
		CursorLoader loader = null;
		
		//using sortOrder arg for passing both mIsRefresh&SortOrder
		String sortOrder = mIsRefresh+"&";
		
		if (id == SortBy.DEFAULT.ordinal()) {
			loader = new CursorLoader(getActivity(),
					ServiceProvider.SERVICES_URI, null, mSelection,
					mSelectionArgs, sortOrder);
		}
		return loader;
	}

	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		// Log.d("ChannelsActivity","onLoadFinished");
		if (mProgressDialog != null && mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
		if (null != cursor && cursor.getCount() != 0) {
			adapter.swapCursor(cursor);
			lv.setSelection(0);
			lv.setSelected(true);
			cursor.moveToFirst();
			mSelectedIdx = 0;
			mCallbacks
					.onItemSelected(
							ChannelsActivity.getServiceFromCursor(cursor),
							mSelectedIdx);
		} else {
			adapter.swapCursor(null);
			((ChannelsActivity) getActivity()).updateEPGDetails(null);
		}
	}

	public void onLoaderReset(Loader<Cursor> loader) {
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {

		ServiceDatum data = ChannelsActivity
				.getServiceFromCursor(((Cursor) parent.getAdapter().getItem(
						position)));
		mSelectedIdx = position;
		mCallbacks.onItemClick(data,mSelectedIdx,mSortBy,mSelection,mSearchString);
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		mSelectedIdx = position;
		ServiceDatum data = ChannelsActivity
				.getServiceFromCursor(((Cursor) parent.getAdapter().getItem(
						position)));
		mCallbacks.onItemLongClick(data, mSelectedIdx);
		return true;
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int position,
			long id) {
		if (position != mSelectedIdx) {
			mSelectedIdx = position;
			ServiceDatum data = ChannelsActivity
					.getServiceFromCursor(((Cursor) parent.getAdapter()
							.getItem(position)));
			mCallbacks.onItemSelected(data, mSelectedIdx);
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