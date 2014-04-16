package com.obs.service.task;

import java.util.Calendar;

import com.obs.service.NotifyService;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

/**
 * Set an alarm for the date passed into the constructor
 * When the alarm is raised it will start the NotifyService
 * 
 * This uses the android build in alarm manager *NOTE* if the phone is turned off this alarm will be cancelled
 * 
 * This will run on it's own thread.
 * 
 * @author paul.blundell
 */
public class AlarmTask implements Runnable{
	// The date selected for the alarm
	private final Calendar date;
	// The android system alarm manager
	private final AlarmManager am;
	// Your context to retrieve the alarm manager from
	private final Context context;
	private String progName;
	private int channelId;
	private String channelName;
	private String url;

	public AlarmTask(Context context, Calendar date,String progName,int channelId,String channelName,String url) {
		this.context = context;
		this.am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		this.date = date;
		this.progName = progName;
		this.channelId = channelId;
		this.channelName = channelName;
		this.url = url;
	}
	
	@Override
	public void run() {
		// Request to start are service when the alarm date is upon us
		// We don't start an activity as we just want to pop up a notification into the system bar not a full activity
		StringBuilder sdate = new StringBuilder();
		sdate.append(date.get(Calendar.DAY_OF_MONTH));
		sdate.append((date.get(Calendar.MONTH)+1));
		sdate.append(date.get(Calendar.HOUR_OF_DAY));
		sdate.append(date.get(Calendar.MINUTE));
		sdate.append(date.get(Calendar.SECOND));
		int reqcode = Integer.parseInt(sdate.toString());
		Intent intent = new Intent(context, NotifyService.class);
		intent.putExtra(NotifyService.INTENT_NOTIFY, true);
		intent.putExtra("PROG", progName);
		intent.putExtra("CHANNELNAME", channelName);
		intent.putExtra("URL", url);
		intent.putExtra("CHANNELID", Integer.toString(channelId));
		PendingIntent pendingIntent = PendingIntent.getService(context, reqcode, intent, 0);		
		// Sets an alarm - note this alarm will be lost if the phone is turned off and on again
		am.set(AlarmManager.RTC, date.getTimeInMillis(), pendingIntent);
	}
}
