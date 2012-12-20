package com.example.test1;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import android.os.Message;

public class DataProcessor implements Runnable {
	
	private boolean _enableIgnore = false;	//	�v���l�̖�����L���ɂ���
	private double _ignoreThreshold = 0.0;	//	���S�ɖ�������v���l�̂������l
	
	private boolean _enableAdjust = false;	//	�v���l�̕␳��L���ɂ���
	private double _adjustValue = 0;		//	�v���l�̕␳�l
	
	private boolean _enableFilter = false;	//	���[�p�X�t�B���^��L���ɂ���
	private double _filterRate = 0.9;		//	���[�p�X�t�B���^
	
	private DataProcessSettings _setting = new DataProcessSettings();	//	�f�[�^�����̐ݒ�
	
	private int	_count = 0;	//	�v����������
	
	//	���݂̔g�̏�Ԃ�\���l
	static final private int STATE_INIT = 0;	//	�����l(��x0�ȊO�ɂȂ�����������̒l�ɂ͂Ȃ�Ȃ�)
	static final private int STATE_UP = 1;		//	�㏸��
	static final private int STATE_DOWN = -1;	//	���~��
	private int	_state = STATE_INIT;
	
	//	�R���r�؂�Ă��Ȃ���
	boolean	_isContinuous = false;
	
	//	�T�[�r�X�̃N���C�A���g�փ��b�Z�[�W�𑗂郁�b�Z���W���[
	private ClientMessenger	_clientMessenger = null;
	
	//	�Z���T�[�ɂ���Čv���l���i�[�����L���[	
	private BlockingQueue<SensorData> _queue = null;
	//	�L���[�̗v�f��
	private int	_queueSize = 10000;
	
	//	�O��̌v���l
	private SensorData _lastSensorData = null;
	
	//	�擾�����g�̏��
	private ArrayList<SpikeData> _spikeList = new ArrayList<SpikeData>();

	public DataProcessor() {
		//	�T�[�r�X����f�[�^���󂯎��L���[�𐶐�
		_queue = new ArrayBlockingQueue<SensorData>(_queueSize);
		//	�f�[�^�����̐ݒ��������
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
	 * �v���l����������
	 * @param sensorData
	 */
	private void consume(SensorData sensorData) {
		//	�␳�l�����Z
		if(_enableAdjust) {
			sensorData.setData(sensorData.data() + _adjustValue);
		}
		
		//	���[�p�X�t�B���^
		if(_enableFilter && _lastSensorData != null) {
			double filterData = _filterRate * _lastSensorData.data() + (1 - _filterRate) * sensorData.data();
			sensorData.setData(filterData);
		}
		
		//	��������v���l
		if(_enableIgnore && Math.abs(sensorData.data()) < _ignoreThreshold) {
			return;
		}
		
		//	���O��\��
		logSensorData(sensorData);
			
		switch(_state) {
			case STATE_INIT: {
				//	�ŏ��̎R�̊J�n
				SpikeData newSpikeData = new SpikeData();
				newSpikeData.setBeginTime(0);
				newSpikeData.setBeginValue(0);
				_state = STATE_UP;
				_spikeList.add(newSpikeData);
				break;
			}
			case STATE_UP: {
				if(sensorData.data() < _lastSensorData.data()) {
					//	�R�̒��_���m�肷��
					SpikeData lastSpikeData = _spikeList.get(_spikeList.size() - 1);
					lastSpikeData.setMaxTime(_lastSensorData.time());
					lastSpikeData.setMaxValue(_lastSensorData.data());
					//	���~���ɕύX
					_state = STATE_DOWN;
				} 
				break;
			}
			case STATE_DOWN: { 
				if(sensorData.data() > _lastSensorData.data()) {
					//	�R��1�m�肷��
					SpikeData lastSpikeData = _spikeList.get(_spikeList.size() - 1);
					lastSpikeData.setEndTime(_lastSensorData.time());
					lastSpikeData.setEndValue(_lastSensorData.data());
					//	�R�̔�����s��
					processSpikeInfo();
					//	���̎R�̃f�[�^��ǉ�
					SpikeData newSpikeInfo = new SpikeData();
					newSpikeInfo.setBeginTime(_lastSensorData.time());
					newSpikeInfo.setBeginValue(_lastSensorData.data());
					_spikeList.add(newSpikeInfo);
					//	�㏸���ɕύX
					_state = STATE_UP;
				} 
				break;
			}
		}

		//	����̌v���l���L��
		_lastSensorData = sensorData;
	}
	
	/**
	 * �m�肵���R����������
	 * @param newSpikeInfo
	 */
	private void processSpikeInfo() {
		SpikeData lastSpikeData = _spikeList.get(_spikeList.size() - 1);
		
		//	���O��\��
		logSpikeInfo(lastSpikeData);	
		
		//	�O�̎R�ƌ������邩�ǂ������ׂ�
		if(_isContinuous) {
			SpikeData lastSpikeData2 = _spikeList.get(_spikeList.size() - 2);
			if(lastSpikeData2.get)
		}
		
		//	�U�����������l�����Ȃ���p����
		if(lastSpikeData.getAmp() < _setting.getMiminimumAmp()) {
			lastSpikeData.setStatus(SpikeData.STATUS_REJECT_SMALL_AMP);
			//	�R���r�؂ꂽ
			_isContinuous = false;
		}
		//	�����Ƃ��č̗p����
		else {
			lastSpikeData.setStatus(SpikeData.STATUS_ACCEPT);
			_count += 1;
			//	�N���C�A���g�Ɍ��݂̃J�E���g���𑗐M
			if(_clientMessenger != null) {
				_clientMessenger.sendMessage(Message.obtain(null, SensorService.MSG_SET_COUNT, _count, 0));
			}
			//	�R���A�����Ă���
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
