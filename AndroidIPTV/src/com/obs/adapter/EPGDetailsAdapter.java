package com.obs.adapter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.obs.androidiptv.R;
import com.obs.data.EpgDatum;

public class EPGDetailsAdapter extends BaseAdapter {
	private List<EpgDatum> data;
	private static LayoutInflater inflater = null;

	public EPGDetailsAdapter(Activity activity, List<EpgDatum> data) {
		this.data = data;
		inflater = (LayoutInflater) activity
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public int getCount() {
		return data.size();
	}

	public Object getItem(int position) {
		return position;
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;

		if (convertView == null) {
			holder = new ViewHolder();
			convertView = inflater.inflate(
					R.layout.fragment_epg_details_list_row, null);
			holder.title = (TextView) convertView
					.findViewById(R.id.epg_details_list_row_new_tv_prog_title);
			holder.time = (TextView) convertView
					.findViewById(R.id.epg_details_list_row_new_tv_start_time);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		final EpgDatum pData = data.get(position);
		holder.title.setText(pData.getProgramTitle());
		SimpleDateFormat tf = new SimpleDateFormat("HH:mm");
		Date sTime = null;
		try {
			sTime = tf.parse(pData.getStartTime());
		} catch (ParseException e) {
			e.printStackTrace();
		}
		holder.time.setText(tf.format(sTime));
		return convertView;
	}

	class ViewHolder {
		TextView title;
		TextView time;
	}
}
