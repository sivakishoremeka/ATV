package com.obs.androidiptv;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.obs.androidiptv.MyApplication.SetAppState;
import com.obs.androidiptv.MyApplication.SortBy;
import com.obs.data.ServiceDatum;
import com.obs.database.DBHelper;
import com.obs.database.ServiceProvider;
import com.obs.service.DoBGTasksService;

public class VideoPlayerActivity extends Activity implements
		SurfaceHolder.Callback, MediaPlayer.OnPreparedListener,
		MediaPlayer.OnErrorListener, VideoControllerView.MediaPlayerControl {

	public static String TAG = VideoPlayerActivity.class.getName();
	public static int mChannelId = -1;
	public static int mChannelIndex;
	public static Uri mChannelUri;
	SurfaceView videoSurface;
	MediaPlayer player;
	VideoControllerView controller;
	private ProgressDialog mProgressDialog;
	private boolean isLiveController;
	private ArrayList<ServiceDatum> mserviceList = new ArrayList<ServiceDatum>();
	private String mVideoType = null;
	private String mIsRefresh = "false";
	private int mSortBy = 0;
	private String mSearchString;
	private String mSelection;
	private String[] mSelectionArgs;

	public boolean stopThread = true;
	public int currentPosition = 0;
	public int lastPosition = 0;
	BufferrChk bChk = null;
	private Handler threadHandler = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.what == 1) {
				if (mProgressDialog != null && !mProgressDialog.isShowing()) {
					showProgressDialog("Buffering...");
				} else if (mProgressDialog == null) {
					showProgressDialog("Buffering...");
				}
			} else if (mProgressDialog != null && mProgressDialog.isShowing()) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
				stopThread = true;
			}
		}
	};
	
	public void showProgressDialog(String msg){
		mProgressDialog = new ProgressDialog(
				VideoPlayerActivity.this,
				ProgressDialog.THEME_HOLO_DARK);
		mProgressDialog.setMessage(msg);
		mProgressDialog.setCancelable(true);
		mProgressDialog.setCanceledOnTouchOutside(false);
		mProgressDialog.setOnKeyListener(new OnKeyListener() {
			
			@Override
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				return dispatchKeyEvent(event);
			}
		});
		mProgressDialog.show();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_video_player);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {

			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		} else {
			View decorView = getWindow().getDecorView();
			int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_LOW_PROFILE;
			decorView.setSystemUiVisibility(uiOptions);
		}

		videoSurface = (SurfaceView) findViewById(R.id.videoSurface);
		SurfaceHolder videoHolder = videoSurface.getHolder();
		videoHolder.addCallback(this);
		player = new MediaPlayer();

		mVideoType = getIntent().getStringExtra("VIDEOTYPE");
		if (mVideoType.equalsIgnoreCase("LIVETV")) {
			isLiveController = true;
			VideoControllerView.sDefaultTimeout = 3000;
			mChannelId = getIntent().getIntExtra("CHANNELID", -1);
			prepareChannelsList();
			if (mChannelId != -1) {
				mChannelIndex = getChannelIndexByChannelId(mChannelId);
				// Log.d("mChannelIndex", "" + mChannelIndex);
			}
		} else if (mVideoType.equalsIgnoreCase("VOD")) {
			isLiveController = false;
			VideoControllerView.sDefaultTimeout = 3000;
		}
		controller = new VideoControllerView(this, (!isLiveController),
				mserviceList);
		try {
			player.setAudioStreamType(AudioManager.STREAM_MUSIC);
			player.setVolume(1.0f, 1.0f);
			player.setDataSource(this,
					Uri.parse(getIntent().getStringExtra("URL")));

			player.setOnPreparedListener(this);
			player.setOnErrorListener(this);
		} catch (IllegalArgumentException e) {
			Log.d(TAG, e.getMessage());
		} catch (SecurityException e) {
			Log.d(TAG, e.getMessage());
		} catch (IllegalStateException e) {
			Log.d(TAG, e.getMessage());
		} catch (IOException e) {
			Log.d(TAG, e.getMessage());
		} catch (Exception e) {
			Log.d(TAG, e.getMessage());
		}
		/*
		 * getLoaderManager().initLoader(getIntent().getIntExtra("REQTYPE", 1),
		 * null, this);
		 */
	}

	@Override
	protected void onStart() {

		// Log.d(TAG, "OnStart");
		MyApplication.startCount++;
		if (!MyApplication.isActive) {
			// Log.d(TAG, "SendIntent");
			Intent intent = new Intent(this, DoBGTasksService.class);
			intent.putExtra(DoBGTasksService.App_State_Req,
					SetAppState.SET_ACTIVE.ordinal());
			startService(intent);
		}
		super.onStart();
	}

	@Override
	protected void onStop() {
		// Log.d(TAG, "onStop");
		MyApplication.stopCount++;
		if (MyApplication.stopCount == MyApplication.startCount
				&& MyApplication.isActive) {
			// Log.d("sendIntent", "SendIntent");
			Intent intent = new Intent(this, DoBGTasksService.class);
			intent.putExtra(DoBGTasksService.App_State_Req,
					SetAppState.SET_INACTIVE.ordinal());
			startService(intent);
		}
		super.onStop();
	}
	private void prepareChannelsList() {
		Cursor cursor = null;
		mSelection = getIntent().getStringExtra("SELECTION");
		mSearchString = getIntent().getStringExtra("SEARCHSTRING");
		mSortBy = getIntent().getIntExtra("SORTBY", 0);
		if (mSearchString != null)
			mSelectionArgs = new String[] { "%" + mSearchString + "%" };
		else
			mSelectionArgs = null;

		if (mSortBy == SortBy.DEFAULT.ordinal()) {
			cursor = getContentResolver().query(ServiceProvider.SERVICES_URI,
					null, mSelection, mSelectionArgs, null);
		} else if (mSortBy == SortBy.CATEGORY.ordinal()) {
			cursor = getContentResolver().query(
					ServiceProvider.SERVICE_CATEGORIES_URI, null, null, null,
					null);
		} else if (mSortBy == SortBy.LANGUAGE.ordinal()) {
			cursor = getContentResolver().query(
					ServiceProvider.SERVICE_SUB_CATEGORIES_URI, null, null,
					null, null);
		}

		if (cursor != null)
			loadServicesfromCursor(cursor);
	}

	private int getChannelIndexByChannelId(int channelId) {
		int idx = -1;
		if (null != mserviceList) {
			for (int i = 0; i < mserviceList.size(); i++) {
				if (mserviceList.get(i).getServiceId() == channelId) {
					idx = i;
				}
			}
		}
		return idx;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		// Log.d(TAG, "onTouchEvent" + event.getAction());
		controller.show();
		return false;
	}

	// Implement SurfaceHolder.Callback
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {

		// Log.d("VideoPlayerActivity", "surfaceCreated");

		player.setDisplay(holder);
		/*
		 * getLoaderManager().initLoader(getIntent().getIntExtra("REQTYPE", 1),
		 * null, this);
		 */
		player.prepareAsync();
		if (mProgressDialog != null && mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
		mProgressDialog = new ProgressDialog(VideoPlayerActivity.this,
				ProgressDialog.THEME_HOLO_DARK);
		mProgressDialog.setMessage("Starting MediaPlayer");
		mProgressDialog.setCanceledOnTouchOutside(false);
		mProgressDialog.setOnCancelListener(new OnCancelListener() {

			public void onCancel(DialogInterface arg0) {
				if (mProgressDialog.isShowing())
					mProgressDialog.dismiss();
				mProgressDialog = null;
				finish();
			}
		});
		mProgressDialog.show();
	}

	@Override
	protected void onPause() {
		// Log.d("VideoPlayerActivity", "surfaceDestroyed");
		if (player != null && player.isPlaying()) {
			stopThread = true;
			controller.hide();
			player.stop();
			player.release();
			player = null;
			finish();
		} else {

		}
		super.onPause();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		stopThread = true;
	}
	
	@Override
	public void onDestroy() {
	    super.onDestroy();
	    if (mProgressDialog != null && mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
	}

	// End SurfaceHolder.Callback

	// Implement MediaPlayer.OnPreparedListener
	@Override
	public void onPrepared(MediaPlayer mp) {

		// Log.d("VideoPlayerActivity", "onPrepared");

		controller.setMediaPlayer(this);
		RelativeLayout rlayout = (RelativeLayout) findViewById(R.id.video_container);
		rlayout.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
		controller.setAnchorView(rlayout);
		controller
				.setAnchorView((RelativeLayout) findViewById(R.id.video_container));
		if (mProgressDialog != null && mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
		bChk = new BufferrChk();
		stopThread = false;
		bChk.start();
		mp.start();
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		// Log.d(TAG, "Media player Error is...what:" + what + " Extra:" +
		// extra);
		stopThread = true;
		if (what == MediaPlayer.MEDIA_ERROR_UNKNOWN && extra == -2147483648) {

			if (mProgressDialog != null && mProgressDialog.isShowing()) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}
			Toast.makeText(
					getApplicationContext(),
					"Incorrect URL or Unsupported Media Format.Media player closed.",
					Toast.LENGTH_LONG).show();
		} else if (what == MediaPlayer.MEDIA_ERROR_UNKNOWN && extra == -1004) {

			if (mProgressDialog != null && mProgressDialog.isShowing()) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}
			Toast.makeText(
					getApplicationContext(),
					"Invalid Stream for this channel... Please try other channel",
					Toast.LENGTH_LONG).show();
		} else {
			controller.mHandler.removeMessages(controller.SHOW_PROGRESS);
			controller.mHandler.removeMessages(controller.FADE_OUT);
			changeChannel(mChannelUri, mChannelId);
		}

		return true;
	}

	// End MediaPlayer.OnPreparedListener

	// Implement VideoMediaController.MediaPlayerControl
	@Override
	public boolean canPause() {
		return true;
	}

	@Override
	public boolean canSeekBackward() {
		return true;
	}

	@Override
	public boolean canSeekForward() {
		return true;
	}

	@Override
	public int getBufferPercentage() {
		return 0;
	}

	@Override
	public int getCurrentPosition() {
		return player.getCurrentPosition();
	}

	@Override
	public int getDuration() {
		return player.getDuration();
	}

	@Override
	public boolean isPlaying() {
		return player.isPlaying();
	}

	@Override
	public void pause() {
		player.pause();
	}

	@Override
	public void seekTo(int i) {
		player.seekTo(i);
	}

	@Override
	public void start() {
		player.start();
	}

	@Override
	public boolean isFullScreen() {
		return false;
	}

	/*
	 * @Override public void toggleFullScreen() {
	 * 
	 * }
	 */

	// End VideoMediaController.MediaPlayerControl

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// Log.d("onKeyDown", keyCode + "");
		if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == 4) {

			// Log.d("onKeyDown", "KeyCodeback");
			stopThread = true;
			if (player != null) {
				if (player.isPlaying()) {
					controller.hide();
					player.stop();
				}
				player.release();
				player = null;
			}
			finish(); /*
					 * } else if (keyCode == 85) { controller.show(); if
					 * (player.isPlaying()) { player.pause(); } else {
					 * player.start(); } } else if (keyCode == 23) {
					 * controller.show(); player.pause(); } else if (keyCode ==
					 * 19) { controller.show(); player.seekTo(0);
					 * player.start(); } else if (keyCode == 89) {
					 * controller.show(); if (player.getCurrentPosition() -
					 * 120000 > 0 && (player.isPlaying())) {
					 * player.seekTo(player.getCurrentPosition() - 120000);
					 * player.start(); } } else if (keyCode == 90) {
					 * controller.show(); if (player.getCurrentPosition() +
					 * 120000 < player.getDuration() && (player.isPlaying())) {
					 * player.seekTo(player.getCurrentPosition() + 120000);
					 * player.start(); }
					 */
		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
				|| keyCode == KeyEvent.KEYCODE_VOLUME_UP
				|| keyCode == KeyEvent.KEYCODE_VOLUME_MUTE
				|| keyCode == KeyEvent.KEYCODE_DPAD_LEFT
				|| keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
			AudioManager audio = (AudioManager) getSystemService(VideoPlayerActivity.this.AUDIO_SERVICE);
			switch (keyCode) {
			case KeyEvent.KEYCODE_DPAD_RIGHT:
			case KeyEvent.KEYCODE_VOLUME_UP:
				audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
						AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
				return true;
			case KeyEvent.KEYCODE_DPAD_LEFT:
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
						AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
				return true;
			default:
				return super.dispatchKeyEvent(event);
			}
		} else if (keyCode == KeyEvent.KEYCODE_MENU) {
			// Log.d(TAG, "onMenuKeyDownEvent" + event.getAction());
			controller.show();
			return true;
		} else if (keyCode >= 7 && keyCode <= 16) {
			if (null != player) {
				if (mserviceList != null) {
					int idx = keyCode - 7;
					if (idx <= (mserviceList.size() - 1)) {
						ServiceDatum service = mserviceList.get(keyCode - 7);
						if (service != null) {
							mChannelId = service.getServiceId();
							changeChannel(Uri.parse(service.getUrl()),
									mChannelId);
						}
					}
				}
				return true;
			}
		} else if (keyCode == 19 || keyCode == 20) {
			if (null != player) {

				if (mserviceList != null && mChannelId != -1) {
					mChannelIndex = getChannelIndexByChannelId(mChannelId);
					if (keyCode == 20) {
						mChannelIndex++;
						if (mChannelIndex == mserviceList.size())
							mChannelIndex = 0;
					} else if (keyCode == 19) {
						mChannelIndex--;
						if (mChannelIndex < 0)
							mChannelIndex = mserviceList.size() - 1;
					}
					changeChannel(
							Uri.parse(mserviceList.get(mChannelIndex).getUrl()),
							mserviceList.get(mChannelIndex).getServiceId());
				}
				return true;
			}
		} else if (keyCode == 23) { // 23

			if (controller.isShowing()) {
				controller.hide();
			} else {
				controller.show();
			}
		} else
			super.onKeyDown(keyCode, event);
		return true;
	}

	@Override
	public void changeChannel(Uri uri, int channelId) {
		// Log.d(TAG, "mChannelIndex: " + mChannelIndex);
		// Log.d(TAG, "channelId: " + channelId);
		// Log.d(TAG, "ChangeChannel: " + uri);
		mChannelId = channelId;
		mChannelUri = uri;
		{
			if (player.isPlaying())
				player.stop();
			player.reset();
			try {

				player.setDataSource(this, uri);
				player.setOnPreparedListener(this);
				player.setOnErrorListener(this);
				player.prepareAsync();
			} catch (IllegalArgumentException e) {
				Log.d(TAG, e.getMessage());
			} catch (SecurityException e) {
				Log.d(TAG, e.getMessage());
			} catch (IllegalStateException e) {
				Log.d(TAG, e.getMessage());
			} catch (IOException e) {
				Log.d(TAG, e.getMessage());
			}
		}

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {

			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
					WindowManager.LayoutParams.FLAG_FULLSCREEN);
		} else {
			View decorView = getWindow().getDecorView();
			int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_LOW_PROFILE;
			decorView.setSystemUiVisibility(uiOptions);
		}

		RelativeLayout rlayout = (RelativeLayout) findViewById(R.id.video_container);
		rlayout.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
	}

	private void loadServicesfromCursor(Cursor cursor) {
		mserviceList.clear();
		try {
			if (mSortBy == SortBy.DEFAULT.ordinal()) {
				{
					cursor.moveToFirst();
					do {
						int serviceIdx = cursor
								.getColumnIndexOrThrow(DBHelper.SERVICE_ID);
						int clientIdx = cursor
								.getColumnIndexOrThrow(DBHelper.CLIENT_ID);
						int chIdx = cursor
								.getColumnIndexOrThrow(DBHelper.CHANNEL_NAME);
						int chDescIdx = cursor
								.getColumnIndexOrThrow(DBHelper.CHANNEL_DESC);
						int imgIdx = cursor
								.getColumnIndexOrThrow(DBHelper.IMAGE);
						int urlIdx = cursor.getColumnIndexOrThrow(DBHelper.URL);

						ServiceDatum service = new ServiceDatum();
						service.setServiceId(Integer.parseInt(cursor
								.getString(serviceIdx)));
						service.setClientId(Integer.parseInt(cursor
								.getString(clientIdx)));
						service.setChannelName(cursor.getString(chIdx));
						service.setChannelDescription(cursor
								.getString(chDescIdx));
						service.setImage(cursor.getString(imgIdx));
						service.setUrl(cursor.getString(urlIdx));
						mserviceList.add(service);
					} while (cursor.moveToNext());

				}
			} else if (mSortBy == SortBy.CATEGORY.ordinal()
					|| mSortBy == SortBy.LANGUAGE.ordinal()) {
				cursor.moveToFirst();
				String selectionArgs = null;
				if (mSelectionArgs != null && mSelectionArgs.length > 0) {
					selectionArgs = mSelectionArgs[0];
				}

				do {
					CursorLoader cursorLoader = null;
					if (mSortBy == SortBy.CATEGORY.ordinal()) {
						if (mSelection != null && selectionArgs != null) {
							cursorLoader = new CursorLoader(
									this,
									ServiceProvider.SERVICES_URI,
									null,
									DBHelper.CATEGORY + "=? AND " + mSelection,
									new String[] {
											cursor.getString(cursor
													.getColumnIndex(DBHelper.CATEGORY)),
											selectionArgs }, null);
						} else if (mSelection != null && selectionArgs == null) {

							cursorLoader = new CursorLoader(
									this,
									ServiceProvider.SERVICES_URI,
									null,
									DBHelper.CATEGORY + "=? AND " + mSelection,
									new String[] { cursor.getString(cursor
											.getColumnIndex(DBHelper.CATEGORY)) },
									null);
						} else if (mSelection == null && selectionArgs == null) {
							cursorLoader = new CursorLoader(
									this,
									ServiceProvider.SERVICES_URI,
									null,
									DBHelper.CATEGORY + "=?",
									new String[] { cursor.getString(cursor
											.getColumnIndex(DBHelper.CATEGORY)) },
									null);
						}
					} else {

						if (mSelection != null && selectionArgs != null) {
							cursorLoader = new CursorLoader(
									this,
									ServiceProvider.SERVICES_URI,
									null,
									DBHelper.SUB_CATEGORY + "=? AND "
											+ mSelection,
									new String[] {
											cursor.getString(cursor
													.getColumnIndex(DBHelper.SUB_CATEGORY)),
											selectionArgs }, null);
						} else if (mSelection != null && selectionArgs == null) {

							cursorLoader = new CursorLoader(
									this,
									ServiceProvider.SERVICES_URI,
									null,
									DBHelper.SUB_CATEGORY + "=? AND "
											+ mSelection,
									new String[] { cursor.getString(cursor
											.getColumnIndex(DBHelper.SUB_CATEGORY)) },
									null);
						} else if (mSelection == null && selectionArgs == null) {
							cursorLoader = new CursorLoader(
									this,
									ServiceProvider.SERVICES_URI,
									null,
									DBHelper.SUB_CATEGORY + "=?",
									new String[] { cursor.getString(cursor
											.getColumnIndex(DBHelper.SUB_CATEGORY)) },
									null);
						}
					}
					Cursor childCursor = null;

					childCursor = cursorLoader.loadInBackground();
					childCursor.moveToFirst();
					do {
						int serviceIdx = childCursor
								.getColumnIndexOrThrow(DBHelper.SERVICE_ID);
						int clientIdx = childCursor
								.getColumnIndexOrThrow(DBHelper.CLIENT_ID);
						int chIdx = childCursor
								.getColumnIndexOrThrow(DBHelper.CHANNEL_NAME);
						int chDescIdx = childCursor
								.getColumnIndexOrThrow(DBHelper.CHANNEL_DESC);
						int imgIdx = childCursor
								.getColumnIndexOrThrow(DBHelper.IMAGE);
						int urlIdx = childCursor
								.getColumnIndexOrThrow(DBHelper.URL);

						ServiceDatum service = new ServiceDatum();
						service.setServiceId(Integer.parseInt(childCursor
								.getString(serviceIdx)));
						service.setClientId(Integer.parseInt(childCursor
								.getString(clientIdx)));
						service.setChannelName(childCursor.getString(chIdx));
						service.setChannelDescription(childCursor
								.getString(chDescIdx));
						service.setImage(childCursor.getString(imgIdx));
						service.setUrl(childCursor.getString(urlIdx));
						mserviceList.add(service);
					} while (childCursor.moveToNext());

				} while (cursor.moveToNext());
			}
			if (mChannelId != -1) {
				mChannelIndex = getChannelIndexByChannelId(mChannelId);
				// Log.d("mChannelIndex", "" + mChannelIndex);
			}
		} catch (Exception e) {
			Log.e(TAG, "Videoplayer-Cursor Exception");
		}
	}

	public class BufferrChk extends Thread {
		@Override
		public void run() {
			try {
				while (player != null) {

					if (stopThread) {
						currentPosition = 0;
						lastPosition = 0;
						break;
					}

					currentPosition = player.getCurrentPosition();
					lastPosition = player.getDuration();
					Message msg = new Message();
					if (currentPosition != lastPosition
							|| currentPosition > lastPosition)
						msg.what = 0;
					else
						msg.what = 1;
					lastPosition = currentPosition;
					threadHandler.sendMessage(msg);

					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						e.printStackTrace();
						System.out.println("interrupt exeption" + e);
					}

				}

			}

			catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("My exeption" + e);
			}

		}
	}

}
