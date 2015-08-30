package com.smartprintscreen;

import java.io.IOException;
import java.util.ArrayList;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.FileObserver;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity {
	private String TAG = "SmartPrintScreenUI";
	CheckBox startOnBoot;
	Button clearList;
	
	ListView screenshotsURLs;
	private ArrayAdapter<String> adapter;
	private ArrayList<String> arrayList;

	private static FileObserver fileObserver;
	
	private static Activity activity;
	
	private void saveLog() {
		try {
        	String[] command = new String[] {"logcat", "-v", "threadtime", "-f", getExternalFilesDir(null).toString()+"/SmartPrintScreen.log"};
            Runtime.getRuntime().exec(command);
        } catch (IOException e) {
        	Log.e(TAG + " saveLog", "getCurrentProcessLog failed", e);
        }
	}
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		saveLog();
		activity = this;
		activity.setTheme(android.R.style.Theme_Holo);

		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);
		startOnBoot = (CheckBox)findViewById(R.id.startOnBoot);
		startOnBoot.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				saveParameters();
			}
		});

		if (!isServiceRunning(SmartPrintScreen.class)) {
			new Thread() {
				@Override
				public void run() {
					Intent service = new Intent(activity, SmartPrintScreen.class);
					Log.d(TAG, "starting service com.smartprintscreen.SmartPrintScreen");
					startService(service);
				}
			}.start();
		}
		
		loadParameters();
		
		clearList = (Button)findViewById(R.id.clearList);
		clearList.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						DialogInterface.OnClickListener ok = new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								arrayList.clear();
								String[] data = {""};
								try {
									SaveLoadData.saveData(activity, data, "screenshotsURLs", false);
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						};
						Shared.showAlert(activity, null, Shared.resStr(activity, R.string.clear_list_alert), ok, null);
					}
			   	});
			}
		});
		
		screenshotsURLs = (ListView)findViewById(R.id.screenshotsURLs);
		screenshotsURLs.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				String url = ((TextView)view).getText().toString();
				if (Shared.copyToClipboard(activity, url))
            		Shared.showToast(activity, Shared.resStr(activity, R.string.copied_to_clipboard_toast) + ":\n" + url);
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				startActivity(browserIntent);
			}
		});
		screenshotsURLs.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				final int pos = position;
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						DialogInterface.OnClickListener ok = new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								arrayList.remove(pos);
								ArrayList<String> arrayListNew = new ArrayList<String>();
								for (int i = arrayList.size()-1; i >= 0; i--) {
									arrayListNew.add(arrayList.get(i));
								}
								String[] data = new String[arrayListNew.size()];
								data = arrayListNew.toArray(data);
								try {
									SaveLoadData.saveData(activity, data, "screenshotsURLs", false);
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						};
						Shared.showAlert(activity, arrayList.get(pos), Shared.resStr(activity, R.string.remove_url_alert), ok, null);
					}
			   	});
				return true;
			};
		});
		
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				reloadURLsList();
			}
	   	});
		
	    fileObserver = new FileObserver(this.getFilesDir().toString()) {
	        @Override
	        public void onEvent(int event, String path) {
	            if (event == FileObserver.CLOSE_WRITE && path.equals("screenshotsURLs")) {
	            	runOnUiThread(new Runnable() {
	            		@Override
	            		public void run() {
	            			reloadURLsList();
	            		}
	            	});
	            }
	        }
	    };
	    fileObserver.startWatching();
	}
	@Override
	public void onDestroy() {
//		stopService(service);
		saveParameters();
	    if (fileObserver != null)
	    	fileObserver.stopWatching();
		super.onDestroy();
	}
	
	private void saveParameters() {
		String[] data = {String.valueOf(startOnBoot.isChecked())};
		try {
			SaveLoadData.saveData(this, data, "parameters", false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private void loadParameters() {
		String[] parameters = null;
		try {
			parameters = SaveLoadData.loadData(this, "parameters");
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (parameters != null && parameters.length > 0)
			startOnBoot.setChecked(Boolean.parseBoolean(parameters[0]));
	}
	
	private void reloadURLsList() {
		String[] URLs = null;
		try {
			URLs = SaveLoadData.loadData(this, "screenshotsURLs");
		    arrayList = new ArrayList<String>();
			if (URLs != null && URLs.length > 0) {
				for (int i = URLs.length-1; i >= 0; i--) {
					String url = URLs[i];
					if (url != null && !url.equals(""))
						arrayList.add(url);
			    }
			}
		    adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, arrayList);
			screenshotsURLs.setAdapter(adapter);
	        adapter.notifyDataSetChanged();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private boolean isServiceRunning(Class<?> serviceClass) {
	    ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (serviceClass.getName().equals(service.service.getClassName())) {
				Log.d(TAG, "service " + serviceClass.getName() + " has been already running");
	            return true;
	        }
	    }
		Log.d(TAG, "service " + serviceClass.getName() + " is not yet started");
	    return false;
	}
}
