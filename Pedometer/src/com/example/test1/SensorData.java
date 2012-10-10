package com.example.test1;

/**
 * �Z���T�[�̌v���l
 */
public class SensorData {
	private String	_id;	//	���ʎq
	private	double	_data;	//	�v���l(m/s^2)
	private long	_time;	//	����(msec)
	
	public SensorData(String id, double data, long time) {
		_id = id;
		_data = data;
		_time = time;
	}
	
	String 	id() {return _id;}
	void	setId(String id) {_id = id;}
	
	double 	data() {return _data;}
	void	setData(double data) {_data = data;}
	
	long 	time() {return _time;}
	void 	setTime(long time) {_time = time;}
}
