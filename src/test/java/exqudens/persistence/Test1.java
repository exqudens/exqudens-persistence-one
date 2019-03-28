package exqudens.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import exqudens.persistence.model.Item;
import exqudens.persistence.model.Order;
import exqudens.persistence.model.Provider;
import exqudens.persistence.model.User;
import exqudens.persistence.util.ClassPaths;
import exqudens.persistence.util.Functions;
import exqudens.persistence.util.Objects;
import exqudens.persistence.util.Predicates;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Test1 {

    private static Logger LOGGER;
    private static MySQLContainer MYSQL_CONTAINER;
    private static String[] TABLE_NAMES;

    @BeforeClass
    public static void beforeClass() {
        LOGGER = LoggerFactory.getLogger(Test1.class);
        MYSQL_CONTAINER = new MySQLContainer();
        MYSQL_CONTAINER.start();
        MYSQL_CONTAINER.followOutput(new Slf4jLogConsumer(LOGGER));
        TABLE_NAMES = new String [] {
                "provider",
                "user",
                "order",
                "item",
                "provider_user"
        };
        Arrays.stream(TABLE_NAMES).forEach(Test1::createTable);
    }

    @AfterClass
    public static void afterClass() {
        MYSQL_CONTAINER.stop();
    }

    private static void createTable(String name) {
        try {
            String prefix = "create-table-";
            String suffix = ".sql";
            String sql = ClassPaths.toString(prefix + name + suffix);
            try (HikariDataSource dataSource = hikariDataSource()) {
                try (Connection connection = dataSource.getConnection()) {
                    try (Statement statement = connection.createStatement()) {
                        statement.execute(sql);
                    }
                }
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

    private static HikariDataSource hikariDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(MYSQL_CONTAINER.getJdbcUrl() + "?rewriteBatchedStatements=true&serverTimezone=UTC&zeroDateTimeBehavior=convertToNull&characterEncoding=UTF-8&characterSetResults=UTF-8&logger=com.mysql.cj.core.log.Slf4JLogger&profileSQL=true");
        config.setUsername(MYSQL_CONTAINER.getUsername());
        config.setPassword(MYSQL_CONTAINER.getPassword());
        config.setConnectionTimeout(40000L);
        config.setMaximumPoolSize(1);
        HikariDataSource dataSource = new HikariDataSource(config);
        return dataSource;
    }

    @Test
    public void test1() {
        try {

            List<Provider> providers = new ArrayList<>();
            List<User> users = new ArrayList<>();
            List<Order> orders = new ArrayList<>();
            List<Item> items = new ArrayList<>();

            providers.add(new Provider(null, "label_1", new ArrayList<>()));
            providers.add(new Provider(null, "label_2", new ArrayList<>()));

            users.add(new User(null, "name_1", new ArrayList<>(), null, new ArrayList<>()));

            orders.add(new Order(null, "number_1", null, new ArrayList<>()));
            orders.add(new Order(null, "number_2", null, new ArrayList<>()));

            items.add(new Item(null, "description_1", null, new ArrayList<>()));
            items.add(new Item(null, "description_2", null, new ArrayList<>()));
            items.add(new Item(null, "description_3", null, new ArrayList<>()));

            providers.get(0).getUsers().add(users.get(0));
            providers.get(1).getUsers().add(users.get(0));

            users.get(0).getProviders().add(providers.get(0));
            users.get(0).getProviders().add(providers.get(1));
            users.get(0).setItem(items.get(2));
            users.get(0).getOrders().add(orders.get(0));
            users.get(0).getOrders().add(orders.get(1));

            orders.get(0).setUser(users.get(0));
            orders.get(1).setUser(users.get(0));
            orders.get(0).getItems().add(items.get(0));
            orders.get(1).getItems().add(items.get(1));
            orders.get(1).getItems().add(items.get(2));

            items.get(0).setOrder(orders.get(0));
            items.get(1).setOrder(orders.get(1));
            items.get(2).setOrder(orders.get(1));
            items.get(0).getUsers().add(users.get(0));
            items.get(1).getUsers().add(users.get(0));
            items.get(2).getUsers().add(users.get(0));

            Class<?>[] classes = {
                    Provider.class,
                    User.class,
                    Order.class,
                    Item.class
            };

            Predicate<Field> idFieldPredicate = Predicates.fieldPredicate(null, null, Arrays.asList(Id.class), null);

            Function<Class<?>, String> tableNameFunction = c -> Stream.of(c.getAnnotationsByType(Table.class)).map(Table::name).findFirst().orElse(null);
            Function<Field, String> fieldJoinTableNameFunction = field -> Arrays.stream(field.getAnnotationsByType(JoinTable.class)).map(JoinTable::name).findFirst().orElse(null);
            Function<Field, String> columnNameFunction = field -> Arrays.stream(field.getAnnotationsByType(Column.class)).map(Column::name).findFirst().orElse(null);
            Function<Field, String> joinColumnNameFunction = field -> Arrays.stream(field.getAnnotationsByType(JoinColumn.class)).map(JoinColumn::name).findFirst().orElse(null);
            Function<Field, String> referencedColumnNameFunction = field -> Arrays.stream(field.getAnnotationsByType(JoinColumn.class)).map(JoinColumn::referencedColumnName).findFirst().orElse(null);
            Function<Object, Map<String, Object>> rowFunction = o -> {
                Map<String, Object> row;
                row = Objects.row(
                        o,
                        Predicates.fieldPredicate(null, null, Arrays.asList(Column.class, JoinColumn.class), Arrays.asList(OneToMany.class)),
                        idFieldPredicate,
                        tableNameFunction,
                        Functions::getterName,
                        columnNameFunction,
                        joinColumnNameFunction,
                        referencedColumnNameFunction,
                        classes
                );
                return row;
            };

            List<Object> createQueue = new ArrayList<>();
            List<Object> updateQueue = new ArrayList<>();

            List<Map<String, Object>> newDbState = new ArrayList<>();

            Map<Integer, List<Integer>> createBatch = new TreeMap<>();
            Map<Integer, List<Integer>> updateBatch = new TreeMap<>();

            Objects.nodes(
                    Object.class,
                    providers.stream().map(Object.class::cast).collect(Collectors.toList()),
                    Functions::fieldClass,
                    fieldJoinTableNameFunction,
                    Functions::getterName,
                    Functions::id,
                    Predicates.fieldPredicate(null, null, Arrays.asList(OneToMany.class, ManyToOne.class, OneToOne.class), null),
                    Predicates.fieldPredicate(null, null, Arrays.asList(ManyToMany.class), null),
                    classes
            ).forEach(createQueue::add);

            createQueue.stream().map(rowFunction).forEach(newDbState::add);

            for (int i = 0; i < createQueue.size(); i++) {
                Object o = createQueue.get(i);
                Integer key;
                if (Arrays.asList(classes).contains(o.getClass())) {
                    key = Integer.valueOf(o.getClass().getAnnotationsByType(Entity.class)[0].name());
                } else {
                    key = Integer.MAX_VALUE;
                }
                createBatch.putIfAbsent(key, new ArrayList<>());
                createBatch.get(key).add(i);
            }

            try (HikariDataSource dataSource = hikariDataSource()) {
                try (Connection connection = dataSource.getConnection()) {
                    connection.setAutoCommit(false);

                    for (List<Integer> indexes : createBatch.values()) {
                        List<Object> objects = indexes.stream().map(createQueue::get).collect(Collectors.toList());

                        if (objects.get(0).getClass().isArray()) {

                            List<Map<String, Object>> rows = objects.stream().map(rowFunction).collect(Collectors.toList());

                            String sql = String.join(
                                    "",
                                    "insert into `",
                                    objects.get(0).getClass().isArray() ? ((Object[]) objects.get(0))[2].toString() : tableNameFunction.apply(objects.get(0).getClass()),
                                    "`(`",
                                    String.join("`, `", rows.get(0).keySet()),
                                    "`) values(",
                                    String.join(", ", rows.get(0).keySet().stream().map(s -> "?").collect(Collectors.toList())),
                                    ")"
                            );

                            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                                for (Map<String, Object> row : rows) {
                                    List<Object> args = new ArrayList<>(row.values());
                                    for (int i = 0; i < row.size(); i++) {
                                        statement.setObject(i + 1, args.get(i));
                                    }
                                    statement.addBatch();
                                }
                                statement.executeBatch();
                            }

                        } else {

                            List<Map<String, Object>> rows = objects.stream().map(rowFunction).collect(Collectors.toList());

                            String sql = String.join(
                                    "",
                                    "insert into `",
                                    objects.get(0).getClass().isArray() ? ((Object[]) objects.get(0))[2].toString() : tableNameFunction.apply(objects.get(0).getClass()),
                                    "`(`",
                                    String.join("`, `", rows.get(0).keySet()),
                                    "`) values(",
                                    String.join(", ", rows.get(0).keySet().stream().map(s -> "?").collect(Collectors.toList())),
                                    ")"
                            );

                            List<Object> ids = new ArrayList<>();
                            try (PreparedStatement statement = connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
                                for (Map<String, Object> row : rows) {
                                    List<Object> args = new ArrayList<>(row.values());
                                    for (int i = 0; i < row.size(); i++) {
                                        statement.setObject(i + 1, args.get(i));
                                    }
                                    statement.addBatch();
                                }
                                statement.executeBatch();
                                Class<?> type = Arrays
                                        .stream(objects.get(0).getClass().getDeclaredFields())
                                        .filter(idFieldPredicate)
                                        .map(Field::getType)
                                        .findFirst()
                                        .orElse(null);
                                try (ResultSet resultSet = statement.getGeneratedKeys()) {
                                    while (resultSet.next()) {
                                        ids.add(resultSet.getObject(1, type));
                                    }
                                }
                            }

                            for (int i = 0; i < ids.size(); i++) {
                                Object node = objects.get(i);
                                Field field = Arrays
                                        .stream(node.getClass().getDeclaredFields())
                                        .filter(idFieldPredicate)
                                        .findFirst()
                                        .orElse(null);
                                String setterName = Functions.setterName(field.getName());
                                node.getClass().getDeclaredMethod(setterName, field.getType()).invoke(node, ids.get(i));
                            }

                        }

                        for (int i = 0; i < objects.size(); i++) {
                            int index = indexes.get(i);
                            Object o = createQueue.get(index);
                            Map<String, Object> row = rowFunction.apply(o);
                            if (!newDbState.get(index).equals(row)) {
                                newDbState.remove(index);
                                newDbState.add(index, row);
                            }
                        }
                    }

                    createBatch.clear();

                    for (int i = 0; i < createQueue.size(); i++) {
                        Object o = createQueue.get(i);
                        Map<String, Object> row = rowFunction.apply(o);
                        if (!newDbState.get(i).equals(row)) {
                            updateQueue.add(o);
                            newDbState.remove(i);
                            newDbState.add(i, row);
                        }
                    }

                    createQueue.clear();
                    newDbState.clear();

                    updateQueue.stream().map(rowFunction).forEach(newDbState::add);

                    for (int i = 0; i < updateQueue.size(); i++) {
                        Object o = updateQueue.get(i);
                        Integer key;
                        if (Arrays.asList(classes).contains(o.getClass())) {
                            key = Integer.valueOf(o.getClass().getAnnotationsByType(Entity.class)[0].name());
                        } else {
                            key = Integer.MAX_VALUE;
                        }
                        updateBatch.putIfAbsent(key, new ArrayList<>());
                        updateBatch.get(key).add(i);
                    }

                    for (List<Integer> indexes : updateBatch.values()) {
                        List<Object> objects = indexes.stream().map(updateQueue::get).collect(Collectors.toList());

                        List<Map<String, Object>> rows = objects.stream().map(rowFunction).collect(Collectors.toList());

                        String sql = String.join(
                                "",
                                "insert into `",
                                objects.get(0).getClass().isArray() ? ((Object[]) objects.get(0))[2].toString() : tableNameFunction.apply(objects.get(0).getClass()),
                                "`(`",
                                String.join("`, `", rows.get(0).keySet()),
                                "`) values(",
                                String.join(", ", rows.get(0).keySet().stream().map(s -> "?").collect(Collectors.toList())),
                                ")",
                                " on duplicate key update ",
                                rows.get(0).keySet().stream().map(c -> "`" + c + "`" + " = values(`" + c + "`)").collect(Collectors.joining(", "))
                        );

                        try (PreparedStatement statement = connection.prepareStatement(sql)) {
                            for (Map<String, Object> row : rows) {
                                List<Object> args = new ArrayList<>(row.values());
                                for (int i = 0; i < row.size(); i++) {
                                    statement.setObject(i + 1, args.get(i));
                                }
                                statement.addBatch();
                            }
                            statement.executeBatch();
                        }

                    }

                    updateBatch.clear();
                    createQueue.clear();
                    newDbState.clear();

                    connection.commit();
                }
            }

            try (HikariDataSource dataSource = hikariDataSource()) {
                try (Connection connection = dataSource.getConnection()) {
                    for (String tableName : TABLE_NAMES) {
                        System.out.println("---");
                        System.out.println(tableName);
                        System.out.println("---");
                        try (Statement statement = connection.createStatement()) {
                            try (ResultSet resultSet = statement.executeQuery("select * from `" + tableName + "`")) {
                                ResultSetMetaData metaData = resultSet.getMetaData();
                                while (resultSet.next()) {
                                    Map<String, Object> row = new LinkedHashMap<>();
                                    for (int i = 1; i <= metaData.getColumnCount(); i++) {
                                        row.put(metaData.getColumnName(i), resultSet.getObject(i));
                                    }
                                    System.out.println(row);
                                }
                            }
                        }
                    }
                }
            }

            System.out.println("---");
            newDbState.stream().forEach(System.out::println);

        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

}
