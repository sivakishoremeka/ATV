package com.obs.androidiptv;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONException;
import org.json.JSONObject;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.obs.adapter.EPGDetailsAdapter;
import com.obs.data.EPGData;
import com.obs.data.EpgDatum;
import com.obs.retrofit.OBSClient;

public class EpgFragment extends Fragment {

	private static final String TAG = "EpgFragment";
	public final static String IS_REFRESH = "isRefresh";
	private ProgressDialog mProgressDialog;
	private SharedPreferences mPrefs;
	private Editor mPrefsEditor;
	public static final String ARG_SECTION_DATE = "section_date";
	ListView list;
	String reqestedDate = null;
	boolean isRefresh = false;

	MyApplication mApplication = null;
	OBSClient mOBSClient;
	ExecutorService mExecutorService;
	boolean mIsReqCanceled = false;
	EpgReqDetails mReqDetails = null;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		final View rootView = inflater.inflate(R.layout.fragment_epg,
				container, false);

		mApplication = ((MyApplication) getActivity().getApplicationContext());
		mExecutorService = Executors.newCachedThreadPool();
		mOBSClient = mApplication.getOBSClient(getActivity(), mExecutorService);

		mPrefs = getActivity().getSharedPreferences(mApplication.PREFS_FILE, 0);
		list = (ListView) rootView.findViewById(R.id.fr_epg_lv_epg_dtls);
		list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		String channelName = mPrefs.getString(IPTVActivity.CHANNEL_NAME, "");
		reqestedDate = getArguments().getString(EpgFragment.ARG_SECTION_DATE);
		isRefresh = mPrefs.getBoolean(IS_REFRESH, false);
		mReqDetails = new EpgReqDetails(rootView, reqestedDate, channelName);
		CheckCacheForEpgDetails(mReqDetails);
		getActivity().findViewById(R.id.a_iptv_rl_root_layout).setVisibility(
				View.VISIBLE);
		return rootView;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
	}

	public void CheckCacheForEpgDetails(EpgReqDetails rd) {

		Log.d(TAG, "frag-getDetails");

		String Epg_Dtls_key = rd.channelName + "_EPG_Details";
		String Epg_Dtls_value = mPrefs.getString(Epg_Dtls_key, "");
		String req_date_Dtls = null;
		boolean getServerData = false;
		getServerData = isRefresh;
		if (!isRefresh) {
			if (Epg_Dtls_value.length() == 0) {
				getServerData = true;
			} else {
				JSONObject json = null;
				try {
					json = new JSONObject(Epg_Dtls_value);
					req_date_Dtls = json.getString(rd.date);
				} catch (JSONException e) {
					req_date_Dtls = "";
					e.printStackTrace();
				}
				if (req_date_Dtls == null || req_date_Dtls.length() == 0) {
					getServerData = true;
				} else {
					getServerData = false;
				}
			}
		}
		if (getServerData) {
			try {
				getEpgDetailsFromServer(rd);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			updateDetails(getEPGListFromJSON(req_date_Dtls), rd.rootview);
		}
	}

	private List<EpgDatum> getEPGListFromJSON(String json) {
		java.lang.reflect.Type t = new TypeToken<List<EpgDatum>>() {
		}.getType();
		List<EpgDatum> EPGList = new Gson().fromJson(json, t);
		return EPGList;
	}

	private void getEpgDetailsFromServer(EpgReqDetails rd) {

		Log.d(TAG, "GetChannelsFromServer");

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
				if (null != mExecutorService)
					if (!mExecutorService.isShutdown())
						mExecutorService.shutdownNow();
			}
		});
		mProgressDialog.show();
		mOBSClient
				.getEPGDetails(rd.channelName, rd.date, getEPGDetailsCallBack);

	}

	final Callback<EPGData> getEPGDetailsCallBack = new Callback<EPGData>() {
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
			}
		}

		@Override
		public void success(EPGData data, Response response) {
			List<EpgDatum> ProgGuideList = null;
			if (!mIsReqCanceled) {
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}

				if (data != null)
					ProgGuideList = data.getEpgData();
				if (ProgGuideList != null && ProgGuideList.size() > 0) {

					/** saving epg details to preferences */
					mPrefsEditor = mPrefs.edit();
					String Epg_Dtls_key = mReqDetails.channelName
							+ "_EPG_Details";
					String Epg_Dtls_value = mPrefs.getString(Epg_Dtls_key, "");
					JSONObject json = null, jsonReq = null;
					try {
						if (Epg_Dtls_value.length() == 0) {
							json = new JSONObject();
						} else {
							json = new JSONObject(Epg_Dtls_value);
							jsonReq = new JSONObject();
							Calendar c = Calendar.getInstance();
							Calendar curr = Calendar.getInstance();
							Date cDate = MyApplication.df
									.parse(MyApplication.df.format(curr
											.getTime()));
							Date keyDate = null;
							Iterator<String> i = json.keys();
							while (i.hasNext()) {
								String key = i.next();
								c.setTime(MyApplication.df.parse(key));
								keyDate = c.getTime();
								if (keyDate.compareTo(cDate) != -1) {
									jsonReq.put(key, json.get(key));
								}
							}
							json = jsonReq;
						}
						json.put(mReqDetails.date,
								new Gson().toJson(ProgGuideList));
						Epg_Dtls_value = json.toString();
					} catch (JSONException e) {
						e.printStackTrace();
					} catch (ParseException e) {
						e.printStackTrace();
					}
					mPrefsEditor.putString(Epg_Dtls_key, Epg_Dtls_value);
					mPrefsEditor.commit();

					/** updating fragment **/
					updateDetails(ProgGuideList, mReqDetails.rootview);
				}/*
				 * else { mErrDialog.setMessage("EPG Data is not Available");
				 * mErrDialog.show(); }
				 */
			}
		}
	};

	public void updateDetails(final List<EpgDatum> mProgGuideList, View rootview) {
		Log.d(TAG, "frag-updateDetails :");

		if (mProgGuideList != null && mProgGuideList.size() > 0) {
			EPGDetailsAdapter adapter = new EPGDetailsAdapter(getActivity(),
					mProgGuideList);
			list.setAdapter(adapter);
			list.setOnItemSelectedListener(new OnItemSelectedListener() {

				@Override
				public void onItemSelected(AdapterView<?> adapterView, View view,
						int position, long arg3) {
					OnItemSelectedOrClicked(adapterView,position,mProgGuideList);
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {
					//nothing selected
					
				}
				
			});
			list.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> adapterView, View view,
						int position, long arg3) {
					OnItemSelectedOrClicked(adapterView,position,mProgGuideList);
				}
			});
			if (mProgGuideList != null) {
				Calendar c = Calendar.getInstance();
				String date = MyApplication.df.format(c.getTime());
				Date d1 = null, d2 = null;
				try {
					d1 = MyApplication.df.parse(reqestedDate);
					d2 = MyApplication.df.parse(date);

				} catch (ParseException e) {
					e.printStackTrace();
				}

				if (d1.compareTo(d2) == 0) {

					for (int i = 0; i < mProgGuideList.size(); i++) {

						EpgDatum data = mProgGuideList.get(i);
						if (isCurrentProgramme(data)) {
							
							list.smoothScrollToPosition(i);
							list.setItemChecked(i, true);
							list.setSelection(i);
							list.setSelected(true);
							TextView chName = (TextView) getActivity()
									.findViewById(R.id.a_iptv_tv_ch_name);
							TextView progName = (TextView) getActivity()
									.findViewById(R.id.a_iptv_tv_Prog_name);
							TextView stTime = (TextView) getActivity()
									.findViewById(
											R.id.a_iptv_tv_prog_start_time);
							TextView endTime = (TextView) getActivity()
									.findViewById(R.id.a_iptv_tv_prog_end_time);
							TextView progDescr = (TextView) getActivity()
									.findViewById(R.id.a_iptv_tv_prog_desc);
							Button btn = (Button) getActivity().findViewById(
									R.id.a_iptv_btn_watch_remind);
							btn.setText(R.string.watch);
							chName.setText(data.getChannelName());
							progName.setText(data.getProgramTitle());
							SimpleDateFormat tf = new SimpleDateFormat("HH:mm");
							Date sTime = null, eTime = null;
							try {
								sTime = tf.parse(data.getStartTime());
								eTime = tf.parse(data.getStopTime());
							} catch (ParseException e) {
								e.printStackTrace();
							}
							stTime.setText("Start Time: " + tf.format(sTime));
							endTime.setText("End Time  : " + tf.format(eTime));
							progDescr.setText(data.getProgramDescription());
						}
					}
				}
			}
		}

	}

	
	protected void OnItemSelectedOrClicked(AdapterView<?> adapterView,
			int position,List<EpgDatum> mProgGuideList) {

		((AbsListView) adapterView).setItemChecked(position, true);
		((AbsListView) adapterView).smoothScrollToPosition(position);
		EpgDatum data = mProgGuideList.get(position);
		TextView chName = (TextView) getActivity().findViewById(
				R.id.a_iptv_tv_ch_name);
		TextView progName = (TextView) getActivity().findViewById(
				R.id.a_iptv_tv_Prog_name);
		TextView stTime = (TextView) getActivity().findViewById(
				R.id.a_iptv_tv_prog_start_time);
		TextView endTime = (TextView) getActivity().findViewById(
				R.id.a_iptv_tv_prog_end_time);
		TextView progDescr = (TextView) getActivity().findViewById(
				R.id.a_iptv_tv_prog_desc);
		chName.setText(data.getChannelName());
		progName.setText(data.getProgramTitle());
		SimpleDateFormat tf = new SimpleDateFormat("HH:mm");
		Date sTime = null, eTime = null;
		try {
			sTime = tf.parse(data.getStartTime());
			eTime = tf.parse(data.getStopTime());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		stTime.setText("Start Time: " + tf.format(sTime));
		endTime.setText("End Time  : " + tf.format(eTime));
		progDescr.setText(data.getProgramDescription());

		Button btn = (Button) getActivity().findViewById(
				R.id.a_iptv_btn_watch_remind);
		if (isCurrentProgramme(data))
			btn.setText(R.string.watch);
		else {
			SimpleDateFormat df1 = new SimpleDateFormat(
					"yyyy-MM-dd", new Locale("en"));
			Calendar c = Calendar.getInstance();
			String progStartTime = data.getStartTime();
			Calendar t = Calendar.getInstance();
			try {
				c.setTime(df1.parse(reqestedDate));
				t.setTime(tf.parse(progStartTime));
			} catch (ParseException e) {
				e.printStackTrace();
			}
			c.set(Calendar.HOUR_OF_DAY, t.get(Calendar.HOUR_OF_DAY));
			c.set(Calendar.MINUTE, t.get(Calendar.MINUTE));
			c.set(Calendar.SECOND, 0);
			ProgDetails progDtls = new ProgDetails(c, data
					.getProgramTitle(), data.getChannelName());
			btn.setTag(progDtls);
			btn.setText(R.string.remind_me);
		}
		
	}

	private boolean isCurrentProgramme(EpgDatum data) {

		String progStartTime = data.getStartTime();
		String progStopTime = data.getStopTime();
		SimpleDateFormat tf = new SimpleDateFormat("HH:mm:ss");
		Date time1 = null, time2 = null;
		Date time3 = new Date();
		String t3 = tf.format(time3);

		try {
			time1 = tf.parse(progStartTime);
			time2 = tf.parse(progStopTime);
			if (time1.compareTo(time2) > 0)
				time2 = tf.parse("24:00:00");
			time3 = tf.parse(t3);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if ((time3.compareTo(time1) > 0 || time3.compareTo(time1) == 0)
				&& (time3.compareTo(time2) < 0 || time3.compareTo(time2) == 0)) {
			return true;
		}
		return false;
	}

	public class EpgReqDetails {
		public View rootview;
		public String date;
		public String channelName;

		public EpgReqDetails(View v, String date, String channelName) {
			this.rootview = v;
			this.date = date;
			this.channelName = channelName;
		}
	}

	public class ProgDetails {
		public Calendar calendar;
		public String progTitle;
		public String channelName;

		public ProgDetails(Calendar cal, String title, String chName) {
			this.calendar = cal;
			this.progTitle = title;
			this.channelName = chName;
		}
	}

}
