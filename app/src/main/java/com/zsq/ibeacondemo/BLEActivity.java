package com.zsq.ibeacondemo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.zsq.ibeacondemo.iBeaconClass.iBeacon;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BLE 低功耗蓝牙
 * 
 * @author Administrator
 *
 */
public class BLEActivity extends Activity{
	
	private BluetoothAdapter mBluetoothAdapter;
	private TextView mTvBLEScan;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_ble);
		
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		initView();

	}

	private void initView() {
		mTvBLEScan = (TextView) findViewById(R.id.tv_ble_scan);
		mLvIBeacons = (ListView) findViewById(R.id.lv_ibeacons);
	}

	protected void onDestroy() {
		super.onDestroy();
		stopTimer();
	};

	
	//--------------------------------检测是否支持BLE------------------------------
	
	public void onClick_judge_iBeacon_support(View view){
		if (checkBleObserverSupport()) {
			Utils.showToast(this, "支持接收蓝牙低功耗广播");
		} else {
			Utils.showToast(this, "不支持接收蓝牙低功耗广播");
		}
	}
	
	public void onClick_judge_iBeacon_support2(View view){
		if (checkBleBroadcasterSupport()) {
			Utils.showToast(this, "支持发送iBeacon信息");
		} else {
			Utils.showToast(this, "不支持发送iBeacon消息");
		}
	}

	public void onClick_to_second_activity(View view){
		Intent intent = new Intent(this,SecondActivity.class);
		startActivity(intent);
	}
	
	
	/**
	 * 判断是否支持蓝牙低功耗广播(4.3+)<br>
	 *
	 * 如果返回true,说明支持iBeacon消息的接收
	 * @return 
	 */
	public boolean checkBleObserverSupport() {
	    return this.getPackageManager().hasSystemFeature(
	            "android.hardware.bluetooth_le");
	}

	/**
	 * 判断是否支持LE<br>
	 * 
	 * 如果返回ture,说明支持iBeacon 的发送
	 * @return
	 */
	@SuppressLint("NewApi")
	public boolean checkBleBroadcasterSupport() {
	    if(this.mBluetoothAdapter == null) {
	        return false;
	    }
	    if (Build.VERSION.SDK_INT >= 21) {
	    	// .isMultipleAdvertisementSupported() 这里报错,需要用5.0以上编译
	        if (this.mBluetoothAdapter.isMultipleAdvertisementSupported()) {
	            Log.d("debug", "support peripheral mode, api support");
	            return true;
	        } else if (null != mBluetoothAdapter.getBluetoothLeAdvertiser()) {
	            Log.d("debug",
	                    "support peripheral mode, BluetoothLeAdvertiser is not null");
	            return true;
	        }
	    }
	    Log.d("TAG", "Build.VERSION.SDK_INT = "+Build.VERSION.SDK_INT);
	    Log.d("TAG", "this device not support peripheral mode");
	    return false;
	}

	//-----------------------扫描Ble--------------------------
	
	/**
	 * "扫描ibeacon设备"按钮被点击时,此方法被调用
	 * @param view
	 */
	public void onclick_scan(View view){
		scanBLE();
		openRemoveTimeOutDevicesTimer();
	}
	
	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	public void scanBLE(){
		mTvBLEScan.setText("");
		mBluetoothAdapter.startLeScan(mLeScanCallback);
	}
	
	
	@SuppressLint("NewApi")
	private BluetoothAdapter.LeScanCallback mLeScanCallback =  new BluetoothAdapter.LeScanCallback() {  
  
        @Override  
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
        	mTvBLEScan.setText("搜索到:device"+device+"; rssi:"+rssi + "; scanRecord:"+scanRecord+"\n");
            final iBeaconClass.iBeacon ibeacon = iBeaconClass.fromScanData(device,rssi,scanRecord);

            addDevicesToMap(ibeacon);
            updateLvContent();
        }
    };

	ListView mLvIBeacons;
	private void updateLvContent(){
		//这里是要显示在listview上的内容(唯一内容)
		Object[] array = mBleDeviceMaps.values().toArray();
		String[] objects = new String[array.length];
		for (int i =0; i< array.length;i++){
			objects[i] = array[i].toString();
		}
		//参1,上下文
		//参2,布局文件的资源id
		//参3,textview的资源id(否则系统不知道显示到哪里)
		//参4,字符串数组(要显示的内容集合)
		mLvIBeacons.setAdapter(new ArrayAdapter<String>(this, R.layout.item_db_arrayadpter_listview, R.id.tv_ble_content, objects));
	}
    
    private ConcurrentHashMap<String, iBeacon> mBleDeviceMaps = new ConcurrentHashMap<String, iBeacon>();
    private ConcurrentHashMap<String, Long> mBlePutTime = new ConcurrentHashMap<String, Long>();
	private ConcurrentHashMap<String, Integer> mBleRssi = new ConcurrentHashMap<String, Integer>();
    private void addDevicesToMap(iBeacon device){
    	if(device==null) { Log.d("TAG", "device==null "); return; }
		// 只搜索与我们自己布置的iBeacon  // TODO 章鱼彩票测试的时候,可以先把这段代码去掉
		if (device.name != null){
			if (!(device.name.contains("zsq"))){
				Log.d("TAG","if (!(device.name.contains(\"zsq\"))){");
				return;
			}
		}else { return; }

        // 键值对存起来
    	// 接收到一个信号,如果没有,就存入
    		// 通过address比较
    		// 如果已有就更新
    		// 如果长时间不更新,就删除
    			// 用另一个map记录时间
    				// 如果3秒没有信号
    				// 删除两个map中的数据
    	
    	mBleDeviceMaps.put(device.bluetoothAddress, device);
    	mBlePutTime.put(device.bluetoothAddress, System.currentTimeMillis());
		mBleRssi.put(device.bluetoothAddress, device.rssi);
    }

	//--------------------------------定时维护集合----------------------------
    
    Timer mRemoveTimeOutDevicesTimer;
    private void openRemoveTimeOutDevicesTimer(){
    	mRemoveTimeOutDevicesTimer = new Timer();
    	mRemoveTimeOutDevicesTimer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				removeTimeOutDevices();
			}
		}, 1000, 1000);
    }

	/**
	 * 3秒内如果没有再次收到该信号,则直接去除该设备
	 */
	private void removeTimeOutDevices(){
		// 循环map集合
		// 挨个取出集合中的时间,与当前时间比对
		// 超过三秒(3000毫秒),则以key为键,删除两个集合中的对应数据
		try{
			for (String deviceBluetoothAddress : mBlePutTime.keySet()) {
				if ((System.currentTimeMillis() - mBlePutTime.get(deviceBluetoothAddress)) >= 3000) {
					mBlePutTime.remove(deviceBluetoothAddress);
					mBleDeviceMaps.remove(deviceBluetoothAddress);
					mBleRssi.remove(deviceBluetoothAddress);
				}else {
				}
			}
		}catch (ConcurrentModificationException e){ // 并发修改的问题,跳过即可,这个不打紧
			e.printStackTrace();
		}

	}
    
    private void stopTimer(){
    	mRemoveTimeOutDevicesTimer.cancel();
    }

	//---------------------------工具函数------------------------------------

	public String[] sortMapvalues(ConcurrentHashMap map){

		// 排序hashmap的值
		List<Map.Entry<String, Integer>> retArray = null;
		retArray = new ArrayList<Map.Entry<String, Integer>>(map.entrySet());
		Collections.sort(retArray, new Comparator<Map.Entry<String, Integer>>()
		{
			public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2)
			{
				if(o2.getValue()!=null&&o1.getValue()!=null&&o2.getValue().compareTo(o1.getValue())>0){
					return 1;
				}else{
					return -1;
				}
			}
		});

		// 按顺序取出另个集合中的iBeacon列表
		String ret[] = new String[retArray.size()];
		for (int i = 0; i<retArray.size(); i++){
			iBeacon iB = mBleDeviceMaps.get(retArray.get(i).getKey());
			if (iB != null){
				ret[i] = iB.getBluetoothAddress();
			}
		}
		return ret;
	}


	//----------------------请求网络,判断当前位置---------------------------

	private static final String POST_LOCAL = "http://www.funhainan.cn:9008/api/iBeacon/IBeaconQuery";
	public void onclick_post_channel(View view){
		if (mBleDeviceMaps == null || mBleDeviceMaps.size() <= 0){
			Toast.makeText(this,"请先扫描周围的蓝牙设备",Toast.LENGTH_SHORT).show();
			return;
		}
		Toast.makeText(BLEActivity.this, "请求URL为:"+POST_LOCAL, Toast.LENGTH_SHORT).show();

		JSONObject paramObj = makeJsonParam();
		Log.e("TAG","请求的参数为:"+paramObj.toString());
		Toast.makeText(BLEActivity.this, "请求参数为: "+paramObj.toString(), Toast.LENGTH_LONG).show();

		Http_ED.sendHttpRequestForPost(POST_LOCAL, paramObj.toString(), new Http_ED.HttpCallbackListener() {
			@Override
			public void onFinish(final String response) {
				Log.e("TAG","请求成功"+response.toString());
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Toast.makeText(BLEActivity.this, "请求成功"+response.toString(), Toast.LENGTH_LONG).show();
						Toast.makeText(BLEActivity.this, "投注的时候,把chanelID作为参数之一传过去,用于以后的渠道分润", Toast.LENGTH_LONG).show();
					}
				});
			}

			@Override
			public void onError(Exception e) {
				e.printStackTrace();
			}
		});
	}

	private JSONObject makeJsonParam() {
		String retJson = null;
		String[] objects = sortMapvalues(mBleRssi); // 按信号远近进行排序
		JSONObject ibeacon = new JSONObject();
		JSONArray iBeaconList = new JSONArray();
		for (int i = 0; i < objects.length; i++) {
			// 最多四条
			if (i > 3){
				continue;
			}
			// 生成每条内容
			try {
				iBeacon ibe = mBleDeviceMaps.get(objects[i]);
				ibeacon.put("name", ibe.name);
				ibeacon.put("uuid", ibe.proximityUuid);
				ibeacon.put("address", ibe.bluetoothAddress);
				ibeacon.put("major", ibe.major);
				ibeacon.put("minor", ibe.minor);
				ibeacon.put("txPower", ibe.txPower);
				ibeacon.put("rssi", ibe.rssi);
			} catch (JSONException e3) {
				e3.printStackTrace();
			}
			iBeaconList.put(ibeacon);
			ibeacon = new JSONObject();
		}

		JSONObject paramObj = new JSONObject();
		try {
			paramObj.put("appid", "kkksss");
			paramObj.put("userid", "23");
			paramObj.put("ibeacon", iBeaconList);
			paramObj.put("type", "1");
		} catch (JSONException e1) {
			e1.printStackTrace();
		}
		return  paramObj;
	}
}
