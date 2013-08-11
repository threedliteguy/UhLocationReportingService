package com.threedlite.userhash.location;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;


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

public class SelectUserActivity extends BaseActivity {

	protected String TAG = "SelectUserActivity";

	private SharedPreferences mPrefs;
	private ArrayAdapter<String> mNames;
	private Activity mActivity;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		mActivity = this;

		mPrefs = setupPrefs();

		setContentView(R.layout.select_user_layout);

		mNames = new ArrayAdapter<String>(this, R.layout.list_item);
		ListView lv = (ListView) findViewById(R.id.content_list);
		lv.setAdapter(mNames);

		loadNames();

		lv.setOnItemClickListener( new AdapterView.OnItemClickListener() {

			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

				final String name = mNames.getItem(position);
				Intent viewIntent = new Intent(mActivity, ViewActivity.class);
				viewIntent.putExtra(ViewActivity.NAME, name);
				startActivity(viewIntent);

			}

		});



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
