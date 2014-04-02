package jp.tenriyorozu.izanaki;

public class LPCParamContainer {  //LPCパラメーターと誤差関数を格納する戻り値用クラス
	double[] a;
	double[] converted;
	double sigma2;
	public LPCParamContainer(double[] a, double sigma2, int LPCOrder, int fftSize) {
		// TODO Auto-generated constructor stub
		this.a = a;
		this.sigma2 = sigma2;
		
		this.converted = new double[fftSize];
		for(int i=0; i<LPCOrder+1; i++){
			converted[i]=a[i];
		}
	}

	public double[] getLPCParam(){
		return a;
	}
	
	public double[] getLPCParamZeroFilled(){
		return converted;
	}
	
	public double getLPCSigma2(){
		return sigma2;
	}
	
}
