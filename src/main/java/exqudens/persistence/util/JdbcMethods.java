package exqudens.persistence.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class JdbcMethods {

    public static void insert(
            Connection connection,
            String jdbcSql,
            List<List<Object>> rows
    ) {
        writeOperation(connection, jdbcSql, rows);
    }

    public static <T> List<T> insert(
            Connection connection,
            String jdbcSql,
            List<List<Object>> rows,
            Class<T> type
    ) {
        return writeOperation(
                connection,
                jdbcSql,
                rows,
                Collections.singletonList(type)
        )
                .stream()
                .map(l -> l.get(0))
                .map(type::cast)
                .collect(Collectors.toList());
    }

    private static void writeOperation(
            Connection connection,
            String jdbcSql,
            List<List<Object>> rows
    ) {
        try {
            try (PreparedStatement statement = connection.prepareStatement(jdbcSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                for (List<Object> row : rows) {
                    for (int i = 0; i < row.size(); i++) {
                        statement.setObject(i + 1, row.get(i));
                    }
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static List<List<Object>> writeOperation(
            Connection connection,
            String jdbcSql,
            List<List<Object>> rows,
            List<Class<?>> types
    ) {
        try {
            List<List<Object>> list = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(jdbcSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                for (List<Object> row : rows) {
                    for (int i = 0; i < row.size(); i++) {
                        statement.setObject(i + 1, row.get(i));
                    }
                    statement.addBatch();
                }
                statement.executeBatch();
                try (ResultSet resultSet = statement.getGeneratedKeys()) {
                    while (resultSet.next()) {
                        List<Object> row = new ArrayList<>();
                        for (int i = 0; i < types.size(); i++) {
                            Object object = resultSet.getObject(i + 1, types.get(i));
                            row.add(object);
                        }
                        list.add(row);
                    }
                }
            }
            return list;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    /*private static List<Map<String, Object>> writeOperation(
            Connection connection,
            String jdbcSql,
            List<Map<String, Object>> rows,
            Map<String, Class<?>> classMap
    ) {
        try {
            List<Map<String, Object>> list = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(jdbcSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                for (Map<String, Object> row : rows) {
                    List<Object> args = new ArrayList<>(row.values());
                    for (int i = 0; i < row.size(); i++) {
                        statement.setObject(i + 1, args.get(i));
                    }
                    statement.addBatch();
                }
                statement.executeBatch();
                try (ResultSet resultSet = statement.getGeneratedKeys()) {
                    while (resultSet.next()) {
                        Map<String, Object> map = new LinkedHashMap<>();
                        for (Entry<String, Class<?>> entry : classMap.entrySet()) {
                            map.putIfAbsent(entry.getKey(), resultSet.getObject(entry.getKey(), entry.getValue()));
                        }
                        list.add(map);
                    }
                }
            }
            return list;
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }*/

    private JdbcMethods() {
        super();
    }

}
