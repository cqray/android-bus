package cn.cqray.android.bus;

import androidx.annotation.Nullable;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 工具类
 * @author LeiJue
 */
final class BusUtils {

    static final String NAME_FOR_CLASS = "class ";
    static final String NAME_FOR_INTERFACE = "interface ";

    /**
     * 从对象中获取类型
     * @param obj 对象
     */
    @Nullable
    public static Class getClassFromObject(final Object obj) {
        if (obj == null) {
            return null;
        }
        Class clazz = obj.getClass();
        if (clazz.isAnonymousClass() || clazz.isSynthetic()) {
            Type[] genericInterfaces = clazz.getGenericInterfaces();
            String className;
            if (genericInterfaces.length == 1) {
                // interface
                Type type = genericInterfaces[0];
                while (type instanceof ParameterizedType) {
                    type = ((ParameterizedType) type).getRawType();
                }
                className = type.toString();
            } else {
                // abstract class or lambda
                Type type = clazz.getGenericSuperclass();
                while (type instanceof ParameterizedType) {
                    type = ((ParameterizedType) type).getRawType();
                }
                if (type == null) {
                    return null;
                }
                className = type.toString();
            }

            if (className.startsWith(NAME_FOR_CLASS)) {
                className = className.substring(6);
            } else if (className.startsWith(NAME_FOR_INTERFACE)) {
                className = className.substring(10);
            }
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return clazz;
    }
}
