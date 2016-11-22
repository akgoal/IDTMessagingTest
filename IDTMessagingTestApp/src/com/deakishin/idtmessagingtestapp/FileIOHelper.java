package com.deakishin.idtmessagingtestapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.os.Environment;
import android.util.Log;

/**
 * Helper to write/read files. Uses the External Storage.
 */
public class FileIOHelper {
	private static final String TAG = "FileIOHelper";

	/* Application context. */
	private Context mContext;

	/* External Storage. */
	private File mExternalStorage;

	/**
	 * Constructor.
	 * 
	 * @param context
	 *            - app context.
	 */
	public FileIOHelper(Context context) {
		mContext = context.getApplicationContext();

		String folderName = mContext.getString(R.string.ext_storage_folder_name);

		mExternalStorage = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + folderName);
		if (!mExternalStorage.exists())
			mExternalStorage.mkdir();
	}

	/**
	 * Save data as a Bitmap image on the external storage.
	 * 
	 * @param data
	 *            - byte array to save
	 * @return filename of the stored image
	 */
	public String saveDataAsBitmap(byte[] data) {
		return writeExtBitmap(BitmapFactory.decodeByteArray(data, 0, data.length));
	}

	/**
	 * Writes Bitmap image data to the External Storage.
	 * 
	 * @param bitmap
	 *            - image to save.
	 * @return filename of the stored image.
	 */
	public String writeExtBitmap(Bitmap bitmap) {
		if (bitmap == null) {
			return null;
		}

		OutputStream stream = null;
		try {
			String filename = "" + new Date().getTime() + ".png";
			File file = new File(mExternalStorage.getAbsolutePath() + "/" + filename);
			stream = new FileOutputStream(file);
			bitmap.compress(CompressFormat.PNG, 100, stream);
			stream.flush();
			Log.d(TAG, "Bitmap is saved.");
			return filename;
		} catch (Exception e) {
			Log.e(TAG, "Error saving bitmap: " + e);
			return null;
		} finally {
			if (stream != null)
				try {
					stream.close();
				} catch (IOException e) {
				}
		}
	}

	/**
	 * Reads Bitmap image from the External Storage. To avoid OOM the image is
	 * scaled depending on width parameter.
	 * 
	 * @param filename
	 *            - name of the file.
	 * @param rotate
	 *            - should be image rotated 180 degrees
	 * @param width
	 *            - width of the region where the image will be displayed, it's
	 *            necessary for avoiding OOM
	 * @return bitmap or null if an error occurred.
	 */
	public Bitmap readExtBitmap(String filename, boolean rotate, int width) {
		InputStream is = null;
		try {
			is = readExtFile(filename);

			/* First just bounds are loaded to calculate scale. */
			BitmapFactory.Options opts = new BitmapFactory.Options();
			opts.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(is, null, opts);

			/* Now full scaled image is loaded. */
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
			is = readExtFile(filename);
			opts.inSampleSize = calculateInSampleSize(opts, width);
			opts.inJustDecodeBounds = false;
			Log.d(TAG, "inSampleSize = " + opts.inSampleSize);
			Bitmap bitmap = BitmapFactory.decodeStream(is, null, opts);

			Log.d(TAG, "Bitmap is read " + (bitmap == null ? "unsuccessfully" : "successfully"));

			if (rotate) {
				// Bitmap resBitmap = rotate(bitmap);
				Bitmap resBitmap = rotateManual(bitmap);

				if (bitmap != null) {
					bitmap.recycle();
					bitmap = null;
				}
				return resBitmap;
			} else {
				return bitmap;
			}
		} catch (IOException e) {
			Log.e(TAG, "Error reading bitmap: " + e);
			return null;
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
				}
			}
		}
	}

	/* Calculates inSampleSize to be able to load scaled image. */
	private int calculateInSampleSize(Options options, int reqWidth) {
		// Raw width of image
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (width > reqWidth && reqWidth > 0) {

			final int halfWidth = width / 2;

			/*
			 * Calculate the largest inSampleSize value that is a power of 2 and
			 * keeps width larger than the requested width.
			 */
			while ((halfWidth / inSampleSize) >= reqWidth) {
				inSampleSize *= 2;
			}
		}

		return inSampleSize;
	}

	/* Rotates given bitmap 180 degrees. */
	@SuppressWarnings("unused")
	private Bitmap rotate(Bitmap srcBitmap) {
		if (srcBitmap == null)
			return null;

		Matrix matrix = new Matrix();
		matrix.postRotate(180);
		return Bitmap.createBitmap(srcBitmap, 0, 0, srcBitmap.getWidth(), srcBitmap.getHeight(), matrix, true);
	}

	/* Rotates given bitmap 180 degrees switching pixels manually. */
	private Bitmap rotateManual(Bitmap srcBitmap) {
		if (srcBitmap == null)
			return null;

		int w = srcBitmap.getWidth();
		int h = srcBitmap.getHeight();
		int s = w * h;
		int[] dstPixels = new int[s];
		for (int i = 0; i < s; i++) {
			int ih = i / w;
			int iw = i - ih * w;
			dstPixels[i] = srcBitmap.getPixel(iw, h - ih - 1);
		}
		return Bitmap.createBitmap(dstPixels, w, h, srcBitmap.getConfig());
	}

	/*
	 * Reads file from the external storage.
	 */
	private InputStream readExtFile(String filename) throws IOException {
		return new FileInputStream(mExternalStorage.getAbsolutePath() + "/" + filename);
	}

	/**
	 * Writes data to a file on the external storage.
	 * 
	 * @param filename
	 *            - name of file to store data in.
	 * @param InputStream
	 *            - InputStream object that provides data.
	 * @throws IOException
	 *             if an error occurred.
	 */
	public void writeExtFile(String filename, InputStream input) throws IOException {
		OutputStream stream = null;
		try {
			File file = new File(mExternalStorage.getAbsolutePath() + "/" + filename);
			stream = new FileOutputStream(file);
			byte[] buffer = new byte[1024];
			int bytesRead;
			while ((bytesRead = input.read(buffer)) != -1) {
				stream.write(buffer, 0, bytesRead);
			}
		} catch (IOException e) {
			Log.e(TAG, "Error saving bitmap: " + e);
			throw e;
		} finally {
			if (stream != null)
				try {
					stream.close();
				} catch (IOException e) {
				}
		}
	}

}
