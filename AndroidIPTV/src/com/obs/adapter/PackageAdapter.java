package com.obs.adapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import com.obs.androidiptv.MyPakagesFragment;
import com.obs.androidiptv.R;
import com.obs.data.PlanDatum;

public class PackageAdapter extends BaseExpandableListAdapter {

	private Context _context;
	private List<PlanDatum> _planData;
	private Map<Integer, RadioButton> _mapRButton;

	public PackageAdapter(Context context, List<PlanDatum> planData) {
		this._context = context;
		this._planData = planData;
		this._mapRButton = new HashMap<Integer, RadioButton>();
	}

	@Override
	public Object getChild(int groupPosition, int childPosititon) {
		return this._planData.get(groupPosition).getServices()
				.get(childPosititon).getServiceDescription();
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public View getChildView(int groupPosition, final int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {

		final String childText = (String) getChild(groupPosition, childPosition);

		if (convertView == null) {
			LayoutInflater infalInflater = (LayoutInflater) this._context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = infalInflater.inflate(R.layout.plan_list_item, null);
		}

		TextView txtListChild = (TextView) convertView
				.findViewById(R.id.plan_list_item_tv);

		txtListChild.setText(childText);
		return convertView;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return this._planData.get(groupPosition).getServices().size();
	}

	@Override
	public Object getGroup(int groupPosition) {
		return this._planData.get(groupPosition).getPlanDescription();
	}

	@Override
	public int getGroupCount() {
		return this._planData.size();
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public View getGroupView(final int groupPosition, boolean isExpanded,
			View convertView, final ViewGroup parent) {
		final String headerTitle = (String) getGroup(groupPosition);
		if (convertView == null) {
			LayoutInflater infalInflater = (LayoutInflater) this._context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = infalInflater.inflate(R.layout.plan_list_group, null);
		}
		TextView lblListHeader = (TextView) convertView
				.findViewById(R.id.plan_list_plan_tv);
		lblListHeader.setTypeface(null, Typeface.BOLD);
		lblListHeader.setText(headerTitle);

		RadioButton rb1 = (RadioButton) convertView
				.findViewById(R.id.plan_list_plan_rb);
		rb1.setFocusable(false);
		if (MyPakagesFragment.selGroupId == groupPosition) {
			rb1.setChecked(true);
		} else {
			rb1.setChecked(false);
		}
		rb1.setTag(groupPosition);
		_mapRButton.put(groupPosition, rb1);
		rb1.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (((RadioButton) v).isChecked()) {
					for (int i = 0; i < _mapRButton.size(); i++) {
						RadioButton rb = _mapRButton.get(i);
						int tag = (Integer) ((RadioButton) v).getTag();
						if (null != rb && tag != i) {
							rb.setChecked(false);
						}
					}
					MyPakagesFragment.selGroupId = (Integer) ((RadioButton) v)
							.getTag();
				}
			}
		});
		return convertView;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}
}
