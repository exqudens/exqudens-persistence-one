package exqudens.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import exqudens.persistence.annotation.WriteOrder;
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

import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Test1 {

    private static Logger LOGGER;
    private static MySQLContainer MYSQL_CONTAINER;

    @BeforeClass
    public static void beforeClass() {
        LOGGER = LoggerFactory.getLogger(Test1.class);
        MYSQL_CONTAINER = new MySQLContainer();
        MYSQL_CONTAINER.start();
        MYSQL_CONTAINER.followOutput(new Slf4jLogConsumer(LOGGER));
        Arrays.asList(
                "provider",
                "user",
                "order",
                "item",
                "provider_user"
        ).forEach(Test1::createTable);
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
        config.setJdbcUrl(MYSQL_CONTAINER.getJdbcUrl());
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

            Function<Field, String> fieldJoinTableNameFunction = field -> Arrays.stream(field.getAnnotationsByType(JoinTable.class)).map(JoinTable::name).findFirst().orElse(null);

            Class<?>[] classes = {
                    Provider.class,
                    User.class,
                    Order.class,
                    Item.class
            };

            List<Object> nodes = Objects.nodes(
                    Object.class,
                    providers.stream().map(Object.class::cast).collect(Collectors.toList()),
                    Functions::fieldClass,
                    fieldJoinTableNameFunction,
                    Functions::getterName,
                    Functions::id,
                    Predicates.fieldPredicate(OneToMany.class, ManyToOne.class, OneToOne.class),
                    Predicates.fieldPredicate(ManyToMany.class),
                    classes
            );

            Map<Integer, List<Object>> collect = nodes
                    .stream()
                    .map(o -> {
                        Entry<Integer, Object> entry;
                        if (Arrays.asList(classes).contains(o.getClass())) {
                            entry = new SimpleEntry<>(o.getClass().getAnnotationsByType(WriteOrder.class)[0].value(), o);
                        } else {
                            entry = new SimpleEntry<>(Integer.MAX_VALUE, o);
                        }
                        return entry;
                    }).collect(Collectors.groupingBy(Entry::getKey, TreeMap::new, Collectors.mapping(Entry::getValue, Collectors.toList())));

            collect.entrySet().forEach(System.out::println);

            try (HikariDataSource dataSource = hikariDataSource()) {
                try (Connection connection = dataSource.getConnection()) {
                    try (Statement statement = connection.createStatement()) {
                        try (ResultSet resultSet = statement.executeQuery("show tables")) {
                            while (resultSet.next()) {
                                System.out.println(resultSet.getString(1));
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
