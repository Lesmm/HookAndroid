package com.example.administrator.hookandroid.Json;

import common.modules.util.IReflectUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

public class JSONObjectEx extends JSONObject {

    public JSONObjectEx(Map copyFrom) {
        super();

        Map<String, Object> nameValuePairs = (Map<String, Object>) IReflectUtil.objectFieldValue(this, "nameValuePairs");

        Map<?, ?> contentsTyped = (Map<?, ?>) copyFrom;
        for (Map.Entry<?, ?> entry : contentsTyped.entrySet()) {
            /*
             * Deviate from the original by checking that keys are non-null and
             * of the proper type. (We still defer validating the values).
             */
            String key = (String) entry.getKey();
            if (key == null) {
                throw new NullPointerException("key == null");
            }
            nameValuePairs.put(key, wrap(entry.getValue()));
        }
    }

    public static Object wrap(Object o) {
        if (o == null) {
            return NULL;
        }
        if (o instanceof JSONArray || o instanceof JSONObject) {
            return o;
        }
        if (o.equals(NULL)) {
            return o;
        }
        try {
            if (o instanceof Collection) {
                return new JSONArrayEx((Collection) o);
            } else if (o.getClass().isArray()) {
                return new JSONArrayEx(o);
            }
            if (o instanceof Map) {
                return new JSONObjectEx((Map) o);
            }
            if (o instanceof Boolean ||
                    o instanceof Byte ||
                    o instanceof Character ||
                    o instanceof Double ||
                    o instanceof Float ||
                    o instanceof Integer ||
                    o instanceof Long ||
                    o instanceof Short ||
                    o instanceof String) {
                return o;
            }
            if (o.getClass().isEnum()) {
                return o.toString();
            }
            return objectToJson(o);
        } catch (Exception ignored) {
        }
        return null;
    }


    /* Extend Methods */
    public static JSONObject objectToJson(Object object) {
        Map<?, ?> result = IReflectUtil.objectFieldNameValues(object);

        int recursiveDepth = 0;
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        for (int i = 0; i < elements.length; i++) {
            StackTraceElement ele = elements[i];
            String traceDescription = ele.toString();
            if (traceDescription.contains("JSONObjectEx.objectToJson")){
                recursiveDepth++;
            }
        }
//        return new JSONObject(result);
        if (recursiveDepth >= 2) {
            return new JSONObject(result);
        } else {
            return new JSONObjectEx(result);
        }
    }

    public static void jsonToObject(Object obj, JSONObject jsonObject) {
        Boolean isClass = obj instanceof Class;
        Class<?> clazz = isClass ? (Class<?>) obj : obj.getClass();
        Field[] fields = clazz.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            try {
                Field field = fields[i];
                field.setAccessible(true);

                String name = field.getName();
                Object value = jsonObject.opt(name);
                if (value != null) {
                    field.set(obj, value);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
