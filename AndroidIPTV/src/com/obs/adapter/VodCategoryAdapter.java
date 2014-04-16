package com.obs.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.obs.androidiptv.R;

public class VodCategoryAdapter extends BaseAdapter {
	private Activity activity;
	private String[] data;
	private static LayoutInflater inflater = null;

	public VodCategoryAdapter(Activity a, String[] d) {
		activity = a;
		data = d;
		inflater = (LayoutInflater) activity
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public int getCount() {
		return data.length;
	}

	public Object getItem(int position) {
		return position;
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View vi = convertView;
		if (convertView == null)
			vi = inflater.inflate(R.layout.vod_categ_list_row, null);

		TextView title = (TextView) vi
				.findViewById(R.id.vod_categ_list_row_tv_name);
		title.setText(data[position]);
		return vi;
	}
}
