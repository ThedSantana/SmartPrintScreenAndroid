package com.smartprintscreen;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

public class Shared {
	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	public static boolean copyToClipboard(Context context, String text) {
        try {
            int sdk = android.os.Build.VERSION.SDK_INT;
            if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
				android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context
                        .getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setText(text);
    			Log.i("copyToClipboard", "oldSDK");
            } else {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context
                        .getSystemService(Context.CLIPBOARD_SERVICE);
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
	public static void showToast(Context context, String msg) {
		Toast toast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
		TextView v = (TextView)toast.getView().findViewById(android.R.id.message);
		if(v != null)
			v.setGravity(Gravity.CENTER);
		toast.show();
	}
	public static void showAlert(Context context, String title, String msg,
			DialogInterface.OnClickListener ok, DialogInterface.OnClickListener cancel) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
		if (title != null)
			alertDialogBuilder.setTitle(title);
		alertDialogBuilder
			.setMessage(msg)
			.setPositiveButton(resStr(context, R.string.ok_alert), ok)
			.setNegativeButton(resStr(context, R.string.cancel_alert), cancel);
		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
	}
	public static String resStr(Context context, int id) {
		return context.getResources().getString(id);
	}
}