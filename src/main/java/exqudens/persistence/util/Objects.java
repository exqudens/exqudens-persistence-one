package exqudens.persistence.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Objects {

    public static Map<String, Object> row(
            Object object,
            Predicate<Field> fieldPredicate,
            Function<String, String> getterNameFunction,
            Function<Field, String> columnNameFunction,
            Class<?>... classes
    ) {
        try {
            Map<String, Object> row = new HashMap<>();
            for (Field field : object.getClass().getDeclaredFields()) {
                if (fieldPredicate.test(field)) {
                    Method method = object.getClass().getDeclaredMethod(getterNameFunction.apply(field.getName()));
                    String key = columnNameFunction.apply(field);
                    Object value = method.invoke(object);
                    if (value == null) {
                        continue;
                    }
                    if (Arrays.asList(classes).contains(value.getClass())) {
                        row.putIfAbsent(key, "123abc");
                    } else {
                        row.putIfAbsent(key, value);
                    }
                }
            }
            return row;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static <T> List<Object> nodes(
            Class<T> objectType,
            List<T> objects,
            Function<Field, Class<?>> fieldClassFunction,
            Function<Field, String> fieldJoinTableNameFunction,
            Function<String, String> getterNameFunction,
            Function<T, Integer> idFunction,
            Predicate<Field> relationFieldPredicate,
            Predicate<Field> manyToManyFieldPredicate,
            Class<?>... classes
    ) {
        try {
            List<Object> result = new ArrayList<>();
            List<Integer> ids = new ArrayList<>();
            Queue<T> queue = new LinkedList<>(objects);
            while (!queue.isEmpty()) {
                T nextObject = queue.remove();
                List<Object> children;
                do {
                    children = unvisitedNodes(
                            objectType,
                            nextObject,
                            ids,
                            fieldClassFunction,
                            fieldJoinTableNameFunction,
                            getterNameFunction,
                            idFunction,
                            relationFieldPredicate,
                            manyToManyFieldPredicate,
                            classes
                    );
                    if (children.isEmpty()) {
                        break;
                    }
                    for (Object o : children) {
                        if (o.getClass().isArray()) {
                            Object[] array = (Object[]) o;
                            T o1 = objectType.cast(array[0]);
                            T o2 = objectType.cast(array[1]);
                            Integer id1 = idFunction.apply(o1);
                            Integer id2 = idFunction.apply(o2);
                            boolean anyMatch = result
                                    .stream()
                                    .filter(obj -> obj.getClass().isArray())
                                    .map(obj -> (Object[]) obj)
                                    .map(arr -> new Object[] { idFunction.apply(objectType.cast(arr[0])), idFunction.apply(objectType.cast(arr[1])), arr[2] })
                                    .anyMatch(arr -> (id1.equals(arr[0]) && id2.equals(arr[1])) || (id2.equals(arr[0]) && id1.equals(arr[1])));
                            if (!anyMatch) {
                                result.add(o);
                            }
                        } else if (objectType.isInstance(o)) {
                            T child = objectType.cast(o);
                            result.add(child);
                            queue.add(child);
                        } else {
                            throw new IllegalArgumentException();
                        }
                    }
                } while (!children.isEmpty());
            }
            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static <T> List<Object> unvisitedNodes(
            Class<T> objectType,
            T object,
            List<Integer> ids,
            Function<Field, Class<?>> fieldClassFunction,
            Function<Field, String> fieldJoinTableNameFunction,
            Function<String, String> getterNameFunction,
            Function<T, Integer> idFunction,
            Predicate<Field> relationFieldPredicate,
            Predicate<Field> manyToManyFieldPredicate,
            Class<?>... classes
    ) {
        try {
            List<Object> result = new ArrayList<>();

            Set<String> fieldNames = Arrays
                    .stream(object.getClass().getDeclaredFields())
                    .filter(relationFieldPredicate)
                    .filter(f -> Arrays.asList(classes).contains(fieldClassFunction.apply(f)))
                    .map(Field::getName)
                    .collect(Collectors.toSet());
            Map<String, String> fieldMethodMap = fieldNames
                    .stream()
                    .map(fieldName -> new SimpleEntry<>(fieldName, getterNameFunction.apply(fieldName)))
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
            for (String fieldName : fieldMethodMap.keySet()) {
                String methodName = fieldMethodMap.get(fieldName);
                Method method = object.getClass().getMethod(methodName);
                Object value = method.invoke(object);
                if (value == null) {
                    continue;
                }
                Class<?> type = value.getClass();
                if (!Collection.class.isAssignableFrom(type)) {
                    if (!Arrays.asList(classes).contains(value.getClass())) {
                        continue;
                    }
                    Integer id = idFunction.apply(objectType.cast(value));
                    if (!ids.contains(id)) {
                        result.add(objectType.cast(value));
                        ids.add(id);
                    }
                } else {
                    Collection<?> collection = (Collection<?>) value;
                    for (Object o : collection) {
                        if (!Arrays.asList(classes).contains(o.getClass())) {
                            break;
                        }
                        Integer id = idFunction.apply(objectType.cast(o));
                        if (!ids.contains(id)) {
                            result.add(objectType.cast(o));
                            ids.add(id);
                        }
                    }
                }
            }

            if (manyToManyFieldPredicate != null) {
                fieldNames = Arrays
                        .stream(object.getClass().getDeclaredFields())
                        .filter(manyToManyFieldPredicate)
                        .filter(f -> Arrays.asList(classes).contains(fieldClassFunction.apply(f)))
                        .map(Field::getName)
                        .collect(Collectors.toSet());
                fieldMethodMap = fieldNames
                        .stream()
                        .map(fieldName -> new SimpleEntry<>(fieldName, getterNameFunction.apply(fieldName)))
                        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
                for (String fieldName : fieldMethodMap.keySet()) {
                    String methodName = fieldMethodMap.get(fieldName);
                    Method method = object.getClass().getMethod(methodName);
                    Object value = method.invoke(object);
                    if (value == null) {
                        continue;
                    }
                    Class<?> type = value.getClass();
                    if (!Collection.class.isAssignableFrom(type)) {
                        continue;
                    }
                    Collection<?> collection = (Collection) value;
                    for (Object o : collection) {
                        if (!Arrays.asList(classes).contains(o.getClass())) {
                            break;
                        }
                        Integer id = idFunction.apply(objectType.cast(o));
                        if (!ids.contains(id)) {
                            result.add(objectType.cast(o));
                            result.add(new Object[] {object, o, fieldJoinTableNameFunction.apply(object.getClass().getDeclaredField(fieldName))});
                            ids.add(id);
                        }
                    }
                }
            }

            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private Objects() {
        super();
    }

}
