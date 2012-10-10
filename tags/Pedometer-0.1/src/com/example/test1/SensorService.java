package com.example.test1;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

public class SensorService extends Service {
	
	SensorService	_this = null;
	NotificationManager	_notificationManager = null;
	
	ClientMessenger	_clientMessenger = new ClientMessenger();
    
    SensorManager 	_sensorManager = null;
	Sensor 			_sensor = null;
	int				_sensorRate = SensorManager.SENSOR_DELAY_NORMAL;
	SensorEventListenerImpl _sensorEventListener = new SensorEventListenerImpl(); 
	boolean			_registListener = false;
	
	int				_counter = 0;
	int				_startId = 0;
	long			_startTime = 0; 	//	計測開始時刻
	
	Preferences		_preferences = null;
	DataProcessor	_dataProcessor = null;
	BlockingQueue<SensorData>	_queue = null;
	Thread			_processorThread = null;
    
    /**
     * Command to the service to register a client, receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client where callbacks should be sent.
     */
    static final int MSG_REGISTER_CLIENT = 1;
    
    /**
     * Command to the service to unregister a client, ot stop receiving callbacks
     * from the service.  The Message's replyTo field must be a Messenger of
     * the client as previously given with MSG_REGISTER_CLIENT.
     */
    static final int MSG_UNREGISTER_CLIENT = 2;
    
    static final int MSG_SET_SENSOR_RATE = 6;
    
    static final int MSG_SEND_SENSOR_DATA = 10;
    static final int MSG_SEND_SPIKE_DATA = 11;
    
    //	クライアントに現在の歩数を送信
    static final int MSG_SET_COUNT = 20;
    //	サーバに現在の歩数を送信するよう要求
    static final int MSG_GET_COUNT = 21;
    
    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
        	Logger.logWithFile("SensorService.handleMessage(): " + msg);
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                	_clientMessenger.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                	_clientMessenger.remove(msg.replyTo);
                    break;
                case MSG_SET_SENSOR_RATE:
                	//	センサーの感度を変更する
                	Logger.logWithFile("MSG_SET_SENSOR_RATE: " + msg.arg1);
                	_preferences.setSensorRate(msg.arg1);
                	//	センサーのリスナーが登録されている場合は再登録する
                	if(_registListener) {
                		registSensorListener();
                	}
                	break;
                case MSG_GET_COUNT:
                	int count = _dataProcessor.getCount();
                	_clientMessenger.sendMessage(Message.obtain(null, MSG_SET_COUNT, count, 0));
                	break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
    
    /**
     * Target we publish for clients to send messages to IncomingHandler.
     */
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    
    @Override
    public void onCreate() {
        Logger.logWithFile("SensorService.onCreate()");
        _this = this;
        _preferences = new Preferences(this);
        _dataProcessor = new DataProcessor();
        _dataProcessor.setClientMessenger(_clientMessenger);
        _queue = _dataProcessor.getQueue();
        _processorThread = new Thread(_dataProcessor);
        _notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        
        _sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        _sensor = _sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	Logger.logWithFile("SensorService.onStartCommand(): start id " + startId + ": " + intent);
    	_startId = startId;
    	_startTime = System.currentTimeMillis();
    	
    	//	センサーの値取得を開始する
    	registSensorListener();
    	
    	//	センサーのデータを処理するスレッドを開始する
    	if(_processorThread.isAlive() == false) {
    		_processorThread.start();
    	}
    	
    	// We want this service to continue running until it is explicitly stopped, so return sticky.
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
    	Logger.logWithFile("SensorService.onDestroy()");
    	unregistSensorListener();
    }
    
    /**
     * When binding to the service, we return an interface to our messenger
     * for sending messages to the service.
     */
    @Override
    public IBinder onBind(Intent intent) {
    	Logger.logWithFile("SensorService.onBind()");
        return mMessenger.getBinder();
    }
    
    private void registSensorListener() {
    	Logger.logWithFile("SensorService.registSensorListener()");
    	_sensorRate = _preferences.getSensorRate();
    	Logger.logWithFile("_sensorRate = " + _sensorRate);
    	if(_registListener) {
    		unregistSensorListener();
    	}
    	_registListener = _sensorManager.registerListener(_sensorEventListener, _sensor, _sensorRate);
    	Logger.logWithFile("_registListener = " + _registListener);
    }
    
    private void unregistSensorListener() {
    	Logger.logWithFile("SensorService.unregistSensorListener()");
    	if(_registListener) {
    		_registListener = false;
    		_sensorManager.unregisterListener(_sensorEventListener);
    	}
    }
    
    class SensorEventListenerImpl implements SensorEventListener {
	    public void onAccuracyChanged(Sensor sensor, int accuracy) {
	    	Logger.logWithFile("onAccuracyChanged: " + accuracy);
	    }
	
	    public void onSensorChanged(SensorEvent event) {
	    	Logger.logWithFile("onSensorChanged()");
	    	_counter += 1;
	    	
	    	String id = _startId + ":" + _counter; 
	    	
	    	float value1 = event.values[0];
	    	float value2 = event.values[1];
	    	float value3 = event.values[2];
	    	double distance = Math.sqrt(Math.pow(value1, 2) + Math.pow(value2, 2) + Math.pow(value3, 2));
	    	//Logger.logWithFile(String.format("%s [%.3f]", id, distance));	
	    	
	    	long time = System.currentTimeMillis() - _startTime;
	    
	    	//	DataProcessorのキューに計測値を追加する
	    	try {
	    		//_clientMessenger.sendMessage(Message.obtain(null, MSG_SEND_SENSOR_DATA, new SensorData(id, distance, time)));
	    		_queue.put(new SensorData(id, distance, time));
	    	}
	    	catch(Exception e) {
	    		e.printStackTrace();
	    		e.printStackTrace(Logger.getPrintStream());
	    	}
	    }
    }
    
    private void showNotification() {
    	Notification notification = 
    			new Notification(R.drawable.ic_launcher, "Hello!", System.currentTimeMillis());
    	PendingIntent pendindIntent = 
    			PendingIntent.getActivity(this, 0, new Intent(this, SensorService.class), 0);
    	notification.setLatestEventInfo(
    			getApplicationContext(), "My notification", "Hello", pendindIntent);
    	_notificationManager.notify(1, notification);
    }
    

}
