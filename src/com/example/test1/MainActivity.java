package com.example.test1;

import com.example.test1.DataProcessor.SpikeInfo;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity {

	MainActivity	_this = null;
	Spinner			_spinner = null;
	ToggleButton	_toggleButton = null;
	ScrollView		_scrollView1 = null;
	TextView		_textView1 = null;
	ScrollView		_scrollView2 = null;
	TextView		_textView2 = null;
	EditText		_editText1 = null;
	GraphView		_graphView = null;
	
	Preferences		_preferences = null;
	
	Logger			_logger = null;

    /** Messenger for communicating with service. */
    Messenger mService = null;
    /** Flag indicating whether we have called bind on the service. */
    boolean mIsBound;
    /** Some text view we are using to show state information. */
    TextView mCallbackText;
    
    /**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        	Logger.logWithFile("MainActivity2.handleMessage(): " + msg);
            switch (msg.what) {
                case SensorService.MSG_SEND_SENSOR_DATA: {
                	SensorData sensorData = (SensorData)msg.obj;
                	String logstr = String.format("%s %d [%10.3f]", 
                    		sensorData.id(), sensorData.time(), sensorData.data());
                	Logger.log(logstr);
                	if(_toggleButton.isChecked()) {
                		outputText(1, logstr);
                	}
                	if(_graphView != null) {
                		_graphView.setSensorData(sensorData);
                	}
                	break;
                }
                case SensorService.MSG_SEND_SPIKE_DATA: {
                	SpikeInfo spikeInfo = (SpikeInfo)msg.obj;
                	String logstr = String.format("[%d] [%d, %.3f] (%d:%.3f, %d:%.3f, %d:%.3f)", 
            				spikeInfo.status, spikeInfo.length, spikeInfo.amp, 
            				spikeInfo.beginTime, spikeInfo.beginValue,
            				spikeInfo.maxTime - spikeInfo.beginTime, spikeInfo.maxValue,
            				spikeInfo.endTime - spikeInfo.maxTime, spikeInfo.endValue);
                	Logger.log(logstr);
                	if(_toggleButton.isChecked()) {
                		outputText(2, logstr);
                	}
                	if(_graphView != null) {
                		_graphView.setSpikeInfo(spikeInfo);
                	}
                	break;	
                }
                case SensorService.MSG_SET_COUNT: {
                	_editText1.setText(String.format("%d", msg.arg1));
                	break;
                }
                case SensorService.MSG_SET_DATA_PROC_SETTING: {
                	if(_graphView != null) {
                		_graphView.setDataProcessSettings((DataProcessSettings)msg.obj);
                	}
                }
                default: {
                    super.handleMessage(msg);
                }
            }
        }
    }
    
    /**
     * Target we publish for clients to send messages to .
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    
    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
        	Logger.logWithFile("onServiceConnected()");
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            mService = new Messenger(service);

            // We want to monitor the service for as long as we are
            // connected to it.
            try {
            	//	Messengerをサービスに登録
                Message msg = Message.obtain(null, SensorService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
                //	現在の歩数を取得
                mService.send(Message.obtain(null, SensorService.MSG_GET_COUNT));
                //	データ処理の設定を取得
                mService.send(Message.obtain(null, SensorService.MSG_GET_DATA_PROC_SETTING));
            } 
            catch (Exception e) {
            	e.printStackTrace();
            	e.printStackTrace(Logger.getPrintStream());
                // In this case the service has crashed before we could even
                // do anything with it; we can count on soon being
                // disconnected (and then reconnected if it can be restarted)
                // so there is no need to do anything here.
            }
            
            // As part of the sample, tell the user what happened.
            Toast.makeText(getApplicationContext(), R.string.remote_service_connected, Toast.LENGTH_SHORT).show();
        }

        public void onServiceDisconnected(ComponentName className) {
        	Logger.logWithFile("onServiceDisconnected()");
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;

            // As part of the sample, tell the user what happened.
            Toast.makeText(getApplicationContext(), R.string.remote_service_disconnected, Toast.LENGTH_SHORT).show();
        }
    };
    
    void doBindService() {
    	if(mIsBound == false) {
	        // Establish a connection with the service.  We use an explicit
	        // class name because there is no reason to be able to let other
	        // applications replace our component.
	        bindService(new Intent(this, SensorService.class), mConnection, Context.BIND_AUTO_CREATE);
	        mIsBound = true;
    	}
    }
    
    void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with
            // it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, SensorService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                    // There is nothing special we need to do if the service has crashed.
                }
            }
            
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }
    
    /**
     * サービスにメッセージを送信する
     * @param msg
     */
    void sendMessageToService(Message msg) {
    	try {
    		if(mService != null) {
    			mService.send(msg);
    		}
    	}
    	catch(RemoteException e) {
    		e.printStackTrace();
    	}
    }
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.logWithFile("MainActivity.onCreate()");
        setContentView(R.layout.activity_main);
        
        _this = this;
        _preferences = new Preferences(this);
        
        _scrollView1= (ScrollView)findViewById(R.id.scrollView1);
        _textView1 = (TextView)findViewById(R.id.textView1);
        _scrollView2= (ScrollView)findViewById(R.id.scrollView2);
        _textView2 = (TextView)findViewById(R.id.textView2);
        _editText1 = (EditText)findViewById(R.id.editText1);
        
        _editText1.setText(String.format("%d", 0));
        
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        adapter.add("SENSOR_DELAY_FASTEST");
        adapter.add("SENSOR_DELAY_GAME");
        adapter.add("SENSOR_DELAY_UI");
        adapter.add("SENSOR_DELAY_NORMAL");
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        _spinner = (Spinner)findViewById(R.id.spinner1);
        _spinner.setAdapter(adapter);
        _spinner.setSelection(_preferences.getSensorRate());	// select SENSOR_DELAY_NORMAL
        _spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
        	 @Override
             public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        		 Logger.logWithFile("select sensor rate: " + position);
        		 sendMessageToService(Message.obtain(null, SensorService.MSG_SET_SENSOR_RATE, position, 0));
        	 }
        	 
        	 @Override
             public void onNothingSelected(AdapterView<?> parent) {
        	 }
		});
        
        _toggleButton = (ToggleButton)findViewById(R.id.toggleButton1);
        _toggleButton.setChecked(true);
        
        Button button1 = (Button)findViewById(R.id.button1);
        button1.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startService(new Intent(_this, SensorService.class));
				doBindService();
			}
		});
        
        Button button2 = (Button)findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				doUnbindService();
				stopService(new Intent(_this, SensorService.class));
			}
		});
        
        Button button3 = (Button)findViewById(R.id.button3);
        button3.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				clearText();
			}
		});
        
        _graphView = new GraphView(this);
        SurfaceView surfaceView1 = (SurfaceView)findViewById(R.id.surfaceView1);
        surfaceView1.getHolder().addCallback(_graphView);
    }

	@Override
	protected void onResume() {
		Logger.logWithFile("MainActivity.onResume()");
		doBindService();
		super.onResume();
	}
	
	@Override
	protected void onRestart() {
		Logger.logWithFile("MainActivity.onRestart()");
		super.onRestart();
	}

	@Override
	protected void onStart() {
		Logger.logWithFile("MainActivity.onStart()");
		super.onStart();
	}
	
	@Override
	protected void onPause() {
		Logger.logWithFile("MainActivity.onPause()");
		doUnbindService();
		super.onPause();
	}

	@Override
	protected void onStop() {
		Logger.logWithFile("MainActivity.onStop()");
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		Logger.logWithFile("MainActivity.onDestroy()");
		super.onDestroy();
	}
	
	@Override
	public void onLowMemory() {
		Logger.logWithFile("MainActivity.onDestroy()");
		super.onLowMemory();
	}
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    private void outputText(int id, String msg) {
    	switch(id) {
    	case 1:
    		_textView1.append(msg + "\n");
    		_scrollView1.fullScroll(View.FOCUS_DOWN);
    		break;
    	case 2:
    		_textView2.append(msg + "\n");
    		_scrollView2.fullScroll(View.FOCUS_DOWN);
    		break;
    	}
    }
    
    private void clearText() {
    	_textView1.setText("");
    	_textView2.setText("");
    }
}
