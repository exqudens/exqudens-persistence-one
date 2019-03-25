package exqudens.persistence.util;

public class BiFunctions {

    public static <T> T mergeException(T u, T v) {
        throw new IllegalStateException(String.format("Duplicate key %s", u));
    }

    private BiFunctions() {
        super();
    }

}
