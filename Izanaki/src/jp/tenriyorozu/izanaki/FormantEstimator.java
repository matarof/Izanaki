package jp.tenriyorozu.izanaki;

import java.util.Arrays;
import java.util.TreeMap;

import org.ejml.data.Complex64F;
import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.EigenDecomposition;
import org.ejml.factory.DecompositionFactory;

public class FormantEstimator {
	
	double[] coefficients;
	Complex64F[] roots;
	int cn;
	int fs;
	double formants[];
	
	public FormantEstimator(double[] c, int fs) {
		// TODO Auto-generated constructor stub
		this.coefficients = c;
		this.cn = coefficients.length;
		this.roots=this.polynomialRootSolver(this.coefficients);
		this.fs = fs;
		this.formants = new double[cn];
		this.doFormantEstimate();
	}

	private void doFormantEstimate(){
		
		TreeMap<Double, Integer> sorter = new TreeMap<Double, Integer>(); //添字付きソートのためのツリーマップ
		
		int an=0;
		for(int i=0; i<cn-1; i++){
			if (roots[i].getImaginary() >= 0){
				sorter.put(Math.atan2(roots[i].getImaginary(), roots[i].getReal()), an);
				an++;
			}
		}
		
		Integer[] indices = sorter.values().toArray(new Integer[0]);
		Double[] angz = sorter.keySet().toArray(new Double[0]);
		
		double[] frqs = new double[an];
		double[] bw = new double[an];

		int fn = 0;
		for(int i=0; i<an-2; i++){
			
			frqs[i] = angz[i]*(fs/(2*Math.PI));
			bw[i] = -1/2*(fs/(2*Math.PI))* Math.log(roots[indices[i]].getMagnitude()); 
			
			if(frqs[i]>90 && bw[i]<400){
				this.formants[fn] = frqs[i];
				fn++;
			}
		}
	
	}
	
	private Complex64F[] polynomialRootSolver(double[] coefficients){
		
		int n = coefficients.length-1;
		DenseMatrix64F c = new DenseMatrix64F(n,n);
		
		double a = coefficients[n];
		for(int i=0; i<n; i++){
			c.set(i,n-1,-coefficients[i]/a);
		}
		for(int i=1; i<n; i++){
			c.set(i,i-1,1);
		}
		
		EigenDecomposition<DenseMatrix64F> evd = DecompositionFactory.eig(n, false);
				
		evd.decompose(c);
		
		Complex64F[] rts = new Complex64F[n];
		
		for(int i=0; i<n; i++){
			rts[i] = evd.getEigenvalue(i);
		}
		
		return rts;
	}

	public double[] getFormants(){
		return formants;
	}

}
