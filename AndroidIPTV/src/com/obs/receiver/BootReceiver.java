package com.obs.receiver;

import com.obs.service.ScheduleAfterBootService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent i = new Intent(context, ScheduleAfterBootService.class);
		context.startService(i);
	}
}