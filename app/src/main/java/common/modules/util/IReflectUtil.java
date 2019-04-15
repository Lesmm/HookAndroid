package common.modules.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IReflectUtil {

    public interface IterateFieldHandler {
        public boolean action(Class<?> clazz, Field field, String fieldName);
    }

    public void iterateFields(Class<?> clazz, IterateFieldHandler handler) {
        Field[] fields = clazz.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            field.setAccessible(true);
            String fieldName = field.getName();
            if (handler.action(clazz, field, fieldName)) {
                break;
            }
        }
    }

    /**
     * Call Object Fields And Values Using Reflect
     */
    public static Object objectFieldValue(Object obj, String fieldName) {
        Boolean isClass = obj instanceof Class;
        Class<?> clazz = isClass ? (Class<?>) obj : obj.getClass();

        Field field = null; // do not use searchField() method, cause here neen to change clazz = ...
        while (clazz != null) {
            try {
                field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                break;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }

        try {
            // clazz now may ge super class
            Object value = field.get(Modifier.isStatic(field.getModifiers()) ? clazz : obj);
            return value;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Search field with superclass
     */
    public static Field searchField(Object obj, String fieldName) {
        Boolean isClass = obj instanceof Class;
        Class<?> clazz = isClass ? (Class<?>) obj : obj.getClass();

        Field field = null;
        while (clazz != null) {
            try {
                field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                break;
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return field;
    }


    public interface FieldFilter {
        boolean filterAction(Field field);
    }

    public interface ValueFilter {
        boolean filterAction(Object value);
    }

    public static Map<?, ?> objectFieldNameValues(Object obj) {
        return objectFieldNameValues(obj, null, null);
    }

    public static Map<?, ?> objectFieldNameValues(Object obj, FieldFilter fieldFilter) {
        return objectFieldNameValues(obj, fieldFilter, null);
    }

    public static Map<?, ?> objectFieldNameValues(Object obj, ValueFilter valueFilter) {
        return objectFieldNameValues(obj, null, valueFilter);
    }

    public static Map<?, ?> objectFieldNameValues(Object obj, FieldFilter fieldFilter, ValueFilter valueFilter) {
        Map<String, Object> result = new HashMap<String, Object>();

        Boolean isClass = obj instanceof Class;
        Class<?> clazz = isClass ? (Class<?>) obj : obj.getClass();
        Field[] fields = clazz.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            try {
                Field field = fields[i];
                if (fieldFilter != null && fieldFilter.filterAction(field)) {
                    continue;
                }
                field.setAccessible(true);

                String name = field.getName();
                Object value = field.get(Modifier.isStatic(field.getModifiers()) ? clazz : obj);

                if (valueFilter != null && valueFilter.filterAction(value)) {
                    continue;
                }

                if (value == null) {
                    value = "<NULL>";
                }
                result.put(name, value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * Call Object all methods with return value and without parameters
     */
    public interface MethodFilter {
        boolean filterAction(Method method);
    }

    public static Map<?, ?> invokeObjectAllNonVoidZeroArgsMethods(Object obj) {
        return invokeObjectAllNonVoidZeroArgsMethods(obj, null);
    }

    public static Map<?, ?> invokeObjectAllNonVoidZeroArgsMethods(Object obj, MethodFilter filter) {
        Map<String, Object> result = new HashMap<String, Object>();

        boolean isClass = obj instanceof Class;
        Class<?> clazz = isClass ? (Class<?>) obj : obj.getClass();
        Method[] methods = clazz.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            if (filter != null && filter.filterAction(method)) {
                continue;
            }
            method.setAccessible(true);
            String methodName = method.getName();
            try {
                Class<?>[] types = method.getParameterTypes();
                Class<?> returnType = method.getReturnType();
                if (returnType != void.class && types.length == 0) {
                    Object value = method.invoke(Modifier.isStatic(method.getModifiers()) ? clazz : obj, new Object[]{});
                    result.put(methodName, value);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * Call Object all "get" methods without parameters
     */
    public static Map<?, ?> invokeObjectAllGetMethods(Object obj) {
        Map<String, Object> result = new HashMap<String, Object>();

        boolean isClass = obj instanceof Class;
        Class<?> clazz = isClass ? (Class<?>) obj : obj.getClass();
        Method[] methods = clazz.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            method.setAccessible(true);
            String methodName = method.getName();
            if (methodName.startsWith("get")) {
                try {
                    Class<?>[] types = method.getParameterTypes();
                    if (types.length == 0) {
                        Object value = method.invoke(Modifier.isStatic(method.getModifiers()) ? clazz : obj, new Object[]{});
                        result.put(methodName, value);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
     * About Field Signature
     */
    public static String getFieldSignature(Class<?> clz, String fieldName) {
        String result = "";
        Field[] fields = clz.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            try {
                Field field = fields[i];
                String name = field.getName();
                if (name.equals(fieldName)) {
                    Method sigMethod = field.getClass().getDeclaredMethod("getSignatureAttribute", new Class[]{});
                    sigMethod.setAccessible(true);
                    String signatureStr = (String) sigMethod.invoke(field, new Object[]{});
                    result = signatureStr;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * About Method Signature
     */
    public static String getMethodSignature(Class<?> clz, String methodName) {
        String result = "";
        Method[] methods = clz.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            try {
                Method method = methods[i];
                String name = method.getName();
                if (name.equals(methodName)) {
                    Method getSigMethod = method.getClass().getDeclaredMethod("getSignatureAttribute", new Class[]{});
                    getSigMethod.setAccessible(true);
                    String signatureStr = (String) getSigMethod.invoke(method, new Object[]{});
                    result = signatureStr;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * About Method Parameters Types
     */
    public static List<Class<?>[]> getMethodParameterTypes(String className, String methodName) {
        List<Class<?>[]> result = new ArrayList<Class<?>[]>();
        try {
            Class<?> clz = Class.forName(className);
            Method[] methods = clz.getDeclaredMethods();
            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];
                String name = method.getName();
                if (name.equals(methodName)) {
                    Class<?>[] types = method.getParameterTypes();
                    result.add(types);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Call Object Method Using Reflect
     */
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
            Method method = searchMethod(obj, methodName, argsTypes);
            method.setAccessible(true);
            return method.invoke(obj, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Method searchMethod(Object obj, String methodName, Class<?>[] argsTypes) throws NoSuchMethodException {
        boolean isClass = obj instanceof Class;
        Class<?> clazz = isClass ? (Class<?>) obj : obj.getClass();
        try {
            Method method = clazz.getDeclaredMethod(methodName, argsTypes);
            return method;
        } catch (NoSuchMethodException e) {
            Method method = recursiveSearchMethod(clazz.getSuperclass(), methodName, argsTypes);
            if (method == null) {
                throw e;
            } else {
                return method;
            }
        }
    }

    public static Method recursiveSearchMethod(Object obj, String methodName, Class<?>[] argsTypes) {
        if (obj == null) {
            return null;
        }
        boolean isClass = obj instanceof Class;
        Class<?> clazz = isClass ? (Class<?>) obj : obj.getClass();
        try {
            Method method = clazz.getDeclaredMethod(methodName, argsTypes);
            return method;
        } catch (NoSuchMethodException e) {
            return recursiveSearchMethod(clazz.getSuperclass(), methodName, argsTypes);
        }
    }

}
