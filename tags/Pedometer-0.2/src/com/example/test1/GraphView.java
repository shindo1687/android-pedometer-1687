package com.example.test1;

import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;

import com.example.test1.DataProcessor.SpikeInfo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GraphView implements SurfaceHolder.Callback, Runnable {

	private int	_width = 0;
	private int _height = 0;

	private int _dataNum = 0;
	private int _dataGap = 4;
	private int _strokeWidth = 2;
	
	class DrawInfo {
		public SensorData	_sensorData = null;
		public boolean		_isSpikeBegin = false;
		public boolean		_isSpikeTop = false;
		public boolean		_isSpikeEnd = false;
		
		DrawInfo(SensorData sensorData) {
			_sensorData = sensorData;
		}
	}
	
	private SurfaceHolder _surfaceHolder = null;
	private Thread	_thread = null;
	private ArrayBlockingQueue<DrawInfo> _dataQueue = null;
	private DataProcessSettings _setting = null;
	
	public GraphView(Context context) {
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Logger.logWithFile("GraphView.surfaceChanged(): " + width + ", " + height);
		_width = width;
		_height = height;
		_dataNum = _width / _dataGap;
		_dataQueue = new ArrayBlockingQueue<DrawInfo>(_dataNum);
		if(_thread != null && _thread.isAlive() == false) {
			_thread.start();
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Logger.logWithFile("GraphView.surfaceCreated()");
		_surfaceHolder = holder;
		_thread = new Thread(this);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Logger.logWithFile("GraphView.surfaceDestroyed()");
		_thread = null;
	}
	
	@Override
	public void run() {
		try {
			while(_thread != null) {
				Canvas canvas = _surfaceHolder.lockCanvas();
				if(canvas != null) {
					canvas.drawColor(Color.GRAY);
					
					drawDaraProcessSettings(canvas);
					drawSensorData(canvas);
					
					_surfaceHolder.unlockCanvasAndPost(canvas);
				}
				Thread.sleep(50);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			e.printStackTrace(Logger.getPrintStream());
		}
	}

	public void setSensorData(SensorData data) {
		//	データをキューに追加
		if(_dataQueue.size() >= _dataNum) {
			_dataQueue.poll();
		}
		_dataQueue.offer(new DrawInfo(data));
	}
	
	public void setDataProcessSettings(DataProcessSettings setting) {
		_setting = setting;
	}
	
	public void setSpikeInfo(SpikeInfo spikeInfo) {
		Iterator<DrawInfo> iterator = _dataQueue.iterator();
		while(iterator.hasNext()) {
			DrawInfo drawInfo = iterator.next();
			long time = drawInfo._sensorData.time();
			if(time == spikeInfo.beginTime) {
				drawInfo._isSpikeBegin = true;
			}
			else if(time == spikeInfo.maxTime) {
				drawInfo._isSpikeTop = true;
			}
			else if(time == spikeInfo.endTime) {
				drawInfo._isSpikeEnd = true;
				break;	//	これ以上は無駄なのでループを抜ける
			}
		}
	}
	
	/**
	 * 計測データを描画する
	 * @param canvas
	 */
	private void drawSensorData(Canvas canvas) {
		if(_dataQueue == null) {
			return;
		}
		
		int index = 0;
		int prevX = 0;
		int prevY = _height;
		Iterator<DrawInfo> iterator = _dataQueue.iterator();
		while(iterator.hasNext()) {
			DrawInfo drawInfo = iterator.next();
			SensorData sensorData = drawInfo._sensorData;
			
			//	このデータのx,y座標
			int x = index * _dataGap;
			int y = calcY(sensorData.data());
			
			//	前のデータからこのデータまで線を引く
			float[] drawPoints = {prevX, prevY, x, y};
			Paint paint = new Paint();
			paint.setColor(Color.GREEN);
			paint.setStrokeWidth(_strokeWidth);
			canvas.drawLines(drawPoints, paint);
			
			index++;
			prevX = x;
			prevY = y;
			
			//	山の情報を描画
			if(drawInfo._isSpikeTop) {
				drawPoint(canvas, x, y, Color.BLUE, 5);
			}
		}
	}
	
	/**
	 * 点を描画する
	 * @param canvas
	 * @param x
	 * @param y
	 * @param color
	 * @param strokeWidth
	 */
	private void drawPoint(Canvas canvas, float x, float y, int color, int strokeWidth) {
		Paint paint = new Paint();
		paint.setColor(color);
		paint.setStrokeWidth(strokeWidth);
		canvas.drawPoint(x, y, paint);
	}
	
	/**
	 * データ処理設定を描画する
	 * @param canvas
	 */
	private void drawDaraProcessSettings(Canvas canvas) {
		if(_setting == null) {
			return;
		}
		
		float y = calcY(_setting.getMiminimumAmp());
		float[] drawPoints = {0, y, _width, y};
		
		Paint paint = new Paint();
		paint.setColor(Color.RED);
		paint.setStrokeWidth(2);
		canvas.drawLines(drawPoints, paint);
	}
	
	
	/**
	 * 計測値のy座標を計算する
	 * @param data
	 * @return
	 */
	private int calcY(double data) {
		double max = 10;
		int y = _height - (int)((double)_height / max * data);
		return y;
	}
}
