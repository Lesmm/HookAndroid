package com.example.administrator.hookandroid.Util;

public class StringUtil {

    public static String getClassNameWithoutPackageName(Class cls) {
        String className = cls.getName();
        String[] names = className.split("\\.");
        String lastName = names[names.length - 1];
        String resultName = lastName.replace("$", ".");
        return resultName;
    }

}
