package com.deakishin.idtmessagingtestapp;

import java.util.Date;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * Service for downloading an image from a given url.
 */
public class DownloadService extends IntentService {

	private static final String TAG = "DownloadService";

	/** Key for Url to start Service. */
	private static final String EXTRA_URL = "extra_url";

	/** Key for filepath of the stored image in a Bundle to send. */
	public static final String EXTRA_FILENAME = "com.deakishin.idtmessagingtestapp.DownloadService.extra_filename";

	/** Constant String for a broadcast intent. */
	public static final String BROADCAST_RESULT_ACTION = "com.deakishin.idtmessagingtestapp.DownloadService.broadcast_result_action";

	/* Flag indicating whether the downloading is in process or not */
	private static boolean sDownloading = false;

	/* Image URL. */
	private String mUrl;

	public DownloadService() {
		this(TAG);
	}

	public DownloadService(String name) {
		super(name);
	}

	/**
	 * Initiates downloading.
	 * 
	 * @param url
	 *            - url to download from.
	 * @param context
	 *            - context of the application.
	 */
	public static void startDownloading(Context context, String url) {
		Log.d(TAG, "Received a command to download");
		Intent intent = new Intent(context, DownloadService.class);
		intent.putExtra(EXTRA_URL, url);
		context.startService(intent);
	}

	/**
	 * Getter for sDownloading flag.
	 * 
	 * @return true if downloading is in process, false otherwise.
	 */
	public static boolean isDownloading() {
		return sDownloading;
	}

	@Override
	protected void onHandleIntent(Intent intent) {

		Log.d(TAG, "Starting to download...");

		sDownloading = true;

		/* Checking network availability. */
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		@SuppressWarnings("deprecation")
		boolean isNetworkAvailable = cm.getBackgroundDataSetting() && cm.getActiveNetworkInfo() != null;
		if (!isNetworkAvailable) {
			Log.e(TAG, "Network is unavailable");
			sendResultBroadcast(null);
			return;
		}

		mUrl = null;
		Bundle extras = intent.getExtras();
		if (extras != null) {
			mUrl = intent.getExtras().getString(EXTRA_URL);
		}
		if (mUrl == null) {
			Log.e(TAG, "Invalid url");
			sendResultBroadcast(null);
			return;
		}

		DownloadHelper dlHelper = new DownloadHelper(this);
		String filename = "" + new Date().getTime() + ".png";
		boolean success = dlHelper.downloadAndSaveData(mUrl, filename);
		Log.d(TAG, "Bitmap is downloaded and saved " + (success ? "unsuccessfully" : "successfully"));
		sendResultBroadcast(success ? filename : null);
	}

	/*
	 * Sends the result of downloading as a filename of the stored image using
	 * local broadcasting.
	 */
	private void sendResultBroadcast(String filename) {
		Log.d(TAG, "Broadcasting the result. Filename: " + (filename == null ? "(null)" : filename));

		Intent intent = new Intent(BROADCAST_RESULT_ACTION);
		if (filename != null) {
			intent.putExtra(EXTRA_FILENAME, filename);
		}
		LocalBroadcastManager bm = LocalBroadcastManager.getInstance(this);
		bm.sendBroadcast(intent);

		sDownloading = false;
	}

}
