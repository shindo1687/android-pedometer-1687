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
	
	//	���݂̏�Ԃ�\���l
	static final private int STATE_INIT = 0;	//	�����l(��x0�ȊO�ɂȂ�����������̒l�ɂ͂Ȃ�Ȃ�)
	static final private int STATE_UP = 1;		//	�㏸��
	static final private int STATE_DOWN = -1;	//	���~��
	private int	_state = STATE_INIT;
	
	//	�T�[�r�X�̃N���C�A���g�փ��b�Z�[�W�𑗂郁�b�Z���W���[
	private ClientMessenger	_clientMessenger = null;
	
	//	�Z���T�[�ɂ���Čv���l���i�[�����L���[	
	private BlockingQueue<SensorData> _queue = null;
	//	�L���[�̗v�f��
	private int	_queueSize = 10000;
	
	//	�O��̌v���l
	private SensorData _lastSensorData = null;
	
	//	�擾�����g�̏��
	private ArrayList<SpikeInfo> _spikeList = new ArrayList<SpikeInfo>();

	/**
	 * �g�`�̃X�p�C�N�̏��
	 * @author john
	 *
	 */
	class SpikeInfo {
		//	���ʒl
		static public final int	STATUS_INIT = 0;			//	�����l
		static public final int	STATUS_ACCEPT = 1;			//	�����Ƃ��č̗p
		static public final int	STATUS_REJECT_SMALL = 2;	//	���p: �U�����������l����
	
		public long		beginTime = 0;	//	�X�p�C�N�̊J�n����(msec)
		public double	beginValue = 0;	//	�X�p�C�N�̊J�n�l(m/s^2)
		public long		maxTime = 0;	//	�X�p�C�N�̍ő�l�̎���(msec)
		public double	maxValue = 0;	//	�X�p�C�N�̍ő�l(m/s^2)
		public long		endTime = 0;	//	�X�p�C�N�̏I������(msec)
		public double	endValue = 0;	//	�X�p�C�N�̏I���l(m/s^2)
		
		public long		length = 0;		//	�X�p�C�N�̒���(msec)
		public double	amp = 0;		//	�X�p�C�N�̐U��(m/s^2)
		public int		status = STATUS_INIT;	//	���ʒl
	}
	
	public DataProcessor() {
		//	�T�[�r�X����f�[�^���󂯎��L���[�𐶐�
		_queue = new ArrayBlockingQueue<SensorData>(_queueSize);
		//	�f�[�^�����̐ݒ��������
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
					//	���~���ɕύX
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
					//	���̃X�p�C�N�f�[�^��ǉ�
					SpikeInfo newSpikeInfo = new SpikeInfo();
					newSpikeInfo.beginTime = _lastSensorData.time();
					newSpikeInfo.beginValue = _lastSensorData.data();
					_spikeList.add(newSpikeInfo);
					//	�㏸���ɕύX
					_state = STATE_UP;
				} 
				break;
			}
		}

		//	����̌v���l���L��
		_lastSensorData = sensorData;
		
		//	���O��\��
		logSensorData(sensorData);
		
		if(ditectedSpikeInfo != null) {
			
			//	�U�����������l�����Ȃ���p����
			if(ditectedSpikeInfo.amp < _setting.getMiminimumAmp()) {
				ditectedSpikeInfo.status = SpikeInfo.STATUS_REJECT_SMALL;
			}
			//	�����Ƃ��č̗p����
			else {
				ditectedSpikeInfo.status = SpikeInfo.STATUS_ACCEPT;
				_count += 1;
				//	�N���C�A���g�Ɍ��݂̃J�E���g���𑗐M
				if(_clientMessenger != null) {
					_clientMessenger.sendMessage(Message.obtain(null, SensorService.MSG_SET_COUNT, _count, 0));
				}
			}
			
			//	���O��\��
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
