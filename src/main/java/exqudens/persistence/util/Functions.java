package exqudens.persistence.util;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

public class Functions {

    public static String getterName(String s) {
        return "get" + s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    public static Integer id(Object o) {
        return System.identityHashCode(o);
    }

    public static Class<?> fieldClass(Field field) {
        if (Collection.class.isAssignableFrom(field.getType())) {
            ParameterizedType parameterizedType = ParameterizedType.class.cast(field.getGenericType());
            Type genericType = parameterizedType.getActualTypeArguments()[0];
            return (Class<?>) genericType;
        }
        return field.getType();
    }

    private Functions() {
        super();
    }

}
