package jp.tenriyorozu.izanaki;

import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.util.AttributeSet;
import android.view.Menu;
import android.util.Log;
import android.view.MenuItem;


public class MainActivity extends Activity {

	SpectrumView SV;
	SPP spp;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		spp = new SPP();
		SV = new SpectrumView(this, spp);
		setContentView(SV);
						
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
	    case R.id.Bluetooth:
	        if(spp.isBluetoothConnected() == false){	        	
	        	Thread thread = new Thread(spp);
	    		thread.start();
	        	Log.d("BTbuttun","is on");
	        }
	        else{
	        	//socketクローズ処理を記述
	        	Log.d("BTbuttun","is off");
	        }
	        return true;
	    case R.id.Spectrum:
	    	SV.setModeSelector(0);
	    	return true;
	    case R.id.Timeline:
	    	SV.setModeSelector(1);
	    	return true;
	    case R.id.Formant:
	    	SV.setModeSelector(2);
	    	return true;
	    case R.id.Shutdown:
	        SV.stopVoiceCapt();
	    	finish();
	        return true;
	    }
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onPause(){
		super.onPause();
					
	}	
	
	@Override
	protected void onResume(){
		super.onResume();
					
	}	

}

