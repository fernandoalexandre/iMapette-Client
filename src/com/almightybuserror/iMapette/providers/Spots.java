package com.almightybuserror.iMapette.providers;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Represents a Spots place in the Android Database and the table's fields.
 * 
 * @author Fernando Alexandre
 */
public class Spots implements BaseColumns {
	public static final Uri CONTENT_URI = Uri.parse("content://"
			+ SpotsContentProvider.AUTHORITY + "/spots");

	public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.almightybuserror.spots";

	public static final String SPOT_ID = "_id";

	public static final String LATD = "latd";

	public static final String LONGD = "longd";
	
	public static final String ACCURACY = "accuracy";
	
	public static final String TIMESTAMP = "timestamp";

	public static final String ALTITUDE = "altitude";

	public static final String SPEED = "speed";
	
	public static final String UPLOADED = "uploaded";

	private Spots() {}
}
