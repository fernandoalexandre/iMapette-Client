package com.almightybuserror.iMapette;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;

import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.OverlayItem;

/**
 * Represents a Overlay with spots to be used on the Map.
 * 
 * @author Fernando Alexandre
 *
 */
public class SpotItemizedOverlay extends ItemizedOverlay<OverlayItem> {

	/**
	 * List of spots to be shown.
	 */
	private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
	
	/**
	 * Context in which this object was created.
	 */
	private Context mContext;

	/**
	 * Creates a new overlay that will contain spots.
	 * 
	 * @param defaultMarker
	 * 		Image which represents a Spot.
	 * @param mContext
	 * 		Context in which to create the overlay.
	 */
	public SpotItemizedOverlay(Drawable defaultMarker, Context mContext) {
		super(boundCenterBottom(defaultMarker));
		this.mContext = mContext;
		
		populate();
	}

	/**
	 * Adds a spot to the overlay.
	 * 
	 * @param item
	 * 		Spot to be added.
	 */
	public void addOverlay(OverlayItem item) {
		mOverlays.add(item);
		populate();
	}

	@Override
	protected OverlayItem createItem(int i) {
		return mOverlays.get(i);
	}

	@Override
	public int size() {
		return mOverlays.size();
	}

	@Override
	protected boolean onTap(int index) {
		OverlayItem item = mOverlays.get(index);
		AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
		dialog.setTitle(item.getTitle());
		dialog.setMessage(item.getSnippet());
		dialog.show();
		return true;
	}

}
