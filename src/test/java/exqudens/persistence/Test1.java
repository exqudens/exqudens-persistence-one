package exqudens.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import exqudens.persistence.util.ClassPaths;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.MySQLContainer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;

public class Test1 {

    private static MySQLContainer MYSQL_CONTAINER;

    @BeforeClass
    public static void beforeClass() {
        MYSQL_CONTAINER = new MySQLContainer();
        MYSQL_CONTAINER.start();
        Arrays.asList(
                "user",
                "order",
                "item"
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
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(MYSQL_CONTAINER.getJdbcUrl());
            config.setUsername(MYSQL_CONTAINER.getUsername());
            config.setPassword(MYSQL_CONTAINER.getPassword());
            config.setConnectionTimeout(40000L);
            config.setMaximumPoolSize(1);
            HikariDataSource dataSource = new HikariDataSource(config);
            try (Connection connection = dataSource.getConnection()) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute(sql);
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

    @Test
    public void test1() {
        try {

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(MYSQL_CONTAINER.getJdbcUrl());
            config.setUsername(MYSQL_CONTAINER.getUsername());
            config.setPassword(MYSQL_CONTAINER.getPassword());
            config.setConnectionTimeout(40000L);
            config.setMaximumPoolSize(1);
            HikariDataSource dataSource = new HikariDataSource(config);
            try (Connection connection = dataSource.getConnection()) {
                try (Statement statement = connection.createStatement()) {
                    try (ResultSet resultSet = statement.executeQuery("show tables")) {
                        while (resultSet.next()) {
                            System.out.println(resultSet.getString(1));
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
