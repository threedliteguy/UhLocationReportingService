package com.threedlite.userhash.location;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings.Secure;
import android.util.Log;

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

public class LocationReportingDelegate  {

	private static final String TAG = "LocationReportingService";


	private Context context;

	public LocationReportingDelegate(Context context) {
		this.context = context;
	}

	public void reportLocation() {

		try {

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			boolean enabled = prefs.getBoolean(Constants.PREF_ENABLED, true);
			if (!enabled) {
				return;
			}
	
			final LrsLocationListener locationListener = new LrsLocationListener() {
				public void onLocationChanged(Location location) {
					if ("network".equals(location.getProvider())) {
						LocationReportingDelegate.getLocationManager(context).removeUpdates(this);
						handleLocationUpdate();
					}
				}
				public void onStatusChanged(String provider, int status, Bundle extras ) {
					if ("network".equals(provider)) {
						LocationReportingDelegate.getLocationManager(context).removeUpdates(this);
					}
				}
			};

			LocationReportingDelegate.getLocationManager(context).requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
		
		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
		}
		
	}

	private void handleLocationUpdate() {

		try {

			Map<String, String> all = new HashMap<String, String>();

			Location networkLocation = getLocationManager(context).getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			if (networkLocation == null) networkLocation = new NullLocation(LocationManager.NETWORK_PROVIDER);
			networkLocation.setAccuracy(networkLocation.getAccuracy() * .5f); // This is a hack since the net accuracy seems too large
			all.putAll(getProperties(networkLocation));

			Location gpsLocation = getLocationManager(context).getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if (gpsLocation == null || !validateGps(gpsLocation, networkLocation)) {
				gpsLocation = new NullLocation(LocationManager.GPS_PROVIDER);
			}
			all.putAll(getProperties(gpsLocation));

			String androidId = Secure.getString(context.getContentResolver(),Secure.ANDROID_ID); 
			all.put("_deviceId", ""+androidId);

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
			String command = buildSetCommand(all, prefs.getString(Constants.PREF_USERNAMES, ""));

			HttpsUtil.send(null, command, context);

		} catch (Exception e) {
			//Log.e(TAG, e.getMessage(), e);
		}
	}
	
	private String buildSetCommand(Map<String, String> map, String usernames) {
		final StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, String> me : map.entrySet()) {
			sb.append(Constants.KEY_PREFIX).append(".").append(me.getKey()).append("=").append(me.getValue()).append("\n");
		}
		sb.append("_=" + usernames); // always update permissions with post in case server lost data
		String command = "set " + sb.toString();
		return command;
	}

	private boolean validateGps(Location gpsLocation, Location networkLocation) {
		float dist = gpsLocation.distanceTo(networkLocation);
		if (networkLocation.hasAccuracy() && dist > 2000) {
			return false;
		}
		return true;
	}

	private Map<String, String> getProperties(Location location) {

		Map<String, String> map = new HashMap<String, String>();
		String provider = location.getProvider();
		map.put(provider + ".latitude", ""+location.getLatitude());
		map.put(provider + ".longitude", ""+location.getLongitude());
		map.put(provider + ".accuracy", ""+location.getAccuracy()); // meters, 0.0f = no accuracy
		map.put(provider + ".time", Constants.formatTime(location.getTime()));

		return map;
	}

	private static final class NullLocation extends Location {
		public NullLocation(String s) {
			super(s);
		}
		public double getLatitude() { return 0; }
		public double getLongitude() { return 0; }
		public float getAccuracy() { return 0; }
		public long getTime() { return 0; }
		public boolean hasAccuracy() { return false; }
	}


	public static LocationManager getLocationManager(Context context) {
		return (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
	}

	public static class LrsLocationListener implements LocationListener {
		public void onLocationChanged(Location location) {
		}
		public void onProviderDisabled(String arg0) {
		}
		public void onProviderEnabled(String provider) {
		}
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	}

}
