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
                if (Modifier.isStatic(fd.getModifiers())) {
                    value = fd.get(cls);
                } else if (obj != null) {
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

    public static Object invokeMethodWithObject(Object obj, String methodName) {
        try {
            boolean isClass = obj instanceof Class;
            Method[] methods = isClass ? ((Class) obj).getDeclaredMethods() : obj.getClass().getDeclaredMethods();
            for (int i = 0; i < methods.length; i++) {
                Method mt = methods[i];
                mt.setAccessible(true);
                String name = mt.getName();
                if (name.equals(methodName)) {
                    return mt.invoke(obj, new Object[]{});
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object callMethodWithObject(Object obj, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            boolean isClass = obj instanceof Class;
            Method mt = isClass ? ((Class) obj).getDeclaredMethod(methodName, parameterTypes) : obj.getClass().getDeclaredMethod(methodName, parameterTypes);
            mt.setAccessible(true);
            return mt.invoke(obj, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object invokeClassMethod(String className, String methodName, Class<?>[] argsTypes, Object[] args) {
        ClassLoader classLoader = String.class.getClassLoader();
        return invokeClassMethod(className, classLoader, methodName, argsTypes, args);
    }

    public static Object invokeClassMethod(String className, ClassLoader classLoader, String methodName, Class<?>[] argsTypes, Object[] args) {
        try {
            Class<?> clazz = classLoader.loadClass(className);
            return invokeMethod(clazz, methodName, argsTypes, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Object invokeMethod(Object obj, String methodName, Class<?>[] argsTypes, Object[] args) {
        try {
            boolean isClass = obj instanceof Class;
            Class<?> clazz = isClass ? (Class<?>) obj : obj.getClass();
            Method method = clazz.getDeclaredMethod(methodName, argsTypes);
            method.setAccessible(true);
            return method.invoke(obj, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
