package jp.tenriyorozu.izanaki;
import java.util.Arrays;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.TimingLogger;


import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;


public class VoiceCapt implements Runnable {


	private static final int SAMPLING_RATE = 44100;  //AVDでは8000, xperiaでは44100
	private boolean isRecording = false;
	private int bufSize;
	private int fftSize;
	AudioRecord audioRec = null;
	private double[] window;
	private int LPCOrder=35; //LPC次数
	
	
	public SpectrumDraw spectrumDrawListner;
	
	public void setSpectrumDrawListner(SpectrumDraw listner){
		this.spectrumDrawListner = listner;
	}

	public VoiceCapt() {
		// TODO Auto-generated constructor stub
		bufSize = AudioRecord.getMinBufferSize(
				SAMPLING_RATE,
				AudioFormat.CHANNEL_IN_MONO,
				AudioFormat.ENCODING_PCM_16BIT);  //*2しないほうがデータ読み待ちがなくてよさそう  tuned!!
		audioRec = new AudioRecord(
				MediaRecorder.AudioSource.MIC,
				SAMPLING_RATE,
				AudioFormat.CHANNEL_IN_MONO,
				AudioFormat.ENCODING_PCM_16BIT,
				bufSize);		
		fftSize = 2048;;  //2の倍数で実装すること tuned@2048!! 
		double d = 1/fftSize;
		window = new double[fftSize];
		for(int i=0; i<fftSize; i++){
			window[i] = 0.54-0.46*Math.cos(2*(Math.PI)*d*i);  // hamming window	を作成			
		}
/*		double d = 1/bufSize;
		window = new double[bufSize];
		for(int i=0; i<bufSize; i++){
			window[i] = 0.54-0.46*Math.cos(2*(Math.PI)*d*i);  // hamming window	を作成			
		}*/
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		audioRec.startRecording();
		isRecording = true;

		short[] readbuffer = new short[bufSize];
//		short[] revshort = new short[bufSize];  //エンディアン変換用配列 使用せず
		double[] data = new double[fftSize];  //計算対象フレーム
		double[] power = new double[fftSize/2]; 
		double[] acf = new double[fftSize*2];
//		double[] brt_acf = new double[fftSize];
		double[] m = new double[fftSize]; //SDFの2乗項計算用配列
//		double[] NSDF = new double[fftSize];
		double[] CMNDF = new double[fftSize/2]; //Cumulative Mean Normalized Difference Function
		double[] SUM_CMNDF = new double[fftSize/2];
		double[] LPCSpectrum = new double[fftSize/2]; //LPCスペクトラム格納用配列
		double[] a = new double[fftSize*2]; //LPCパラメーター
		double[] b = new double[LPCOrder+1];
		double sigma2; 
		double[] formants;
		
		DoubleFFT_1D fft = new DoubleFFT_1D(fftSize);
				
		while(isRecording){
			TimingLogger logger = new TimingLogger("TAG_TEST", "testTimingLogger");
			audioRec.read(readbuffer, 0, bufSize);
			logger.addSplit("buffer loaded");
/*			for(int i=0; i<bufSize; i++){		//エンディアン変換 今回は無効
				revshort[i]= Short.reverseBytes(buffer[i]);
			}	*/
		
			for(int i=0; i<fftSize; i++){
				data[i] = (double)readbuffer[i] * window[i];  // コンストラクタで作成したwindowで切り出し	
/*			Arrays.fill(data, 0);
			for(int i=0; i<bufSize; i++){
				data[i] = (double)readbuffer[i] * window[i]; */ // コンストラクタで作成したwindowで切り出し				
			}
			
//			brt_acf = bruteForceACF(data, fftSize);   //検算用　自己相関関数を直接計算

			m[fftSize-1]= data[0]*data[0]+data[fftSize-1]*data[fftSize-1];	//SDFの2乗項を作成
			for(int i=fftSize-2; i>=0; i--){
				m[i] = m[i+1]+data[fftSize-1-i]*data[fftSize-1-i]+data[i]*data[i];
			}
			
			
			fft.realForward(data);  //FFT
			logger.addSplit("FFT");
			acf[0] = data[0]*data[0];
//			acf[0] = 0;  //For statistical convention, zero out the mean
			for(int i=0; i<fftSize/2; i++){
				power[i] = data[i*2]*data[i*2] + data[i*2+1]*data[i*2+1]; //パワースペクトルを計算 Math.pow()よりも直接乗算のほうが高速 tuned!!
				acf[i*2] = power[i]; //ACF計算前データ　実部
				acf[i*2+1]= 0; //ACF計算前データ 虚部は0で埋める　
				acf[fftSize*2-2-i*2]= power[i]; //パワースペクトル反転部分の実部を埋める
				acf[fftSize*2-1-i*2]= 0; //パワースペクトル反転部分の虚部を埋める
			}
			acf[1] = power[fftSize/2-1]; //realInverseのデータ規定 a[1]=Re[n/2]
					
			fft.realInverse(acf, true);  //実領域のみIFFT
			
/*			for(int i=0; i<fftSize/2; i++){  //NSDF計算 ウインドウ幅は1/2
				NSDF[i]=2*acf[i]/m[i];
			}*/
			
			CMNDF[0]=1;		//CMNDF計算
			Arrays.fill(SUM_CMNDF, 0);			
			for(int i=1; i<fftSize/2; i++){
				double d = m[i]-2*acf[i];
				SUM_CMNDF[i]+=SUM_CMNDF[i-1]+d;
				CMNDF[i]=d*i/SUM_CMNDF[i];
			}
			
			for(int i=1; i<fftSize; i++){  //自己相関関数を正規化  ビューに表示しない場合は省略可,levに渡す場合はあったほうがよい
				acf[i] /= acf[0];
			}
			acf[0] = 1;
			
			
			LPCParamContainer LPCParam = Lev(acf);
			a = LPCParam.getLPCParamZeroFilled();
			//sigma2 = LPCParam.getLPCSigma2();
			fft.realForward(a);
			for(int i=0; i<fftSize/2; i++){
				LPCSpectrum[i]=-10*Math.log10(a[i*2]*a[i*2]+a[i*2+1]*a[i*2+1]);  //+20*Math.log10(sigma2); 誤差関数の乗算はピーク検出には不要、高速化のため省略
			}
			
			b = LPCParam.getLPCParam();
			FormantEstimator fE = new FormantEstimator(b, SAMPLING_RATE);
			formants = fE.getFormants();
			
			
			logger.addSplit("ACF");
			int peakIndex = find_dip(CMNDF, fftSize/2);
			logger.addSplit("peak picking");
			double ra = Ra(peakIndex, power, fftSize);
			logger.addSplit("HNR");
			this.spectrumDrawListner.surfaceDraw(power, fftSize/2, CMNDF, fftSize/2, LPCSpectrum, fftSize/2, peakIndex, ra, formants);
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
	
	private int find_peak(double data[],int dataSize){  //単純に最大ピークのインデックス値を返す　未使用
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
	
	private int find_peak2(double data[], int dataSize){  //最大ピーク*thresholdよりも大きなピークのうち最初に出現するインデックス値を返す　acfに使用
		
		int peakIndex = 0; 
		int peakPointer = 0;
		int pending_peakIndex = 1;
		int peakIndexArray[] = new int [dataSize];

		for(int i=1; i<dataSize-1; i++){
			if(data[i] >= 0){	//データが正の範囲にある間
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
		for(int i=0; i<=maxPeakPointer; i++){ //最大ピークのインデックスを求める
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
	

		

	
	private LPCParamContainer Lev(double r[]){  //Levinson Durgin アルゴリズムによるLPCパラメーター計算
		double[] a = new double[LPCOrder+1];  //LPC parameter
		double[] rc = new double[LPCOrder+1];  //PARCOR
		
		rc[0] =(-r[1])/r[0];
		a[0] = (double)1.0;
		a[1] = rc[0];
		double err = r[0] + r[1]*rc[0];
		
		for (int i=2; i<=LPCOrder; i++){
			
			//compute PARCOR
			double s = 0;
			for(int j=0; j<i; j++){
				s += r[i-j]*a[j];  //(*)
			}
			rc[i-1] = -s/err;
			
			//update a[]
			for(int j=1; j<=i/2; j++){
				int l = i-j;
				double at =a[j] + rc[i-1]*a[l];  //at=a_temp
				a[l] += rc[i-1]*a[j];
				a[j] = at;
			}
			a[i] = rc[i-1];
			err += rc[i-1]*s;  //(*)より err*=1.0-(rc[i-1])^2と同義
					
		}
		
		LPCParamContainer LPCParam = new LPCParamContainer(a, err, LPCOrder, fftSize);
		return LPCParam;
		
		
	}
		
	private int find_dip(double data[], int dataSize){  //threshold以下のdipのうち最初に出現するインデックス値を返す
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
	
	private double[] bruteForceACF(double data[], int dataSize){  //検算用　自己相関関数を直接計算
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
