package com.example.administrator.hookandroid.Util;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

public class JSONObjectUtil {

    public interface KeyTransformer {
        String transformAction(String key, Object value);
    }

    public static JSONObject transformJSONObjectKeys(JSONObject jsonObject, KeyTransformer transformer) {
        JSONObject result = new JSONObject();
        try {
            java.util.Iterator iterator = jsonObject.keys();
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                Object value = jsonObject.get(key);
                String newKey = transformer.transformAction(key, value);
                result.put(newKey, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public interface IterateHandler {
        void iterateAction(String key, Object value);
    }

    public static void iterateJSONObject(JSONObject jsonObject, IterateHandler handler) {
        try {
            java.util.Iterator iterator = jsonObject.keys();
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                Object value = jsonObject.get(key);
                handler.iterateAction(key, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void mergeJSONObject(JSONObject destination, JSONObject source) {
        try {
            Iterator iteratorOne = source.keys();
            while (iteratorOne.hasNext()) {
                String name = (String) iteratorOne.next();
                Object value = source.get(name);
                destination.put(name, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String toSortedJSONString(JSONObject jsonObject) {
        Iterator<String> keysIterator = jsonObject.keys();
        ArrayList<String> sortedKeys = new ArrayList<String>();
        while (keysIterator.hasNext()) {
            sortedKeys.add(keysIterator.next());
        }
        Collections.sort(sortedKeys);

        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (int i = 0; i < sortedKeys.size(); i++) {
            String k = sortedKeys.get(i);
            Object v = jsonObject.opt(k);

            sb.append("\"" + k + "\":");
            if (v instanceof String) {
                sb.append("\"" + v /* JSONStringUtil.stringToJson((String)v) */ + "\"");
            } else {
                sb.append(v);       // int, float, boolean
            }

            if (i != sortedKeys.size() - 1) sb.append(",");
        }
        sb.append("}");
        return sb.toString();
    }

    public static String objToJson(Object obj) {
        Field[] fields = obj.getClass().getDeclaredFields();
        JSONObject jsonObject = new JSONObject();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                String key = field.getName();
                Object value = field.get(obj);
                if (key != null && value != null && !TextUtils.isEmpty(key)) {
                    jsonObject.put(key, value /*value.toString()*/);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return jsonObject.toString();
    }

    public static String listToJson(Collection objs) {
        JSONArray jsonArray = new JSONArray();
        for (Object obj : objs) {
            JSONObject jsonObject = new JSONObject();
            Field[] fields = obj.getClass().getDeclaredFields();
            for (Field field : fields) {
                try {
                    field.setAccessible(true);
                    String key = field.getName();
                    Object value = field.get(obj);
                    if (key != null && value != null && !TextUtils.isEmpty(key)) {
                        jsonObject.put(key, value.toString());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            jsonArray.put(jsonObject);
        }
        return jsonArray.toString();
    }

}
