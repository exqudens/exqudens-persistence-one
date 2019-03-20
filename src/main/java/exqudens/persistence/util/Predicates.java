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
            Class<? extends Annotation>... includeAnnotationClasses
    ) {
        return fieldPredicate(Arrays.asList(includeAnnotationClasses), Collections.emptyList());
    }

    public static Predicate<Field> fieldPredicate(
            List<Class<? extends Annotation>> includeAnnotationClasses,
            List<Class<? extends Annotation>> excludeAnnotationClasses
    ) {
        return field -> {
            Set<Class<? extends Annotation>> fieldAnnotations = Arrays.stream(field.getAnnotations()).map(Annotation::annotationType).collect(Collectors.toSet());
            if (
                    fieldAnnotations.stream().filter(includeAnnotationClasses::contains).findFirst().isPresent()
                    && !fieldAnnotations.stream().filter(excludeAnnotationClasses::contains).findFirst().isPresent()
            ) {
                return true;
            }
            return false;
        };
    }

    private Predicates() {
        super();
    }

}
