package com.example.administrator.hookandroid.Test;

import android.content.Context;
import android.os.Environment;
import android.provider.Settings;

import common.modules.util.IFileUtil;


public class TestCases {

    public static void testSdcard(Context mContext) {

        // 2. Test Monitor Write SDCard Files
        String sdCardPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        String oneFile = sdCardPath + "/" + "__monitor_file__.txt";
        IFileUtil.writeTextToFile(new java.util.Date().toString() +  " just for test\r\n", oneFile);

        String content = IFileUtil.readFileToText(oneFile);

        String threeFile = "/sdcard/" + "__just_for_look__.txt";
        IFileUtil.writeTextToFile(new java.util.Date().toString() +  " just for look\r\n", threeFile);

        String twoFile = sdCardPath + "/" + "abc/__monitor_file__.txt";
        IFileUtil.writeTextToFile(new java.util.Date().toString() +  " just for look\r\n", twoFile);

    }


    public static void testSettings(Context mContext) {
        try {

            // 1. Test HookSettings _____________________

            // will save to /data/data/com.android.providers.settings/databases/settings.db
            Context context = mContext.getApplicationContext();
            String customResult = null;
            String systemResult = null;
            Settings.System.putString(context.getContentResolver(), "custom_key", "--->check_it_out<---");
            customResult = Settings.System.getString(context.getContentResolver(), "custom_key");

            Settings.System.putString(context.getContentResolver(), "status_bar_notif_count", "1");
            systemResult = Settings.System.getString(context.getContentResolver(), "status_bar_notif_count");

            Settings.System.putString(context.getContentResolver(),"custom_key", null);
            customResult = Settings.System.getString(context.getContentResolver(),"custom_key");

            Settings.System.putString(context.getContentResolver(),"status_bar_notif_count", null);
            systemResult = Settings.System.getString(context.getContentResolver(),"status_bar_notif_count");

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
