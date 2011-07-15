package com.almightybuserror.iMapette;

import java.util.Date;
import java.util.List;

import com.almightybuserror.iMapette.R;
import com.almightybuserror.iMapette.providers.Spots;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

/**
 * Main application activity which handles the map.
 * 
 * @author Fernando Alexandre
 *
 */
public class IMapette extends MapActivity {

	/**
	 * Tag the represents this class in log.
	 */
	public static final String TAG = IMapette.class.getName();

	/**
	 * Intent used to trigger a GUI refresh.
	 */
	public static final String REFRESH_GUI = "com.almightybuserror.intent.action.REFRESH_GUI";
	
	/**
	 * Intent used to trigger a GUI refresh.
	 */
	public static final String PRIVACY_CHANGE = "com.almightybuserror.intent.action.PRIVACY_CHANGED";

	/**
	 * The MapView which contains the points.
	 */
	private MapView mapView;

	/**
	 * Dialog containing all debugging options.
	 */
	protected static Dialog debugDialog;
	
	/**
	 * Dialog containing all debugging options.
	 */
	protected static Dialog controlPanelDialog;

	/**
	 * List of the points on the map.
	 */
	List<Overlay> mapOverlays;
	
	/**
	 * Image used as a marker of the points.
	 */
	Drawable drawable;
	
	/**
	 * Overlay with all the points in it.
	 */
	SpotItemizedOverlay itemizedOverlay;

	/**
	 * Object that receives all intents that interact with the GUI.
	 */
	private NotificationCenter mNotificationCenter;
	
	/**
	 * Whether the collected data is private or public.
	 */
	private boolean isPrivate;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.map);

		mNotificationCenter = new NotificationCenter();
		
		isPrivate = false;

		this.registerReceiver(mNotificationCenter, 
				new IntentFilter(REFRESH_GUI));

		mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);

		startTrackingService();
		startTransmissionService();

		populateMap();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onResume() {
		super.onResume();

		this.registerReceiver(mNotificationCenter, new IntentFilter(REFRESH_GUI));
	}

	@Override
	public void onPause() {
		super.onPause();

		this.unregisterReceiver(mNotificationCenter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.opt_debug:
			openDebugMenu();
			break;
		case R.id.opt_control_panel:
			openControlPanel();
			break;
		default:
			super.onOptionsItemSelected(item);
			break;
		}

		return true;
	}

	/**
	 * Opens the debug menu;
	 */
	public void openDebugMenu() {
		debugDialog = new Dialog(this);
		debugDialog.setContentView(R.layout.debug_dialog);
		debugDialog.setTitle("Debugging Options");

		Button b = (Button) debugDialog.findViewById(R.id.btn_start_service);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				startTrackingService();
				startTransmissionService();
			}
		});

		b = (Button) debugDialog.findViewById(R.id.btn_stop_service);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				stopTrackingService();
				stopTransmissionService();
			}
		});

		b = (Button) debugDialog.findViewById(R.id.btn_clear_db);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				getContentResolver().delete(Spots.CONTENT_URI, null, null);
				refreshUI();
			}
		});
		
		b = (Button) debugDialog.findViewById(R.id.btn_upload);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				sendBroadcast(new Intent(TransmissionService.DIRTY_INTENT));
			}
		});
		
		b = (Button) debugDialog.findViewById(R.id.btn_reset_db);
		b.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				sendBroadcast(new Intent(TransmissionService.RESET_INTENT));
			}
		});

		debugDialog.show();
	}
	
	/**
	 * Opens the User Control Panel
	 */
	public void openControlPanel() {
		controlPanelDialog = new Dialog(this);
		controlPanelDialog.setContentView(R.layout.control_panel);
		controlPanelDialog.setTitle("User Control Panel");
		
		Button b = (Button) controlPanelDialog.findViewById(R.id.btn_privacy);
		if(isPrivate)
			b.setText(R.string.opt_not_private);
		b.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent i = new Intent(TransmissionService.PRIVACY_INTENT);
				isPrivate = !isPrivate;

				i.putExtra("privacy", isPrivate);
				sendBroadcast(i);
			}
			
		});
		
		b = (Button) controlPanelDialog.findViewById(R.id.btn_sync);
		b.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				sendBroadcast(new Intent(TransmissionService.DOWNLOAD_INTENT));
			}
			
		});
		
		controlPanelDialog.show();
	}

	/**
	 * Adds points to the map.
	 */
	protected void populateMap() {
		String title;
		String info;

		mapOverlays = mapView.getOverlays();
		drawable = this.getResources().getDrawable(R.drawable.androidmarker);
		itemizedOverlay = new SpotItemizedOverlay(drawable, this);

		Cursor c = getContentResolver().query(Spots.CONTENT_URI, null, null, null, Spots.TIMESTAMP + " desc");

		int latdIndex = c.getColumnIndex(Spots.LATD);
		int longdIndex = c.getColumnIndex(Spots.LONGD);
		int accIndex = c.getColumnIndex(Spots.ACCURACY);
		int altIndex = c.getColumnIndex(Spots.ALTITUDE);
		int speedIndex = c.getColumnIndex(Spots.SPEED);
		int timeIndex = c.getColumnIndex(Spots.TIMESTAMP);
		int uploadedIndex = c.getColumnIndex(Spots.UPLOADED);

		if(c.moveToFirst()) {
			do {
				title = String.format("Lat: %f\nLong: %f", c.getFloat(latdIndex), c.getFloat(longdIndex));
				info = String.format("Acc: %.2f\nAlt: %.2f\nSpeed: %.2f\nTime:%s\nUploaded:%s", c.getFloat(accIndex), 
						c.getFloat(altIndex), c.getFloat(speedIndex), new Date(c.getLong(timeIndex)).toString(),
						c.getInt(uploadedIndex) == 1 ? "Yes" : "No");

				GeoPoint point = new GeoPoint((int) (c.getFloat(latdIndex) * 1E6),
						(int) (c.getFloat(longdIndex) * 1E6));
				OverlayItem overlayItem = new OverlayItem(point, title, info);

				itemizedOverlay.addOverlay(overlayItem);
			} while (c.moveToNext());
		}

		mapOverlays.add(itemizedOverlay);
		c.close();
	}
	
	/**
	 * Shows a information dialog.
	 * @param message
	 * 	String resource used to define the message.
	 * @param duration
	 * 	Dialog's TTL (in seconds).
	 */
	private void showInformation(int contentRes, long duration) {
		final Dialog mDialog = new Dialog(this);

		mDialog.setContentView(contentRes);
		mDialog.setTitle("Information");
		mDialog.show();

		(new Handler()).postDelayed(new Runnable() {
			public void run() {
				mDialog.dismiss();
			}}, duration*1000); // Close dialog after delay
	}


	/**
	 * Starts the tracking service.
	 * @return
	 * 	Returns if the tracking service has been started.
	 */
	private Boolean startTrackingService() {
		if(startService(new Intent(this, TrackingService.class)) != null)	
			return true;
		return false;
	}

	/**
	 * Stops the tracking service.
	 * @return
	 * 	Returns if the tracking service has been stopped.
	 */
	private Boolean stopTrackingService() {
		if(stopService(new Intent(this, TrackingService.class)))		
			return true;
		return false;
	}

	/**
	 * Starts the tracking service.
	 * @return
	 * 	Returns if the tracking service has been started.
	 */
	private Boolean startTransmissionService() {
		if(startService(new Intent(this, TransmissionService.class)) != null)	
			return true;
		return false;
	}

	/**
	 * Stops the tracking service.
	 * @return
	 * 	Returns if the tracking service has been stopped.
	 */
	private Boolean stopTransmissionService() {
		if(stopService(new Intent(this, TransmissionService.class)))		
			return true;
		return false;
	}

	/**
	 * Refreshes the map interface.
	 */
	private void refreshUI() {
		populateMap();
		mapView.invalidate();
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	/**
	 * Handles the notifications from other entities such as services.
	 * 
	 * @author Fernando Alexandre
	 *
	 */
	private class NotificationCenter extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals(REFRESH_GUI)) {
				refreshUI();
			} if (intent.getAction().equals(PRIVACY_CHANGE)) {
				// For the control panel UI change
				isPrivate = !isPrivate;
				
				if(isPrivate)
					showInformation(R.layout.privacy_private, 5);
				else
					showInformation(R.layout.privacy_not_private, 5);
			}
		}

	}
}