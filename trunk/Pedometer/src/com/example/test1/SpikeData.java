package com.example.test1;

public class SpikeData {

	//	判別値
	static public final int	STATUS_INIT = 0;			//	初期値
	static public final int	STATUS_ACCEPT = 1;			//	歩数として採用
	static public final int	STATUS_REJECT_SMALL_AMP = 2;	//	棄却: 振幅がしきい値未満
	static public final int	STATUS_REJECT_LONG_PERIOD = 3;	//	棄却: 波長が長すぎる
	static public final int	STATUS_REJECT_SHORT_PERIOD = 4;	//	棄却: 波長が短すぎる

	private long	_beginTime = 0;	//	スパイクの開始時刻(msec)
	private double	_beginValue = 0;	//	スパイクの開始値(m/s^2)
	private long	_maxTime = 0;	//	スパイクの最大値の時刻(msec)
	private double	_maxValue = 0;	//	スパイクの最大値(m/s^2)
	private long	_endTime = 0;	//	スパイクの終了時刻(msec)
	private double	_endValue = 0;	//	スパイクの終了値(m/s^2)
	
	private long	_length = 0;		//	スパイクの長さ(msec)
	private double	_amp = 0;		//	スパイクの振幅(m/s^2)
	private int		_status = STATUS_INIT;	//	判別値
	
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
