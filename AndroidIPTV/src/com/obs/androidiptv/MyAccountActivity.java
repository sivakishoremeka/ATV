package com.obs.androidiptv;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.obs.adapter.MyAccountMenuAdapter;
import com.obs.androidiptv.MyApplication.SetAppState;
import com.obs.service.DoBGTasksService;

public class MyAccountActivity extends Activity {

	// private static final String TAG = MyAccountActivity.class.getName();
	ListView listView;
	private static final String FRAG_TAG = "My Fragment";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Log.d(TAG, "onCreate");
		setContentView(R.layout.activity_my_account);
		listView = (ListView) findViewById(R.id.a_my_acc_lv_menu);
		MyAccountMenuAdapter menuAdapter = new MyAccountMenuAdapter(this);
		listView.setAdapter(menuAdapter);
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {

				switch (arg2) {
				case 0:
					Fragment myPackageFrag = new MyPakagesFragment();
					FragmentTransaction transaction1 = getFragmentManager()
							.beginTransaction();
					transaction1.replace(R.id.a_my_acc_frag_container,
							myPackageFrag, FRAG_TAG);
					transaction1.commit();
					break;

				case 1:
					Fragment myProfileFrag = new MyProfileFragment();
					FragmentTransaction transaction2 = getFragmentManager()
							.beginTransaction();
					transaction2.replace(R.id.a_my_acc_frag_container,
							myProfileFrag, FRAG_TAG);
					transaction2.commit();
					break;
				}
			}
		});
		Fragment myPackageFrag = new MyPakagesFragment();
		FragmentTransaction transaction = getFragmentManager()
				.beginTransaction();
		transaction.add(R.id.a_my_acc_frag_container, myPackageFrag, FRAG_TAG);
		transaction.commit();
	}

	public void btnCancel_onClick(View v) {

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
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == 4) {
			Fragment frag = getFragmentManager().findFragmentByTag(FRAG_TAG);
			if (frag instanceof MyPakagesFragment) {
				((MyPakagesFragment) frag).onBackPressed();
				return true;
			}
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

	public void btnSubmit_onClick(View v) {
		Fragment frag = getFragmentManager().findFragmentByTag(FRAG_TAG);
		if (frag instanceof MyPakagesFragment) {
			((MyPakagesFragment) frag).btnSubmit_onClick(v);
		}

	}

}