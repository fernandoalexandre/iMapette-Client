package com.almightybuserror.iMapette;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import com.almightybuserror.iMapette.Json.Spot;
import com.almightybuserror.iMapette.providers.Spots;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.provider.Settings.Secure;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


/**
 * Represents the service that is responsible to upload all data.
 * 
 * @author Fernando Alexandre
 *
 */
public class TransmissionService extends Service {

	/**
	 * Identifying tag of the service.
	 */
	private static String TAG = TransmissionService.class.getName();

	/**
	 * Link to the webservice.
	 */
	private String RPC_Domain = "http://leimapette.appspot.com/rpc";

	/**
	 * Intent used to notify this service that the database has been changed.
	 */
	public static String DIRTY_INTENT = "com.almightybuserror.intent.action.DIRTY";

	/**
	 * Intent used to notify this service to re-download all locations from other users.
	 */
	public static String DOWNLOAD_INTENT = "com.almightybuserror.intent.action.DOWNLOAD";

	/**
	 * Intent used to notify this service to delete database and
	 * re-download all locations, including this user's.
	 */
	public static String RESET_INTENT = "com.almightybuserror.intent.action.RESET";

	/**
	 * Intent used to trigger a change in the privacy settings.
	 */
	public static String PRIVACY_INTENT = "com.almightybuserror.intent.action.PRIVACY";

	/**
	 * Unique ID that represents this device on the webservice.
	 */
	private String id;

	/**
	 * NotificationCenter used to handle connectivity issues.
	 */
	private NotificationCenter mNotificationCenter;

	/**
	 * Is there anything to upload or not.
	 */
	private boolean isDirty;

	@Override
	public void onCreate() {
		mNotificationCenter = new NotificationCenter();
		this.registerReceiver(mNotificationCenter, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		this.registerReceiver(mNotificationCenter, new IntentFilter(DIRTY_INTENT));
		this.registerReceiver(mNotificationCenter, new IntentFilter(DOWNLOAD_INTENT));
		this.registerReceiver(mNotificationCenter, new IntentFilter(RESET_INTENT));
		this.registerReceiver(mNotificationCenter, new IntentFilter(PRIVACY_INTENT));

		isDirty = false;

		id = getUniqueID();

		Log.i(TAG, "Service started");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onDestroy() {
		this.unregisterReceiver(mNotificationCenter);

		Log.i(TAG, "Service stopped");
	}

	/**
	 * Generates this android's install unique ID.
	 * @return
	 * 	This device's unique ID hash, or the unique id itself in a fallback. 
	 */
	private String getUniqueID() {
		// Generate the unique id.
		String android_id = Secure.getString(getContentResolver(), Secure.ANDROID_ID);

		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA");
			md.update(android_id.getBytes());
			byte[] digest = md.digest();

			// Convert to hex.
			String hexStr = "";
			for (int i = 0; i < digest.length; i++) {
				hexStr +=  Integer.toString( ( digest[i] & 0xff ) + 0x100, 16).substring( 1 );
			}

			return hexStr;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();

			return android_id; // Ugly fallback, but works.
		}
	}

	/**
	 * Gets all points not yet uploaded and creates a hashmap with pair <POST var, VALUE>
	 * to be uploaded to the webservice.
	 * 
	 * @return
	 * 	The hashmap.
	 */
	private HashMap<String, String> createAddSpotsContent() {
		HashMap<String, String> values = new HashMap<String,String>();
		int counter = 0;

		Cursor c = getContentResolver().query(Spots.CONTENT_URI,
				null,
				Spots.UPLOADED + "=0",
				null,
				Spots.TIMESTAMP + " desc");

		if(c.moveToFirst()) {

			int latdIndex = c.getColumnIndex(Spots.LATD);
			int longdIndex = c.getColumnIndex(Spots.LONGD);
			int accIndex = c.getColumnIndex(Spots.ACCURACY);
			int altIndex = c.getColumnIndex(Spots.ALTITUDE);
			int speedIndex = c.getColumnIndex(Spots.SPEED);
			int timeIndex = c.getColumnIndex(Spots.TIMESTAMP);

			values.put("method", "addSpots");
			values.put("user_id", id);

			do {
				values.put(String.format("%s%d", Spots.LATD, counter),
						Float.toString(c.getFloat(latdIndex)));
				values.put(String.format("%s%d", Spots.LONGD, counter),
						Float.toString(c.getFloat(longdIndex)));
				values.put(String.format("%s%d", Spots.ACCURACY, counter),
						Float.toString(c.getFloat(accIndex)));
				values.put(String.format("%s%d", Spots.ALTITUDE ,counter),
						Float.toString(c.getFloat(altIndex)));
				values.put(String.format("%s%d", Spots.SPEED, counter),
						Float.toString(c.getFloat(speedIndex)));
				values.put(String.format("%s%d", Spots.TIMESTAMP, counter),
						Long.toString(c.getLong(timeIndex)));

				c.moveToNext();
				counter++;
			} while(c.moveToNext());

			values.put("spotCount", Integer.toString(counter));

			isDirty = false;

			c.close();

			return values;
		}
		c.close();

		return null;
	}

	/**
	 * Checks the status of the Internet connection.
	 * @return
	 * 	True if there is a link to the Internet, false otherwise.
	 */
	private boolean isConnected() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		// test for connection
		if (cm.getActiveNetworkInfo() != null
				&& cm.getActiveNetworkInfo().isAvailable()
				&& cm.getActiveNetworkInfo().isConnected()) {

			return true;
		} else {
			Log.v(TAG, "Internet Connection Not Present");

			return false;
		}
	}

	/**
	 * 	Uploads the POST Content.
	 * @return
	 * 	True if the status code received is 204 (OK and not more information).
	 */
	private boolean upload() {
		HttpResponse resp = doPost(RPC_Domain, createAddSpotsContent(), null, null, getClient());

		if(resp != null) {
			Log.i(TAG, "Code: " + resp.getStatusLine().getStatusCode());

			return resp.getStatusLine().getStatusCode() == 204; // Ok and no more info, same as in webservice.
		}
		return false;
	}

	/**
	 * Marks all the spots as updated.
	 */
	private void markAllUploaded() {
		// Set all Spots as "uploaded"
		ContentValues c = new ContentValues();
		c.put(Spots.UPLOADED, 1);

		getContentResolver().update(Spots.CONTENT_URI, c, Spots.UPLOADED + "=0", null);
	}

	/**
	 *  Downloads Spots from the webservice.
	 * @param dlAll
	 * 	Only downloads other people's spots if false. Downloads all if true.
	 * @return
	 * 	True if the request was successful and Json response parsed correctly.
	 */
	private boolean download(boolean dlAll) {
		HashMap<String, String> values = new HashMap<String, String>();
		values.put("method", "getSpots");
		if(!dlAll)
			values.put("user_id", id);

		HttpResponse resp = doPost(RPC_Domain, values, null, null, getClient());
		if(resp != null) {
			try {
				Gson s = new Gson();
				InputStream in = resp.getEntity().getContent();
				Reader r = new InputStreamReader(in);

				Type listType = new TypeToken<List<Spot>>() {}.getType(); 
				List<Spot> sp = s.fromJson(r, listType);

				for(Spot spot : sp) {
					ContentValues v = new ContentValues();
					v.put(Spots.LATD, spot.getLatd());
					v.put(Spots.LONGD, spot.getLongd());
					v.put(Spots.SPEED, spot.getSpeed());
					v.put(Spots.ALTITUDE, spot.getAltitude());
					v.put(Spots.ACCURACY, spot.getAccuracy());
					v.put(Spots.TIMESTAMP, spot.getTimestamp());
					v.put(Spots.UPLOADED, 1);

					getContentResolver().insert(Spots.CONTENT_URI, v);
				}
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}

			return resp.getStatusLine().getStatusCode() == 200;
		}
		return false;
	}

	/**
	 * Sends a request to change the privacy settings to the RPC.
	 * @param newPrivacy
	 * 	New privacy setting.
	 * @return
	 * 	True if the request was successful, false otherwise.
	 */
	private boolean changePrivacy(boolean newPrivacy) {
		HashMap<String, String> values = new HashMap<String, String>();
		values.put("method", "setPrivacy");
		values.put("user_id", id);
		values.put("privacy", "" + (newPrivacy ? 1 : 0));

		HttpResponse resp = doPost(RPC_Domain, values, null, null, getClient());
		if(resp != null)
			return resp.getStatusLine().getStatusCode() == 204;
		return false;
	}

	/**
	 * 	Represents the NotificationCenter that receives changes to the database 
	 * and the Internet connection.
	 * 
	 * @author Fernando Alexandre
	 *
	 */
	private class NotificationCenter extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
				Log.v(TAG, "Entered ConnectivityAction.");

				if(isDirty) {
					if(isConnected() && upload()) {
						isDirty = false;

						Log.v(TAG, "Uploaded.");

						markAllUploaded();

						sendBroadcast(new Intent(IMapette.REFRESH_GUI));

					}
				}
			} else if(intent.getAction().equals(DIRTY_INTENT)) {
				Log.i(TAG, "Entered DirtyIntent.");

				if(isConnected() && upload()) {					
					isDirty = false;

					Log.v(TAG, "Uploaded.");

					markAllUploaded();

					sendBroadcast(new Intent(IMapette.REFRESH_GUI));
				} else {
					isDirty = true;

					Log.v(TAG, "Waited.");
				}
			} else if(intent.getAction().equals(DOWNLOAD_INTENT)) {
				if(isConnected() && download(false)) {
					Log.v(TAG, "Downloaded.");
					sendBroadcast(new Intent(IMapette.REFRESH_GUI));
				} else {
					Log.e(TAG, "Failed to download.");
				}
			} else if(intent.getAction().equals(RESET_INTENT)) {
				if(isConnected()) {
					getContentResolver().delete(Spots.CONTENT_URI, null, null);
					Log.v(TAG, "Deleted local DB.");

					if(download(true)) {
						Log.v(TAG, "Downloaded.");
						sendBroadcast(new Intent(IMapette.REFRESH_GUI));
						return;
					}
					sendBroadcast(new Intent(IMapette.REFRESH_GUI));
				}
				Log.e(TAG, "Failed to download.");
			} else if(intent.getAction().equals(PRIVACY_INTENT)) {
				boolean newPrivacy = intent.getExtras().getBoolean("privacy");
				
				if(isConnected() && changePrivacy(newPrivacy))
					sendBroadcast(new Intent(IMapette.PRIVACY_CHANGE));
					Log.v(TAG, "Changed privacy to " + newPrivacy);
			}
		}

	}

	/**
	 * Based off http://thinkandroid.wordpress.com/2009/12/31/creating-an-http-client-example/
	 * @return
	 * 	A configured DefaultHTTPClient.
	 */
	public DefaultHttpClient getClient() {
		DefaultHttpClient ret = null;

		//sets up parameters
		HttpParams params = new BasicHttpParams();
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
		HttpProtocolParams.setContentCharset(params, "utf-8");
		params.setBooleanParameter("http.protocol.expect-continue", false);

		//registers schemes for both http and https
		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		final SSLSocketFactory sslSocketFactory = SSLSocketFactory.getSocketFactory();
		sslSocketFactory.setHostnameVerifier(SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
		registry.register(new Scheme("https", sslSocketFactory, 443));

		ThreadSafeClientConnManager manager = new ThreadSafeClientConnManager(params, registry);
		ret = new DefaultHttpClient(manager, params);
		return ret;
	}


	/**
	 * Based off http://thinkandroid.wordpress.com/2009/12/31/how-to-do-a-restful-post-call/
	 * 
	 * @param mUrl
	 * 	Url to post to
	 * @param hm
	 * 	Values on the POST.
	 * @param username
	 * 	Username for authentication (Optional)
	 * @param password
	 *  Password for authentication (Optional)
	 * @param httpClient
	 * 	DefaultHttpClient to be used to send the POST
	 * @return
	 * 	Response of the query.
	 */
	public static HttpResponse doPost(String mUrl, HashMap<String, String> hm, String username, String password,
			DefaultHttpClient httpClient) {
		HttpResponse response = null;

		if (username != null && password != null) {
			httpClient.getCredentialsProvider().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
					new UsernamePasswordCredentials(username, password));
		}

		HttpPost postMethod = new HttpPost(mUrl);

		if (hm == null) return null;

		try {
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			Iterator<String> it = hm.keySet().iterator();
			String k, v;

			while (it.hasNext()) {
				k = it.next();
				v = hm.get(k);
				nameValuePairs.add(new BasicNameValuePair(k, v));
			}

			postMethod.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			Log.v(TAG, postMethod.getURI().toString());
			response = httpClient.execute(postMethod);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return response;
	}
}
