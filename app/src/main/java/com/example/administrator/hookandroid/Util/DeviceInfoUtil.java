package com.example.administrator.hookandroid.Util;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.webkit.WebView;

import com.example.administrator.hookandroid.Activity.MainActivity;
import com.example.administrator.hookandroid.network.HTTPSender;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeviceInfoUtil {

    private static final String TAG = "DeviceInfoUtil";
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
                    synchronized (DeviceInfoUtil.class) {
                        try {
                            DeviceInfoUtil.class.notify();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Looper.loop();
                    Log.d(TAG, "end loop");
                }
            }).start();

            synchronized (DeviceInfoUtil.class) {
                try {
                    DeviceInfoUtil.class.wait();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return bgHandler;
    }

    public static void uploadDeviceInfo(final Context context) {
        getBGHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(TAG, "start post device info");
                    JSONObject result = DeviceInfoUtil.getDeviceInfo(context);
                    String resultJson = result.toString();
                    Log.d(TAG, JSONStringUtil.jsonFormart(resultJson));
                    JSONObject res = HTTPSender.post(APPSettings.uploadPhoneInfoAPI, resultJson);

                    Log.d(TAG, "<<<<< -- >>>>>" + res.toString());
                } catch (Exception e) {
                    Log.d(TAG, "------------->>> Exception <<<-------------");
                    Log.d(TAG, e.toString());
                    e.printStackTrace();
                }
            }
        }, 5 * 1000);
    }

    public static JSONObject getDeviceInfo(final Context context) {
        // compile/build info
        JSONObject result = new JSONObject();

        JSONObject buildJson = JavaReflectUtil.getObjectFieldNameValues(Build.class);
        JSONObject buildJsonInfo = JSONObjectUtil.transformJSONObjectKeys(buildJson, new JSONObjectUtil.KeyTransformer() {
            @Override
            public String transformAction(String key, Object value) {
                return "Build." + key;
            }
        });
        JSONObject buildVersionJson = JavaReflectUtil.getObjectFieldNameValues(Build.VERSION.class);
        JSONObject buildVersionJsonInfo = JSONObjectUtil.transformJSONObjectKeys(buildVersionJson, new JSONObjectUtil.KeyTransformer() {
            @Override
            public String transformAction(String key, Object value) {
                return "Build.VERSION." + key;
            }
        });
        JSONObject buildVersionCodesJson = JavaReflectUtil.getObjectFieldNameValues(Build.VERSION_CODES.class);
        JSONObject buildVersionCodesJsonInfo = JSONObjectUtil.transformJSONObjectKeys(buildVersionCodesJson, new JSONObjectUtil.KeyTransformer() {
            @Override
            public String transformAction(String key, Object value) {
                return "Build.VERSION_CODES." + key;
            }
        });
        JSONObjectUtil.mergeJSONObject(result, buildJsonInfo);
        JSONObjectUtil.mergeJSONObject(result, buildVersionJsonInfo);
//        JSONObjectUtil.mergeJSONObject(result, buildVersionCodesJsonInfo);

        // telephony info
        JSONObject teleInfos = getTelephonyManagerInfo(context);

        // telephony properties
        final String __SystemPropertiesPrefix__ = "SystemProperties.";
        JSONObject teleProperties = DeviceInfoUtil.getTelephonySystemProperties();
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


        // display info
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        JSONObject displayMetricsJson = JavaReflectUtil.getObjectFieldNameValues(displayMetrics, new JavaReflectUtil.FieldFilter() {
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
        });
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
            JSONObject wifiConInfoJson = JavaReflectUtil.invokeGetMethodsWithObject(wifiInfo);
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
            Object obj = new Object();
            Class clzzz = Object.class;
            Class czzzzz = obj.getClass();

            NetworkInterface defInterface = (NetworkInterface)JavaReflectUtil.invokeMethodWithObject(NetworkInterface.class, "getDefault");
            NetworkInterface amDefInterface = (NetworkInterface)JavaReflectUtil.callMethodWithObject(NetworkInterface.class, "getDefault", new Class[]{}, new Object[]{});

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

                synchronized (DeviceInfoUtil.class) {
                    try {
                        DeviceInfoUtil.class.notify();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        synchronized (DeviceInfoUtil.class) {
            try {
                DeviceInfoUtil.class.wait();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // merges
        JSONObjectUtil.mergeJSONObject(result, teleInfos);
        JSONObjectUtil.mergeJSONObject(result, telePropInfos);
        JSONObjectUtil.mergeJSONObject(result, systemPropInfo);
        JSONObjectUtil.mergeJSONObject(result, displayInfo);
        JSONObjectUtil.mergeJSONObject(result, bluetoothInfo);
        JSONObjectUtil.mergeJSONObject(result, wifiJsonInfo);
        JSONObjectUtil.mergeJSONObject(result, settingsOutterInfo);
        JSONObjectUtil.mergeJSONObject(result, runtimePropertieInfo);
        JSONObjectUtil.mergeJSONObject(result, userAgentInfo);

        return result;
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

            JSONObject fieldNamesValues = JavaReflectUtil.getObjectFieldNameValues(settingCls);
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

    public static JSONObject getTelephonyManagerInfo(Context context) {
        JSONObject result = new JSONObject();
        final TelephonyManager ts = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        Method[] methods = TelephonyManager.class.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            Method mt = methods[i];
            String name = mt.getName();
            if (name.startsWith("get")) {
                try {
                    mt.setAccessible(true);

                    Class rt = mt.getReturnType();
                    Class[] types = mt.getParameterTypes();
                    /*
                    Parameter[] params = mt.getParameters();
                    int paramsCount = mt.getParameterCount();
                    */
                    Log.d(TAG, name + " return type : " + rt + " parameters count: " + types.length);

                    /*
                    if (!(rt == String.class || rt == Integer.class || rt == int.class || rt == Long.class || rt == long.class || rt == Boolean.class || rt == boolean.class)) {
                        continue;
                    }
                    */

                    if (types.length != 0) {
                        /*
                        getNetworkClass return type : int parameters count: 1
                        getNetworkTypeName return type : class java.lang.String parameters count: 1
                        getPhoneType return type : int parameters count: 1
                        getTelephonyProperty return type : class java.lang.String parameters count: 3
                         */
                    }

                    Object value = mt.invoke(ts, new Object[]{});
                    Log.d(TAG, "method name: " + name + " value: " + value);
                    if (value == null) {
                        value = "NULL";
                    }
                    String key = name.replaceFirst("get", "Telephony.");
                    result.put(key, value);
                } catch (Exception e) {
                    e.printStackTrace();
                    /*
                    getCompleteVoiceMailNumber return type : class java.lang.String parameters count: 0
                    Caused by: java.lang.SecurityException: Requires CALL_PRIVILEGED: Neither user 10063 nor current process has android.permission.CALL_PRIVILEGED.

                    getIsimDomain return type : class java.lang.String parameters count: 0
                    Caused by: java.lang.SecurityException: Requires READ_PRIVILEGED_PHONE_STATE: Neither user 10063 nor current process has android.permission.READ_PRIVILEGED_PHONE_STATE.

                    getIsimImpi return type : class java.lang.String parameters count: 0
                    Caused by: java.lang.SecurityException: Requires READ_PRIVILEGED_PHONE_STATE: Neither user 10063 nor current process has android.permission.READ_PRIVILEGED_PHONE_STATE.

                     */
                }
            }
        }
        return result;
    }

    public static JSONObject getTelephonySystemProperties() {
        JSONObject result = new JSONObject();
        try {
            JSONObject jsonObject = JavaReflectUtil.getObjectFieldNameValues(Class.forName("com.android.internal.telephony.TelephonyProperties"));
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

        return array;
    }

}
