package com.smartprintscreen;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.FileObserver;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;

public class SmartPrintScreen extends Service {
	private String TAG = "SmartPrintScreen";
	private String ClientId = "a964b399e5b6022";
	
	private static String eStorage = Environment.getExternalStorageDirectory().toString();
	private static String sep = File.separator;
	private static FileObserver[] fileObserver;
	private static String[] screenshotsFolder = {
		eStorage + sep + "Screenshots",
		eStorage + sep + Environment.DIRECTORY_PICTURES + sep + "Screenshots"};
	
	private Service service;
	public SmartPrintScreen() {
		super();
		Log.i(TAG, "SmartPrintScreen()");
		service = this;
	}
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	public void onCreate() {
		super.onCreate();
		service.setTheme(android.R.style.Theme_Holo);
		Log.i(TAG, "onCreate");
	}
	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "onDestroy");
		//I hope it's not that bad
		new Thread() {
			@Override
			public void run() {
				Intent in = new Intent(service, SmartPrintScreen.class);
				Log.d(TAG, "restarting service com.smartprintscreen.SmartPrintScreen");
				startService(in);
			}
		}.start();
	}
    @Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "onStartCommand");
    	service = this;
    	fileObserver = new FileObserver[screenshotsFolder.length];
    	for (int i = 0; i < screenshotsFolder.length; i++) {
    		final int j = i;
		    if (!(new File(screenshotsFolder[i])).exists()) {
			    Log.d(TAG, screenshotsFolder[i] + " missing");
		    	continue;
		    }
		    Log.d(TAG, screenshotsFolder[i]);
		    fileObserver[i] = new FileObserver(screenshotsFolder[i]) {
		        @Override
		        public void onEvent(int event, String path) {
		            if (event == FileObserver.CLOSE_WRITE) {
		            	String screenshotFile = screenshotsFolder[j] + File.separator + path;
		            	if (!(new File(screenshotFile)).exists()) {
		    			    Log.e(TAG, screenshotFile + " not found");
		    		    	return;
		    		    }
			            Log.d(TAG, screenshotFile);
			            BitmapFactory.Options opt = new BitmapFactory.Options();
			            opt.inDither = true;
			            opt.inPreferredConfig = Bitmap.Config.ARGB_8888;
		            	Bitmap bitmap = BitmapFactory.decodeFile(screenshotFile, opt);
		            	if (bitmap != null)
		            		new uploadToImgurTask().execute(bitmap);
		            }
		        }
		    };
		    fileObserver[i].startWatching();
    	}
//	    startForeground(17, null);
		return START_STICKY;
	}
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	class uploadToImgurTask extends AsyncTask<Bitmap, Void, String> {
		@Override
	    protected String doInBackground(Bitmap... params) {
	    	try {
				Log.d("getUploadedShotURL", "start");
				ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
				params[0].compress(Bitmap.CompressFormat.PNG, 100, byteArray); // Not sure whether this should be jpeg or png, try both and see which works best
				URL url = new URL("https://api.imgur.com/3/image");
			    byte[] byteImage = byteArray.toByteArray();
			    String dataImage = Base64.encodeToString(byteImage, Base64.DEFAULT);
			    String data = URLEncoder.encode("image", "UTF-8") + "=" + URLEncoder.encode(dataImage, "UTF-8");
				
				HttpURLConnection conn = (HttpURLConnection)url.openConnection();
			    conn.setDoOutput(true);
			    conn.setDoInput(true);
			    conn.setRequestMethod("POST");
			    conn.setRequestProperty("Authorization", "Client-ID " + ClientId);
			    
			    conn.connect();
			    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
			    wr.write(data);
			    wr.flush();
			    wr.close();

				InputStream is;
				int response = conn.getResponseCode();
				if (response != HttpURLConnection.HTTP_OK) {
					Log.d("getUploadedShotURL", "bad https response: " + response);
				    is = conn.getErrorStream();
				} else {
				    is = conn.getInputStream();
				}
				
			    BufferedReader rd = new BufferedReader(new InputStreamReader(is));

			    StringBuilder stb = new StringBuilder();
			    String line;
			    while ((line = rd.readLine()) != null) {
			        stb.append(line);
			    }
			    String result = stb.toString();
				Log.d("getUploadedShotURL", "result: " + result);
				if (response != HttpURLConnection.HTTP_OK)
					return null;
			    
			    Pattern reg = Pattern.compile("link\":\"(.*?)\"");
				Log.d("getUploadedShotURL", "reg: " + reg);
				Matcher match = reg.matcher(result);
				Log.d("getUploadedShotURL", "match: " + match);
				Log.d("getUploadedShotURL", "end");
				//our image url
				if (match.find())
					return match.group(0).replace("link\":\"", "").replace("\"", "").replace("\\/", "/");
			} catch (Exception e) {
				Log.e("getUploadedShotURL", e.getMessage());
				e.printStackTrace();
			}
			return null;
	    }
	    @Override
	    protected void onPostExecute(String url) {
	    	WifiManager wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        	if (url != null) {
        		if (Shared.copyToClipboard(service, url)) {
            		Log.d(TAG, "Screenshot URL copied to clipboard: " + url);
            		Shared.showToast(service, Shared.resStr(service, R.string.copied_to_clipboard_toast) + ":\n" + url);
            		
            		String[] data = {url};
            		try {
						SaveLoadData.saveData(service, data, "screenshotsURLs", true);
					} catch (IOException e) {
						e.printStackTrace();
					}
        		} else {
            		Log.d(TAG, "Screenshot URL failed to get copied: " + url);
        		}
        	//if wi-fi is enabled then we actually failed
        	} else if (wifi.isWifiEnabled()) {
        		Log.w(TAG, "Failed to upload file");
        	}
	        super.onPostExecute(url);
	    }
	}
}