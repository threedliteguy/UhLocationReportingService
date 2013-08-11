package com.threedlite.userhash.location;

import java.text.SimpleDateFormat;

public class Constants {

	public static final String KEY_PREFIX = "lrs.data";

	public static final String PREF_ENABLED = "pref_enabled";
	public static final String PREF_MINUTES = "pref_minutes";

	public static final String PREF_USERNAMES = "pref_usernames";
	public static final String PREF_MAP_KEY = "pref_map_key";
	public static final String PREF_MAP_TEMPLATE = "pref_map_template";

	public static final String PREF_URL = "pref_url";
	public static final String PREF_USERNAME = "pref_username";
	public static final String PREF_PWD = "pref_pwd";
	public static final String PREF_CERT = "pref_cert";



	public static String formatTime(long time) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");
		return sdf.format(new java.util.Date(time));
	}



}
