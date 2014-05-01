package com.obs.androidiptv;

import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Loader;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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

import com.obs.data.ServiceDatum;
import com.obs.database.DBHelper;
import com.obs.database.ServiceProvider;

public class VideoPlayerActivity extends Activity implements
		SurfaceHolder.Callback, MediaPlayer.OnPreparedListener,
		MediaPlayer.OnInfoListener, MediaPlayer.OnErrorListener,
		VideoControllerView.MediaPlayerControl, LoaderCallbacks<Cursor> {

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

		// prepareChannelsList();
		videoSurface = (SurfaceView) findViewById(R.id.videoSurface);
		SurfaceHolder videoHolder = videoSurface.getHolder();
		videoHolder.addCallback(this);
		player = new MediaPlayer();

		mVideoType = getIntent().getStringExtra("VIDEOTYPE");
		if (mVideoType.equalsIgnoreCase("LIVETV")) {
			isLiveController = true;
			VideoControllerView.sDefaultTimeout = 3000;
			mChannelId = getIntent().getIntExtra("CHANNELID", -1);
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
			// Log.d("VideoPlayerActivity", "VideoURL:"+
			// getIntent().getStringExtra("URL"));
			player.setOnPreparedListener(this);
			player.setOnInfoListener(this);
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
		getLoaderManager().initLoader(getIntent().getIntExtra("REQTYPE", 1),
				null, this);
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
				finish();
			}
		});
		mProgressDialog.show();
	}

	@Override
	protected void onPause() {
		// Log.d("VideoPlayerActivity", "surfaceDestroyed");
		if (player != null && player.isPlaying()) {
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
		if (Build.VERSION.SDK_INT < 17) {
			if (mProgressDialog != null && mProgressDialog.isShowing()) {
				mProgressDialog.dismiss();
				mProgressDialog = null;
			}
		}
		mp.start();
	}

	@Override
	public boolean onError(MediaPlayer mp, int what, int extra) {
		// Log.d(TAG, "Media player Error is...what:" + what + " Extra:" +
		// extra);

		if (what == MediaPlayer.MEDIA_ERROR_UNKNOWN && extra == -2147483648) {

			Toast.makeText(
					getApplicationContext(),
					"Incorrect URL or Unsupported Media Format.Media player closed.",
					Toast.LENGTH_LONG).show();
		} else if (what == MediaPlayer.MEDIA_ERROR_UNKNOWN && extra == -1004) {

			Toast.makeText(
					getApplicationContext(),
					"Invalid Stream for this channel... Please try other channel",
					Toast.LENGTH_LONG).show();
		} else {
			controller.mHandler.removeMessages(controller.SHOW_PROGRESS);
			controller.mHandler.removeMessages(controller.FADE_OUT);
			changeChannel(mChannelUri, mChannelId);
		}

		/*
		 * if (player != null && player.isPlaying()) player.stop();
		 * player.release(); player = null; if (mProgressDialog != null &&
		 * mProgressDialog.isShowing()) { mProgressDialog.dismiss();
		 * mProgressDialog = null; } finish();
		 */return true;
	}

	@Override
	public boolean onInfo(MediaPlayer mp, int what, int extra) {
		/*
		 * if (Build.VERSION.SDK_INT >= 17) { if (what ==
		 * MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) { if (mProgressDialog
		 * != null && mProgressDialog.isShowing()) { mProgressDialog.dismiss();
		 * mProgressDialog = null; } } } if (what ==
		 * MediaPlayer.MEDIA_INFO_BUFFERING_START) { if (mProgressDialog != null
		 * && mProgressDialog.isShowing()) { mProgressDialog.dismiss();
		 * mProgressDialog = null; } mProgressDialog = new
		 * ProgressDialog(VideoPlayerActivity.this,
		 * ProgressDialog.THEME_HOLO_DARK);
		 * mProgressDialog.setMessage("Buffering"); //
		 * mProgressDialog.setCancelable(true);
		 * mProgressDialog.setCanceledOnTouchOutside(false);
		 * mProgressDialog.setOnCancelListener(new OnCancelListener() {
		 * 
		 * public void onCancel(DialogInterface arg0) { if
		 * (mProgressDialog.isShowing()) mProgressDialog.dismiss(); finish(); }
		 * }); mProgressDialog.show(); } else if (what ==
		 * MediaPlayer.MEDIA_INFO_BUFFERING_END) { if (mProgressDialog != null
		 * && mProgressDialog.isShowing()) { mProgressDialog.dismiss();
		 * mProgressDialog = null; } }
		 *//*
			 * else if (what == MediaPlayer.MEDIA_ERROR_TIMED_OUT) { if
			 * (mProgressDialog.isShowing()) { mProgressDialog.dismiss(); }
			 * Log.d(TAG, "Request timed out.Closing MediaPlayer"); finish(); }
			 */
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
			case KeyEvent.KEYCODE_DPAD_LEFT:
			case KeyEvent.KEYCODE_VOLUME_UP:
				audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
						AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
				return true;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
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
			/*
			 * } else if (keyCode == 9) {// 20) { Log.d(TAG, "menu key" +
			 * keyCode); if (null != player) { if (!controller.isShowing())
			 * controller.show(); else controller.hide(); } return true; } else
			 * if (keyCode == 22) {// 23 22) { Log.d(TAG, "menu key" + keyCode);
			 * if (null != player) { controller.mPauseListener.onClick(null); }
			 * return true;
			 */
		}
		/*
		 * else if (keyCode == 21) {// 23) { Log.d(TAG, "menu key" + keyCode);
		 * if (null != player) { controller.mRewListener.onClick(null); } return
		 * true; }
		 * 
		 * else if (keyCode == 22) {// 22 19) { Log.d(TAG, "menu key" +
		 * keyCode); if (null != player) {
		 * controller.mFfwdListener.onClick(null); } return true; }
		 */else if (keyCode >= 7 && keyCode <= 16) {
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
					if (keyCode == 19) {
						mChannelIndex++;
						if (mChannelIndex == mserviceList.size())
							mChannelIndex = 0;
					} else if (keyCode == 20) {
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
		} else if (keyCode == 23) {
			View focusedView = getWindow().getCurrentFocus();
			focusedView.performClick();
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
				player.setOnInfoListener(this);
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

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		CursorLoader loader = null;
		if (id == ServiceProvider.SERVICES) {
			String selection = null;
			String[] selectionArgs = null;
			if (null != getIntent()) {
				selection = getIntent().getStringExtra("SELECTION");
				String srchStr = getIntent().getStringExtra("SEARCHSTRING");
				if (null != srchStr) {
					selectionArgs = new String[] { "%" + srchStr + "%" };
				}
				loader = new CursorLoader(this, ServiceProvider.SERVICES_URI,
						null, selection, selectionArgs, null);
			}
		}
		if (id == ServiceProvider.SERVICES_ON_REFRESH) {
			loader = new CursorLoader(this,
					ServiceProvider.SERVICES_ONREFREFRESH_URI, null, null,
					null, null);
		}
		return loader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

		// Log.d("ChannelsActivity","onLoadFinished");
		if (mProgressDialog != null && mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
		if (null != cursor)
			loadServicesfromCursor(cursor);
	}

	private void loadServicesfromCursor(Cursor cursor) {
		mserviceList.clear();
		try {
			int serviceIdx = cursor
					.getColumnIndexOrThrow(DBHelper.SERVICE_KEY_SERVICE_ID);
			int clientIdx = cursor
					.getColumnIndexOrThrow(DBHelper.SERVICE_KEY_CLIENT_ID);
			int chIdx = cursor
					.getColumnIndexOrThrow(DBHelper.SERVICE_KEY_CHANNEL_NAME);
			int imgIdx = cursor
					.getColumnIndexOrThrow(DBHelper.SERVICE_KEY_IMAGE);
			int urlIdx = cursor.getColumnIndexOrThrow(DBHelper.SERVICE_KEY_URL);
			cursor.moveToFirst();
			do {
				ServiceDatum service = new ServiceDatum();
				service.setServiceId(Integer.parseInt(cursor
						.getString(serviceIdx)));
				service.setClientId(Integer.parseInt(cursor
						.getString(clientIdx)));
				service.setChannelName(cursor.getString(chIdx));
				service.setImage(cursor.getString(imgIdx));
				service.setUrl(cursor.getString(urlIdx));
				mserviceList.add(service);
			} while (cursor.moveToNext());
		} catch (Exception e) {
			Log.d("VideoPlayerActivity", e.getMessage());
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {

	}

}
