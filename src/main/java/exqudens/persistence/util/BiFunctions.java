package exqudens.persistence.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BiFunctions {

    public static Entry<String, List<Object>> jdbcEntry(String sql, List<Object> args) {
        try {
            List<List<Object>> listOfLists = Arrays
                    .asList(args)
                    .stream()
                    .map(o -> Collection.class.isInstance(o) ? Collection.class.cast(o).toArray() : o)
                    .map(o -> o == null ? new Object[]{o} : o)
                    .map(o -> !o.getClass().isArray() ? new Object[]{o} : o)
                    .map(o -> Arrays.asList(Object[].class.cast(o)))
                    .collect(Collectors.toList());

            AtomicInteger argsIndex = new AtomicInteger(0);

            List<Entry<String, List<Object>>> listEntries = sql
                    .chars()
                    .mapToObj(i -> String.valueOf(Character.toChars(i)))
                    .map(s -> "?".equals(s) ? new SimpleEntry<>(s, listOfLists.get(argsIndex.getAndIncrement())) : new SimpleEntry<>(s, Collections.emptyList()))
                    .peek(e -> {
                        if ("?".equals(e.getKey()) && e.getValue().isEmpty()) {
                            throw new IllegalStateException("'?' #" + argsIndex.get() + " is empty! sql: " + sql);
                        }
                    })
                    .map(e -> {
                        if (e.getValue().size() > 0) {
                            String newKey = IntStream
                                    .range(0, e.getValue().size())
                                    .mapToObj(i -> "?")
                                    .collect(Collectors.joining(","));
                            return new SimpleEntry<>(newKey, e.getValue());
                        } else {
                            return new SimpleEntry<>(e);
                        }
                    })
                    .collect(Collectors.toList());

            String newSql = listEntries
                    .stream()
                    .map(e -> e.getKey())
                    .collect(Collectors.joining());

            List<Object> newArgs = listEntries
                    .stream()
                    .map(Entry::getValue)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

            return new SimpleEntry<>(newSql, newArgs);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static Entry<Boolean, String> jdbcEntryCheck(String sql, List<Object> args) {
        try {
            boolean passCheck = true;
            String errorMessage = "";
            long countPlaceHolders = sql.chars().filter(c -> '?' == c).count();
            if (countPlaceHolders > Integer.MAX_VALUE) {
                passCheck = false;
                errorMessage = "Count of '?' is greater than integer max value!";
            }
            if (args.size() != countPlaceHolders) {
                passCheck = false;
                errorMessage = "Count of '?' = " + countPlaceHolders + " not equals to count of 'args' = " + args.size();
            }
            return new SimpleEntry<>(passCheck, errorMessage);
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static List<Object> executeInsert(Entry<String, List<Object>> je, DataSource ds) {
        try {

            String sql = je.getKey();
            List<Object> objects = je.getValue();
            try (Connection c = ds.getConnection()) {
                try (PreparedStatement ps = c.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                    for (int i = 0; i < objects.size(); i++) {
                        ps.setObject(i + 1, objects.get(i));
                    }
                }
                return null;
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static <T> T mergeException(T u, T v) {
        throw new IllegalStateException(String.format("Duplicate key %s", u));
    }

    private BiFunctions() {
        super();
    }

}
