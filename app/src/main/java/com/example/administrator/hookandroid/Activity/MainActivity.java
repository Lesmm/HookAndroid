package com.example.administrator.hookandroid.Activity;

import android.Manifest;
import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Point;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.text.Layout;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.administrator.hookandroid.Info.DeviceInfo;
import com.example.administrator.hookandroid.R;
import com.example.administrator.hookandroid.Test.TestCases;
import com.example.administrator.hookandroid.Util.APPSettings;
import com.example.administrator.hookandroid.Util.CommonUtils;
import com.example.administrator.hookandroid.Util.JSONObjectUtil;
import com.example.administrator.hookandroid.Util.JSONStringUtil;
import com.example.administrator.hookandroid.Util.SystemPropertiesUtil;
import com.example.administrator.hookandroid.Util.UmengUtil;
import com.umeng.analytics.MobclickAgent;

import org.json.JSONObject;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import common.modules.util.IFileUtil;
import common.modules.util.IHTTPUtil;
import common.modules.util.IReflectUtil;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private static int flag = 0;


    public void onResume() {
        super.onResume();

        UmengUtil.onResumeToActivity(this);

        MobclickAgent.onPageStart("MainActivity"); //统计页面(仅有Activity的应用中SDK自动调用，不需要单独写。"SplashScreen"为页面名称，可自定义)
        MobclickAgent.onResume(this);          //统计时长
    }

    public void onPause() {
        super.onPause();

        UmengUtil.onPauseToActivity(this);

        MobclickAgent.onPageEnd("MainActivity"); // （仅有Activity的应用中SDK自动调用，不需要单独写）保证 onPageEnd 在onPause 之前调用,因为 onPause 中会保存信息。"SplashScreen"为页面名称，可自定义
        MobclickAgent.onPause(this);
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.support.v4.app.ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.INTERNET,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_SETTINGS,
            }, 1);
        }

        try {
            Runtime.getRuntime().exec("su");
        } catch (Exception e) {
            e.printStackTrace();
        }

        String userAgent = new WebView(this).getSettings().getUserAgentString();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String permissions[] = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
            this.requestPermissions(permissions, 1000);
        }

        try {


            // 联系人Uri
            Uri uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
            // 查询的字段
            String[] projection = { ContactsContract.CommonDataKinds.Phone._ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.DATA1, "sort_key",
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_ID,
                    ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY };
            AsyncQueryHandler asyncQueryHandler = new AsyncQueryHandler(getContentResolver()) {

                @Override
                protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                    if (cursor != null && cursor.getCount() > 0) {
                        cursor.moveToFirst(); // 游标移动到第一项
                        for (int i = 0; i < cursor.getCount(); i++) {
                            cursor.moveToPosition(i);
                            String name = cursor.getString(1);
                            String number = cursor.getString(2);
                            String sortKey = cursor.getString(3);
                            int contactId = cursor.getInt(4);
                            Long photoId = cursor.getLong(5);
                            String lookUpKey = cursor.getString(6);
                        }

                        super.onQueryComplete(token, cookie, cursor);
                    }
                }
            };
            asyncQueryHandler.startQuery(0, null, uri, projection, null, null, "sort_key COLLATE LOCALIZED asc");


            ContentResolver cr = getContentResolver();
            Uri uri2 = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
            String[] projection2 = new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DATA, ContactsContract.CommonDataKinds.Phone.TYPE};
            Cursor cursor = cr.query(uri2, projection2, null, null, null);
            while (cursor.moveToNext()) {
                String number = cursor.getString(0);
                long date = cursor.getLong(1);
                int type = cursor.getInt(2);
            }
            cursor.close();






        } catch (Exception e) {
            e.printStackTrace();
        }
        try {

//            String contents = getPhoneInfo(this);
//            Log.d("Hook", "getPhoneInfo: " + contents);

            TestCases.testSdcard(this);
            TestCases.testSettings(this);

            // share preference
            SharedPreferences sharedPreferences = getSharedPreferences(this.getPackageName(), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("username", "Tom");
            editor.putInt("password", 123456);
            editor.commit();
            String value = sharedPreferences.getString("username", "");
            Log.d("Hook", "SharedPreferences value: " + value);

            File mBackupFile = (File)IReflectUtil.objectFieldValue(sharedPreferences, "mBackupFile");
            File mFile = (File)IReflectUtil.objectFieldValue(sharedPreferences, "mFile");

            // infos
            final TextView mTextView = (TextView) findViewById(R.id.textView_info);
            mTextView.setMovementMethod(ScrollingMovementMethod.getInstance());
            DeviceInfo.getBGHandler().post(new Runnable() {
                @Override
                public void run() {
                    final String contents = showDeviceInfo(MainActivity.this);

                    IHTTPUtil.getAsync("http://myip.ipip.net", new IHTTPUtil.ResponseCallBack() {
                        @Override
                        public void done(JSONObject json) {
                            if (json == null) return;
                            final String jsonString = json.toString();
                            DeviceInfo.getMainHandler().post(new Runnable() {
                                @Override
                                public void run() {
                                    CharSequence sequence = mTextView.getText();
                                    String str = sequence == null ? "" : sequence.toString();
                                    String result = str + "\r\n" + jsonString;
                                    mTextView.setText(result);
                                }
                            });
                        }
                    });

                    DeviceInfo.getMainHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            CharSequence sequence = mTextView.getText();
                            String str = sequence == null ? "" : sequence.toString();
                            String result = str + "\r\n" + contents;
                            mTextView.setText(result);
                        }
                    });
                }
            });

            mTextView.post(new Runnable() {
                @Override
                public void run() {
                    Layout layout = mTextView.getLayout();
                    if (layout != null) {
                        int scrollAmount = layout.getLineTop(mTextView.getLineCount()) - mTextView.getHeight();
                        mTextView.scrollTo(0, scrollAmount);
                    }
                }
            });

            // button scroll event
            Button scrollButton = (Button) findViewById(R.id.button_scroll);
            scrollButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    flag++;
                    int scrollAmount = mTextView.getLayout().getLineTop(mTextView.getLineCount()) - mTextView.getHeight();
                    mTextView.scrollTo(0, flag % 2 == 0 ? 0 : scrollAmount);
                }
            });

            // button change activity
            Button activityButton = (Button) findViewById(R.id.button_change_activity);
            activityButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put("type", "book");
                    map.put("quantity", "3");
                    MobclickAgent.onEvent(MainActivity.this, "purchase", map);

                    Intent intent = new Intent(MainActivity.this, SecondActivity.class);
                    MainActivity.this.startActivity(intent);
                }
            });
            MobclickAgent.onProfileSignIn("WB", "userID");

            // button upload
            final Button uploadButton = (Button) findViewById(R.id.button_upload);
            uploadButton.setText("版本3 | 上传设备信息");
            uploadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Toast.makeText(MainActivity.this, "正在上传/更新设备信息...", Toast.LENGTH_LONG).show();
                    uploadButton.setText("正在上传，请稍后...");

                    try {
                        DeviceInfo.getBGHandler().postDelayed(new Runnable() {
                            @Override
                            public void run() {

                                try {
                                    String postURL = APPSettings.getUploadPhoneInfoAPI(MainActivity.this, "tb_phonetype");

                                    DeviceInfo.getMainHandler().post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(MainActivity.this, "正在收集信息...", Toast.LENGTH_LONG).show();
                                        }
                                    });

                                    // POST THE JSON
                                    JSONObject result = DeviceInfo.getDeviceInfo(MainActivity.this);
                                    String resultJson = result.toString();

                                    DeviceInfo.getMainHandler().post(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(MainActivity.this, "收集信息完毕，正在上传...", Toast.LENGTH_LONG).show();
                                        }
                                    });

                                    IFileUtil.writeTextToFile(JSONStringUtil.jsonFormart(resultJson), "/sdcard/phoneInfo.json");

//                                    final JSONObject res = HTTPSender.post(postURL, resultJson);

                                    DeviceInfo.getMainHandler().post(new Runnable() {
                                        @Override
                                        public void run() {
                                            uploadButton.setText("版本3 | 上传设备信息");
                                            Toast.makeText(MainActivity.this, "收集完成!", Toast.LENGTH_LONG).show();
//                                            if (res != null) {
//                                                Toast.makeText(MainActivity.this, "上传/更新设备信息完成: " + res.toString(), Toast.LENGTH_LONG).show();
//                                            } else {
//                                                Toast.makeText(MainActivity.this, "上传/更新设备信息失败(无信息)", Toast.LENGTH_LONG).show();
//                                            }
                                        }
                                    });

                                } catch (Exception e) {
                                    Log.d(TAG, "------------->>> Exception <<<-------------");
                                    Log.d(TAG, e.toString());
                                    e.printStackTrace();
                                    uploadButton.setText("版本3 | 上传设备信息");
                                    Toast.makeText(MainActivity.this, "上传/更新设备信息错误", Toast.LENGTH_LONG).show();
                                }
                            }
                        }, 2 * 1000);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });


            // Location
            final EditText editText = (EditText) findViewById(R.id.editText);
            editText.clearFocus();


            final LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5 * 1000, 1, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    double lat = location.getLatitude();
                    double lng = location.getLongitude();

                    String timeString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                    String string = "latitude: " + lat + " longitude: " + lng;
                    String log = timeString + " - " + string + "\r\n";

                    String filename = MainActivity.this.getFilesDir().getAbsolutePath() + "/locations.log";
                    IFileUtil.appendTextToFile(log, filename);

                    Log.d(TAG, "----->>>>> " + string);

                    File f = new File(filename);
                    if (f.length() >= 5 * 1024 * 1024) { // 10MB
                        IFileUtil.devideFile(f);
                    }

                    editText.setText("lat:" + lat + " lng:" + lng);
                    CommonUtils.printLocationInformation(MainActivity.this, lat, lng);
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

                        Map map = IReflectUtil.invokeObjectAllGetMethods(location);
                        Log.d(TAG, "----->>>>> getLastKnownLocation lat:" + lat + " lng:" + lng);

                        String timeString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                        String string = "latitude: " + lat + " longitude: " + lng;
                        String log = timeString + " - " + string + "\r\n";

                        String filename = MainActivity.this.getFilesDir().getAbsolutePath() + "/locations.log";
                        IFileUtil.appendTextToFile(log, filename);
                    }
                }
            }, 3 * 1000);


            locationManager.addGpsStatusListener(new GpsStatus.Listener() {
                @Override
                public void onGpsStatusChanged(int event) {
//                    Log.d(TAG, "----->>>>> onGpsStatusChanged event:" + event);
                }
            });


        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void onCreate__(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("Hello Hook Test", "<<<<<<<------------------------>>>>>>>>>");
        DeviceInfo.uploadDeviceInfoInBGThread(this, "tb_phonetype");
    }

    public static String showDeviceInfo(Context context) {
        StringBuffer sb = new StringBuffer();

        try {

            int myUid = android.os.Process.myUid();
            int uid = context.getApplicationInfo().uid;
            int packageInfo_uid = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_GIDS).applicationInfo.uid;
            int applicationInfo_uid = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA).uid;

            int installed_uid = 0;
            List<PackageInfo> packageInfos = context.getPackageManager().getInstalledPackages(PackageManager.GET_GIDS);
            for (int i = 0; i < packageInfos.size(); i++) {
                PackageInfo info = packageInfos.get(i);
                if (info.packageName.equals(context.getPackageName())) {
                    installed_uid = info.applicationInfo.uid;
                    break;
                }
            }

            String uid_str = "myUid: " + myUid + ",ApplicationInfo.uid: " + uid + ",pm packageInfo_uid: " + packageInfo_uid + ",pm applicationInfo_uid: " + applicationInfo_uid
                    + ", pm installed_uid: " + installed_uid;
            sb.append(uid_str + "\n");

            final TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String imei = telephonyManager.getDeviceId();
            String imsi = telephonyManager.getSubscriberId();

            sb.append("UM: " + CommonUtils.getDeviceInfo(context) + "\n");
            sb.append("MAC: " + CommonUtils.getMac(context) + "\n");
            sb.append("IMEI: " + imei + "\n");
            sb.append("IMSI: " + imsi + "\n");
            sb.append("Baseband: " + SystemPropertiesUtil.get("gsm.version.baseband") + "\n");
            sb.append("Bluetooth: " + DeviceInfo.invokeIBluetoothManagerMethod("getAddress") + "\n");
            sb.append("Bluetooth Name: " + DeviceInfo.invokeIBluetoothManagerMethod("getName") + "\n");
            sb.append("User Agent: " + System.getProperty("http.agent") + "\n");

//            NetworkInterface.getNetworkInterfaces();

            // Display
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

            DisplayMetrics displayRealMetrics = new DisplayMetrics();
            ((Activity) context).getWindowManager().getDefaultDisplay().getRealMetrics(displayRealMetrics);
            int heigth_4 = displayRealMetrics.heightPixels;
            int width_4 = displayRealMetrics.widthPixels;

            Point outPoint = new Point();
            if (Build.VERSION.SDK_INT >= 19) {
                // 可能有虚拟按键的情况
                display.getRealSize(outPoint);
            } else {
                // 不可能有虚拟按键
                display.getSize(outPoint);
            }
            int mRealSizeWidth = outPoint.y;//手机屏幕真实宽度
            int mRealSizeHeight = outPoint.x;//手机屏幕真实高度

            sb.append("Display H: " + heigth + " | " + heigth_2 + " | " + heigth_3 + " | " + heigth_4 + " | " + mRealSizeHeight + "\n");
            sb.append("Display W: " + width + " | " + width_2 + " | " + width_3 + " | " + width_4 + " | " + mRealSizeWidth + "\n");


            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkINfo = cm.getActiveNetworkInfo();
            if (networkINfo != null && networkINfo.getType() == ConnectivityManager.TYPE_WIFI) {
                sb.append("Using: " + "WIFI\n");
            }
            if (networkINfo != null && networkINfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                sb.append("Using: " + "2G/3G/4G\n");
            }

            NetworkInfo info = cm.getNetworkInfo(1);
            if (info != null) {
                sb.append("WIFI STATE: " + info.getState());
            }


            // Build
            sb.append("\n\n--------------- Build ---------------" + "\n");
            sb.append("修订版本列表(Build.ID): " + Build.ID + "\n");
            sb.append("显示屏参数: " + Build.DISPLAY + "\n");
            sb.append("手机制造商: " + Build.PRODUCT + "\n");
            sb.append("设置参数: " + Build.DEVICE + "\n");
            sb.append("主板: " + Build.BOARD + "\n");
//            sb.append("cpu指令集:  " + Build.CPU_ABI + "\n");
//            sb.append("cpu指令集2: " + Build.CPU_ABI2 + "\n");
//            sb.append("硬件制造商: " + Build.MANUFACTURER + "\n");
//            sb.append("系统定制商: " + Build.BRAND + "\n");
//            sb.append("版本: " + Build.MODEL + "\n");
//            sb.append("系统启动程序版本号:  " + Build.BOOTLOADER + "\n");
//            sb.append("无线电版本: " + Build.RADIO + "\n");
//            sb.append("无线电固件基带版本: " + Build.getRadioVersion() + "\n");
//            sb.append("硬件名称: " + Build.HARDWARE + "\n");
////        sb.append("是否是模拟器: " + Build.IS_EMULATOR + "\n");
//            sb.append("硬件序列号: " + Build.SERIAL + "\n");
////        sb.append("序列号: " + Build.getSerial() + "\n");
////        sb.append("支持指令集: " + Build.SUPPORTED_ABIS + "\n");
////        sb.append("支持32位指令集: " + Build.SUPPORTED_32_BIT_ABIS + "\n");
////        sb.append("支持64位指令集: " + Build.SUPPORTED_64_BIT_ABIS + "\n");
//            sb.append("编译标签: " + Build.TAGS + "\n");
//            sb.append("编译类型: " + Build.TYPE + "\n");
//            sb.append("硬件识别码: " + Build.FINGERPRINT + "\n");
////        sb.append("IS_TREBLE_ENABLED: " + Build.IS_TREBLE_ENABLED + "\n");
////        sb.append("isBuildConsistent: " + Build.isBuildConsistent() + "\n");
//            sb.append("编译时间: " + Build.TIME + "\n");
//            sb.append("编译用户: " + Build.USER + "\n");
//            sb.append("编译主机: " + Build.HOST + "\n");
////        sb.append("IS_DEBUGGABLE: " + Build.IS_DEBUGGABLE + "\n");
////        sb.append("IS_DEBUGGABLE: " + SystemProperties.getInt("ro.debuggable", 0) == 1 + "\n");
////        sb.append("IS_ENG: " + Build.IS_ENG + "\n");
////        sb.append("IS_USERDEBUG: " + Build.IS_USERDEBUG + "\n");
////        sb.append("IS_USER: " + Build.IS_USER + "\n");
////        sb.append("IS_CONTAINER: " + Build.IS_CONTAINER + "\n");
////        sb.append("IS_CONTAINER: " + Build.PERMISSIONS_REVIEW_REQUIRED + "\n");


            sb.append("\n");
            sb.append("--------------- Build VERSION ---------------" + "\n");
//            sb.append("INCREMENTAL: " + Build.VERSION.INCREMENTAL + "\n");
//            sb.append("RELEASE: " + Build.VERSION.RELEASE + "\n");
////        sb.append("BASE_OS: " + Build.VERSION.BASE_OS + "\n");
////        sb.append("SECURITY_PATCH: " + Build.VERSION.SECURITY_PATCH + "\n");
//            sb.append("SDK: " + Build.VERSION.SDK + "\n");
//            sb.append("SDK_INT: " + Build.VERSION.SDK_INT + "\n");
////        sb.append("PREVIEW_SDK_INT: " + Build.VERSION.PREVIEW_SDK_INT + "\n");
//            sb.append("CODENAME: " + Build.VERSION.CODENAME + "\n");
////        sb.append("ALL_CODENAMES: " + Build.VERSION.ALL_CODENAMES + "\n");
////        sb.append("ACTIVE_CODENAMES: " + Build.VERSION.ACTIVE_CODENAMES + "\n");
////        sb.append("RESOURCES_SDK_INT: " + Build.VERSION.RESOURCES_SDK_INT + "\n");


            sb.append("\n--------------- All Information ---------------" + "\n");
//            JSONObject result = DeviceInfo.getDeviceInfo(context);
//            String resultJson = JSONObjectUtil.toSortedJSONString(result);
//            String formatString = JSONStringUtil.jsonFormart(resultJson);
//            sb.append(formatString);

        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return sb.toString();
    }


    public static String  getPhoneInfo(Context cxt) {
        try {
            TelephonyManager tm = (TelephonyManager) cxt.getSystemService(Context.TELEPHONY_SERVICE);
            StringBuilder sb = new StringBuilder();

            sb.append("\nDeviceId(IMEI) = " + tm.getDeviceId());
            sb.append("\nDeviceSoftwareVersion = " + tm.getDeviceSoftwareVersion());
            sb.append("\nLine1Number = " + tm.getLine1Number());
            sb.append("\nNetworkCountryIso = " + tm.getNetworkCountryIso());
            sb.append("\nNetworkOperator = " + tm.getNetworkOperator());
            sb.append("\nNetworkOperatorName = " + tm.getNetworkOperatorName());
            sb.append("\nNetworkType = " + tm.getNetworkType());
            sb.append("\nPhoneType = " + tm.getPhoneType());
            sb.append("\nSimCountryIso = " + tm.getSimCountryIso());
            sb.append("\nSimOperator = " + tm.getSimOperator());
            sb.append("\nSimOperatorName = " + tm.getSimOperatorName());
            sb.append("\nSimSerialNumber = " + tm.getSimSerialNumber());
            sb.append("\nSimState = " + tm.getSimState());
            sb.append("\nSubscriberId(IMSI) = " + tm.getSubscriberId());
            sb.append("\nVoiceMailNumber = " + tm.getVoiceMailNumber());
            return sb.toString();
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        return "";
    }

    }
