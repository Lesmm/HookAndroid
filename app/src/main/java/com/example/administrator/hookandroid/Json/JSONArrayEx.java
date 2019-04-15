package com.example.administrator.hookandroid.Json;

import org.json.JSONArray;
import org.json.JSONException;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class JSONArrayEx extends JSONArray {

    public JSONArrayEx(Collection copyFrom) {
        super();
        if (copyFrom != null) {
            for (Iterator it = copyFrom.iterator(); it.hasNext();) {
                put(JSONObjectEx.wrap(it.next()));
            }
        }
    }

    public JSONArrayEx(Object array) throws JSONException {
        if (!array.getClass().isArray()) {
            throw new JSONException("Not a primitive array: " + array.getClass());
        }
        final int length = Array.getLength(array);

        try {
            Field field = this.getClass().getSuperclass().getDeclaredField("values");
            field.setAccessible(true);

            ArrayList<Object> values = new ArrayList<Object>(length);

            field.set(this, values);
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (int i = 0; i < length; ++i) {
            put(JSONObjectEx.wrap(Array.get(array, i)));
        }
    }
}
