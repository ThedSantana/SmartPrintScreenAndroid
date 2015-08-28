package com.smartprintscreen;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Pattern;

import android.content.Context;

public class SaveLoadData {
	static private String lineSeparator = "\n";
	static public String []loadData(Context context, String from) throws IOException {
		String []ret = null;
		File f = new File(context.getFilesDir().toString() + File.separator + from);
		if (f.exists()) {
			int length = (int)f.length();
			byte []buffer = new byte[length];
			FileInputStream in = new FileInputStream(f);
			try {
			    in.read(buffer);
			} finally {
			    in.close();
			}
			String s = new String(buffer);
			ret = s.split(Pattern.quote(lineSeparator));
		}
		return ret;
	}
	static public void saveData(Context context, String []data, String to, boolean append) throws IOException {
		File f = new File(context.getFilesDir().toString() + File.separator + to);
		FileOutputStream stream = new FileOutputStream(f, append);
		try {
		    for (String p: data) {
		    	String s = p+lineSeparator;
		    	stream.write(s.getBytes());
		    }
		} finally {
		    stream.close();
		}
	}
}