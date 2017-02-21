package com.zsq.ibeacondemo;

import android.bluetooth.BluetoothDevice;

// 在iBbeaconClass类中对其进行数据的解析处理
public class iBeaconClass {  
	/**
	 * 苹果定义了iBeacon其中32位广播的数据格式
	 * <pre>
                +------------------+
                |   Data           |
                | (up to 31 bytes) |
                +--------+---------+
                         |
                         |
+------------------------v---------------------------+
| iBeacon  |Proximity |Major    |Minor    |TX power  |
| prefix   |UUID      |         |         |          |
| (9 bytes)|(16 bytes)|(2 bytes)|(2 bytes)|(2 bytes) |
+----------------------------------------------------+
	 *
	 */
    static public  class iBeacon{  
        public String name;  
        /** 相当于群组号*/
        public int major;
        /** 相当于识别群组里单个的Beacon*/
        public int minor;  
        /** 厂商识别号*/
        public String proximityUuid;  
        public String bluetoothAddress;
        /** 用于测量设备离Beacon的距离, iBeacon目前只定义了大概的三个粗略级别:非常近(Immediate):大概10厘米内,近(Near):1米内,远(Far):1米内*/
        public int txPower;
        /** RSSI值（接收信号强度），可以用来计算发射端和接收端间距离。
         *  计算公式: d = 10^((abs(RSSI) - A) / (10 * n))
         *  其中：
		    d - 计算所得距离
		    RSSI - 接收信号强度（负值）
		    A - 发射端和接收端相隔1米时的信号强度 ,(经验值 59)
		    n - 环境衰减因子 ,经验值(2.0)
         * 
         * */
        public int rssi;
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public int getMajor() {
			return major;
		}
		public void setMajor(int major) {
			this.major = major;
		}
		public int getMinor() {
			return minor;
		}
		public void setMinor(int minor) {
			this.minor = minor;
		}
		public String getProximityUuid() {
			return proximityUuid;
		}
		public void setProximityUuid(String proximityUuid) {
			this.proximityUuid = proximityUuid;
		}
		public String getBluetoothAddress() {
			return bluetoothAddress;
		}
		public void setBluetoothAddress(String bluetoothAddress) {
			this.bluetoothAddress = bluetoothAddress;
		}
		public int getTxPower() {
			return txPower;
		}
		public void setTxPower(int txPower) {
			this.txPower = txPower;
		}
		public int getRssi() {
			return rssi;
		}
		public void setRssi(int rssi) {
			this.rssi = rssi;
		}
		@Override
		public String toString() {
			return "iBeacon [name=" + name + ", major=" + major + ", minor=" + minor + ", proximityUuid="
					+ proximityUuid + ", bluetoothAddress=" + bluetoothAddress + ", txPower=" + txPower + ", rssi="
					+ rssi + "]";
		}  
    }  
    public static iBeacon fromScanData(BluetoothDevice device, int rssi,byte[] scanData) {  
  
        int startByte = 2;  
        boolean patternFound = false;  
        while (startByte <= 5) {  
            if (((int)scanData[startByte+2] & 0xff) == 0x02 &&  
                ((int)scanData[startByte+3] & 0xff) == 0x15) {            
                // yes!  This is an iBeacon   
                patternFound = true;  
                break;  
            }  
            else if (((int)scanData[startByte] & 0xff) == 0x2d &&  
                    ((int)scanData[startByte+1] & 0xff) == 0x24 &&  
                    ((int)scanData[startByte+2] & 0xff) == 0xbf &&  
                    ((int)scanData[startByte+3] & 0xff) == 0x16) {  
                iBeacon iBeacon = new iBeacon();  
                iBeacon.major = 0;  
                iBeacon.minor = 0;  
                iBeacon.proximityUuid = "00000000-0000-0000-0000-000000000000";  
                iBeacon.txPower = -55;  
                return iBeacon;  
            }  
            else if (((int)scanData[startByte] & 0xff) == 0xad &&  
                     ((int)scanData[startByte+1] & 0xff) == 0x77 &&  
                     ((int)scanData[startByte+2] & 0xff) == 0x00 &&  
                     ((int)scanData[startByte+3] & 0xff) == 0xc6) {  
                     
                    iBeacon iBeacon = new iBeacon();  
                    iBeacon.major = 0;  
                    iBeacon.minor = 0;  
                    iBeacon.proximityUuid = "00000000-0000-0000-0000-000000000000";  
                    iBeacon.txPower = -55;  
                    return iBeacon;  
            }  
            startByte++;  
        }  
  
  
        if (patternFound == false) {  
            // This is not an iBeacon  
            return null;  
        }  
  
        iBeacon iBeacon = new iBeacon();  
  
        iBeacon.major = (scanData[startByte+20] & 0xff) * 0x100 + (scanData[startByte+21] & 0xff);  
        iBeacon.minor = (scanData[startByte+22] & 0xff) * 0x100 + (scanData[startByte+23] & 0xff);  
        iBeacon.txPower = (int)scanData[startByte+24]; // this one is signed  
        iBeacon.rssi = rssi;  
  
        // AirLocate:  
        // 02 01 1a 1a ff 4c 00 02 15  # Apple's fixed iBeacon advertising prefix  
        // e2 c5 6d b5 df fb 48 d2 b0 60 d0 f5 a7 10 96 e0 # iBeacon profile uuid  
        // 00 00 # major   
        // 00 00 # minor   
        // c5 # The 2's complement of the calibrated Tx Power  
  
        // Estimote:          
        // 02 01 1a 11 07 2d 24 bf 16   
        // 394b31ba3f486415ab376e5c0f09457374696d6f7465426561636f6e00000000000000000000000000000000000000000000000000  
  
        byte[] proximityUuidBytes = new byte[16];  
        System.arraycopy(scanData, startByte+4, proximityUuidBytes, 0, 16);   
        String hexString = bytesToHexString(proximityUuidBytes);  
        StringBuilder sb = new StringBuilder();  
        sb.append(hexString.substring(0,8));  
        sb.append("-");  
        sb.append(hexString.substring(8,12));  
        sb.append("-");  
        sb.append(hexString.substring(12,16));  
        sb.append("-");  
        sb.append(hexString.substring(16,20));  
        sb.append("-");  
        sb.append(hexString.substring(20,32));  
        iBeacon.proximityUuid = sb.toString();  
  
        if (device != null) {  
            iBeacon.bluetoothAddress = device.getAddress();  
            iBeacon.name = device.getName();  
        }  
  
        return iBeacon;  
    }  
  
    public static String bytesToHexString(byte[] src){    
        StringBuilder stringBuilder = new StringBuilder("");    
        if (src == null || src.length <= 0) {    
            return null;    
        }    
        for (int i = 0; i < src.length; i++) {    
            int v = src[i] & 0xFF;    
            String hv = Integer.toHexString(v);    
            if (hv.length() < 2) {    
                stringBuilder.append(0);    
            }    
            stringBuilder.append(hv);    
        }    
        return stringBuilder.toString();    
    }    
}  

