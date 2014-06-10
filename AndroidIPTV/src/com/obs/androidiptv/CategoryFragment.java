package com.obs.androidiptv;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.GridView;
import android.widget.Toast;

import com.obs.adapter.VODGridViewAdapter;
import com.obs.data.MediaDatum;
import com.obs.data.MediaDetailRes;
import com.obs.retrofit.OBSClient;

public class CategoryFragment extends Fragment {

	private static final String TAG = CategoryFragment.class.getName();
	private ProgressDialog mProgressDialog;
	private SearchDetails searchDtls;
	private SharedPreferences mPrefs;

	MyApplication mApplication = null;
	OBSClient mOBSClient;
	boolean mIsReqCanceled = false;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		final View rootView = inflater.inflate(R.layout.fragment_category,
				container, false);

		mApplication = ((MyApplication) getActivity().getApplicationContext());
		mOBSClient = mApplication.getOBSClient(getActivity());

		mPrefs = getActivity().getSharedPreferences(mApplication.PREFS_FILE, 0);
		String category = mPrefs.getString("CATEGORY", "RELEASE");
		searchDtls = new SearchDetails(rootView, getArguments()
				.getInt("pageno"), category);
		getDetails(searchDtls);
		return rootView;
	}

	public void getDetails(SearchDetails sd) {

		Log.d(TAG, "getDetails");

		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
		mProgressDialog = new ProgressDialog(getActivity(),
				ProgressDialog.THEME_HOLO_DARK);
		mProgressDialog.setMessage("Retriving Detials");
		mProgressDialog.setCanceledOnTouchOutside(false);
		mProgressDialog.setOnCancelListener(new OnCancelListener() {

			public void onCancel(DialogInterface arg0) {
				if (mProgressDialog.isShowing())
					mProgressDialog.dismiss();
				mIsReqCanceled = true;
			}
		});
		mProgressDialog.show();

		String deviceId = Settings.Secure.getString(getActivity()
				.getApplicationContext().getContentResolver(),
				Settings.Secure.ANDROID_ID);
		try {
			mOBSClient.getPageCountAndMediaDetails(searchDtls.category,
					searchDtls.pageNumber + "", deviceId,
					getPageCountAndDetailsCallBack);
		} catch (Exception e) {
			e.printStackTrace();
		}
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
							getActivity(),
							getActivity().getApplicationContext().getString(
									R.string.error_network), Toast.LENGTH_LONG)
							.show();
				} else {
					Toast.makeText(
							getActivity(),
							"Server Error : "
									+ retrofitError.getResponse().getStatus(),
							Toast.LENGTH_LONG).show();
				}
			} else
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
					updateDetails(objDetails, searchDtls.rootview);
				}
			} else
				mIsReqCanceled = false;
		}
	};

	public void updateDetails(final MediaDetailRes response, View rootview) {

		if (response != null && response.getMediaDetails().size() > 0) {

			searchDtls.pageNumber = response.getPageNo();
			final GridView gridView = (GridView) (rootview
					.findViewById(R.id.f_category_gv_movies));
			gridView.setAdapter(new VODGridViewAdapter(response
					.getMediaDetails(), getActivity()));
			gridView.setDrawSelectorOnTop(true);
			gridView.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View imageVw,
						int position, long arg3) {

					Intent intent = new Intent(getActivity(),
							VodMovieDetailsActivity.class);
					MediaDatum mediaObj = response.getMediaDetails().get(
							position);
					intent.putExtra("MediaId", mediaObj.getMediaId() + "");
					intent.putExtra("EventId", mediaObj.getEventId() + "");
					startActivity(intent);
				}
			});
			gridView.setOnItemSelectedListener(new OnItemSelectedListener() {

				@Override
				public void onItemSelected(AdapterView<?> arg0, View arg1,
						int arg2, long arg3) {
					if (arg1 != null) {
						arg1.startAnimation(AnimationUtils.loadAnimation(
								getActivity(), R.anim.zoom_selection));
					}
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {

				}
			});
		}
	}

	public class SearchDetails {
		public View rootview;
		public int pageNumber;
		public String category;

		public SearchDetails(View v, int pno, String category) {
			this.rootview = v;
			this.pageNumber = pno;
			this.category = category;
		}
	}

}
