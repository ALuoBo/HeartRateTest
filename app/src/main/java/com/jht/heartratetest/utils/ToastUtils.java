package com.jht.heartratetest.utils;

import android.content.Context;
import android.widget.Toast;
/*
*
* 单例toast，不重复弹出
* */
public class ToastUtils {
  private static Toast mToast;
    public static void showBottomToast(Context context, String string) {
        if (mToast != null){
            mToast.cancel();
            mToast = mToast.makeText(context, string, Toast.LENGTH_SHORT);
        }else {
            mToast = mToast.makeText(context, string, Toast.LENGTH_SHORT);
        }
        mToast.show();
    }
}
