package exqudens.persistence.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Predicates {

    public static Predicate<Field> fieldPredicate(
            List<Class<?>> includeClasses,
            List<Class<?>> excludeClasses,
            List<Class<? extends Annotation>> includeAnnotationClasses,
            List<Class<? extends Annotation>> excludeAnnotationClasses
    ) {
        return field -> {
            Set<Class<? extends Annotation>> fieldAnnotations;
            fieldAnnotations = Arrays
                    .stream(field.getAnnotations())
                    .map(Annotation::annotationType)
                    .collect(Collectors.toSet());
            return (includeClasses == null || includeClasses.contains(field.getType()))
            && (excludeClasses == null || !excludeClasses.contains(field.getType()))
            && (includeAnnotationClasses == null || fieldAnnotations.stream().anyMatch(includeAnnotationClasses::contains))
            && (excludeAnnotationClasses == null || !fieldAnnotations.stream().anyMatch(excludeAnnotationClasses::contains));
        };
    }

    private Predicates() {
        super();
    }

}
