package com.threedlite.userhash.location;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class BaseActivity extends Activity {

	protected String TAG = "LocationReportingService";

	private static final String SETTINGS = "Settings";

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		menu.add(SETTINGS);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		String title = item.getTitle().toString();
		if (title.equals(SETTINGS)) {
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}

	}

	protected void toast(String s, Throwable t) {
		String msg = s + ": " + t.getMessage();
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
		error(msg, t);
	}

	protected void toast(String s) {
		Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
	}

	private void error(String s, Throwable t) {
		Log.e(TAG, s, t);
		t.printStackTrace();
	}


}
