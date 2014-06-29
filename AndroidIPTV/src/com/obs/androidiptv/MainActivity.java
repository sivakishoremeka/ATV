package com.obs.androidiptv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.obs.adapter.MainMenuAdapter;
import com.obs.androidiptv.MyApplication.SetAppState;
import com.obs.service.DoBGTasksService;

public class MainActivity extends Activity {

	// private static final String TAG = MainActivity.class.getName();
	ListView listView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Log.d(TAG, "onCreate");
		setContentView(R.layout.activity_main);
		listView = (ListView) findViewById(R.id.a_main_lv_menu);
		MainMenuAdapter menuAdapter = new MainMenuAdapter(this);
		listView.setAdapter(menuAdapter);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				switch (arg2) {
				case 0:
					startActivity(new Intent(MainActivity.this,
							ChannelsActivity.class));
					break;
				case 1:
					Intent intent1 = new Intent(MainActivity.this,
							VodActivity.class);
					startActivity(intent1);
					break;
				}
			}
		});
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.nav_menu, menu);
		MenuItem searchItem = menu.findItem(R.id.action_search);
		searchItem.setVisible(false);
		MenuItem accountItem = menu.findItem(R.id.action_account);
		accountItem.setVisible(true);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.action_account:
			startActivity(new Intent(this, MyAccountActivity.class));
			break;
		default:
			break;
		}
		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK) || keyCode == 4) {
			AlertDialog mConfirmDialog = ((MyApplication) getApplicationContext())
					.getConfirmDialog(this);
			mConfirmDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					});
			mConfirmDialog.show();
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
}