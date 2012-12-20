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
	
	//	現在の波の状態を表す値
	static final private int STATE_INIT = 0;	//	初期値(一度0以外になったらもうこの値にはならない)
	static final private int STATE_UP = 1;		//	上昇中
	static final private int STATE_DOWN = -1;	//	下降中
	private int	_state = STATE_INIT;
	
	//	山が途切れていないか
	boolean	_isContinuous = false;
	
	//	サービスのクライアントへメッセージを送るメッセンジャー
	private ClientMessenger	_clientMessenger = null;
	
	//	センサーによって計測値が格納されるキュー	
	private BlockingQueue<SensorData> _queue = null;
	//	キューの要素数
	private int	_queueSize = 10000;
	
	//	前回の計測値
	private SensorData _lastSensorData = null;
	
	//	取得した波の情報
	private ArrayList<SpikeData> _spikeList = new ArrayList<SpikeData>();

	public DataProcessor() {
		//	サービスからデータを受け取るキューを生成
		_queue = new ArrayBlockingQueue<SensorData>(_queueSize);
		//	データ処理の設定を初期化
		_setting.setMinimumAmp(0.5);
		_setting.setConcatAmp(0.2);
		_setting.setMaximunPeriod(2000);
		_setting.setMinimumPeriod(200);
		_setting.setDetectPeriod(7000);
		_setting.setDetectCount(5);
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
	
	/**
	 * 計測値を処理する
	 * @param sensorData
	 */
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
		
		//	ログを表示
		logSensorData(sensorData);
			
		switch(_state) {
			case STATE_INIT: {
				//	最初の山の開始
				SpikeData newSpikeData = new SpikeData();
				newSpikeData.setBeginTime(0);
				newSpikeData.setBeginValue(0);
				_state = STATE_UP;
				_spikeList.add(newSpikeData);
				break;
			}
			case STATE_UP: {
				if(sensorData.data() < _lastSensorData.data()) {
					//	山の頂点を確定する
					SpikeData lastSpikeData = _spikeList.get(_spikeList.size() - 1);
					lastSpikeData.setMaxTime(_lastSensorData.time());
					lastSpikeData.setMaxValue(_lastSensorData.data());
					//	下降中に変更
					_state = STATE_DOWN;
				} 
				break;
			}
			case STATE_DOWN: { 
				if(sensorData.data() > _lastSensorData.data()) {
					//	山が1つ確定する
					SpikeData lastSpikeData = _spikeList.get(_spikeList.size() - 1);
					lastSpikeData.setEndTime(_lastSensorData.time());
					lastSpikeData.setEndValue(_lastSensorData.data());
					//	山の判定を行う
					processSpikeInfo();
					//	次の山のデータを追加
					SpikeData newSpikeInfo = new SpikeData();
					newSpikeInfo.setBeginTime(_lastSensorData.time());
					newSpikeInfo.setBeginValue(_lastSensorData.data());
					_spikeList.add(newSpikeInfo);
					//	上昇中に変更
					_state = STATE_UP;
				} 
				break;
			}
		}

		//	今回の計測値を記憶
		_lastSensorData = sensorData;
	}
	
	/**
	 * 確定した山を処理する
	 * @param newSpikeInfo
	 */
	private void processSpikeInfo() {
		SpikeData lastSpikeData = _spikeList.get(_spikeList.size() - 1);
		
		//	ログを表示
		logSpikeInfo(lastSpikeData);	
		
		//	前の山と結合するかどうか調べる
		if(_isContinuous) {
			SpikeData lastSpikeData2 = _spikeList.get(_spikeList.size() - 2);
			if(lastSpikeData2.get)
		}
		
		//	振幅がしきい値未満なら棄却する
		if(lastSpikeData.getAmp() < _setting.getMiminimumAmp()) {
			lastSpikeData.setStatus(SpikeData.STATUS_REJECT_SMALL_AMP);
			//	山が途切れた
			_isContinuous = false;
		}
		//	歩数として採用する
		else {
			lastSpikeData.setStatus(SpikeData.STATUS_ACCEPT);
			_count += 1;
			//	クライアントに現在のカウント数を送信
			if(_clientMessenger != null) {
				_clientMessenger.sendMessage(Message.obtain(null, SensorService.MSG_SET_COUNT, _count, 0));
			}
			//	山が連続している
			_isContinuous = true;
		}
		
		
	}
	
	private void logSensorData(SensorData sensorData) {
    	if(_clientMessenger != null) {
    		_clientMessenger.sendMessage(Message.obtain(null, SensorService.MSG_SEND_SENSOR_DATA, sensorData));
    	}
	}
	
	private void logSpikeInfo(SpikeData spikeData) {
		if(_clientMessenger != null) {
			_clientMessenger.sendMessage(Message.obtain(null, SensorService.MSG_SEND_SPIKE_DATA, spikeData));
		}
	}
}
