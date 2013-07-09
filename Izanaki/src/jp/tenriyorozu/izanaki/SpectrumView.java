package jp.tenriyorozu.izanaki;

import java.text.DecimalFormat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

class SpectrumView extends SurfaceView implements
SurfaceHolder.Callback, SpectrumDraw {
	Thread thread;
	boolean isAttached;
	VoiceCapt mVoiceCapt;
	SPP spp;
	
	private float screenWidth, screenHeight;
	private int fftsize = 2048;
	private int pitchTimeLine[] = new int[800];
	private int pitchTimeLinePosition = 0;
	private int modeSelector = 1;
	static private float freqOfCArray[] = {16.352f, 32.703f, 65.406f, 130.81f, 261.63f, 523.25f, 1046.5f, 2093.0f, 4186.0f, 8372.0f, 16744.0f};
	static private float noteRangeArray[] = {1.0293022f, 1.0905077f, 1.1553527f, 1.2240535f, 1.2968396f, 1.3739536f, 1.4556532f, 1.5422108f, 1.6339155f, 1.7310731f, 1.8340081f, 1.9430639f};
	static private String noteArray[] = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
	

	
	public SpectrumView(Context context, SPP spp) {
		super(context);
		this.spp = spp;
		startVoiceCapt();		
	}
	
	public void startVoiceCapt(){		//VoiceCaptスレッドスタータ
		getHolder().addCallback(this);
		
		mVoiceCapt = new VoiceCapt();
		mVoiceCapt.setSpectrumDrawListner(this);
		
		Thread thread = new Thread(mVoiceCapt);
		thread.start();
	}
	
	public void stopVoiceCapt(){
		mVoiceCapt.halt();
	}
		
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		screenWidth = width;
		screenHeight = height;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		isAttached=true;
	}
	
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		isAttached=false;
		Log.d("IzanakiSurfaceView", "surfaceDestroyed");
	}
	
	@Override
	public void surfaceDraw(double buffer1[], int bufSize, double buffer2[], int bufSize2, int peakIndex, double ra){
		
		 if (!isAttached) {      //surfaceCreatedが呼ばれる前にlockCanvasするとエラーとなるためチェック
	            return;  
	        }  
		
		SurfaceHolder holder = getHolder();
		Canvas canvas = holder.lockCanvas();
		float freq = getFreq(peakIndex);
		String note = getPitchNote(freq);
		pitchTimeLine[pitchTimeLinePosition] = (int)freq;
		switch(modeSelector){
		case 0:
			doSpectrumDraw(canvas, buffer1, bufSize, buffer2, bufSize2, peakIndex, ra, freq, note);
			break;
		case 1:
			doTimeLineDraw(canvas, peakIndex, freq, note, pitchTimeLine, pitchTimeLinePosition);
		}
		holder.unlockCanvasAndPost(canvas);
		pitchTimeLinePosition++;
		if(pitchTimeLinePosition == 400){
			pitchTimeLinePosition = 0;
		}
		
		if(spp.isBluetoothConnected() & freq!=-1){
			byte[] send = toBytes((int)freq);
			spp.write(send);
		}
		
	}

	private void doSpectrumDraw(Canvas canvas, double buffer1[], int bufSize1, double buffer2[], int bufSize2, int peakIndex, double ra, float freq, String note){

		Paint paint = new Paint();
		paint.setColor(Color.GREEN);
		paint.setAntiAlias(true);
		paint.setStrokeWidth(1);
		canvas.drawColor(Color.BLACK);
								
		for(int i=0; i<bufSize2; i++){
						
			int y1 = (int)(Math.log10(buffer1[i])*20);	
			canvas.drawLine(i*8, 670, i*8, 670-y1, paint);  //PowerSpectrum描画
			
			int y2 =(int)(buffer2[i]*1.0E1);  
			canvas.drawLine(i, 200, i, 200-y2*2, paint);  //autocorr描画
			
		}
		
			
		paint.setTextSize(90);
		paint.setColor(Color.GREEN);
		if(freq != -1){
			canvas.drawText(String.valueOf((int)freq), 20, 900, paint);
		}
		else{
			canvas.drawText("--", 20, 900, paint);
		}
		
		paint.setTextSize(44);
		paint.setColor(Color.LTGRAY);
		canvas.drawText(note, 190, 790, paint);
		
		DecimalFormat exFormat = new DecimalFormat("###.#"); 
		String strRa = exFormat.format(ra);
		paint.setTextSize(90);
		paint.setColor(Color.CYAN);
		canvas.drawText(strRa, 500, 900, paint);
		
		
		paint.setTextSize(24);
		paint.setColor(Color.CYAN);
		canvas.drawText("HNR", 500, 780, paint);
		paint.setColor(Color.GREEN);
		canvas.drawText("CMSDF", 20, 230, paint);
		canvas.drawText("PowerSpectrum", 20, 450, paint);
		canvas.drawText("F0", 10, 780, paint);
		paint.setTextSize(36);
		canvas.drawText("Hz", 190, 900, paint);
		paint.setColor(Color.CYAN);
		canvas.drawText("dB", 680, 900, paint);
		
		paint.setColor(Color.RED);
		paint.setStrokeWidth(4);
		if(peakIndex != 0){
			canvas.drawLine(peakIndex, 0, peakIndex, 200, paint);
			canvas.drawLine(freq/(44100/fftsize)*8, 520, freq/(44100/fftsize)*8, 670, paint);
		}

		
	}
	
	private void doTimeLineDraw(Canvas canvas, int peakIndex, float freq, String note, int pitchTimeLine[], int pitchTimeLinePosition){

		Paint paint = new Paint();
		paint.setColor(Color.GREEN);
		paint.setAntiAlias(true);
		canvas.drawColor(Color.BLACK);
								
		paint.setStrokeWidth(4);
		for(int i=0; i<400; i++){
			if(pitchTimeLine[i]>0){
				canvas.drawPoint(40+i*2, 570-pitchTimeLine[i], paint);
			}
		}
		
		paint.setTextSize(150);
		paint.setColor(Color.GREEN);
		if(freq != -1){
			canvas.drawText(String.valueOf((int)freq), 20, 700, paint);
		}
		else{
			canvas.drawText("--", 20, 700, paint);
		}
	
		paint.setTextSize(150);
		paint.setColor(Color.LTGRAY);
		canvas.drawText(note, 500, 700, paint);
		
		paint.setTextSize(24);
		paint.setColor(Color.GREEN);
		canvas.drawText("Pitch", 0, 30, paint);
		paint.setTextSize(50);
		canvas.drawText("Hz", 350, 700, paint);

		
		paint.setColor(Color.RED);
		paint.setStrokeWidth(2);
		if(pitchTimeLine[pitchTimeLinePosition]>0){
			canvas.drawLine(0, 570-pitchTimeLine[pitchTimeLinePosition], 799, 570-pitchTimeLine[pitchTimeLinePosition], paint);
		}
				
		paint.setColor(Color.GREEN);
		for(int i=0; i<17; i++){
			canvas.drawLine(25, 470-i*25, 35, 470-i*25, paint);
		}
		paint.setTextSize(12);
		canvas.drawText("100", 0, 475, paint);
		canvas.drawText("200", 0, 375, paint);
		canvas.drawText("300", 0, 275, paint);
		canvas.drawText("400", 0, 175, paint);
		canvas.drawText("500", 0, 75, paint);
		
		paint.setStrokeWidth(8);
		canvas.drawLine(40+pitchTimeLinePosition*2, 545, 40+pitchTimeLinePosition*2, 565, paint);
	}

	private byte[] toBytes(int a) {
		byte[] bs = new byte[4];
		bs[3] = (byte) (0x000000ff & (a));
		bs[2] = (byte) (0x000000ff & (a >>> 8));
		bs[1] = (byte) (0x000000ff & (a >>> 16));
		bs[0] = (byte) (0x000000ff & (a >>> 24));
		return bs;
	}

	private float getFreq(int peakIndex){
		if(peakIndex != 0){
			return 44100.0f/peakIndex;
		}
		else{
			return -1;
		}
	}
	
	private String getPitchNote(float freq){
		if((freq < 16.352f) | (freq > 31608.5)){
			return "N/A";
		}
		else{
			int octaveNumber = 10;
			int note = 0;			
			for(int i=1; i<11; i++){
				if(freq < freqOfCArray[i]){
					octaveNumber = i-1;
					break;
				}				
			}
			float freqRatio = freq/freqOfCArray[octaveNumber];
			for(int i=0; i<12; i++){
				if(freqRatio < noteRangeArray[i]){
					note = i;
					break;
				}				
			}
			if(freqRatio >= noteRangeArray[11]){
				octaveNumber +=1;
			}
			return noteArray[note].concat(String.valueOf(octaveNumber));			
		}
	}
	
	public void setModeSelector(int i){
		modeSelector = i;
	}
}