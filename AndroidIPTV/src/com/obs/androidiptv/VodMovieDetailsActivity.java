package com.obs.androidiptv;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.obs.data.ResponseObj;
import com.obs.retrofit.OBSClient;
import com.obs.utils.Utilities;

public class VodMovieDetailsActivity extends Activity {

	public static String TAG = VodMovieDetailsActivity.class.getName();
	private final static String NETWORK_ERROR = "NETWORK_ERROR";
	private final static String BOOK_ORDER = "BOOK_ORDER";
	private ProgressDialog mProgressDialog;
	String mediaId;
	String eventId;

	MyApplication mApplication = null;
	OBSClient mOBSClient;
	ExecutorService mExecutorService;
	boolean mIsReqCanceled = false;
	String mDeviceId;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_vod_mov_details);
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		Bundle b = getIntent().getExtras();
		mediaId = b.getString("MediaId");
		eventId = b.getString("EventId");

		mApplication = ((MyApplication) getApplicationContext());
		mExecutorService = Executors.newCachedThreadPool();
		mOBSClient = mApplication.getOBSClient(this, mExecutorService);

		mDeviceId = Settings.Secure.getString(
				mApplication.getContentResolver(), Settings.Secure.ANDROID_ID);

		if ((!(mediaId.equalsIgnoreCase("")) || mediaId != null)
				&& (!(eventId.equalsIgnoreCase("")) || eventId != null)) {
			RelativeLayout rl = (RelativeLayout) findViewById(R.id.a_vod_mov_dtls_root_layout);
			rl.setVisibility(View.INVISIBLE);
			UpdateDetails();
		}

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			break;
		default:
			break;
		}
		return true;
	}

	public void btnOnClick(View v) {

		Log.d("Btn Click", ((Button) v).getText().toString());

		AlertDialog dialog = new AlertDialog.Builder(
				VodMovieDetailsActivity.this, AlertDialog.THEME_HOLO_LIGHT)
				.create();
		dialog.setIcon(R.drawable.ic_logo_confirm_dialog);
		dialog.setTitle("Confirmation");
		dialog.setMessage("Do you want to continue?");
		dialog.setCancelable(false);

		dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Yes",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int buttonId) {
						BookOrder();
					}
				});
		dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "No",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int buttonId) {

					}
				});
		dialog.show();
	}

	private void BookOrder() {
		new doBackGround().execute(BOOK_ORDER, "HD", "RENT");
	}

	public void UpdateDetails() {
		mOBSClient.getMediaDetails(mediaId, eventId, mDeviceId,
				getMovDetailsCallBack);
	}

	final Callback<Object> getMovDetailsCallBack = new Callback<Object>() {
		@Override
		public void failure(RetrofitError retrofitError) {
			if (!mIsReqCanceled) {
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
				if (retrofitError.isNetworkError()) {
					Toast.makeText(
							VodMovieDetailsActivity.this,
							getApplicationContext().getString(
									R.string.error_network), Toast.LENGTH_LONG)
							.show();
				} else {
					Toast.makeText(
							VodMovieDetailsActivity.this,
							"Server Error : "
									+ retrofitError.getResponse().getStatus(),
							Toast.LENGTH_LONG).show();
				}
			}
		}

		@Override
		public void success(Object objDetails, Response response) {
			if (!mIsReqCanceled) {
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
					mProgressDialog = null;
				}
				if (response != null) {
					String resOnSuccess = mApplication
							.getResponseOnSuccess(response);
					if (resOnSuccess != null) {
						updateUI(resOnSuccess);
					}
				} else {
					Toast.makeText(VodMovieDetailsActivity.this,
							"Server Error  ", Toast.LENGTH_LONG).show();
				}
			}
		}
	};

	private class doBackGround extends AsyncTask<String, Void, ResponseObj> {
		private String taskName = "";

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			if (mProgressDialog != null) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}
			mProgressDialog = new ProgressDialog(VodMovieDetailsActivity.this,
					ProgressDialog.THEME_HOLO_DARK);
			mProgressDialog.setMessage("Retrieving Details...");
			mProgressDialog.setCancelable(true);
			mProgressDialog.show();
		}

		@Override
		protected ResponseObj doInBackground(String... params) {
			taskName = params[0];
			ResponseObj resObj = new ResponseObj();
			if (Utilities.isNetworkAvailable(VodMovieDetailsActivity.this
					.getApplicationContext())) {

				HashMap<String, String> map = new HashMap<String, String>();
				String sDateFormat = "yyyy-mm-dd";
				DateFormat df = new SimpleDateFormat(sDateFormat);
				String formattedDate = df.format(new Date());

				map.put("TagURL", "/eventorder");
				map.put("locale", "en");
				map.put("dateFormat", sDateFormat);
				map.put("eventBookedDate", formattedDate);
				map.put("formatType", params[1]);
				map.put("optType", params[2]);
				map.put("eventId", eventId);
				map.put("deviceId", mDeviceId);

				resObj = Utilities.callExternalApiPostMethod(
						VodMovieDetailsActivity.this.getApplicationContext(),
						map);
				return resObj;
			} else {
				resObj.setFailResponse(100, NETWORK_ERROR);
				return resObj;
			}
		}

		@Override
		protected void onPostExecute(ResponseObj resObj) {
			super.onPostExecute(resObj);

			Log.d(TAG, "onPostExecute");

			if (resObj.getStatusCode() == 200) {
				if (mProgressDialog.isShowing()) {
					mProgressDialog.dismiss();
				}
				Intent intent = new Intent();
				try {
					intent.putExtra("URL",
							((String) (new JSONObject(resObj.getsResponse()))
									.get("resourceIdentifier")));
					intent.putExtra("VIDEOTYPE", "VOD");
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				((MyApplication) (VodMovieDetailsActivity.this
						.getApplicationContext())).startPlayer(intent,
						VodMovieDetailsActivity.this);
				finish();
			} else {
				if (mProgressDialog.isShowing()) {
					mProgressDialog.dismiss();
				}
				AlertDialog.Builder builder = new AlertDialog.Builder(
						VodMovieDetailsActivity.this,
						AlertDialog.THEME_HOLO_LIGHT);
				// Add the buttons
				builder.setPositiveButton("OK",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								// MovieDetailsActivity.this.finish();
							}
						});
				AlertDialog dialog = builder.create();
				dialog.setMessage(resObj.getsErrorMessage());
				dialog.show();
			}
		}

	}

	public void updateUI(String json) {
		if (json != null && json.length() != 0) {

			MovieDetailsObj obj = new MovieDetailsObj(json);

			ImageLoader.getInstance().displayImage(obj.image,
					((ImageView) findViewById(R.id.a_vod_mov_dtls_iv_mov_img)));

			((RatingBar) findViewById(R.id.a_vod_mov_dtls_rating_bar))
					.setRating(Float.parseFloat(obj.rating));

			((TextView) findViewById(R.id.a_vod_mov_dtls_tv_mov_title))
					.setText(obj.title);
			((TextView) findViewById(R.id.a_vod_mov_dtls_tv_descr_value))
					.setText(obj.overview);
			((TextView) findViewById(R.id.a_vod_mov_dtls_tv_durn_value))
					.setText(obj.duration);
			((TextView) findViewById(R.id.a_vod_mov_dtls_tv_lang_value))
					.setText(getResources()
							.getStringArray(R.array.arrLangauges)[1]);
			/*
			 * ((TextView) findViewById(R.id.a_vod_mov_dtls_tv_lang_value))
			 * .setText
			 * (getResources().getStringArray(R.array.arrLangauges)[obj.language
			 * ]);
			 */
			((TextView) findViewById(R.id.a_vod_mov_dtls_tv_release_value))
					.setText(obj.releaseDate);
			((TextView) findViewById(R.id.a_vod_mov_dtls_tv_cast_value))
					.setText(obj.Actors);

			RelativeLayout rl = (RelativeLayout) findViewById(R.id.a_vod_mov_dtls_root_layout);
			if (rl.getVisibility() == View.INVISIBLE)
				rl.setVisibility(View.VISIBLE);
		}
	}

	private class MovieDetailsObj {
		public String image;
		public String rating;
		public String title;
		public String overview;
		public String duration;
		public int language;
		public String releaseDate;
		public String Actors;

		public MovieDetailsObj(String json) {
			parseJson(json);
		}

		private void parseJson(String json) {
			JSONObject movieDtls;
			try {
				movieDtls = new JSONObject(json);

				String Replace = "[\\[\\]\"]";
				String ReplaceTo = "";

				title = movieDtls.getString("title").replaceAll(Replace,
						ReplaceTo);
				image = movieDtls.getString("image").replaceAll(Replace,
						ReplaceTo);
				rating = movieDtls.getString("rating").replaceAll(Replace,
						ReplaceTo);
				overview = movieDtls.getString("overview").replaceAll(Replace,
						ReplaceTo);
				duration = movieDtls.getString("duration").replaceAll(Replace,
						ReplaceTo);
				releaseDate = movieDtls.getString("releaseDate").replaceAll(
						Replace, ReplaceTo);

				Actors = movieDtls.getString("Actor").replaceAll(Replace,
						ReplaceTo);

				JSONArray jsonLocnArr = new JSONArray(
						movieDtls.getString("filmLocations"));
				JSONObject filmLocObj = (JSONObject) jsonLocnArr.get(0);
				language = filmLocObj.getInt("languageId");
			} catch (JSONException e) {
				Log.d(TAG, e.getMessage());
			}

		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == 4) {
			if (mProgressDialog != null) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}
			mIsReqCanceled = true;
			mExecutorService.shutdownNow();
			this.finish();
		} else if (keyCode == 23) {
			View focusedView = getWindow().getCurrentFocus();
			focusedView.performClick();
		}
		return super.onKeyDown(keyCode, event);
	}
}
