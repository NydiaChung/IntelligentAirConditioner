package com.newland.airconditionercontroller;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import com.newland.airconditionercontroller.utils.CrashHandlerUtil;
import com.newland.airconditionercontroller.utils.LogUtil;

public class AirConditionerControllerApplication extends Application {
    private static final String TAG = "AirConditionerController";

    @SuppressLint("StaticFieldLeak")
    public static Context mContext = null;
    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.d(TAG, "onCreate");
        mContext = getApplicationContext();
        CrashHandlerUtil.getmInstance().init(mContext);
    }

    public static Context getInstance() {
        return mContext;
    }
}
