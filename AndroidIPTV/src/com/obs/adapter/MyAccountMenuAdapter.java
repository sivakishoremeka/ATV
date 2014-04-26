package com.obs.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.StateListDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.obs.androidiptv.R;

public class MyAccountMenuAdapter extends BaseAdapter {
	private Activity activity;
	private String[] arrMenu;
	private String[] imgNames;
	private static LayoutInflater inflater = null;
	public int clientId;

	public MyAccountMenuAdapter(Activity activity) {
		this.activity = activity;
		inflater = (LayoutInflater) activity
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.arrMenu = activity.getResources().getStringArray(
				R.array.arrMyAccMenu);
		imgNames = activity.getResources().getStringArray(
				R.array.arrMyAccImageNames);

	}

	public int getCount() {
		return arrMenu.length;
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
			convertView = inflater.inflate(R.layout.main_menu_list_row, null);
			holder.image = (ImageView) convertView
					.findViewById(R.id.main_menu_list_row_iv_menu_image);
			holder.menu = (TextView) convertView
					.findViewById(R.id.main_menu_list_row_tv_menu_name);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		String imgName = "img_acc_menu_" + (imgNames[position]);
		String imgNameSel = "img_acc_menu_" + imgNames[position] + "_sel";

		StateListDrawable states = new StateListDrawable();


		states.addState(
				new int[] { android.R.attr.state_activated },
				activity.getResources().getDrawable(
						activity.getResources().getIdentifier(imgNameSel,
								"drawable", "com.obs.androidiptv")));
		
		states.addState(
				new int[] { android.R.attr.state_selected },
				activity.getResources().getDrawable(
						activity.getResources().getIdentifier(imgNameSel,
								"drawable", "com.obs.androidiptv")));
		states.addState(
				new int[] { android.R.attr.state_pressed,android.R.attr.state_selected },
				activity.getResources().getDrawable(
						activity.getResources().getIdentifier(imgName,
								"drawable", "com.obs.androidiptv")));
		states.addState(
				new int[] {},
				activity.getResources().getDrawable(
						activity.getResources().getIdentifier(imgName,
								"drawable", "com.obs.androidiptv")));
		holder.image.setImageDrawable(states);
		holder.menu.setText(arrMenu[position]);
		return convertView;
	}

	class ViewHolder {
		ImageView image;
		TextView menu;
	}
}
