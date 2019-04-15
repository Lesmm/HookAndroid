package com.example.administrator.hookandroid.Util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.UserHandle;

import java.io.RandomAccessFile;
import java.lang.reflect.Method;

public class PackageUtil {

    private static final String TAG = "PackageUtil";

    public static int[] getPackageGids(Context context, String pkgname) {
        try {
            PackageManager pm = context.getPackageManager();

            int permGids[] = null;
            try {
                permGids = pm.getPackageGids(pkgname);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            if (permGids == null) {
                return null;
            }
            int[] gids = new int[permGids.length + 1];
            System.arraycopy(permGids, 0, gids, 1, permGids.length);
            PackageInfo pf = pm.getPackageInfo(pkgname, 0);
            int uid = Binder.getCallingUid(); // pf.applicationInfo.uid

            Method mdGetAppId = UserHandle.class.getDeclaredMethod("getAppId", new Class[] { int.class });
            mdGetAppId.setAccessible(true);
            int appid = (Integer) mdGetAppId.invoke(UserHandle.class, new Object[] { uid });

            Method mdGetSharedAppGid = UserHandle.class.getDeclaredMethod("getSharedAppGid", new Class[] { int.class });
            mdGetSharedAppGid.setAccessible(true);
            int appGid = (Integer) mdGetSharedAppGid.invoke(UserHandle.class, new Object[] { appid });
            gids[0] = appGid;

//            gids = new int[]{appGid, 1035, 1015, 1028};

            return gids;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void setMyOomadj() {
        if (android.os.Process.myUid() == 0) {
            try {
                RandomAccessFile rf = new RandomAccessFile("/proc/" + android.os.Process.myPid() + "/oom_adj", "rw");
                rf.setLength(0);
                rf.write("-16".getBytes());
                rf.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
