package com.example.administrator.hookandroid.Activity;

import android.app.Activity;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.administrator.hookandroid.R;
import com.example.administrator.hookandroid.Util.APPSettings;
import com.example.administrator.hookandroid.Util.DeviceInfoUtil;
import com.example.administrator.hookandroid.Util.FileUtil;
import com.example.administrator.hookandroid.Util.JSONObjectUtil;
import com.example.administrator.hookandroid.Util.JSONStringUtil;
import com.example.administrator.hookandroid.Util.JavaReflectUtil;
import com.example.administrator.hookandroid.Util.MapDistance;
import com.example.administrator.hookandroid.Util.SystemPropertiesUtil;
import com.example.administrator.hookandroid.network.HTTPSender;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private static int flag = 0;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        // infos
        final TextView mTextView = (TextView) findViewById(R.id.textView1);
        mTextView.setMovementMethod(ScrollingMovementMethod.getInstance());

        DeviceInfoUtil.getBGHandler().post(new Runnable() {
            @Override
            public void run() {
                final String contents = showDeviceInfo(MainActivity.this);

                DeviceInfoUtil.getMainHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        mTextView.setText(contents);
                    }
                });
            }
        });

        mTextView.post(new Runnable() {
            @Override
            public void run() {
                int scrollAmount = mTextView.getLayout().getLineTop(mTextView.getLineCount()) - mTextView.getHeight();
                mTextView.scrollTo(0, scrollAmount);
            }
        });

        Button btn = (Button) findViewById(R.id.button2);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flag++;
                int scrollAmount = mTextView.getLayout().getLineTop(mTextView.getLineCount()) - mTextView.getHeight();
                mTextView.scrollTo(0, flag % 2 == 0 ? 0 : scrollAmount);
            }
        });

        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    DeviceInfoUtil.getBGHandler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Log.d(TAG, "start post device info");
                                JSONObject result = DeviceInfoUtil.getDeviceInfo(MainActivity.this);
                                String resultJson = result.toString();
                                Log.d(TAG, JSONStringUtil.jsonFormart(resultJson));
                                final JSONObject res = HTTPSender.post(APPSettings.uploadPhoneInfoAPI, resultJson);

                                DeviceInfoUtil.getMainHandler().post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(MainActivity.this, "上传/更新设备信息完成: " + res.toString(), Toast.LENGTH_LONG).show();
                                    }
                                });

                                Log.d(TAG, "<<<<< -- >>>>>" + res.toString());
                            } catch (Exception e) {
                                Log.d(TAG, "------------->>> Exception <<<-------------");
                                Log.d(TAG, e.toString());
                                e.printStackTrace();
                            }
                        }
                    }, 5 * 1000);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        final EditText editText = (EditText) findViewById(R.id.editText);
        editText.clearFocus();

        try {
            Log.d(TAG, "HOOK IMEI is: " + getIMEI(this));

            // Location
            Location location1 = new Location("gps");
            Location location2 = new Location("gps");

            location1.setLatitude(36.09872724920132);
            location1.setLongitude(120.37287494546644);

            location2.setLatitude(36.09872724920132);
            location2.setLongitude(120.37187494546644);

            float distance = location1.distanceTo(location2);
            double mapDistance = MapDistance.getShortDistance(location1.getLongitude(), location1.getLatitude(), location2.getLongitude(), location2.getLatitude());
            double diff = MapDistance.getDistance(location1.getLongitude(), location1.getLatitude(), location2.getLongitude(), location2.getLatitude());

            final LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5 * 1000, 1, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    double lat = location.getLatitude();
                    double lng = location.getLongitude();
                    String filename = MainActivity.this.getFilesDir().getAbsolutePath() + "/locations.log";
                    String timeString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                    String string = "latitude: " + lat + " longitude: " + lng;
                    String log = timeString + " - " + string + "\r\n";
                    FileUtil.appendTextToFile(log, filename);
                    Log.d(TAG, "----->>>>> " + string);

                    File f = new File(filename);
                    if (f.length() >= 5 * 1024 * 1024) { // 10MB
                        FileUtil.devideFile(f);
                    }

                    editText.setText("lat:" + lat + " lng:" + lng);
                    printLocationInformation(MainActivity.this, lat, lng);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                @Override
                public void onProviderEnabled(String provider) {
                }

                @Override
                public void onProviderDisabled(String provider) {
                }
            });

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER); // LocationManager.NETWORK_PROVIDER
                    if (location != null) {
                        double lat = location.getLatitude();
                        double lng = location.getLongitude();

                        Map map = JavaReflectUtil.invokeObjectGetMethods(location);
                        Log.d(TAG, "----->>>>> getLastKnownLocation lat:" + lat + " lng:" + lng);
                    }
                }
            }, 3 * 1000);

            locationManager.addGpsStatusListener(new GpsStatus.Listener() {
                @Override
                public void onGpsStatusChanged(int event) {
                    Log.d(TAG, "----->>>>> onGpsStatusChanged event:" + event);
                }
            });

            List<String> providers = locationManager.getAllProviders();
            LocationProvider gpsProvider = locationManager.getProvider(LocationManager.GPS_PROVIDER);
            LocationProvider netProvider = locationManager.getProvider(LocationManager.NETWORK_PROVIDER);

        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//        Log.d("Hello Hook Test", "<<<<<<<------------------------>>>>>>>>>");
//        DeviceInfoUtil.uploadDeviceInfo(this);
//    }

    public static String showDeviceInfo(Context context) {
        StringBuffer sb = new StringBuffer();


        sb.append("IMEI: " + getIMEI(context) + "\n");
        sb.append("IMSI: " + getIMSI(context) + "\n");
        sb.append("Baseband: " + SystemPropertiesUtil.get("gsm.version.baseband") + "\n");

        sb.append("Bluetooth: " + DeviceInfoUtil.invokeIBluetoothManagerMethod("getAddress") + "\n");
        sb.append("Bluetooth Name: " + DeviceInfoUtil.invokeIBluetoothManagerMethod("getName") + "\n");

        sb.append("User Agent: " + System.getProperty("http.agent") + "\n");


        DisplayMetrics adm = context.getResources().getDisplayMetrics();
        int heigth = adm.heightPixels;
        int width = adm.widthPixels;

        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        int heigth_2 = display.getHeight();
        int width_2 = display.getWidth();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int heigth_3 = displayMetrics.heightPixels;
        int width_3 = displayMetrics.widthPixels;
        sb.append("Display heigth: " + heigth + "\n");
        sb.append("Display width: " + width + "\n");


        sb.append("\n");
        sb.append("--------------- Build ---------------" + "\n");
        sb.append("修订版本列表(Build.ID): " + Build.ID + "\n");
        sb.append("显示屏参数: " + Build.DISPLAY + "\n");
        sb.append("手机制造商: " + Build.PRODUCT + "\n");
        sb.append("设置参数: " + Build.DEVICE + "\n");
        sb.append("主板: " + Build.BOARD + "\n");
        sb.append("cpu指令集:  " + Build.CPU_ABI + "\n");
        sb.append("cpu指令集2: " + Build.CPU_ABI2 + "\n");
        sb.append("硬件制造商: " + Build.MANUFACTURER + "\n");
        sb.append("系统定制商: " + Build.BRAND + "\n");
        sb.append("版本: " + Build.MODEL + "\n");
        sb.append("系统启动程序版本号:  " + Build.BOOTLOADER + "\n");
        sb.append("无线电版本: " + Build.RADIO + "\n");
        sb.append("无线电固件基带版本: " + Build.getRadioVersion() + "\n");
        sb.append("硬件名称: " + Build.HARDWARE + "\n");
//        sb.append("是否是模拟器: " + Build.IS_EMULATOR + "\n");
        sb.append("硬件序列号: " + Build.SERIAL + "\n");
//        sb.append("序列号: " + Build.getSerial() + "\n");
//        sb.append("支持指令集: " + Build.SUPPORTED_ABIS + "\n");
//        sb.append("支持32位指令集: " + Build.SUPPORTED_32_BIT_ABIS + "\n");
//        sb.append("支持64位指令集: " + Build.SUPPORTED_64_BIT_ABIS + "\n");
        sb.append("编译标签: " + Build.TAGS + "\n");
        sb.append("编译类型: " + Build.TYPE + "\n");
        sb.append("硬件识别码: " + Build.FINGERPRINT + "\n");
//        sb.append("IS_TREBLE_ENABLED: " + Build.IS_TREBLE_ENABLED + "\n");
//        sb.append("isBuildConsistent: " + Build.isBuildConsistent() + "\n");
        sb.append("编译时间: " + Build.TIME + "\n");
        sb.append("编译用户: " + Build.USER + "\n");
        sb.append("编译主机: " + Build.HOST + "\n");
//        sb.append("IS_DEBUGGABLE: " + Build.IS_DEBUGGABLE + "\n");
//        sb.append("IS_DEBUGGABLE: " + SystemProperties.getInt("ro.debuggable", 0) == 1 + "\n");
//        sb.append("IS_ENG: " + Build.IS_ENG + "\n");
//        sb.append("IS_USERDEBUG: " + Build.IS_USERDEBUG + "\n");
//        sb.append("IS_USER: " + Build.IS_USER + "\n");
//        sb.append("IS_CONTAINER: " + Build.IS_CONTAINER + "\n");
//        sb.append("IS_CONTAINER: " + Build.PERMISSIONS_REVIEW_REQUIRED + "\n");


        sb.append("\n");
        sb.append("--------------- Build VERSION ---------------" + "\n");
        sb.append("INCREMENTAL: " + Build.VERSION.INCREMENTAL + "\n");
        sb.append("RELEASE: " + Build.VERSION.RELEASE + "\n");
//        sb.append("BASE_OS: " + Build.VERSION.BASE_OS + "\n");
//        sb.append("SECURITY_PATCH: " + Build.VERSION.SECURITY_PATCH + "\n");
        sb.append("SDK: " + Build.VERSION.SDK + "\n");
        sb.append("SDK_INT: " + Build.VERSION.SDK_INT + "\n");
//        sb.append("PREVIEW_SDK_INT: " + Build.VERSION.PREVIEW_SDK_INT + "\n");
        sb.append("CODENAME: " + Build.VERSION.CODENAME + "\n");
//        sb.append("ALL_CODENAMES: " + Build.VERSION.ALL_CODENAMES + "\n");
//        sb.append("ACTIVE_CODENAMES: " + Build.VERSION.ACTIVE_CODENAMES + "\n");
//        sb.append("RESOURCES_SDK_INT: " + Build.VERSION.RESOURCES_SDK_INT + "\n");


        /*
        sb.append("\n");
        sb.append("--------------- Build VERSION_CODES ---------------" + "\n");
        sb.append("CUR_DEVELOPMENT: " + Build.VERSION_CODES.CUR_DEVELOPMENT + "\n");
        sb.append("BASE: " + Build.VERSION_CODES.BASE + "\n");
        sb.append("BASE_1_1: " + Build.VERSION_CODES.BASE_1_1 + "\n");
        sb.append("CUPCAKE: " + Build.VERSION_CODES.CUPCAKE + "\n");
        sb.append("DONUT: " + Build.VERSION_CODES.DONUT + "\n");
        sb.append("ECLAIR: " + Build.VERSION_CODES.ECLAIR + "\n");
        sb.append("ECLAIR_0_1: " + Build.VERSION_CODES.ECLAIR_0_1 + "\n");
        sb.append("ECLAIR_MR1: " + Build.VERSION_CODES.ECLAIR_MR1 + "\n");
        sb.append("FROYO: " + Build.VERSION_CODES.FROYO + "\n");
        sb.append("GINGERBREAD: " + Build.VERSION_CODES.GINGERBREAD + "\n");
        sb.append("GINGERBREAD_MR1: " + Build.VERSION_CODES.GINGERBREAD_MR1 + "\n");
        sb.append("HONEYCOMB: " + Build.VERSION_CODES.HONEYCOMB + "\n");
        sb.append("HONEYCOMB_MR1: " + Build.VERSION_CODES.HONEYCOMB_MR1 + "\n");
        sb.append("HONEYCOMB_MR2: " + Build.VERSION_CODES.HONEYCOMB_MR2 + "\n");
        sb.append("ICE_CREAM_SANDWICH: " + Build.VERSION_CODES.ICE_CREAM_SANDWICH + "\n");
        sb.append("ICE_CREAM_SANDWICH_MR1: " + Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1 + "\n");
        sb.append("JELLY_BEAN: " + Build.VERSION_CODES.JELLY_BEAN + "\n");
        sb.append("JELLY_BEAN_MR1: " + Build.VERSION_CODES.JELLY_BEAN_MR1 + "\n");
        sb.append("JELLY_BEAN_MR2: " + Build.VERSION_CODES.JELLY_BEAN_MR2 + "\n");
        sb.append("KITKAT: " + Build.VERSION_CODES.KITKAT + "\n");
        sb.append("KITKAT_WATCH: " + Build.VERSION_CODES.KITKAT_WATCH + "\n");
//        sb.append("L: " + Build.VERSION_CODES.L + "\n");
        sb.append("LOLLIPOP: " + Build.VERSION_CODES.LOLLIPOP + "\n");
        sb.append("LOLLIPOP_MR1: " + Build.VERSION_CODES.LOLLIPOP_MR1 + "\n");
        sb.append("M: " + Build.VERSION_CODES.M + "\n");
        sb.append("N: " + Build.VERSION_CODES.N + "\n");
        sb.append("N_MR1: " + Build.VERSION_CODES.N_MR1 + "\n");
        sb.append("O: " + Build.VERSION_CODES.O + "\n");
        */

        sb.append("\n");
        sb.append("--------------- All Information ---------------" + "\n");
        JSONObject result = DeviceInfoUtil.getDeviceInfo(context);
        String resultJson = JSONObjectUtil.toSortedJSONString(result);
        String formatString = JSONStringUtil.jsonFormart(resultJson);
        sb.append(formatString);

        return sb.toString();
    }

    public static String getIMEI(final Context context) {
        String result = "";
        final TelephonyManager ts = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            result = ts.getDeviceId();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String getIMSI(final Context context) {
        String result = "";
        final TelephonyManager ts = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            result = ts.getSubscriberId();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static void printLocationInformation(Context context, double latitude, double longitude) {
        Geocoder gc = new Geocoder(context, Locale.getDefault());
        List<Address> locationList = null;
        try {
            locationList = gc.getFromLocation(latitude, longitude, 10);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (locationList != null && locationList.size() > 0) {
            Address address = locationList.get(0);//得到Address实例
            Log.i(TAG, "address =" + address);
            String countryName = address.getCountryName();//得到国家名称，比如：中国
            Log.i(TAG, "countryName = " + countryName);
            String locality = address.getLocality();//得到城市名称，比如：北京市
            Log.i(TAG, "locality = " + locality);
            for (int i = 0; address.getAddressLine(i) != null; i++) {
                String addressLine = address.getAddressLine(i);//得到周边信息，包括街道等，i=0，得到街道名称
                Log.i(TAG, "addressLine = " + addressLine);
            }
        }
    }

}
