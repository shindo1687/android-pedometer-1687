package com.example.test1;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import android.os.Message;

public class DataProcessor implements Runnable {
	
	private boolean _enableIgnore = false;	//	計測値の無視を有効にする
	private double _ignoreThreshold = 0.0;	//	完全に無視する計測値のしきい値
	
	private boolean _enableAdjust = false;	//	計測値の補正を有効にする
	private double _adjustValue = 0;		//	計測値の補正値
	
	private boolean _enableFilter = false;	//	ローパスフィルタを有効にする
	private double _filterRate = 0.9;		//	ローパスフィルタ
	
	private DataProcessSettings _setting = new DataProcessSettings();	//	データ処理の設定
	
	private int	_count = 0;	//	計測した歩数
	
	//	現在の状態を表す値
	static final private int STATE_INIT = 0;	//	初期値(一度0以外になったらもうこの値にはならない)
	static final private int STATE_UP = 1;		//	上昇中
	static final private int STATE_DOWN = -1;	//	下降中
	private int	_state = STATE_INIT;
	
	//	サービスのクライアントへメッセージを送るメッセンジャー
	private ClientMessenger	_clientMessenger = null;
	
	//	センサーによって計測値が格納されるキュー	
	private BlockingQueue<SensorData> _queue = null;
	//	キューの要素数
	private int	_queueSize = 10000;
	
	//	前回の計測値
	private SensorData _lastSensorData = null;
	
	//	取得した波の情報
	private ArrayList<SpikeInfo> _spikeList = new ArrayList<SpikeInfo>();

	/**
	 * 波形のスパイクの情報
	 * @author john
	 *
	 */
	class SpikeInfo {
		//	判別値
		static public final int	STATUS_INIT = 0;			//	初期値
		static public final int	STATUS_ACCEPT = 1;			//	歩数として採用
		static public final int	STATUS_REJECT_SMALL = 2;	//	棄却: 振幅がしきい値未満
	
		public long		beginTime = 0;	//	スパイクの開始時刻(msec)
		public double	beginValue = 0;	//	スパイクの開始値(m/s^2)
		public long		maxTime = 0;	//	スパイクの最大値の時刻(msec)
		public double	maxValue = 0;	//	スパイクの最大値(m/s^2)
		public long		endTime = 0;	//	スパイクの終了時刻(msec)
		public double	endValue = 0;	//	スパイクの終了値(m/s^2)
		
		public long		length = 0;		//	スパイクの長さ(msec)
		public double	amp = 0;		//	スパイクの振幅(m/s^2)
		public int		status = STATUS_INIT;	//	判別値
	}
	
	public DataProcessor() {
		//	サービスからデータを受け取るキューを生成
		_queue = new ArrayBlockingQueue<SensorData>(_queueSize);
		//	データ処理の設定を初期化
		_setting.setMinimumAmp(0.5);
	}
	
	public void setClientMessenger(ClientMessenger messenger) {
		_clientMessenger = messenger;
	}
	
	public BlockingQueue<SensorData> getQueue() {
		return _queue;
	}
	
	public int getCount() {
		return _count;
	}
	
	public DataProcessSettings getDataProcessSettings() {
		return _setting;
	}
	
	public void run() {
		try {
			while(true) {
				consume(_queue.take());
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			e.printStackTrace(Logger.getPrintStream());
		}
	}
	
	private void consume(SensorData sensorData) {
		//	補正値を加算
		if(_enableAdjust) {
			sensorData.setData(sensorData.data() + _adjustValue);
		}
		
		//	ローパスフィルタ
		if(_enableFilter && _lastSensorData != null) {
			double filterData = _filterRate * _lastSensorData.data() + (1 - _filterRate) * sensorData.data();
			sensorData.setData(filterData);
		}
		
		//	無視する計測値
		if(_enableIgnore && Math.abs(sensorData.data()) < _ignoreThreshold) {
			return;
		}
		
		SpikeInfo ditectedSpikeInfo = null;
			
		switch(_state) {
			case STATE_INIT: {
				SpikeInfo newSpikeInfo = new SpikeInfo();
				newSpikeInfo.beginTime = 0;
				newSpikeInfo.beginValue = 0;
				_state = STATE_UP;
				_spikeList.add(newSpikeInfo);
				break;
			}
			case STATE_UP: {
				if(sensorData.data() < _lastSensorData.data()) {
					SpikeInfo lastSpikeInfo = _spikeList.get(_spikeList.size() - 1);
					lastSpikeInfo.maxTime = _lastSensorData.time();
					lastSpikeInfo.maxValue = _lastSensorData.data();
					//	下降中に変更
					_state = STATE_DOWN;
				} 
				break;
			}
			case STATE_DOWN: { 
				if(sensorData.data() > _lastSensorData.data()) {
					SpikeInfo lastSpikeInfo = _spikeList.get(_spikeList.size() - 1);
					lastSpikeInfo.endTime = _lastSensorData.time();
					lastSpikeInfo.endValue = _lastSensorData.data();
					lastSpikeInfo.length = lastSpikeInfo.endTime - lastSpikeInfo.beginTime;
					lastSpikeInfo.amp = lastSpikeInfo.maxValue - Math.min(lastSpikeInfo.beginValue, lastSpikeInfo.endValue);
					ditectedSpikeInfo = lastSpikeInfo;
					//	次のスパイクデータを追加
					SpikeInfo newSpikeInfo = new SpikeInfo();
					newSpikeInfo.beginTime = _lastSensorData.time();
					newSpikeInfo.beginValue = _lastSensorData.data();
					_spikeList.add(newSpikeInfo);
					//	上昇中に変更
					_state = STATE_UP;
				} 
				break;
			}
		}

		//	今回の計測値を記憶
		_lastSensorData = sensorData;
		
		//	ログを表示
		logSensorData(sensorData);
		
		if(ditectedSpikeInfo != null) {
			
			//	振幅がしきい値未満なら棄却する
			if(ditectedSpikeInfo.amp < _setting.getMiminimumAmp()) {
				ditectedSpikeInfo.status = SpikeInfo.STATUS_REJECT_SMALL;
			}
			//	歩数として採用する
			else {
				ditectedSpikeInfo.status = SpikeInfo.STATUS_ACCEPT;
				_count += 1;
				//	クライアントに現在のカウント数を送信
				if(_clientMessenger != null) {
					_clientMessenger.sendMessage(Message.obtain(null, SensorService.MSG_SET_COUNT, _count, 0));
				}
			}
			
			//	ログを表示
			logSpikeInfo(ditectedSpikeInfo);
		}
	}
	
	private void logSensorData(SensorData sensorData) {
    	if(_clientMessenger != null) {
    		_clientMessenger.sendMessage(Message.obtain(null, SensorService.MSG_SEND_SENSOR_DATA, sensorData));
    	}
	}
	
	private void logSpikeInfo(SpikeInfo spikeInfo) {
		if(_clientMessenger != null) {
			_clientMessenger.sendMessage(Message.obtain(null, SensorService.MSG_SEND_SPIKE_DATA, spikeInfo));
		}
	}
}
