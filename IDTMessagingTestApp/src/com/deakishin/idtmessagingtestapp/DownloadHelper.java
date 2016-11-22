package com.deakishin.idtmessagingtestapp;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;

/**
 * Helper for downloading data from a Url.
 */
public class DownloadHelper {

	/* Helper for reading/writing data. */
	private FileIOHelper mFileIOHelper;

	/**
	 * Constructor.
	 * 
	 * @param context
	 *            - app context.
	 */
	public DownloadHelper(Context context) {
		mFileIOHelper = new FileIOHelper(context);
	}

	/**
	 * Downloads data from the Internet and saves it on the External Storage.
	 * 
	 * @param urlString
	 *            - a Url to download image from.
	 * @param filename
	 *            - name of a file to store data in.
	 * @return false if an error occurred, otherwise true.
	 */
	public boolean downloadAndSaveData(String urlString, String filename) {
		HttpURLConnection connection = null;
		InputStream is = null;
		try {
			URL url = new URL(urlString);
			connection = (HttpURLConnection) url.openConnection();
			is = connection.getInputStream();
			if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
				return false;
			}

			mFileIOHelper.writeExtFile(filename, is);
			
			return true;
		} catch (IOException e) {
			return false;
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
}
