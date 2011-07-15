package com.almightybuserror.iMapette.providers;

import java.util.HashMap;

import com.almightybuserror.iMapette.providers.Spots;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

/**
 * Provides a interface to the Database.
 * 
 * Based off http://thinkandroid.wordpress.com/2010/01/13/writing-your-own-contentprovider/
 * 	 	by Jason Wei.
 * 
 * @author Fernando Alexandre
 *
 */
public class SpotsContentProvider extends ContentProvider {

    /**
     * Tag that represents this class in the logs.
     */
    private static final String TAG = SpotsContentProvider.class.getName();

    /**
     * Name of the database file.
     */
    private static final String DATABASE_NAME = "spots.db";

    /**
     * Version of the database.
     */
    private static final int DATABASE_VERSION = 2;

    /**
     * Name of the database table used.
     */
    private static final String SPOTS_TABLE_NAME = "spots";

    /**
     * Content provider's authority string.
     */
    public static final String AUTHORITY = "com.almightybuserror.iMapette.providers.SpotsContentProvider";

    /**
     * The URI matcher.
     */
    private static final UriMatcher sUriMatcher;

    /**
     * Value URI matcher returns on success.
     */
    private static final int IS_SPOT = 1;

    /**
     * Hashmap with a list of database field names.
     */
    private static HashMap<String, String> notesProjectionMap;

    /**
     * Defines a database helper which handles its Creation/Update.
     * 
     * @author Fernando Alexandre
     *
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + SPOTS_TABLE_NAME + " (" + Spots.SPOT_ID
                    + " INTEGER PRIMARY KEY AUTOINCREMENT, " + Spots.LATD + " REAL, " + Spots.LONGD
                    + " REAL, " + Spots.ACCURACY + " REAL, " + Spots.TIMESTAMP + " INTEGER, "
                    + Spots.ALTITUDE + " REAL, " + Spots.SPEED + " REAL, " + 
                    Spots.UPLOADED + " INTEGER DEFAULT 0" + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
                    + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + SPOTS_TABLE_NAME);
            onCreate(db);
        }
    }

    /**
     * The link to the database.
     */
    private DatabaseHelper dbHelper;

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case IS_SPOT:
                count = db.delete(SPOTS_TABLE_NAME, where, whereArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case IS_SPOT:
                return Spots.CONTENT_TYPE;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        if (sUriMatcher.match(uri) != IS_SPOT) { throw new IllegalArgumentException("Unknown URI " + uri); }

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long rowId = db.insert(SPOTS_TABLE_NAME, null, values);
        if (rowId > 0) {
            Uri noteUri = ContentUris.withAppendedId(Spots.CONTENT_URI, rowId);
            getContext().getContentResolver().notifyChange(noteUri, null);
            return noteUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public boolean onCreate() {
        dbHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (sUriMatcher.match(uri)) {
            case IS_SPOT:
                qb.setTables(SPOTS_TABLE_NAME);
                qb.setProjectionMap(notesProjectionMap);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);

        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count;
        switch (sUriMatcher.match(uri)) {
            case IS_SPOT:
                count = db.update(SPOTS_TABLE_NAME, values, where, whereArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, SPOTS_TABLE_NAME, IS_SPOT);

        notesProjectionMap = new HashMap<String, String>();
        notesProjectionMap.put(Spots.SPOT_ID, Spots.SPOT_ID);
        notesProjectionMap.put(Spots.LATD, Spots.LATD);
        notesProjectionMap.put(Spots.LONGD, Spots.LONGD);
        notesProjectionMap.put(Spots.ACCURACY, Spots.ACCURACY);
        notesProjectionMap.put(Spots.TIMESTAMP, Spots.TIMESTAMP);
        notesProjectionMap.put(Spots.ALTITUDE, Spots.ALTITUDE);
        notesProjectionMap.put(Spots.SPEED, Spots.SPEED);
        notesProjectionMap.put(Spots.UPLOADED, Spots.UPLOADED);

    }
}
