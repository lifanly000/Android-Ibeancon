package com.zsq.ibeacondemo;

import android.content.Context;
import android.graphics.Rect;
import android.support.v4.view.MotionEventCompat;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

public class Utils {

	public static Toast mToast;

	/**
	 * 单例吐司对话框(如果上一个没显示完,会把上一个覆盖掉显示最新的)
	 * <br>其实原理就是把当前正在显示的吐司里面显示的内容改了
	 * @param mContext 上下文
	 * @param msg 要显示的信息
	 */
	public static void showToast(Context mContext, String msg) {
		if (mToast == null) {
			mToast = Toast.makeText(mContext, "", Toast.LENGTH_SHORT);
		}
		mToast.setText(msg);
		mToast.show();
	}
}
