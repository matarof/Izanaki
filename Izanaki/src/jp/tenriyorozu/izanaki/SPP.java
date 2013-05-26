package jp.tenriyorozu.izanaki;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
 
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.util.Log;

public class SPP implements Runnable{

	private UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    
	private static final String LOG_TAG ="BT_Arduino";
	private BluetoothDevice btDevice;
    private BluetoothSocket btSocket;
    private BluetoothAdapter btAdapter;
    private OutputStream outStream;
	
    private boolean isBluetoothConnected;
	
	public SPP() {
		// TODO Auto-generated constructor stub
		isBluetoothConnected = false;
		btAdapter = BluetoothAdapter.getDefaultAdapter();
        // ペアリング済みのデバイス一覧を取得
        Set<BluetoothDevice> btDeviceSet = btAdapter.getBondedDevices();
        Iterator<BluetoothDevice> it = btDeviceSet.iterator();
         
        if(it.hasNext()){
            // とりあえず最初にマッチしたもの。本来はちゃんとデバイス判定しないとダメ
            btDevice = it.next();
            Log.d(LOG_TAG, "btAddr = " + btDevice.getAddress());
        }
             
	}
	
	public void run(){
        try {
            
        	btSocket = btDevice.createRfcommSocketToServiceRecord(uuid);
            Log.d(LOG_TAG, "btSocket available");  
            btAdapter.cancelDiscovery();  //connectの前に必ずdiscovery処理をキャンセルする
            Log.d(LOG_TAG, "btAdapter discovery canceled");
            btSocket.connect();
            Log.d(LOG_TAG, "btSocket connected");  
            outStream = btSocket.getOutputStream();
            Log.d(LOG_TAG, "btSocket OutputStream available");
            isBluetoothConnected = true;
        } catch (IOException e) {
            e.printStackTrace();
            try {
                btSocket.close();
            } catch (IOException closeException) { }
        }
	}
	
	public void write(byte[] i){
		try{
			outStream.write(0x0A);
			outStream.write(i);
		}catch(IOException e){
			Log.e(LOG_TAG, "Esception during write", e);
		}
	}

	public boolean isBluetoothConnected(){
		return isBluetoothConnected;
	}
}
