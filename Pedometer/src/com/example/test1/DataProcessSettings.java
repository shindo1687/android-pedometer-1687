package com.example.test1;

public class DataProcessSettings {

	private double	_minimumAmp = 0;	//	山と認識する最低限の振幅値
	private double	_concatAmp = 0;		//	前の山と連結される振幅値
	private long	_maximunPeriod = 0;	//	山と認識する最大長(msec)
	private long	_minimumPeriod = 0;	//	山と認識する最低長(msec)
	private long	_detectPeriod = 0;	//	山の検知期間(msec)
	private int		_detectCount = 0;		//	山の検知期間中に必要な山の数
	
	public DataProcessSettings() {
	}

	public double getMiminimumAmp() {
		return _minimumAmp;
	}
	public double getConcatAmp() {
		return _concatAmp;
	}
	public long getMaximunPeriod() {
		return _maximunPeriod;
	}
	public long getMinimumPeriod() {
		return _minimumPeriod;
	}
	public long getDetectPeriod() {
		return _detectPeriod;
	}
	public int getDetectCount() {
		return _detectCount;
	}
	
	public void setMinimumAmp(double _minimumAmp) {
		this._minimumAmp = _minimumAmp;
	}
	public void setConcatAmp(double _concatAmp) {
		this._concatAmp = _concatAmp;
	}
	public void setMaximunPeriod(long _maximunPeriod) {
		this._maximunPeriod = _maximunPeriod;
	}
	public void setMinimumPeriod(long _minimumPeriod) {
		this._minimumPeriod = _minimumPeriod;
	}
	public void setDetectPeriod(long _detectPeriod) {
		this._detectPeriod = _detectPeriod;
	}
	public void setDetectCount(int _detectCount) {
		this._detectCount = _detectCount;
	}

	
}
