package jp.tenriyorozu.izanaki;
import java.util.Arrays;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.TimingLogger;


import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;

public class VoiceCapt implements Runnable {


	private static final int SAMPLING_RATE = 44100;  //AVD�ł�8000, xperia�ł�44100
	private boolean isRecording = false;
	private int bufSize;
	private int fftSize;
	AudioRecord audioRec = null;
	private double[] window;
	

	
	public SpectrumDraw spectrumDrawListner;
	
	public void setSpectrumDrawListner(SpectrumDraw listner){
		this.spectrumDrawListner = listner;
	}

	public VoiceCapt() {
		// TODO Auto-generated constructor stub
		bufSize = AudioRecord.getMinBufferSize(
				SAMPLING_RATE,
				AudioFormat.CHANNEL_IN_MONO,
				AudioFormat.ENCODING_PCM_16BIT);  //*2���Ȃ��ق����f�[�^�ǂݑ҂����Ȃ��Ă悳����  tuned!!
		audioRec = new AudioRecord(
				MediaRecorder.AudioSource.MIC,
				SAMPLING_RATE,
				AudioFormat.CHANNEL_IN_MONO,
				AudioFormat.ENCODING_PCM_16BIT,
				bufSize);		
		fftSize = 2048;;  //2�̔{���Ŏ������邱�� tuned@2048!! 
		double d = 1/fftSize;
		window = new double[fftSize];
		for(int i=0; i<fftSize; i++){
			window[i] = 0.54-0.46*Math.cos(2*(Math.PI)*d*i);  // hamming window	���쐬			
		}
/*		double d = 1/bufSize;
		window = new double[bufSize];
		for(int i=0; i<bufSize; i++){
			window[i] = 0.54-0.46*Math.cos(2*(Math.PI)*d*i);  // hamming window	���쐬			
		}*/
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		audioRec.startRecording();
		isRecording = true;

		short[] readbuffer = new short[bufSize];
//		short[] revshort = new short[bufSize];  //�G���f�B�A���ϊ��p�z�� �g�p����
		double[] data = new double[fftSize];  //�v�Z�Ώۃt���[��
		double[] power = new double[fftSize/2]; 
		double[] acf = new double[fftSize*2];
//		double[] brt_acf = new double[fftSize];
		double[] m = new double[fftSize]; //SDF��2�捀�v�Z�p�z��
//		double[] NSDF = new double[fftSize];
		double[] CMNDF = new double[fftSize/2]; //Cumulative Mean Normalized Diffrerence Fucntion
		double[] SUM_CMNDF = new double[fftSize/2];

		DoubleFFT_1D fft = new DoubleFFT_1D(fftSize);

		
		while(isRecording){
			TimingLogger logger = new TimingLogger("TAG_TEST", "testTimingLogger");
			audioRec.read(readbuffer, 0, bufSize);
			logger.addSplit("buffer loaded");
/*			for(int i=0; i<bufSize; i++){		//�G���f�B�A���ϊ� ����͖���
				revshort[i]= Short.reverseBytes(buffer[i]);
			}	*/
		
			for(int i=0; i<fftSize; i++){
				data[i] = (double)readbuffer[i] * window[i];  // �R���X�g���N�^�ō쐬����window�Ő؂�o��	
/*			Arrays.fill(data, 0);
			for(int i=0; i<bufSize; i++){
				data[i] = (double)readbuffer[i] * window[i]; */ // �R���X�g���N�^�ō쐬����window�Ő؂�o��				
			}
			
//			brt_acf = bruteForceACF(data, fftSize);   //���Z�p�@���ȑ��֊֐��𒼐ڌv�Z

			m[fftSize-1]= data[0]*data[0]+data[fftSize-1]*data[fftSize-1];	//SDF��2�捀���쐬
			for(int i=fftSize-2; i>=0; i--){
				m[i] = m[i+1]+data[fftSize-1-i]*data[fftSize-1-i]+data[i]*data[i];
			}
			
			
			fft.realForward(data);  //FFT
			logger.addSplit("FFT");
			acf[0] = data[0]*data[0];
//			acf[0] = 0;  //For statistical convention, zero out the mean
			for(int i=0; i<fftSize/2; i++){
				power[i] = data[i*2]*data[i*2] + data[i*2+1]*data[i*2+1]; //�p���[�X�y�N�g�����v�Z Math.pow()�������ڏ�Z�̂ق������� tuned!!
				acf[i*2] = power[i]; //ACF�v�Z�O�f�[�^�@����
				acf[i*2+1]= 0; //ACF�v�Z�O�f�[�^ ������0�Ŗ��߂�@
				acf[fftSize*2-2-i*2]= power[i]; //�p���[�X�y�N�g�����]�����̎����𖄂߂�
				acf[fftSize*2-1-i*2]= 0; //�p���[�X�y�N�g�����]�����̋����𖄂߂�
			}
			acf[1] = power[fftSize/2-1]; //realInverse�̃f�[�^�K�� a[1]=Re[n/2]
					
			fft.realInverse(acf, true);  //���̈�̂�IFFT
			
/*			for(int i=0; i<fftSize/2; i++){  //NSDF�v�Z �E�C���h�E����1/2
				NSDF[i]=2*acf[i]/m[i];
			}*/
			
			CMNDF[0]=1;		//CMNDF�v�Z
			Arrays.fill(SUM_CMNDF, 0);			
			for(int i=1; i<fftSize/2; i++){
				double d = m[i]-2*acf[i];
				SUM_CMNDF[i]+=SUM_CMNDF[i-1]+d;
				CMNDF[i]=d*i/SUM_CMNDF[i];
			}
			
/*			for(int i=1; i<fftSize; i++){  //���ȑ��֊֐��𐳋K��  �r���[�ɕ\�����Ȃ��ꍇ�͏ȗ���
				acf[i] /= acf[0];
			}
			acf[0] = 1;*/
			
			logger.addSplit("ACF");
			int peakIndex = find_dip(CMNDF, fftSize/2);
			logger.addSplit("peak picking");
			double ra = Ra(peakIndex, power, fftSize);
			logger.addSplit("HNR");
			this.spectrumDrawListner.surfaceDraw(power, fftSize/2, CMNDF, fftSize/2, peakIndex, ra);
			logger.addSplit("surface draw");
			logger.dumpToLog();
		}
		audioRec.stop();
		audioRec.release();
	}
	
	public void halt(){
		
		isRecording = false;
		audioRec.stop();
		audioRec.release();
	}
	
	private int find_peak(double data[],int dataSize){  //�P���ɍő�s�[�N�̃C���f�b�N�X�l��Ԃ��@���g�p
		double mv = 0;
		int mi = 0;
		double dy = 0;
		
		for(int i=1; i<dataSize; i++){
			double old_dy = dy;
			dy = data[i] - data[i-1];
			if(old_dy > 0 && dy <= 0){
				if(data[i]>mv){
					mv = data[i];
					mi = i;
				}
			}
		}
		return mi;	
	}
	
	private int find_peak2(double data[], int dataSize){  //�ő�s�[�N*threshold�����傫�ȃs�[�N�̂����ŏ��ɏo������C���f�b�N�X�l��Ԃ��@acf�Ɏg�p
		
		int peakIndex = 0; 
		int peakPointer = 0;
		int pending_peakIndex = 1;
		int peakIndexArray[] = new int [dataSize];

		for(int i=1; i<dataSize-1; i++){
			if(data[i] >= 0){	//�f�[�^�����͈̔͂ɂ����
				if(data[i] > data[pending_peakIndex]){
					pending_peakIndex = i;					
				}	
				if(data[i]*data[i+1]<=0){
				peakIndexArray[peakPointer] = pending_peakIndex;
				peakPointer++;
				}
			}
			else if(data[i]*data[i+1]<=0){
				pending_peakIndex = i+1;
			}
		}
		int maxPeakPointer = peakPointer;
		int maxPeakIndex = 0;		
		for(int i=0; i<=maxPeakPointer; i++){ //�ő�s�[�N�̃C���f�b�N�X�����߂�
			if(data[peakIndexArray[i]]>data[peakIndexArray[maxPeakIndex]]){
				maxPeakIndex = i;
			}
		}
		for(int i=1; i<maxPeakPointer; i++){
			if(data[peakIndexArray[i]]>0.75*data[peakIndexArray[maxPeakIndex]]){
				peakIndex = peakIndexArray[i];
				break;
			}
		}
		return peakIndex;		
	}
	
	private int find_dip(double data[], int dataSize){  //threshold�ȉ���dip�̂����ŏ��ɏo������C���f�b�N�X�l��Ԃ�
		double mv = 0.1; 
		int mi = 0;
		
		for(int i=0; i<dataSize-1; i++){
			if(data[i] < 0.1){
				if(data[i]<mv){
					mv = data[i];	
					mi = i;
				}
				if(data[i+1]>=0.1){
					break;
				}
			}
		}
		return mi;
	}
	
	private double[] bruteForceACF(double data[], int dataSize){  //���Z�p�@���ȑ��֊֐��𒼐ڌv�Z
		double[] autocorr = new double[dataSize];
		Arrays.fill(autocorr, 0);
		for(int k = 0; k<dataSize; k++){
			for(int l = 0; l<dataSize-k; l++){
				autocorr[k] += data[l] * data[l+k];
			}
/*			for(int l = 0; l<dataSize; l++){
				autocorr[k] += data[l] * data[(dataSize + l - k) % dataSize];  // This is a "wrapped" signal processing-style autocorrelation. 
			}
*/		}
		return autocorr;
	}

	private double Ra(int peakIndex, double[] powerSpectrum, int frameSize){
		if(peakIndex != 0){
		float s = 0;
		float total = 0;
		for(int i=0; i<powerSpectrum.length; i++){
			total += powerSpectrum[i];
		}
		for(int i=1; i<peakIndex/2; i++){
			for(int k=0; k<3; k++){
			s += powerSpectrum[i*frameSize/peakIndex-1+k];
			}
		}		
		return 10 * Math.log10(s/(total-s));
		}
		else return 0;
	}
}
