package com.obs.adapter;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.graphics.drawable.StateListDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorTreeAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.obs.androidiptv.ChannelsActivity;
import com.obs.androidiptv.MyApplication.SortBy;
import com.obs.androidiptv.R;
import com.obs.database.DBHelper;
import com.obs.database.ServiceProvider;

public class ServiceCategoryListAdapter extends CursorTreeAdapter {

	//public HashMap<String, View> childView = new HashMap<String, View>();

	private static final String TAG = ServiceCategoryListAdapter.class
			.getName();
	private ChannelsActivity mActivity;
	private LayoutInflater mInflater;
	int mSortBy;
	String mSelection;
	String[] mSelectionArgs;

	public ServiceCategoryListAdapter(Cursor cursor, Context context,
			int sortBy, String selection, String[] selectionArgs) {

		super(cursor, context);
		mActivity = (ChannelsActivity) context;
		mInflater = LayoutInflater.from(context);
		mSortBy = sortBy;
		mSelection = selection;
		mSelectionArgs = selectionArgs;

	}

	@Override
	public View newGroupView(Context context, Cursor cursor,
			boolean isExpanded, ViewGroup parent) {
		final View view = mInflater.inflate(R.layout.f_list_category_row_item,
				parent, false);
		return view;
	}

	@Override
	public void bindGroupView(View view, Context context, Cursor cursor,
			boolean isExpanded) {

		TextView categoryName = (TextView) view
				.findViewById(R.id.category_name);
		if (mSortBy == SortBy.CATEGORY.ordinal())
			categoryName.setText(cursor.getString(cursor
					.getColumnIndex(DBHelper.CATEGORY)));
		else
			categoryName.setText(cursor.getString(cursor
					.getColumnIndex(DBHelper.SUB_CATEGORY)));

		ImageView thumb_image = (ImageView) view
				.findViewById(R.id.f_ch_lv_group_arrow);

		String imgName = "ic_navigation_expand";
		String imgNameSel = "ic_navigation_expand_sel";
		if (!isExpanded) {
			imgName = "ic_navigation_collapse";
			imgNameSel = "ic_navigation_collapse_sel";
		}
		StateListDrawable states = new StateListDrawable();

		states.addState(
				new int[] { android.R.attr.state_selected },
				mActivity.getResources().getDrawable(
						mActivity.getResources().getIdentifier(imgNameSel,
								"drawable", "com.obs.androidiptv")));

		states.addState(
				new int[] { android.R.attr.state_pressed,
						android.R.attr.state_selected },
				mActivity.getResources().getDrawable(
						mActivity.getResources().getIdentifier(imgName,
								"drawable", "com.obs.androidiptv")));
		states.addState(
				new int[] {},
				mActivity.getResources().getDrawable(
						mActivity.getResources().getIdentifier(imgName,
								"drawable", "com.obs.androidiptv")));
		thumb_image.setImageDrawable(states);
	}

	@Override
	public View newChildView(Context context, Cursor cursor,
			boolean isLastChild, ViewGroup parent) {

		final View view = mInflater.inflate(R.layout.f_list_channel_item,
				parent, false);

		return view;
	}

	@Override
	public void bindChildView(View view, Context context, Cursor cursor,
			boolean isLastChild) {
		TextView channelname = (TextView) view
				.findViewById(R.id.ch_lv_item_tv_ch_Name);

		channelname.setText(cursor.getString(cursor
				.getColumnIndex(DBHelper.CHANNEL_DESC)));
		channelname.setTag(ChannelsActivity.getServiceFromCursor(cursor));
	}

	protected Cursor getChildrenCursor(Cursor groupCursor) {

		Cursor itemCursor = getGroup(groupCursor.getPosition());
		CursorLoader cursorLoader = null;
		if (mSortBy == SortBy.CATEGORY.ordinal()) {

			String selectionArgs = null;
			if (mSelectionArgs != null && mSelectionArgs.length > 0) {
				selectionArgs = mSelectionArgs[0];
			}
				if (mSelection != null && selectionArgs != null) {
					cursorLoader = new CursorLoader(
							mActivity,
							ServiceProvider.SERVICES_URI,
							null,
							DBHelper.CATEGORY + "=? AND " + mSelection,
							new String[] {
									itemCursor.getString(itemCursor
											.getColumnIndex(DBHelper.CATEGORY)),
									selectionArgs }, null);
				} else if (mSelection != null && selectionArgs == null) {

					cursorLoader = new CursorLoader(mActivity,
							ServiceProvider.SERVICES_URI, null,
							DBHelper.CATEGORY + "=? AND " + mSelection,
							new String[] { itemCursor.getString(itemCursor
									.getColumnIndex(DBHelper.CATEGORY)) }, null);
				} else if (mSelection == null && selectionArgs == null) {
					cursorLoader = new CursorLoader(mActivity,
							ServiceProvider.SERVICES_URI, null,
							DBHelper.CATEGORY + "=?",
							new String[] { itemCursor.getString(itemCursor
									.getColumnIndex(DBHelper.CATEGORY)) }, null);
				}
			}
			 else {
			String selectionArgs = null;
			if (mSelectionArgs != null && mSelectionArgs.length > 0) {
				selectionArgs = mSelectionArgs[0];
			}
				if (mSelection != null && selectionArgs != null) {
					cursorLoader = new CursorLoader(
							mActivity,
							ServiceProvider.SERVICES_URI,
							null,
							DBHelper.SUB_CATEGORY + "=? AND " + mSelection,
							new String[] {
									itemCursor.getString(itemCursor
											.getColumnIndex(DBHelper.SUB_CATEGORY)),
									selectionArgs }, null);
				} else if (mSelection != null && selectionArgs == null) {

					cursorLoader = new CursorLoader(mActivity,
							ServiceProvider.SERVICES_URI, null,
							DBHelper.SUB_CATEGORY + "=? AND " + mSelection,
							new String[] { itemCursor.getString(itemCursor
									.getColumnIndex(DBHelper.SUB_CATEGORY)) }, null);
				} else if (mSelection == null && selectionArgs == null) {
					cursorLoader = new CursorLoader(mActivity,
							ServiceProvider.SERVICES_URI, null,
							DBHelper.SUB_CATEGORY + "=?",
							new String[] { itemCursor.getString(itemCursor
									.getColumnIndex(DBHelper.SUB_CATEGORY)) }, null);
				}
			}
		Cursor childCursor = null;

		try {
			childCursor = cursorLoader.loadInBackground();
			//Log.d(TAG, "childCursor " + childCursor.getCount());
			childCursor.moveToFirst();
		} catch (Exception e) {
			Log.e(TAG, "Cursor Exception");
		}

		return childCursor;
	}
}
