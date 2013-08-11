package com.threedlite.userhash.location;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class OnBootReceiver extends BroadcastReceiver {

	private static final int DELAY = 5000;  
	private static final int DEFAULT_INTERVAL = 20;


	@Override
	public void onReceive(Context context, Intent intent) {

		PreferenceManager.setDefaultValues(context, R.xml.preferences, false);

		// Get interval
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		String pref = prefs.getString(Constants.PREF_MINUTES, ""+DEFAULT_INTERVAL);

		int interval = DEFAULT_INTERVAL;
		try {
			interval = Integer.parseInt(pref);
		} catch (Exception e) {
			Toast.makeText(context, "Error reading reporting interval from settings: "+pref, Toast.LENGTH_LONG).show();
		}

		AlarmManager alarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(context, OnAlarmReceiver.class), 0);

		alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + DELAY,
				1000*60*interval, pendingIntent);

	}

}