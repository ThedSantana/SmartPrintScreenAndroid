package com.smartprintscreen;

import java.io.IOException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
	private String TAG = "BootReceiver";
	@Override
	public void onReceive(Context context, Intent intent) {
		String[] parameters = null;
		boolean startOnBoot = false;
		try {
			parameters = SaveLoadData.loadData(context, "parameters");
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (parameters != null && parameters.length > 0)
			startOnBoot = Boolean.parseBoolean(parameters[0]);
		Log.d(TAG, "Are we able to start on startup? Answer: " + String.valueOf(startOnBoot));
		
		if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction()) && startOnBoot) {
			Log.d(TAG, "SmartPrintScreen boot startup");
			Intent pushIntent = new Intent(context, SmartPrintScreen.class);
			context.startService(pushIntent);
		} else {
			Log.d(TAG, "SmartPrintScreen won't be started on boot");
		}
	}
}