package com.example.administrator.hookandroid.Util;

import android.content.Context;
import android.os.Looper;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import common.modules.util.IHTTPUtil;

public class APPSettings {

    public static String getUploadPhoneInfoAPI(Context mContext, String tableName) {
        String ipstring = companyPublicIP(mContext);
        String baseURL = "http://" + ipstring + ":9090";
        String uploadURL = baseURL + "/zfyuncontrol/addphoneinfo" + "?table=" + tableName;
        return uploadURL;
    }

    // 判断是否在公司网络，如果不在公司网络，则返回公司公网地址
    public static String companyPublicIP(final Context mContext) {
//        String selfPublicIP = "";
//        JSONObject json = IHTTPUtil.get("http://pv.sohu.com/cityjson");
//        if (json != null) {
//            String string = json.optString("__raw_response__");
//            Pattern pattern = Pattern.compile("\\d+\\.\\d+\\.\\d+\\.\\d+");
//            Matcher matcher = pattern.matcher(string);
//            if (matcher.find()) {
//                String result = matcher.group(0);
//                selfPublicIP = result;
//            }
//        }
//
//        JSONObject jsonObject = IHTTPUtil.get("http://www.9qianshu.com/cao/ni/ma/shijiebei/neimaer/IPSwitch.txt");
//        if (jsonObject == null) {
//            new android.os.Handler(Looper.getMainLooper()).post(new Runnable() {
//                @Override
//                public void run() {
//                    Toast.makeText(mContext, "获取上传IP失败", Toast.LENGTH_LONG).show();
//                }
//            });
//        } else {
//            JSONArray array = jsonObject.optJSONArray("ip");
//            String companyIP = array.optString(0);
//
//            if (!selfPublicIP.equals(companyIP)) {
//                return companyIP;
//            }
//        }
//        return "192.168.3.116";
        return "2063d9955e.51mypc.cn";
    }
}
