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
	
	private SurfaceHolder _surfaceHolder = null;
	private Thread	_thread = null;
	private ArrayBlockingQueue<Double> _dataQueue = null;
	
	public GraphView(Context context) {
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Logger.logWithFile("GraphView.surfaceChanged(): " + width + ", " + height);
		_width = width;
		_height = height;
		_dataNum = _width / _dataGap;
		_dataQueue = new ArrayBlockingQueue<Double>(_dataNum);
		if(_thread != null && _thread.isAlive() == false) {
			_thread.start();
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Logger.logWithFile("GraphView.surfaceCreated()");
		_surfaceHolder = holder;
		_dataQueue = new ArrayBlockingQueue<Double>(1);
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
				canvas.drawColor(Color.GRAY);
				
				Iterator<Double> iterator = _dataQueue.iterator();
				float[] drawPoints = new float[_dataNum * 4];
				int index = 0;
				int prevX = 0;
				int prevY = _height;
				while(iterator.hasNext()) {
					int x = index * _dataGap;
					int y = calcY(iterator.next());
					drawPoints[index * 4] = prevX;	
					drawPoints[(index * 4) + 1] = prevY;
					drawPoints[(index * 4) + 2] = x;	
					drawPoints[(index * 4) + 3] = y;
					index++;
					prevX = x;
					prevY = y;
				}
				while(index < _dataNum) {
					int x = index * _dataGap;
					drawPoints[index * 4] = prevX;
					drawPoints[(index * 4) + 1] = _height;
					drawPoints[(index * 4) + 2] = x;	
					drawPoints[(index * 4) + 3] = _height;
					index++;
					prevX = x;
				}
				
				Paint paint = new Paint();
				paint.setColor(Color.GREEN);
				paint.setStrokeWidth(_strokeWidth);
				canvas.drawLines(drawPoints, paint);
				
				_surfaceHolder.unlockCanvasAndPost(canvas);
				
				Thread.sleep(100);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			e.printStackTrace(Logger.getPrintStream());
		}
	}

	public void drawSensorData(SensorData data) {
		//	データをキューに追加
		if(_dataQueue.size() >= _dataNum) {
			_dataQueue.poll();
		}
		_dataQueue.offer(new Double(data.data()));
	}
	
	private void drawSpikeInfo(SpikeInfo info) {
		
	}
	
	private int calcY(double data) {
		double max = 10;
		int y = _height - (int)((double)_height / max * data);
		return y;
	}


}
