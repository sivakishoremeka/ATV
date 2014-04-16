package com.obs.adapter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.obs.androidiptv.EpgFragment;

/**
 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to one
 * of the sections/tabs/pages.
 */
public class EPGFragmentPagerAdapter extends FragmentPagerAdapter {

	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", new Locale("en"));
	Calendar c = null;

	public EPGFragmentPagerAdapter(FragmentManager fm) {
		super(fm);
	}

	@Override
	public Fragment getItem(int position) {
		Fragment fragment = new EpgFragment();
		c = Calendar.getInstance();
		c.add(Calendar.DATE, position); // number of days to add
		String date = df.format(c.getTime()); // dt is now the new date
		Bundle args = new Bundle();
		args.putString(EpgFragment.ARG_SECTION_DATE, date);
		fragment.setArguments(args);
		return fragment;
	}

	@Override
	public int getCount() {
		// Show 7 total pages.
		return 7;
	}

	@Override
	public CharSequence getPageTitle(int position) {
		c = Calendar.getInstance();
		c.add(Calendar.DATE, position); // number of days to add
		String date = df.format(c.getTime()); // dt is now the new date
		return date;
	}
}
