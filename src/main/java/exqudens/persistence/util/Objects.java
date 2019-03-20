package exqudens.persistence.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

    public static <T> List<Object> nodeMap(
            Class<T> objectType,
            List<T> objects,
            Function<Field, Class<?>> fieldClassFunction,
            Function<String, String> getterNameFunction,
            Function<T, Integer> idFunction,
            Predicate<Field> relationFieldPredicate,
            Predicate<Field> manyToManyFieldPredicate,
            Class<?>... classes
    ) {
        try {
            List<Object> result = new ArrayList<>();
            List<Integer> ids = new ArrayList<>();
            Queue<T> queue = new LinkedList<>();
            for (T object : objects) {
                Integer id = idFunction.apply(object);
                if (!ids.contains(id)) {
                    queue.add(object);
                }
            }
            while (!queue.isEmpty()) {
                T nextObject = queue.remove();
                List<Object> children;
                do {
                    children = unvisitedNodeMap(
                            objectType,
                            nextObject,
                            ids,
                            fieldClassFunction,
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
                        if (o instanceof Entry) {
                            Entry<?, ?> entry = (Entry) o;
                            T o1 = objectType.cast(entry.getKey());
                            T o2 = objectType.cast(entry.getValue());
                            Integer id1 = idFunction.apply(o1);
                            Integer id2 = idFunction.apply(o2);
                            boolean present = result
                                    .stream()
                                    .filter(Entry.class::isInstance)
                                    .map(Entry.class::cast)
                                    .map(e -> new SimpleEntry<>(objectType.cast(e.getKey()), objectType.cast(e.getValue())))
                                    .map(e -> new SimpleEntry<>(idFunction.apply(e.getKey()), idFunction.apply(e.getValue())))
                                    .filter(e -> (id1.equals(e.getKey()) && id2.equals(e.getValue())) || (id2.equals(e.getKey()) && id1.equals(e.getValue())))
                                    .findFirst()
                                    .isPresent();
                            if (!present) {
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

    private static <T> List<Object> unvisitedNodeMap(
            Class<T> objectType,
            T object,
            List<Integer> ids,
            Function<Field, Class<?>> fieldClassFunction,
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
                    Collection<?> collection = Collection.class.cast(value);
                    for (Object o : collection) {
                        if (!Arrays.asList(classes).contains(o.getClass())) {
                            break;
                        }
                        Integer id = idFunction.apply(objectType.cast(o));
                        if (!ids.contains(id)) {
                            result.add(objectType.cast(o));
                            result.add(new SimpleEntry<>(object, o));
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
