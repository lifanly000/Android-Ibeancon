#### 请求URL

	http://www.funhainan.cn:9008/api/iBeacon/IBeaconQuery

#### 请求参数
POST /api/iBeacon/IBeaconQuery HTTP/1.1
Host: www.funhainan.cn:9008
Content-Type: application/json

	{
	    "appid": "kkksss",
	    "userid": "23",
	    "ibeacon": [
	        {
	            "name": "zsq_demo",
	            "uuid": "demo6152-demo-demo-900c-3ae56d41352c",
	            "address": "E3:E6:3B:4C:BD:83",
	            "major": 10034,
	            "minor": 58888,
	            "txPower": -59,
	            "rssi": -35
	        },
	        {
	            "name": "zsq_demo2",
	            "uuid": "fda50693-demo-demo-demo-c6eb07647825",
	            "address": "DE:MO:DE:MO:DE:MO",
	            "major": 10,
	            "minor": 7,
	            "txPower": -59,
	            "rssi": -84
	        }
	    ],
	    "type": "1"
	}



#### 请求成功
	
	{
	  "ErrorCode": "0",
	  "ErrorMessage": "成功",
	  "content": "{\"ChannelId\":\"1\"}"
	}


#### 请求失败

	{
	  "ErrorCode": "-100",
	  "ErrorMessage": "没有对应的商户",
	  "content": "{\"ChannelId\":null}"
	}