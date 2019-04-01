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
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class TestRead1 {

    private static Logger LOGGER;
    private static MySQLContainer MYSQL_CONTAINER;
    private static String[] TABLE_NAMES;

    @BeforeClass
    public static void beforeClass() {
        LOGGER = LoggerFactory.getLogger(TestRead1.class);
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
        Arrays.stream(TABLE_NAMES).forEach(TestRead1::createTable);
        insert("all");
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

    private static void insert(String name) {
        try {
            String prefix = "insert-";
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
        config.setJdbcUrl(MYSQL_CONTAINER.getJdbcUrl() + "?allowMultiQueries=true&rewriteBatchedStatements=true&serverTimezone=UTC&zeroDateTimeBehavior=convertToNull&characterEncoding=UTF-8&characterSetResults=UTF-8&logger=com.mysql.cj.core.log.Slf4JLogger&profileSQL=true");
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

            Function<Object, Integer> idFunction = Functions::id;
            Function<String, String> getterNameFunction = Functions::getterName;
            Function<String, String> setterNameFunction = Functions::setterName;
            Function<Field, Class<?>> fieldClassFunction = Functions::fieldClass;
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
                        getterNameFunction,
                        columnNameFunction,
                        joinColumnNameFunction,
                        referencedColumnNameFunction,
                        classes
                );
                return row;
            };

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

        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException(t);
        }
    }

}
