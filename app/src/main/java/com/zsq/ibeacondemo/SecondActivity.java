package com.zsq.ibeacondemo;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

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

public class SecondActivity extends AppCompatActivity {

    private TextView textView;
    private BluetoothAdapter mBluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        textView = (TextView) findViewById(R.id.text);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        scan_devices();
    }

    public void onClick_btn(View view){
        JSONObject paramObj = makeJsonParam();
        Log.e("TAG","请求的参数为:"+paramObj.toString());
        Toast.makeText(SecondActivity.this, "请求参数为: "+paramObj.toString(), Toast.LENGTH_SHORT).show();
    }

    //-----------------------扫描Ble--------------------------

    /**
     * "扫描ibeacon设备"按钮被点击时,此方法被调用
     */
    public void scan_devices(){
        scanBLE();
//        openRemoveTimeOutDevicesTimer();
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public void scanBLE(){
        mBluetoothAdapter.startLeScan(mLeScanCallback);
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

    @SuppressLint("NewApi")
    private BluetoothAdapter.LeScanCallback mLeScanCallback =  new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            final iBeaconClass.iBeacon ibeacon = iBeaconClass.fromScanData(device,rssi,scanRecord);

            addDevicesToMap(ibeacon);

        }
    };

    private ConcurrentHashMap<String, iBeaconClass.iBeacon> mBleDeviceMaps = new ConcurrentHashMap<String, iBeaconClass.iBeacon>();
    private ConcurrentHashMap<String, Long> mBlePutTime = new ConcurrentHashMap<String, Long>();
    private ConcurrentHashMap<String, Integer> mBleRssi = new ConcurrentHashMap<String, Integer>();
    private void addDevicesToMap(iBeaconClass.iBeacon device){
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
            iBeaconClass.iBeacon iB = mBleDeviceMaps.get(retArray.get(i).getKey());
            if (iB != null){
                ret[i] = iB.getBluetoothAddress();
            }
        }
        return ret;
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
                iBeaconClass.iBeacon ibe = mBleDeviceMaps.get(objects[i]);
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
