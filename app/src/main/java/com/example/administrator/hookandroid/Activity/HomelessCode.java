package com.example.administrator.hookandroid.Activity;

import android.content.Context;
import android.os.Binder;
import android.os.Handler;
import android.util.Log;

import common.modules.util.IReflectUtil;
import com.example.administrator.hookandroid.Network.HTTPSender;
import com.example.administrator.hookandroid.Util.PackageUtil;

import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HomelessCode {

    private static final String TAG = "HomelessCode";

    public static void main(String[] args) {
        Log.d(TAG, "main start uid = " + android.os.Process.myUid());

        while (true) {
            Log.d(TAG, "Heartbeat for uid = " + android.os.Process.myUid());

            try {
                Thread.sleep(2 * 1000);
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }




        double initLatitude = 0;
        double initLongitude = 0;
        String baiduMapAPIURL = "http://api.map.baidu.com/location/ip?ak=UMYEP8FPwG0n2QnGgq6n6p7y&coor=bd09ll&qq-pf-to=pcqq.c2c";
        JSONObject locationJson = HTTPSender.get(baiduMapAPIURL);
        if (locationJson != null && locationJson.optInt("status") == 0) {
            JSONObject contentJson = locationJson.optJSONObject("content");
            if (contentJson != null) {
                JSONObject pointJson = contentJson.optJSONObject("point");
                if (pointJson != null) {
                    double longitude = pointJson.optDouble("x");
                    double latitude = pointJson.optDouble("y");

                    initLatitude = latitude;
                    initLongitude = longitude;
                }
            }
        }

        String initIP = "";
        String IP_API = "http://2018.ip138.com/ic.asp";
        JSONObject ipJson = HTTPSender.get(IP_API);
        if (ipJson != null) {
            String contents = ipJson.optString("STRING_CONTENTS");
            Pattern p = Pattern.compile("\\[(.*?)\\]");
            Matcher m = p.matcher(contents);
            while (m.find()) {
                String groupFound = m.group();
                String ipString = m.group(1);
                initIP = ipString;
            }
        }

        String roadPathAPI = "http://192.168.3.68:9090/zfyuncontrol/location?adv_id=" + 11 + "&ip=" + initIP + "&lon=" + initLongitude + "&lat=" + initLatitude;
        JSONObject roadPathJson = HTTPSender.get(roadPathAPI);
    }


    public static void homelessOne(final Context context) {
        // start new process test
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    // Binder.getCallingUid() == android.os.Process.myUid(), MainActivity.this.getApplicationContext().getPackageName();
                    String packageName = context.getPackageManager().getNameForUid(Binder.getCallingUid());
                    int[] gids = PackageUtil.getPackageGids(context, packageName);

                    Log.d(TAG, "HOOK UID:" + android.os.Process.myUid() + " PID:" + android.os.Process.myPid());

//                    MainActivity.this.getPackageManager().getPackageInfo(packageName, 0).applicationInfo.uid; == Binder.getCallingUid() == android.os.Process.myUid()
                    int uid = Binder.getCallingUid();

                    Class[] paramTypes = new Class[]{String.class, String.class, int.class, int.class,
                            int[].class, int.class, int.class, int.class, String.class, boolean.class, String[].class};
                    Object[] paramValues = new Object[]{"com.example.administrator.hookandroid.Activity.HomelessCode", "New_Main_Porccess", uid, uid,
                            gids, 0, 2, 0, null, false, new String[]{"", ""}};
                    IReflectUtil.invokeMethod(android.os.Process.class, "start", paramTypes, paramValues);

                    Log.d(TAG, "");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 3 * 1000);



        // new a ScanResult for test
        /*
        try {
            Class wifiSsidClz = Class.forName("android.net.wifi.WifiSsid");
            Constructor constructor = ScanResult.class.getConstructor(new Class[]{wifiSsidClz, String.class, String.class, int.class, int.class, long.class});
            Method createWifiSsidMd = wifiSsidClz.getDeclaredMethod("createFromAsciiEncoded", new Class[]{String.class});
            Object wifiSsid = createWifiSsidMd.invoke(wifiSsidClz, "abcd-wifi");
            ScanResult scResult = (ScanResult) constructor.newInstance(new Object[]{wifiSsid, "88:88:88:88:88:88", "caps", 1, 2, 3});
            Log.d(TAG, scResult.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        */

    }
}
