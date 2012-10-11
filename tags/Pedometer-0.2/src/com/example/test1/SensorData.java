package com.example.test1;

/**
 * センサーの計測値
 */
public class SensorData {
	private String	_id;	//	識別子
	private	double	_data;	//	計測値(m/s^2)
	private long	_time;	//	時刻(msec)
	
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
