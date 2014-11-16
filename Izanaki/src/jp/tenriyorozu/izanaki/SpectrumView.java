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
	private int pitchTimeLine[] = new int[540];
	private int pitchTimeLinePosition = 0;
	private int modeSelector = 2;
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
	public void surfaceDraw(double buffer1[], int bufSize, double buffer2[], int bufSize2, double buffer3[], int bufSize3, int peakIndex, double[] formants){
		
		 if (!isAttached) {      //surfaceCreatedが呼ばれる前にlockCanvasするとエラーとなるためチェック
	            return;  
	        }  
		
		SurfaceHolder holder = getHolder();
		Canvas canvas = holder.lockCanvas();
		float freq = getFreq(peakIndex);
		String note = getPitchNote(freq);
		String vowel = getVowel(formants);
		pitchTimeLine[pitchTimeLinePosition] = (int)freq;
		switch(modeSelector){
		case 0:
			doSpectrumDraw(canvas, buffer1, bufSize, buffer2, bufSize2, buffer3, bufSize3, peakIndex, freq, note, formants, vowel);
			break;
		case 1:
			doTimeLineDraw(canvas, peakIndex, freq, note, pitchTimeLine, pitchTimeLinePosition);
			break;
		case 2:
			doFormantGraphDraw(canvas, freq, note, formants);
		}
		holder.unlockCanvasAndPost(canvas);
		pitchTimeLinePosition++;
		if(pitchTimeLinePosition == 250){
			pitchTimeLinePosition = 0;
		}
		
		if(spp.isBluetoothConnected() & freq!=-1){
			byte[] send = toBytes((int)freq);
			spp.write(send);
		}
		
	}

	private void doSpectrumDraw(Canvas canvas, double buffer1[], int bufSize1, double buffer2[], int bufSize2,  double buffer3[], int bufsize3, int peakIndex, float freq, String note, double[] formants, String vowel){

		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setStrokeWidth(1);
		canvas.drawColor(Color.BLACK);
								
		for(int i=0; i<bufSize2; i++){
			paint.setColor(Color.GREEN);
			paint.setStrokeWidth(1);
			int y1 = (int)(Math.log10(buffer1[i])*20)*2;	
			canvas.drawLine(i, 500, i, 500-y1, paint);  //PowerSpectrum描画
			//canvas.drawPoint(i, 500-y1, paint);
			int y2 =(int)(buffer2[i]*1.0E1);  
			canvas.drawLine(i, 100, i, 100-y2, paint);  //CMSDF描画
			paint.setColor(Color.MAGENTA);
			int y3 = (int)(buffer3[i]*2.5);	
			//canvas.drawLine(i, 500, i, 470-y3-80, paint);  //LPC Spectrum描画
			paint.setStrokeWidth(3);
			canvas.drawPoint(i, 500-y3-80, paint);
		}
		
			
		paint.setTextSize(90);
		paint.setColor(Color.GREEN);
		if(freq != -1){
			canvas.drawText(String.valueOf((int)freq), 20, 700, paint);
		}
		else{
			canvas.drawText("--", 20, 700, paint);
		}
		
		paint.setTextSize(44);
		paint.setColor(Color.LTGRAY);
		canvas.drawText(note, 190, 590, paint);
		
		DecimalFormat exFormat = new DecimalFormat("###.#"); 
		//String strRa = exFormat.format(ra);
		String strFor0 = exFormat.format(formants[0]);
		String strFor1 = exFormat.format(formants[1]);
		String strFor2 = exFormat.format(formants[2]);
		String strFor3 = exFormat.format(formants[3]);
		String strFor4 = exFormat.format(formants[4]);
		
		paint.setTextSize(30);
		paint.setColor(Color.CYAN);
		//canvas.drawText(strRa, 380, 580, paint);
		canvas.drawText(strFor0, 380, 610, paint);
		canvas.drawText(strFor1, 380, 640, paint);
		canvas.drawText(strFor2, 380, 670, paint);
		canvas.drawText(strFor3, 380, 700, paint);
		canvas.drawText(strFor4, 380, 730, paint);
		canvas.drawText(vowel, 380, 760, paint);
		
		paint.setTextSize(24);
		paint.setColor(Color.CYAN);
		//canvas.drawText("HNR", 300, 580, paint);
		canvas.drawText("F1", 300, 610, paint);
		canvas.drawText("F2", 300, 640, paint);
		canvas.drawText("F3", 300, 670, paint);
		canvas.drawText("F4", 300, 700, paint);
		canvas.drawText("F5", 300, 730, paint);
		paint.setColor(Color.GREEN);
		canvas.drawText("CMSDF", 20, 30, paint);
		canvas.drawText("PowerSpectrum + LPC Spectrum", 20, 230, paint);
		canvas.drawText("F0", 10, 580, paint);
		paint.setTextSize(36);
		canvas.drawText("Hz", 190, 700, paint);

		
		
		paint.setStrokeWidth(3);  //マーカー描画
		if(peakIndex != 0){
			paint.setColor(Color.RED);
			canvas.drawLine(peakIndex, 0, peakIndex, 200, paint);
			paint.setColor(Color.BLUE);
			canvas.drawLine((int)formants[0]/(44100/fftsize), 300, (int)formants[0]/(44100/fftsize), 500, paint);
			canvas.drawLine((int)formants[1]/(44100/fftsize), 300, (int)formants[1]/(44100/fftsize), 500, paint);
			canvas.drawLine((int)formants[2]/(44100/fftsize), 300, (int)formants[2]/(44100/fftsize), 500, paint);
			canvas.drawLine((int)formants[3]/(44100/fftsize), 300, (int)formants[3]/(44100/fftsize), 500, paint);
			canvas.drawLine((int)formants[4]/(44100/fftsize), 300, (int)formants[4]/(44100/fftsize), 500, paint);
			canvas.drawLine((int)formants[5]/(44100/fftsize), 300, (int)formants[5]/(44100/fftsize), 500, paint);
			
			paint.setColor(Color.RED);
			canvas.drawLine(freq/(44100/fftsize), 300, freq/(44100/fftsize), 500, paint);
			
		}

		
	}
	
	private void doTimeLineDraw(Canvas canvas, int peakIndex, float freq, String note, int pitchTimeLine[], int pitchTimeLinePosition){

		Paint paint = new Paint();
		paint.setColor(Color.GREEN);
		paint.setAntiAlias(true);
		canvas.drawColor(Color.BLACK);
								
		paint.setStrokeWidth(4);
		for(int i=0; i<250; i++){
			if(pitchTimeLine[i]>0){
				canvas.drawPoint(40+i*2, 570-pitchTimeLine[i], paint);
			}
		}
		
		paint.setTextSize(90);
		paint.setColor(Color.GREEN);
		if(freq != -1){
			canvas.drawText(String.valueOf((int)freq), 20, 700, paint);
		}
		else{
			canvas.drawText("--", 20, 700, paint);
		}
	
		paint.setTextSize(90);
		paint.setColor(Color.LTGRAY);
		canvas.drawText(note, 300, 700, paint);
		
		paint.setTextSize(24);
		paint.setColor(Color.GREEN);
		canvas.drawText("Pitch", 0, 30, paint);
		paint.setTextSize(36);
		canvas.drawText("Hz", 190, 700, paint);

		
		paint.setColor(Color.RED);
		paint.setStrokeWidth(2);
		if(pitchTimeLine[pitchTimeLinePosition]>0){
			canvas.drawLine(0, 570-pitchTimeLine[pitchTimeLinePosition], 539, 570-pitchTimeLine[pitchTimeLinePosition], paint);
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

	private void doFormantGraphDraw(Canvas canvas, float freq, String note, double[] formants){

		Paint paint = new Paint();
		paint.setColor(Color.GREEN);
		paint.setAntiAlias(true);
		canvas.drawColor(Color.BLACK);
			
		paint.setTextSize(90);
		paint.setColor(Color.GREEN);
		if(freq != -1){
			canvas.drawText(String.valueOf((int)freq), 20, 700, paint);
		}
		else{
			canvas.drawText("--", 20, 700, paint);
		}
	
		paint.setTextSize(90);
		paint.setColor(Color.LTGRAY);
		canvas.drawText(note, 300, 700, paint);
		
		paint.setTextSize(24);
		paint.setColor(Color.GREEN);
		canvas.drawText("F1", 490, 620, paint);
		canvas.drawText("F2", 0, 30, paint);
		paint.setTextSize(36);
		canvas.drawText("Hz", 190, 700, paint);

		
		paint.setColor(Color.RED);
		paint.setStrokeWidth(8);
		canvas.drawPoint(50+(int)formants[0]/2, 570-((int)formants[1]/6), paint);
		
		paint.setColor(Color.GREEN);
		paint.setStrokeWidth(2);
		for(int i=0; i<21; i++){
			canvas.drawLine(40, 570-i*25, 50, 570-i*25, paint);
			canvas.drawLine(50+i*25, 580, 50+i*25, 570, paint);
		}
		paint.setTextSize(12);
		canvas.drawText("600", 0, 475, paint);
		canvas.drawText("1200", 0, 375, paint);
		canvas.drawText("1800", 0, 275, paint);
		canvas.drawText("2400", 0, 175, paint);
		canvas.drawText("3000", 0, 75, paint);

		canvas.drawText("200", 140, 595, paint);
		canvas.drawText("400", 240, 595, paint);
		canvas.drawText("600", 340, 595, paint);
		canvas.drawText("800", 440, 595, paint);

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
	
	private String getVowel(double[] formants){
//		if (formants[0] > 600 && formants[0] < 1400 && formants[1] > 900  && formants[1] < 2000) return "あ";
//		if (formants[0] > 100 && formants[0] < 410  && formants[1] > 1900 && formants[1] < 3500) return "い";
//		if (formants[0] > 100 && formants[0] < 700  && formants[1] > 1100 && formants[1] < 2000) return "う";
//		if (formants[0] > 400 && formants[0] < 800  && formants[1] > 1700 && formants[1] < 3000) return "え";
//		if (formants[0] > 300 && formants[0] < 900  && formants[1] > 500  && formants[1] < 1300) return "お";
		
//		if (formants[0] > 600 && formants[0] < 1200 && formants[1] > 1000  && formants[1] < 2300) return "あ";
//		if (formants[0] > 200 && formants[0] < 400  && formants[1] > 2000 && formants[1] < 4000) return "い";
//		if (formants[0] > 250 && formants[0] < 800  && formants[1] > 1800 && formants[1] < 3000) return "う";
//		if (formants[0] > 200 && formants[0] < 500  && formants[1] > 1100 && formants[1] < 2100) return "え";
//		if (formants[0] > 400 && formants[0] < 800  && formants[1] > 500  && formants[1] < 1700) return "お";
		
		if (formants[0] > 600 && formants[0] < 1200 && formants[1] > 1000 && formants[1] < 2300) return "あ";
		if (formants[0] > 200 && formants[0] < 400  && formants[1] > 2000 && formants[1] < 4000) return "い";
		if (formants[0] > 220 && formants[0] < 500  && formants[1] > 1000 && formants[1] < 1800) return "う";
		if (formants[0] > 400 && formants[0] < 650  && formants[1] > 2100 && formants[1] < 2800) return "え";
		if (formants[0] > 350 && formants[0] < 650  && formants[1] > 500  && formants[1] < 1000) return "お";
		
		return "-";
		
	}
}