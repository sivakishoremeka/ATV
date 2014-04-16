package com.obs.adapter;

import java.util.List;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.obs.androidiptv.R;
import com.obs.data.ServiceDatum;



public class ChannelGridViewAdapter extends BaseAdapter {
	private List<ServiceDatum> channelList;
	private LayoutInflater inflater;

	public ChannelGridViewAdapter(List<ServiceDatum> channelList,
			Activity context) {
		this.channelList = channelList;
		inflater = LayoutInflater.from(context);
	}

	@Override
	public int getCount() {

		return channelList.size();
	}

	@Override
	public Object getItem(int position) {

		return channelList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if(convertView==null){
			holder = new ViewHolder();
			convertView =  inflater.inflate(
				R.layout.ch_gridview_item, null);
		   holder.image = ((ImageView) convertView.findViewById(R.id.ch_gv_item_img));
		   convertView.setTag(holder);
		}
	else{
		holder = (ViewHolder) convertView.getTag();
	}
		ServiceDatum data = channelList.get(position);
		 holder.image.setPadding(2, 2, 2, 2);
		com.nostra13.universalimageloader.core.ImageLoader.getInstance()
				.displayImage(data.getImage(), holder.image);
		return convertView;
	}
	
	class ViewHolder{
		ImageView image;
	} 
}
