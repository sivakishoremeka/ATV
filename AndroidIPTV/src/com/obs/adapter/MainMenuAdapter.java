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

public class MainMenuAdapter extends BaseAdapter {
	private Activity activity;
	private String[] arrMenu;
	private String[] imgNames;
	private static LayoutInflater inflater = null;
	public int clientId;

	public MainMenuAdapter(Activity activity) {
		this.activity = activity;
		inflater = (LayoutInflater) activity
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.arrMenu = activity.getResources().getStringArray(R.array.arrMainMenu);
		imgNames = activity.getResources().getStringArray(
				R.array.arrMenuImageNames);

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
		View vi = convertView;
		if (convertView == null)
			vi = inflater.inflate(R.layout.main_menu_list_row, null);

		TextView title = (TextView) vi
				.findViewById(R.id.main_menu_list_row_tv_menu_name);
		ImageView thumb_image = (ImageView) vi
				.findViewById(R.id.main_menu_list_row_iv_menu_image);

		String imgName = "img_menu_" + (imgNames[position]);
		String imgNameSel = "img_menu_" + imgNames[position] + "_sel";

		StateListDrawable states = new StateListDrawable();

		states.addState(
				new int[] { android.R.attr.state_selected },
				activity.getResources().getDrawable(
						activity.getResources().getIdentifier(imgNameSel,
								"drawable", "com.obs.androidiptv")));
		
		states.addState(
				new int[] { android.R.attr.state_pressed, android.R.attr.state_selected },
				activity.getResources().getDrawable(
						activity.getResources().getIdentifier(imgName,
								"drawable", "com.obs.androidiptv")));
		states.addState(
				new int[] {},
				activity.getResources().getDrawable(
						activity.getResources().getIdentifier(imgName,
								"drawable", "com.obs.androidiptv")));
		thumb_image.setImageDrawable(states);
		title.setText(arrMenu[position]);
		return vi;
	}
}
