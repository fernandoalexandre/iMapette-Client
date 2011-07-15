package com.almightybuserror.iMapette;

import java.util.Timer;
import java.util.TimerTask;

import com.almightybuserror.iMapette.providers.Spots;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

/**
 * Represents the service which is reponsible to acquire and save the locations locally.
 * 
 * @author Fernando Alexandre
 *
 */
public class TrackingService extends Service {

	/**
	 * Tag the represents this service.
	 */
	public final static String TAG = TrackingService.class.getName();

	/**
	 * Timer that executed location acquisition.
	 */
	private Timer t;

	/**
	 * Keeps track is GPS is available.
	 */
	private boolean isGPSAvailable;

	/**
	 * Current best GPS location, in a iteration. (set to null after iteration)
	 */
	private Location currentGPSLocation;

	/**
	 * Current best Network location, in a iteration. (set to null after iteration)
	 */
	private Location currentNetworkLocation;

	/**
	 * Previous best location to identify if the device moved.
	 */
	private Location previous;

	/**
	 * Handles the locations directly acquired from the sensors.
	 */
	private LocListener mLocationListener;

	/**
	 * Handles the timed tasks.
	 */
	private Handler handler = new Handler();

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();

		isGPSAvailable = true;

		mLocationListener = new LocListener();
		currentGPSLocation = null;
		currentNetworkLocation = null;

		startSampling();

		Log.i(TAG, "Service created");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		stopSampling();
		unregisterLocationListener();

		Log.i(TAG, "Service stopped");

		super.onDestroy();
	}

	/**
	 * Starts location Sampling.
	 */
	private void startSampling() {
		t = new Timer();

		t.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				handler.post(new LocationHandlingTask(getBaseContext()));				
			}

		}, 0, 30*1000); // 300 seconds fixed rate		
	}

	/**
	 * Stops sampling.
	 */
	private void stopSampling() {
		t.cancel();
	}

	/**
	 * Obtains the LocationManager from system services.
	 * 
	 * @return
	 * 		The LocationManager.
	 */
	private LocationManager getLocationManager() {
		return (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
	}

	/**
	 * Unregisters the location listener.
	 */
	public void unregisterLocationListener() {
		getLocationManager().removeUpdates(mLocationListener);
	}

	/**
	 * Handles the location acquiring.
	 * 
	 * @author Fernando Alexandre
	 *
	 */
	class LocListener implements LocationListener {
		@Override
		public void onLocationChanged(Location location) {
			// Network Location
			if(location.getProvider().equals(LocationManager.NETWORK_PROVIDER)) {
				if(isBetterLocation(location, currentNetworkLocation))
					currentNetworkLocation = location;
				// GPS Location
			} else {
				if(isBetterLocation(location, currentGPSLocation))
					currentGPSLocation = location;
			}
		}

		@Override
		public void onProviderDisabled(String provider) {
			if(provider.equals(LocationManager.GPS_PROVIDER))
				isGPSAvailable = false;
		}

		@Override
		public void onProviderEnabled(String provider) {
			if(provider.equals(LocationManager.GPS_PROVIDER))
				isGPSAvailable = true;
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			if(provider.equals(LocationManager.GPS_PROVIDER)) {
				switch(status) {
				case LocationProvider.AVAILABLE:
					isGPSAvailable = true;
					break;
				case LocationProvider.OUT_OF_SERVICE:
					isGPSAvailable = false;
					break;
				case LocationProvider.TEMPORARILY_UNAVAILABLE:
					isGPSAvailable = false;
					break;
				}

			}					
		}

	}

	/** Determines whether one Location reading is better than the current Location fix
	 * @param location  The new Location that you want to evaluate
	 * @param currentBestLocation  The current Location fix, to which you want to compare the new one
	 */
	protected boolean isBetterLocation(Location location, Location currentBestLocation) {
		if (currentBestLocation == null) {
			// A new location is always better than no location
			return true;
		}

		// Check whether the new location fix is more or less accurate
		int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());

		return accuracyDelta < 0;
	}

	/** 
	 * Determines whether one location is different than the other given a certain delta.
	 * @param location  The new Location that you want to evaluate
	 * @param SecondLocation  The reference location.
	 * @param delta The delta in meters that defines if a location is different than the other
	 */
	protected boolean isDifferentLocation(Location location, Location SecondLocation, float delta) {
		if (SecondLocation == null) {
			// A new location is always diferent than no location
			return true;
		}

		return SecondLocation.distanceTo(location) < delta;
	}

	/**
	 * Registers the available providers for locations and launches the correct handler task.
	 * 
	 * @author Fernando Alexandre
	 *
	 */
	class LocationHandlingTask implements Runnable {
		private Context mContext;

		public LocationHandlingTask(Context context) {
			mContext = context;
		}


		@Override
		public void run() {
			getLocationManager().requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, mLocationListener);
			Timer x = new Timer();

			if(isGPSAvailable) {
				getLocationManager().requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
			}
			
			x.schedule(new TimerTask() {

				@Override
				public void run() {
					unregisterLocationListener();

					// Save the current location and set null
					if(currentGPSLocation != null || currentNetworkLocation != null) {
						ContentValues values = new ContentValues();
						Location l;

						if(currentGPSLocation != null && isBetterLocation(currentGPSLocation, currentNetworkLocation)) {
							l = currentGPSLocation;
						} else {
							l = currentNetworkLocation;
						}

						boolean isDifferent = isDifferentLocation(l, previous, (float) 15.0);

						if(isDifferent) {

							values.put(Spots.LATD, l.getLatitude());
							values.put(Spots.LONGD, l.getLongitude());
							values.put(Spots.ACCURACY, l.getAccuracy());
							values.put(Spots.ALTITUDE, l.getAltitude());
							values.put(Spots.SPEED, l.getSpeed());
							values.put(Spots.TIMESTAMP, System.currentTimeMillis());


							getContentResolver().insert(Spots.CONTENT_URI, values);
							Log.i(TAG, "Saving a network/GPS spot.");

							mContext.sendBroadcast(new Intent(IMapette.REFRESH_GUI));
							mContext.sendBroadcast(new Intent(TransmissionService.DIRTY_INTENT));

							previous = l;

							currentGPSLocation = null;
							currentNetworkLocation = null;
						}
					}			
				}
			}, 10*1000); // 40 Seconds
		}

	}
}
