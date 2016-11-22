package com.deakishin.idtmessagingtestapp;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;

/**
 * Main Activity of the app. Contains a field for an URL and a view to display
 * the rotated image.
 *
 */
public class MainActivity extends Activity {

	/* Request codes for child activities. */
	private static final int REQUEST_ASK_PERMISSIONS = 0;

	/* Keys for saving state on rotation. */
	private static final String KEY_FILENAME = "filename";

	/* Widgets. */
	private EditText mUrlEditText;
	private Button mDownloadButton;
	private ImageView mImageView;
	private ProgressBar mLoadingFormStorageProgressBar;

	/* Filename of the downloaded image. */
	private String mImgFilename;

	/* Broadcast receiver that receives the result of downloading. */
	private DownloadResultReceiver mDownloadResultReceiver;

	/* Helper for reading/writing files. */
	private FileIOHelper mFileIO;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/* Restoring data after rotation. */
		if (savedInstanceState != null) {
			mImgFilename = savedInstanceState.getString(KEY_FILENAME);
		}

		mFileIO = new FileIOHelper(this);

		setContentView(R.layout.activity_main);

		mUrlEditText = (EditText) findViewById(R.id.imageurl_editText);
		
		/* Debug URL. */
		mUrlEditText.setText("http://i.imgur.com/wHM9piA.png");

		mDownloadButton = (Button) findViewById(R.id.download_button);
		mDownloadButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String url = null;
				if (mUrlEditText.getText() != null) {
					url = mUrlEditText.getText().toString();
				}

				if (url != null && !url.equals("")) {
					startDownloading(url);
				}
			}
		});

		mImageView = (ImageView) findViewById(R.id.image_imageview);

		mLoadingFormStorageProgressBar = (ProgressBar) findViewById(R.id.loadingFromStorageProgressBar);

		checkPermissions();
	}

	/*
	 * Initiates downloading an image from the given @param url.
	 */
	private void startDownloading(String url) {
		enableWidgets(false);
		DownloadService.startDownloading(this, url);
	}

	@Override
	public void onResume() {
		super.onResume();

		updateWidgets();

		/* Registering a local broadcast receiver. */
		if (mDownloadResultReceiver == null)
			mDownloadResultReceiver = new DownloadResultReceiver();
		IntentFilter intentFilter = new IntentFilter(DownloadService.BROADCAST_RESULT_ACTION);
		LocalBroadcastManager bm = LocalBroadcastManager.getInstance(this);
		bm.registerReceiver(mDownloadResultReceiver, intentFilter);
	}

	@Override
	public void onPause() {
		super.onPause();

		/* Unregistering the broadcast receiver. */
		LocalBroadcastManager bm = LocalBroadcastManager.getInstance(this);
		bm.unregisterReceiver(mDownloadResultReceiver);
	}

	/*
	 * Updates widgets.
	 */
	private void updateWidgets() {
		enableWidgets(!DownloadService.isDownloading());
		updateImageView();
	}

	/*
	 * Enables or disables widgets depending on @param enable.
	 */
	private void enableWidgets(boolean enable) {
		mDownloadButton.setEnabled(enable);
		int textResId;
		if (enable)
			textResId = R.string.download;
		else
			textResId = R.string.downloading;
		mDownloadButton.setText(textResId);

		/* Hiding keyboard. */
		if (!enable) {
			View view = getCurrentFocus();
			if (view != null) {
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
			}
		}
	}

	/* Object that loads image from storage and sets it to ImageView. */
	private LoadingImageFromMemoryTask mLoadingImageFromMemoryTask;

	/*
	 * Updates display of the image.
	 */
	private void updateImageView() {
		if (mImgFilename == null) {
			return;
		}

		/*
		 * To load an image from storage in the optimal way and display it on
		 * the ImageView, we need to know the width of the view. That's why we
		 * wait till it draws itself and then update it.
		 */
		mImageView.post(new Runnable() {
			@Override
			public void run() {
				if (mLoadingImageFromMemoryTask != null)
					mLoadingImageFromMemoryTask.cancel(false);

				mLoadingImageFromMemoryTask = new LoadingImageFromMemoryTask(mImageView, mImgFilename);
				mLoadingImageFromMemoryTask.execute();
			}
		});
	}

	/*
	 * AsyncTask that loads image from memory in a background thread and sets it
	 * to ImageView. In case of huge images the loading can take some time and
	 * block the UI thread - that's why it is done in background.
	 */
	private class LoadingImageFromMemoryTask extends AsyncTask<Void, Void, Bitmap> {

		/* Widget that's has to be updated once the image is loaded. */
		private ImageView mImageViewToUpdate;
		/* Name of the file in which the image is stored. */
		private String mFilename;

		LoadingImageFromMemoryTask(ImageView imageView, String filename) {
			mImageViewToUpdate = imageView;
			mFilename = filename;
		}

		@Override
		protected void onPreExecute() {
			mLoadingFormStorageProgressBar.setVisibility(View.VISIBLE);
		}

		@Override
		protected Bitmap doInBackground(Void... arg0) {
			return mFileIO.readExtBitmap(mFilename, true, mImageViewToUpdate.getWidth());
		}

		@Override
		protected void onPostExecute(Bitmap loadedBitmap) {
			if (!isCancelled()) {
				mLoadingFormStorageProgressBar.setVisibility(View.GONE);

				/*
				 * Before assigning new image to the ImageView we recycle
				 * previous one.
				 */
				if (mImageViewToUpdate.getDrawable() != null) {
					Bitmap oldBitmap = ((BitmapDrawable) mImageViewToUpdate.getDrawable()).getBitmap();
					mImageViewToUpdate.setImageDrawable(null);
					oldBitmap.recycle();
				}
				mImageViewToUpdate.setImageBitmap(loadedBitmap);
			}
		}

	}

	/*
	 * Performs premissions checking. Necessary for api level >=23 to get
	 * Runtime Permissions granted. We need permission to save the downloaded
	 * image on the external storage. To avoid crashing on older devices,
	 * classes from a support library are used.
	 */
	private void checkPermissions() {
		int hasWriteExtStoragePermission = ContextCompat.checkSelfPermission(this,
				Manifest.permission.WRITE_EXTERNAL_STORAGE);
		if (hasWriteExtStoragePermission != PackageManager.PERMISSION_GRANTED) {
			enableWidgets(false);
			if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
				showMessageOKCancel(new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						requestPermissions();
					}
				});
				return;
			}
			requestPermissions();
		}
	}

	/*
	 * Performs requesting necessary permission.
	 */
	private void requestPermissions() {
		ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
				REQUEST_ASK_PERMISSIONS);
	}

	/*
	 * Shows dialog asking for permission.
	 * 
	 * @param okListener - listener to click on OK button.
	 */
	private void showMessageOKCancel(DialogInterface.OnClickListener okListener) {
		new AlertDialog.Builder(MainActivity.this).setMessage(R.string.permission_ext_storage_rationale_text)
				.setPositiveButton(R.string.ok, okListener).setNegativeButton(R.string.cancel, null).create().show();
	}

	@TargetApi(23)
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		switch (requestCode) {
		case REQUEST_ASK_PERMISSIONS:
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				enableWidgets(true);
			}
		default:
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
				super.onRequestPermissionsResult(requestCode, permissions, grantResults);
			return;
		}
	}

	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);

		if (mImgFilename != null) {
			savedInstanceState.putString(KEY_FILENAME, mImgFilename);
		}
	}

	/*
	 * Shows message dialog that indicates that there was an error when loading
	 * the image.
	 */
	private void showErrorMessage() {
		new AlertDialog.Builder(MainActivity.this).setMessage(R.string.error_loading_image)
				.setPositiveButton(R.string.ok, null).create().show();
	}

	/*
	 * Broadcast receiver that receives the result of the image downloading as a
	 * filename of the stored downloaded image.
	 */
	private class DownloadResultReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(DownloadService.BROADCAST_RESULT_ACTION)) {
				String filename = null;
				if (intent.getExtras() != null) {
					filename = intent.getExtras().getString(DownloadService.EXTRA_FILENAME);
				}

				if (filename != null) {
					mImgFilename = filename;
					updateImageView();
				} else {
					showErrorMessage();
				}

				enableWidgets(true);
			}
		}
	}

}
