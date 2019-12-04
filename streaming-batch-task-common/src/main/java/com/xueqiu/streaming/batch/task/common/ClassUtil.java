package com.xueqiu.streaming.batch.task.common;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ClassUtil {


    /**
     * Create new instance of specified class and type
     *
     * @param clazz of instance
     * @param <T>   type of object
     * @return new Class instance
     */
    public static <T> T getInstance(Class<T> clazz) {
        T t = null;
        try {
            t = clazz.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return t;
    }

    /**
     * Retrieving fields list of specified class
     * If recursively is true, retrieving fields from all class hierarchy
     *
     * @param clazz       where fields are searching
     * @param recursively param
     * @return list of fields
     */
    public static Field[] getDeclaredFields(Class clazz, boolean recursively) {
        List<Field> fields = new LinkedList<Field>();
        Field[] declaredFields = clazz.getDeclaredFields();
        Collections.addAll(fields, declaredFields);

        Class superClass = clazz.getSuperclass();

        if (superClass != null && recursively) {
            Field[] declaredFieldsOfSuper = getDeclaredFields(superClass, recursively);
            if (declaredFieldsOfSuper.length > 0)
                Collections.addAll(fields, declaredFieldsOfSuper);
        }

        return fields.toArray(new Field[fields.size()]);
    }


    /**
     * Retrieving fields list of specified class and which
     * are annotated by incoming annotation class
     * If recursively is true, retrieving fields from all class hierarchy
     *
     * @param clazz           - where fields are searching
     * @param annotationClass - specified annotation class
     * @param recursively     param
     * @return list of annotated fields
     */
    public static Field[] getAnnotatedDeclaredFields(Class clazz,
                                                     Class<? extends Annotation> annotationClass,
                                                     boolean recursively) {
        List<Field> annotatedFields = new LinkedList<>();
        if (annotationClass == null) {
            return null;
        }
        Field[] allFields = getDeclaredFields(clazz, recursively);

        for (Field field : allFields) {
            if (field.isAnnotationPresent(annotationClass))
                annotatedFields.add(field);
        }

        return annotatedFields.toArray(new Field[annotatedFields.size()]);
    }

    public static Method[] getAnnotatedDeclaredMethodArr(Class clazz,
                                                         Class<? extends Annotation> annotationClass) {
        List<Method> annotatedMethods = getAnnotatedDeclaredMethodList(clazz, annotationClass);
        return annotatedMethods.toArray(new Method[annotatedMethods.size()]);
    }

    public static List<Method> getAnnotatedDeclaredMethodList(Class clazz,
                                                              Class<? extends Annotation> annotationClass) {
        List<Method> annotatedMethods = new ArrayList<>();
        if (annotationClass == null) {
            return annotatedMethods;
        }
        Method[] allMethods = clazz.getMethods();

        for (Method method : allMethods) {
            if (method.isAnnotationPresent(annotationClass))
                annotatedMethods.add(method);
        }
        return annotatedMethods;
    }


    public static Object getFieldValue(String filedName, Object ob) {


        try {
            Method method = null;
            try {
                Field declaredField = ob.getClass().getDeclaredField(filedName);
                if (declaredField.getType() == boolean.class || declaredField.getType() == Boolean.class) {
                    try {
                        method = ob.getClass().getDeclaredMethod("is" + filedName.substring(0, 1).toUpperCase() + filedName.substring(1), new Class[]{});
                    } catch (Exception e) {
                        // no found method
                    }
                }
                if (method == null) {
                    method = ob.getClass().getDeclaredMethod("get" + filedName.substring(0, 1).toUpperCase() + filedName.substring(1), new Class[]{});
                }
            } catch (NoSuchFieldException e) {
                method = ob.getClass().getDeclaredMethod("get" + filedName.substring(0, 1).toUpperCase() + filedName.substring(1), new Class[]{});
            }
            if(method ==null){
                return null;
            }

            return method.invoke(ob, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Object> getFieldValues(String filedName, List<?> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }

        try {
            Object ob = list.get(0);
            Method method = null;
            try {
                Field declaredField = ob.getClass().getDeclaredField(filedName);
                if (declaredField.getType() == boolean.class || declaredField.getType() == Boolean.class) {
                    try {
                        method = ob.getClass().getDeclaredMethod("is" + filedName.substring(0, 1).toUpperCase() + filedName.substring(1), new Class[]{});
                    } catch (Exception e) {
                        // no found method
                    }
                }
                if (method == null) {
                    method = ob.getClass().getDeclaredMethod("get" + filedName.substring(0, 1).toUpperCase() + filedName.substring(1), new Class[]{});
                }
            } catch (NoSuchFieldException e) {
                method = ob.getClass().getDeclaredMethod("get" + filedName.substring(0, 1).toUpperCase() + filedName.substring(1), new Class[]{});
            }
            if(method ==null){
                return null;
            }

            List<Object> values = new ArrayList<>();

            for (Object o : list) {
                values.add(method.invoke(o, null));
            }

            return values;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static Object setFieldValue(String filedName, Object ob, Object value) {
        if (value != null) {
            return setFieldValue(filedName, ob, value, value.getClass());
        }
        return ob;
    }

    public static Object setFieldValue(String filedName, Object ob, Object value, Class clz) {
        try {
            Method method = ob.getClass().getDeclaredMethod("set" + filedName.substring(0, 1).toUpperCase() + filedName.substring(1), new Class[]{clz});
            return method.invoke(ob, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
