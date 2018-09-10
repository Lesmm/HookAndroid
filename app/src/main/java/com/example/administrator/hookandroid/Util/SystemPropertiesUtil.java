package com.example.administrator.hookandroid.Util;

import java.lang.reflect.Method;

public class SystemPropertiesUtil {

    private static Method getLongMethod = null;
    private static Method getStringMethod = null;
    private static Method getIntMethod = null;
    private static Method getBooleanMethod = null;

    public static String get(final String key) {
        try {
            if (getStringMethod == null) {
                getStringMethod = Class.forName("android.os.SystemProperties").getMethod("get", String.class);
            }
            return (String) getStringMethod.invoke(null, key);
        } catch (Exception e) {
            e.printStackTrace();
            return key;
        }
    }

    public static String get(final String key, final String def) {
        try {
            if (getStringMethod == null) {
                getStringMethod = Class.forName("android.os.SystemProperties").getMethod("get", String.class, String.class);
            }
            return (String) getStringMethod.invoke(null, key, def);
        } catch (Exception e) {
            e.printStackTrace();
            return def;
        }
    }

    public static int getInt(final String key, final int def) {
        try {
            if (getIntMethod == null) {
                getIntMethod = Class.forName("android.os.SystemProperties").getMethod("getInt", String.class, int.class);
            }
            return (Integer) getIntMethod.invoke(null, key, def);
        } catch (Exception e) {
            e.printStackTrace();
            return def;
        }
    }

    public static long getLong(final String key, final long def) {
        try {
            if (getLongMethod == null) {
                getLongMethod = Class.forName("android.os.SystemProperties").getMethod("getLong", String.class, long.class);
            }
            return (Long) getLongMethod.invoke(null, key, def);
        } catch (Exception e) {
            return def;
        }
    }

    public static boolean getBoolean(final String key, final boolean def) {
        try {
            if (getBooleanMethod == null) {
                getBooleanMethod = Class.forName("android.os.SystemProperties").getMethod("getBoolean", String.class, boolean.class);
            }
            return (Boolean) getBooleanMethod.invoke(null, key, def);
        } catch (Exception e) {
            e.printStackTrace();
            return def;
        }
    }

    public static void setProperty(String key, String value) {
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method set = c.getMethod("set", String.class, String.class);
            set.invoke(c, key, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
