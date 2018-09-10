package com.example.administrator.hookandroid.Util;

import android.telephony.TelephonyManager;
import android.util.Log;

import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.PublicKey;

public class JavaReflectUtil {

    public interface FieldFilter {
        boolean filterAction(Field field);
    }

    public static String getClassNameWithoutPackageName(Class cls) {
        String className = cls.getName();
        String[] names = className.split("\\.");
        String lastName = names[names.length - 1];
        String resultName = lastName.replace("$", ".");
        return resultName;
    }

    public static JSONObject getObjectFieldNameValues(Object cls_or_obj) {
        return getObjectFieldNameValues(cls_or_obj, null);
    }

    public static JSONObject getObjectFieldNameValues(Object cls_or_obj, FieldFilter filter) {
        JSONObject jsonObject = new JSONObject();
        Boolean isClass = cls_or_obj instanceof Class;
        try {
            Object obj = null;
            Class cls = null;
            if (!isClass) {
                obj = cls_or_obj;
                cls = obj.getClass();
            } else {
                cls = (Class) cls_or_obj;
            }

            Field[] fds = cls.getDeclaredFields();
            for (int i = 0; i < fds.length; i++) {
                Field fd = fds[i];
                fd.setAccessible(true);
                if (filter != null && filter.filterAction(fd)) {
                    continue;
                }

                String name = fd.getName();
                Object value = null;
                if(Modifier.isStatic(fd.getModifiers())) {
                    value = fd.get(cls);
                } else if (obj != null){
                    value = fd.get(obj);
                }
                if (value == null) {
                    value = "";
                }
                jsonObject.put(name, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public static JSONObject invokeGetMethodsWithObject(Object obj) {
        JSONObject result = new JSONObject();

        Method[] methods = obj.getClass().getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            Method mt = methods[i];
            mt.setAccessible(true);
            String name = mt.getName();
            if (name.startsWith("get")) {
                try {

                    Class returnType = mt.getReturnType();
                    Class[] types = mt.getParameterTypes();

                    if (types.length != 0) {
                        continue;
                    }

                    Object value = "NULL";

                    if (Modifier.isStatic(mt.getModifiers())) {
                        value = mt.invoke(obj.getClass(), new Object[]{});
                    } else {
                        value = mt.invoke(obj, new Object[]{});
                    }

                    result.put(name, value);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }

}
