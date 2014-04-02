package jp.tenriyorozu.izanaki;

import org.ejml.data.Complex64F;
import org.ejml.data.DenseMatrix64F;
import org.ejml.factory.EigenDecomposition;
import org.ejml.factory.DecompositionFactory;

public class PolynomialRootSolver {
	
	double[] coefficients;
	
	
	public PolynomialRootSolver(double[] c) {
		// TODO Auto-generated constructor stub
		this.coefficients = c;
	}

	public static Complex64F[] findRoots(double[] coefficients){
		
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
		
		Complex64F[] roots = new Complex64F[n];
		
		for(int i=0; i<n; i++){
			roots[i] = evd.getEigenvalue(i);
		}
		
		return roots;
	}
}
