package com.example.test1;

public class SpikeData {

	//	���ʒl
	static public final int	STATUS_INIT = 0;			//	�����l
	static public final int	STATUS_ACCEPT = 1;			//	�����Ƃ��č̗p
	static public final int	STATUS_REJECT_SMALL_AMP = 2;	//	���p: �U�����������l����
	static public final int	STATUS_REJECT_LONG_PERIOD = 3;	//	���p: �g������������
	static public final int	STATUS_REJECT_SHORT_PERIOD = 4;	//	���p: �g�����Z������

	private long	_beginTime = 0;	//	�X�p�C�N�̊J�n����(msec)
	private double	_beginValue = 0;	//	�X�p�C�N�̊J�n�l(m/s^2)
	private long	_maxTime = 0;	//	�X�p�C�N�̍ő�l�̎���(msec)
	private double	_maxValue = 0;	//	�X�p�C�N�̍ő�l(m/s^2)
	private long	_endTime = 0;	//	�X�p�C�N�̏I������(msec)
	private double	_endValue = 0;	//	�X�p�C�N�̏I���l(m/s^2)
	
	private long	_length = 0;		//	�X�p�C�N�̒���(msec)
	private double	_amp = 0;		//	�X�p�C�N�̐U��(m/s^2)
	private int		_status = STATUS_INIT;	//	���ʒl
	
	public SpikeData() {
		// TODO Auto-generated constructor stub
	}

	public long getBeginTime() {
		return _beginTime;
	}
	public double getBeginValue() {
		return _beginValue;
	}
	public long getMaxTime() {
		return _maxTime;
	}
	public double getMaxValue() {
		return _maxValue;
	}
	public long getEndTime() {
		return _endTime;
	}
	public double getEndValue() {
		return _endValue;
	}
	public long getLength() {
		if(_length == 0) {
			_length = _endTime - _beginTime;
		}
		return _length;
	}
	public double getAmp() {
		if(_amp == 0) {
			_amp = _maxValue - Math.min(_beginValue, _endValue);
		}
		return _amp;
	}
	public int getStatus() {
		return _status;
	}

	public void setBeginTime(long _beginTime) {
		this._beginTime = _beginTime;
	}
	public void setBeginValue(double _beginValue) {
		this._beginValue = _beginValue;
	}
	public void setMaxTime(long _maxTime) {
		this._maxTime = _maxTime;
	}
	public void setMaxValue(double _maxValue) {
		this._maxValue = _maxValue;
	}
	public void setEndTime(long _endTime) {
		this._endTime = _endTime;
	}
	public void setEndValue(double _endValue) {
		this._endValue = _endValue;
	}
	public void setStatus(int _status) {
		this._status = _status;
	}

}
