package com.obs.adapter;

import java.util.List;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;

import com.obs.androidiptv.R;
import com.obs.data.MediaDatum;

public class VODGridViewAdapter extends BaseAdapter {
	private List<MediaDatum> mediaList;
	private LayoutInflater inflater;

	public VODGridViewAdapter(List<MediaDatum> mediaList, Activity context) {
		this.mediaList = mediaList;
		inflater = LayoutInflater.from(context);
	}

	@Override
	public int getCount() {

		return mediaList.size();
	}

	@Override
	public Object getItem(int position) {

		return mediaList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		ViewHolder holder;
		if (convertView == null) {
			holder = new ViewHolder();
			convertView = (LinearLayout) inflater.inflate(
					R.layout.vod_gridview_item, null);
			holder.image = ((ImageView) convertView
					.findViewById(R.id.vod_gv_item_img));
			holder.rating = (RatingBar) convertView
					.findViewById(R.id.vod_gv_item_rating_bar);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		MediaDatum media = mediaList.get(position);

		com.nostra13.universalimageloader.core.ImageLoader.getInstance()
				.displayImage(media.getMediaImage(), holder.image);
		holder.rating.setRating(media.getMediaRating());

		return convertView;
	}

	class ViewHolder {
		ImageView image;
		RatingBar rating;
	}

}
