package com.smartprintscreen;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.FileObserver;
import android.os.IBinder;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

public class SmartPrintScreen extends Service {
	private String TAG = "SmartPrintScreen";
	private String ClientId = "a964b399e5b6022";
	private static FileObserver fileObserver;
	String screenshotsFolder;
	private Service service;
	public SmartPrintScreen() {
		super();
		Log.i(TAG, "SmartPrintScreen()");
		service = this;
	}
	@Override
	public void onCreate() {
		super.onCreate();
		service.setTheme(android.R.style.Theme_Holo);
		Log.i(TAG, "onCreate");
	}
    @Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "onStartCommand");
    	service = this;
		screenshotsFolder = Environment.getExternalStorageDirectory() + File.separator + "Screenshots";
	    Log.d(TAG, screenshotsFolder);
	    fileObserver = new FileObserver(screenshotsFolder) {
	        @Override
	        public void onEvent(int event, String path) {
	            if (event == FileObserver.CLOSE_WRITE) {
	            	String screenshotFile = screenshotsFolder + File.separator + path;
		            Log.d(TAG, screenshotFile);
		            BitmapFactory.Options opt = new BitmapFactory.Options();
		            opt.inDither = true;
		            opt.inPreferredConfig = Bitmap.Config.ARGB_8888;
	            	Bitmap bitmap = BitmapFactory.decodeFile(screenshotFile, opt);
		            new uploadToImgurTask().execute(bitmap);
	            }
	        }
	    };
	    fileObserver.startWatching();
		return super.onStartCommand(intent, flags, startId);
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
        		if (copyToClipboard(service, url)) {
            		Log.d(TAG, "Screenshot URL copied to clipboard: " + url);
            		Toast toast = Toast.makeText(service, "Screenshot URL copied to clipboard:\n" + url, Toast.LENGTH_SHORT);
            		TextView v = (TextView)toast.getView().findViewById(android.R.id.message);
            		if(v != null)
            			v.setGravity(Gravity.CENTER);
            		toast.show();
        		} else {
            		Log.d(TAG, "Screenshot URL failed to get copied: " + url);
        		}
        	//if wi-fi is enabled then we actually failed
        	} else if (wifi.isWifiEnabled()) {
        		Log.w(TAG, "Failed to read a file from " + screenshotsFolder);
        	}
	        super.onPostExecute(url);
	    }
	}
	@SuppressLint("NewApi")
	public boolean copyToClipboard(Context context, String text) {
        try {
            int sdk = android.os.Build.VERSION.SDK_INT;
            if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
                android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context
                        .getSystemService(context.CLIPBOARD_SERVICE);
                clipboard.setText(text);
    			Log.i("copyToClipboard", "oldSDK");
            } else {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context
                        .getSystemService(context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData
                        .newPlainText(text + "copied to clipboard", text);
                clipboard.setPrimaryClip(clip);
    			Log.i("copyToClipboard", "newSDK");
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}