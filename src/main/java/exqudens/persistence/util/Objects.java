package exqudens.persistence.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
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

    public static <T> Map<Integer, T> nodeMap(
            Class<T> objectType,
            List<T> objects,
            Function<Field, Class<?>> fieldClassFunction,
            Function<String, String> getterNameFunction,
            Function<T, Integer> idFunction,
            Predicate<Field> manyToManyFieldPredicate,
            Class<?>... classes
    ) {
        try {
            Map<Integer, T> result = new LinkedHashMap<>();
            List<Integer> ids = new ArrayList<>();
            Queue<T> queue = new LinkedList<>();
            for (T object : objects) {
                Integer id = idFunction.apply(object);
                result.putIfAbsent(id, object);
                queue.add(object);
                ids.add(id);
            }
            while (!queue.isEmpty()) {
                T nextObject = queue.remove();
                Map<Integer, T> children;
                do {
                    children = unvisitedNodeMap(
                            objectType,
                            nextObject,
                            ids,
                            fieldClassFunction,
                            getterNameFunction,
                            idFunction,
                            manyToManyFieldPredicate,
                            classes
                    );
                    if (children.isEmpty()) {
                        break;
                    }
                    for (Integer childId : children.keySet()) {
                        T child = children.get(childId);
                        ids.add(childId);
                        result.putIfAbsent(childId, child);
                        queue.add(child);
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

    private static <T> Map<Integer, T> unvisitedNodeMap(
            Class<T> objectType,
            T object,
            List<Integer> ids,
            Function<Field, Class<?>> fieldClassFunction,
            Function<String, String> getterNameFunction,
            Function<T, Integer> idFunction,
            Predicate<Field> manyToManyFieldPredicate,
            Class<?>... classes
    ) {
        try {
            Map<Integer, T> result = new LinkedHashMap<>();

            Set<String> fieldNames = Arrays
                    .stream(object.getClass().getDeclaredFields())
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
                    if (ids == null || !ids.contains(id)) {
                        result.putIfAbsent(id, objectType.cast(value));
                    }
                } else {
                    Collection<?> collection = (Collection<?>) value;
                    for (Object o : collection) {
                        if (!Arrays.asList(classes).contains(o.getClass())) {
                            break;
                        }
                        Integer id = idFunction.apply(objectType.cast(o));
                        if (ids == null || !ids.contains(id)) {
                            result.putIfAbsent(id, objectType.cast(o));
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
                    Collection<?> collection = Collection.class.cast(value);
                    for (Object o : collection) {
                        if (!Arrays.asList(classes).contains(o.getClass())) {
                            break;
                        }
                        Integer id = idFunction.apply(objectType.cast(o));
                        if (ids == null || !ids.contains(id)) {
                            result.putIfAbsent(id, objectType.cast(o));
                            result.putIfAbsent(1, objectType.cast(new SimpleEntry<>(object, o)));
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
