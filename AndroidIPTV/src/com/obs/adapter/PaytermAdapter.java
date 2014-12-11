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
import android.widget.BaseAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import com.obs.androidiptv.MyPakagesFragment;
import com.obs.androidiptv.PlanActivity;
import com.obs.androidiptv.R;
import com.obs.data.Paytermdatum;

public class PaytermAdapter extends BaseAdapter {
	private static LayoutInflater inflater = null;
	public int clientId;
	
	private Context _context;
	private List<Paytermdatum> _paytermData;
	private Map<Integer, RadioButton> _mapRButton;
	private boolean _isNewPlan;
	
	public PaytermAdapter(Context context,List<Paytermdatum> paytermData,boolean isNewPlan) {
		this._context = context;
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this._paytermData = paytermData;
		this._mapRButton = new HashMap<Integer, RadioButton>();
		this._isNewPlan = isNewPlan;
	}

	public int getCount() {
		return _paytermData.size();
	}

	public Paytermdatum getItem(int position) {
		return _paytermData.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View vi = convertView;
		if (convertView == null)
			vi = inflater.inflate(R.layout.a_plan_payterm_row,parent,false);

		TextView duration = (TextView) vi
				.findViewById(R.id.a_plan_payterm_row_duration_tv);
		duration.setTypeface(null, Typeface.BOLD);
		duration.setText(getItem(position).getPaytermtype());
		
		RadioButton rb1 = (RadioButton) vi
				.findViewById(R.id.a_plan_payterm_row_rb);
		rb1.setFocusable(false);
		
		if(_isNewPlan)
		if (PlanActivity.selPaytermId == position) {
			rb1.setChecked(true);
		}
		else
		{
			rb1.setChecked(false);
		}
		else if(!_isNewPlan)
			if (MyPakagesFragment.selPaytermId == position) {
				rb1.setChecked(true);
			}
			else
			{
				rb1.setChecked(false);
			}
		rb1.setTag(position);
		_mapRButton.put(position, rb1);
		rb1.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (((RadioButton) v).isChecked()) {
					for (int i = 0; i < _mapRButton.size(); i++) {
						RadioButton rb = _mapRButton.get(i);
						int tag = (Integer) ((RadioButton) v).getTag();
						if (null != rb && tag!=i) {
							rb.setChecked(false);
						}
					}
					if(_isNewPlan)
					PlanActivity.selPaytermId = (Integer) ((RadioButton) v)
							.getTag();
					else if(!_isNewPlan)
						MyPakagesFragment.selPaytermId = (Integer) ((RadioButton) v)
						.getTag();
				}
			}
		});	
		return vi;
	}
}
