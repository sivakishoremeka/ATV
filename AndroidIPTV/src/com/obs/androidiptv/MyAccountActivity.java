package com.obs.androidiptv;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.obs.adapter.MyAccountMenuAdapter;

public class MyAccountActivity extends Activity {

	private static final String TAG = MyAccountActivity.class.getName();
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
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == 4) {
			this.finish();
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