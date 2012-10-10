package com.example.test1;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.SensorManager;

public class Preferences {
	
	private SharedPreferences _sharedPref = null;
	private SharedPreferences.Editor _sharedPrefEditor = null;
	
	//	設定ファイル名
	private final String PREF_FILE_NAME = "preference";
	
	//	センサーの感度
	public final String SENSOR_RATE = "sensor_rate";
	
	public Preferences(Context context) {
		_sharedPref = context.getSharedPreferences(PREF_FILE_NAME, 0);
		_sharedPrefEditor = _sharedPref.edit();
	}
	
	public int getSensorRate() {
		return _sharedPref.getInt(SENSOR_RATE, SensorManager.SENSOR_DELAY_NORMAL);
	}
	public void setSensorRate(int rate) {
		_sharedPrefEditor.putInt(SENSOR_RATE, rate);
		_sharedPrefEditor.commit();
	}
}
