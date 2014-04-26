package com.obs.adapter;

import java.util.List;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.obs.androidiptv.R;
import com.obs.data.ServiceDatum;



public class ChannelListAdapter extends BaseAdapter {
	private List<ServiceDatum> channelList;
	private LayoutInflater inflater;

	public ChannelListAdapter(List<ServiceDatum> channelList,
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
				R.layout.ch_list_item, null);
		   holder.channelName = ((TextView) convertView.findViewById(R.id.ch_lv_item_tv_ch_Name));
		   convertView.setTag(holder);
		}
	else{
		holder = (ViewHolder) convertView.getTag();
	}
		ServiceDatum data = channelList.get(position);
		 holder.channelName.setText(data.getChannelName());
		return convertView;
	}
	
	class ViewHolder{
		TextView channelName;
	} 
}
