package exqudens.persistence.util;

public class BinaryOperators {

    public static <T> T mergeException(T u, T v) {
        throw new IllegalStateException(String.format("Duplicate key %s", u));
    }

    private BinaryOperators() {
        super();
    }

}
