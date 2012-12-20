package com.example.test1;

public class DataProcessSettings {

	private double	_minimumAmp = 0;	//	�R�ƔF������Œ���̐U���l
	private double	_concatAmp = 0;		//	�O�̎R�ƘA�������U���l
	private long	_maximunPeriod = 0;	//	�R�ƔF������ő咷(msec)
	private long	_minimumPeriod = 0;	//	�R�ƔF������Œᒷ(msec)
	private long	_detectPeriod = 0;	//	�R�̌��m����(msec)
	private int		_detectCount = 0;		//	�R�̌��m���Ԓ��ɕK�v�ȎR�̐�
	
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
