package com.example.test1;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends Activity implements SensorEventListener {

	MainActivity	_this = null;
	Spinner			_spinner = null;
	ToggleButton	_toggleButton = null;
	ScrollView		_scrollView = null;
	TextView		_textView = null;
	
	SensorManager 	_sensorManager = null;
	Sensor 			_sensor = null;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        _this = this;
        
        _scrollView = (ScrollView)findViewById(R.id.scrollView1);
        _textView = (TextView)findViewById(R.id.textView1);
        
        _sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        _sensor = _sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        adapter.add("SENSOR_DELAY_FASTEST");
        adapter.add("SENSOR_DELAY_GAME");
        adapter.add("SENSOR_DELAY_UI");
        adapter.add("SENSOR_DELAY_NORMAL");
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        _spinner = (Spinner)findViewById(R.id.spinner1);
        _spinner.setAdapter(adapter);
        _spinner.setSelection(3);	// select SENSOR_DELAY_NORMAL
        
        _toggleButton = (ToggleButton)findViewById(R.id.toggleButton1);
        _toggleButton.setChecked(true);
        
        Button button1 = (Button)findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				try {
					int rate = getSensorRate();
					outputText("rate = " + rate);
					_sensorManager.registerListener(_this, _sensor, rate);
				}
				catch(Exception e) {
					e.printStackTrace();
				}
			}
		});
        
        Button button2 = (Button)findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				_sensorManager.unregisterListener(_this);
			}
		});
        
        Button button3 = (Button)findViewById(R.id.button3);
        button3.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				clearText();
			}
		});
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    	outputText("onAccuracyChanged: " + accuracy);
    }

    public void onSensorChanged(SensorEvent event) {
    	float value1 = event.values[0];
    	float value2 = event.values[1];
    	float value3 = event.values[2];
    	double distance = Math.sqrt(Math.pow(value1, 2) + Math.pow(value2, 2) + Math.pow(value3, 2));
    	
    	String msg = String.format("[%10.3f] % 8.3f, % 8.3f, % 8.3f", distance, value1, value2, value3);
    	if(_toggleButton.isChecked()) {
    		outputText(msg);
    	}
    }
    
    private int getSensorRate() throws Exception {
    	String selectedRate = (String)_spinner.getSelectedItem();
    	if(selectedRate.equals("SENSOR_DELAY_FASTEST")) {
    		return SensorManager.SENSOR_DELAY_FASTEST;
    	}
    	else if(selectedRate.equals("SENSOR_DELAY_GAME")) {
    		return SensorManager.SENSOR_DELAY_GAME;
    	}
    	else if(selectedRate.equals("SENSOR_DELAY_UI")) {
    		return SensorManager.SENSOR_DELAY_UI;
    	}
    	else if(selectedRate.equals("SENSOR_DELAY_NORMAL")) {
    		return SensorManager.SENSOR_DELAY_NORMAL;
    	}
    	throw new Exception("invalid value selected: " + selectedRate);
    }
    
    private void outputText(String msg) {
    	_textView.append(msg + "\n");
		_scrollView.fullScroll(View.FOCUS_DOWN);
		Log.v("##", msg);
    }
    
    private void clearText() {
    	_textView.setText("");
    }
}
