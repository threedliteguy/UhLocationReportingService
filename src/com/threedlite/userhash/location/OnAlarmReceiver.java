package com.threedlite.userhash.location;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

public class OnAlarmReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {


		PowerManager mgr = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock lock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, 
				"com.threedlite.userhash.location.OnAlarmReceiver");
		lock.acquire(1000*30);
		try {

			context.startService(new Intent(context, ReportingService.class));

		} finally {
			try {
				lock.release();
			} catch (Exception e) {
				// Ignore
			}
		}

	}

}