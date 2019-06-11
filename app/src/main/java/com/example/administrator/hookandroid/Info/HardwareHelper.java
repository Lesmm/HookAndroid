package com.example.administrator.hookandroid.Info;


import org.json.JSONObject;

import common.modules.util.IFileUtil;

public class HardwareHelper {

    public static JSONObject getInfoInProc() {

        JSONObject procInfos  = new JSONObject();

        try {
            String key = "/proc/cpuinfo";
            String info = IFileUtil.readFileToText(key);
            procInfos.put(key, info);

            key = "/proc/meminfo";
            info = IFileUtil.readFileToText(key);
            procInfos.put(key, info);

            key = "/proc/cmdline";
            info = IFileUtil.readFileToText(key);
            procInfos.put(key, info);

            key = "/proc/mounts";
            info = IFileUtil.readFileToText(key);
            procInfos.put(key, info);

            key = "/proc/version";
            info = IFileUtil.readFileToText(key);
            procInfos.put(key, info);

            key = "/proc/net/if_inet6";
            info = IFileUtil.readFileToText(key);
            procInfos.put(key, info);

            key = "/sys/class/net/wlan0/address";
            info = IFileUtil.readFileToText(key);
            procInfos.put(key, info);

            key = "/sys/devices/fb000000.qcom,wcnss-wlan/net/wlan0/address";
            info = IFileUtil.readFileToText(key);
            procInfos.put(key, info);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return  procInfos;
    }


}
