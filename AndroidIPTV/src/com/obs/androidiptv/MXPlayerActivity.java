package com.obs.androidiptv;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class MXPlayerActivity extends Activity {

	// MX-Player
	public static final String TAG = "mxvp.intent.test";

	public static final String MXVP = "com.mxtech.videoplayer.ad";
	public static final String MXVP_PRO = "com.mxtech.videoplayer.pro";

	public static final String MXVP_PLAYBACK_CLASS = "com.mxtech.videoplayer.ad.ActivityScreen";
	public static final String MXVP_PRO_PLAYBACK_CLASS = "com.mxtech.videoplayer.ActivityScreen";

	public static final String RESULT_VIEW = "com.mxtech.intent.result.VIEW";
	public static final String EXTRA_DECODE_MODE = "decode_mode"; // (byte)
	public static final String EXTRA_VIDEO_LIST = "video_list";
	public static final String EXTRA_SUBTITLES = "subs";
	public static final String EXTRA_SUBTITLES_ENABLE = "subs.enable";
	public static final String EXTRA_TITLE = "title";
	public static final String EXTRA_POSITION = "position";
	public static final String EXTRA_RETURN_RESULT = "return_result";
	public static final String EXTRA_HEADERS = "headers";

	// MX-Player

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Uri mUri = Uri.parse(getIntent().getStringExtra("URL"));
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setDataAndType(mUri, "application/*");
		// s/w decoder
		i.putExtra(EXTRA_DECODE_MODE, (byte) 2);
		// request result
		i.putExtra(EXTRA_RETURN_RESULT, true);
		String[] headers = new String[] { "User-Agent",
				"MX Player Caller App/1.0", "Extra-Header", "911" };
		i.putExtra(EXTRA_HEADERS, headers);
		try {
			i.setPackage(MXVP_PRO);
			startActivityForResult(i, 0);
			return;
		} catch (ActivityNotFoundException e) {
		}
		try {
			i.setPackage(MXVP);
			i.setClassName(MXVP, MXVP_PLAYBACK_CLASS);
			startActivityForResult(i, 0);
			return;
		} catch (ActivityNotFoundException e2) {
			Log.d("MxException", e2.getMessage().toString());
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		finish();
		if (resultCode != RESULT_OK) {
			Log.d(TAG, "Canceled.");
			return;
		}
		// handle result.
		Uri lastVideoUri = data.getData();
		byte lastDecodingMode = data.getByteExtra(EXTRA_DECODE_MODE, (byte) 0);
		int lastPosition = data.getIntExtra(EXTRA_POSITION, 0);
		Log.i(TAG, "OK: " + lastVideoUri + " last-decoding-mode="
				+ lastDecodingMode + " last-position=" + lastPosition);
	}
}
