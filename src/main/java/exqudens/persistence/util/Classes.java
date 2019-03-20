package exqudens.persistence.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Classes {

    public static Set<Entry<Class<?>, Class<?>>> relations(
            List<Class<?>> classes,
            Predicate<Field> fieldPredicate,
            Function<Field, Class<?>> fieldClassFunction
    ) {
        try {
            Set<Entry<Class<?>, Class<?>>> relations = new HashSet<>();
            for (Class<?> c : classes) {
                Set<Class<?>> associatedClasses = Arrays
                        .stream(c.getDeclaredFields())
                        .filter(fieldPredicate)
                        .map(fieldClassFunction)
                        .filter(classes::contains)
                        .collect(Collectors.toSet());
                for (Class<?> associatedClass : associatedClasses) {
                    Entry<Class<?>, Class<?>> newEntry = new SimpleEntry<>(c, associatedClass);
                    Entry<Class<?>, Class<?>> checkEntry = new SimpleEntry<>(associatedClass, c);
                    if (!relations.contains(newEntry) && !relations.contains(checkEntry)) {
                        relations.add(newEntry);
                    }
                }
            }
            return relations;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private Classes() {
        super();
    }

}
