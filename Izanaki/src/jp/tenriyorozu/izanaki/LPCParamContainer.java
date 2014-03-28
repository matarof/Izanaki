package jp.tenriyorozu.izanaki;

public class LPCParamContainer {
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

	public double [] getLPCParam(){
		return converted;
	}
	
	public double getLPCSigma2(){
		return sigma2;
	}
	
}
