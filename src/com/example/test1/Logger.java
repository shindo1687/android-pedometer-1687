package com.example.test1;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Calendar;

import android.os.Environment;
import android.util.Log;

public class Logger {
	
	static private final String TAG = "##";
	
	static public void log(String msg) {
    	Log.v(TAG, msg);
    }
	 
   static public void logWithFile(String msg) {
    	try {
    		Log.v(TAG, msg);
    		msg = "[" + getTimeStamp() + "]: " + msg + "\n";
	    	FileOutputStream output = new FileOutputStream(getLogFile(), true);
	    	output.write(msg.getBytes());
	    	output.close();
    	}
    	catch(Exception e) {
    		e.printStackTrace();
    	}
    }
   
   	static public PrintStream getPrintStream() {
   		try {
   			return new PrintStream(new FileOutputStream(getLogFile(), true));
   		}
   		catch(Exception e) {
   			e.printStackTrace();
   			return null;
   		}
   	}
   	
   	static private String getLogFile() {
   		return Environment.getExternalStorageDirectory() + "/debug.log";
   	}
    
    static private String getTimeStamp() {
    	Calendar c = Calendar.getInstance() ;
    	c.setTimeInMillis(System.currentTimeMillis());
    	return String.format("%4d/%02d/%02d %02d:%02d:%02d %03d", 
    			c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH),
    			c.get(Calendar.HOUR), c.get(Calendar.MINUTE), c.get(Calendar.SECOND), c.get(Calendar.MILLISECOND));
    }
	
}
