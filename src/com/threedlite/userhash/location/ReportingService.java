package com.threedlite.userhash.location;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;


/*

Copyright 2013 Dan Meany

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

public class ReportingService extends IntentService {

	public ReportingService() {
		super("ReportingService");
	}

	private synchronized void doReporting(Intent intent) {

		PowerManager mgr = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock lock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, 
				"com.threedlite.userhash.location.ReportingService");
		lock.acquire(1000*30);
		try {

			try {
				new LocationReportingDelegate(this).reportLocation();
			} catch (Exception e) {
				Toast.makeText(this, ""+e.getMessage(), Toast.LENGTH_LONG).show();
				Log.e("lrs", "lrs", e);
				e.printStackTrace();
			}

		} finally {
			try {
				lock.release();
			} catch (Exception e) {
				// Ignore
			}
		}

	}

	// Need to do reporting here since onHandleIntent() is not called 
	// from receiver start 
	@Override
	public void onStart(Intent intent, int startId) {
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		doReporting(intent);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		doReporting(intent);
	}

}