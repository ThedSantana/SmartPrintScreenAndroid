package com.smartprintscreen;

import java.io.IOException;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class Settings extends Activity {
	private String TAG = "SmartPrintScreen Settings";
	CheckBox startOnBoot, removeShots;
	
	private static Activity activity;
	
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		activity = this;
		activity.setTheme(android.R.style.Theme_Holo);

		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.settings);

		startOnBoot = (CheckBox)findViewById(R.id.startOnBoot);
		startOnBoot.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				saveParameters();
			}
		});
		removeShots = (CheckBox)findViewById(R.id.removeShots);
		removeShots.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				saveParameters();
				stopService(new Intent(activity, SmartPrintScreen.class));
				new Thread() {
					@Override
					public void run() {
						Intent service = new Intent(activity, SmartPrintScreen.class);
						Log.d(TAG, "restarting service com.smartprintscreen.SmartPrintScreen");
						startService(service);
					}
				}.start();
			}
		});
		
		loadParameters();
	}
	@Override
	public void onDestroy() {
		saveParameters();
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
		super.onDestroy();
	}

	private void saveParameters() {
		String[] data = {String.valueOf(startOnBoot.isChecked()),
						String.valueOf(removeShots.isChecked())};
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
		if (parameters != null && parameters.length > 1)
			removeShots.setChecked(Boolean.parseBoolean(parameters[1]));
	}
}