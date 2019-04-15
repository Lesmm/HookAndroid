package com.example.administrator.hookandroid;

import android.app.Application;
import android.util.Log;

import com.example.administrator.hookandroid.Util.UmengUtil;
import com.umeng.analytics.MobclickAgent;
import com.umeng.commonsdk.UMConfigure;

public class MainApplication extends Application {

    private static final String TAG = "MainApplication";

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "onCreate");

        UmengUtil.initUmeng();

        String appkey = "5caedd6d570df312590001bc";

        UMConfigure.init(this, UMConfigure.DEVICE_TYPE_PHONE, appkey);

        UMConfigure.setLogEnabled(true);

        MobclickAgent.openActivityDurationTrack(true);
    }


}
