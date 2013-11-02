package com.threedlite.userhash.location;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.threedlite.userhash.location.HttpsUtil.StatusCallback;
import com.threedlite.userhash.location.LocationReportingDelegate.LrsLocationListener;


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

public class ViewActivity extends BaseActivity {

	protected String TAG = "ViewActivity";

	private SharedPreferences mPrefs;
	private ArrayAdapter<String> mNames;
	private String mDefaultTemplate;
	private Activity mActivity;
	private Handler mHandler;
	private boolean mRefresh = true;
	private final Runnable mRunnable = new Runnable() {
		public void run() {
			getUpdates();
			if (mRefresh) {
				mHandler.postDelayed(mRunnable, 1000*60);
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		mActivity = this;
		mHandler = new Handler();

		mPrefs = setupPrefs();

		setContentView(R.layout.view_layout);

		setupWebView();


		final EditText et = (EditText) findViewById(R.id.edit_text_out);

		Button btSend = (Button) findViewById(R.id.button_send);

		btSend.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				String command = "set "
						+Constants.KEY_PREFIX+".status="
						+et.getText()+"\n"
						+Constants.KEY_PREFIX+".status.time="
						+Constants.formatTime(new Date().getTime());

				HttpsUtil.send(null, command, mActivity);

				et.setText("");

			}
		});

		Button btGps = (Button) findViewById(R.id.button_gps);

		btGps.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {

				final LrsLocationListener locationListener = new LrsLocationListener() {
					public void onLocationChanged(Location location) {
						if ("gps".equals(location.getProvider())) {
							LocationReportingDelegate.getLocationManager(mActivity).removeUpdates(this);
							mActivity.startService(new Intent(mActivity, ReportingService.class));
						}
					}
					public void onStatusChanged(String provider, int status, Bundle extras ) {
						if ("gps".equals(provider)) {
							LocationReportingDelegate.getLocationManager(mActivity).removeUpdates(this);
						}
					}
				};
				try {
					LocationReportingDelegate.getLocationManager(mActivity).requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
				} catch (Exception e) {}
			}
		});

		getUpdates();

		mHandler.postDelayed(mRunnable, 1000*60);

	}

	@Override
	public void onDestroy() {
		mRefresh = false;
		super.onDestroy();
	}

	@Override
	public void onPause() {
		mRefresh = false;
		super.onPause();
	}

	@Override
	public void onResume() {
		mRefresh = true;
		super.onResume();
	}


	private void getUpdates() {

		try {

			if (!mRefresh) {
				return;
			}

			final String name = getIntent().getExtras().getString(NAME);

			String command = "list "+name+"."+Constants.KEY_PREFIX+".";

			StatusCallback cb = new StatusCallback() {
				public void status(String msg) {
				}
				public void result(String msg) {
					handleResult(name, msg);
				}
			};

			HttpsUtil.send(cb, command, mActivity);

		} catch (Exception e) {
			Log.e(TAG, "Read intent name", e);
		}

	}

	private void setupWebView() {

		WebView engine = (WebView) findViewById(R.id.web_engine);
		engine.loadData("<html><body bgcolor='#000000' /></html>", "text/html", "UTF-8");

		mDefaultTemplate = getDefaultTemplate();

	}

	private SharedPreferences setupPrefs() {
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
			public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
				loadNames();
			}
		};
		prefs.registerOnSharedPreferenceChangeListener(listener);
		return prefs;
	}

	private String getDefaultTemplate() {
		Resources res = getResources();
		InputStream in = res.openRawResource(R.raw.map_template);
		byte[] b;
		try {
			b = new byte[in.available()];
			in.read(b);
			return new String(b);
		} catch (IOException e) {
			return "";
		}
	}

	@SuppressLint("SetJavaScriptEnabled")
	private void handleResult(String name, String msg) {

		try {

			Map<String, String> props = getProps(msg);

			String time = props.get("network.time");
			if (time == null) {
				// server may be down
				return;
			}

			String key = mPrefs.getString(Constants.PREF_MAP_KEY, "");
			props.put("key", key);


			String browserTemplate = getTemplate();
			validateProps(props);
			String browserHtml = replace(browserTemplate, props);

			WebView engine = (WebView) findViewById(R.id.web_engine);
			WebSettings webSettings = engine.getSettings();
			webSettings.setJavaScriptEnabled(true);   
			engine.loadData(browserHtml,"text/html","UTF-8");  

			String status = props.get("status");
			if (status == null) status = "";
			if (status.length() > MAX_STATUS_LEN) status = status.substring(0,MAX_STATUS_LEN);
			String statusTime = props.get("status.time");
			if (statusTime == null) statusTime = "";

			TextView tv = (TextView) findViewById(R.id.location_info);
			tv.setText(name+"\n"+status+"\nLocation "+time+"\nStatus "+statusTime);

		} catch (Exception e) {
			Log.e(TAG, "handleResult:", e);
		}

	}

	private void validateProps(Map<String, String> map) {
		Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, String> me = it.next();
			if (jsProperty(me.getKey())) {
				if (!validNumber(me.getValue())) {
					it.remove();
				}
			}
		}
	}

	// Numeric props that may be used in template's javascript 
	private boolean jsProperty(String s) {
		return (
				s.endsWith(".latitude") 
				|| s.endsWith(".longitude") 
				|| s.endsWith(".accuracy")
				);
	}

	private boolean validNumber(String s) {
		try {
			Double.parseDouble(s);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private static final int MAX_STATUS_LEN = 140;

	public static final String NAME = "name";

	private String getTemplate() {
		String browserTemplate = mPrefs.getString(Constants.PREF_MAP_TEMPLATE, "");
		if (browserTemplate == null || browserTemplate.trim().length() == 0) {
			browserTemplate = mDefaultTemplate;
		}
		return browserTemplate;
	}

	private String replace(String s, Map<String, String> map) {
		for (Map.Entry<String, String> me: map.entrySet()) {
			String lookfor = "${"+me.getKey()+"}";
			s = s.replace(lookfor, escape(me.getValue()));
		}
		return s;
	}

	private String escape(String s) {
		if (s == null) return null;
		s = s.replace("&", "&amp;");
		s = s.replace("<", "&lt;");
		s = s.replace(">", "&gt;");
		s = s.replace("\'", "&apos;");
		s = s.replace("\"", "&quot;");
		return s;
	}

	private Map<String, String> getProps(String msg) {
		Map<String, String> map = new HashMap<String, String>();
		String[] a = msg.split("\n");
		for (String s: a) {
			s = s.trim();
			if (s.length() == 0) continue;
			String[] kv = s.split("=");
			if (kv.length == 0) continue;
			String key = kv[0];
			String value = kv.length > 1 ? kv[1] : "";
			value = value.trim();
			map.put(key, value);
		}
		return map;
	}


	private void loadNames() {
		mNames.clear();
		String pref = mPrefs.getString(Constants.PREF_USERNAMES, "");
		String[] items = pref.trim().split("\\s+");
		for (String s: items) {
			mNames.add(s);
		}
		mNames.notifyDataSetChanged();
		setViewPermissions(pref);
	}

	private void setViewPermissions(String usernames) {
		String command = "set " + "_=" +usernames;
		HttpsUtil.send(null, command, this);
	}

}
