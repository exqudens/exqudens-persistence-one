package exqudens.persistence;

import exqudens.persistence.model.Item;
import exqudens.persistence.model.Order;
import exqudens.persistence.model.Provider;
import exqudens.persistence.model.User;
import exqudens.persistence.util.Functions;
import exqudens.persistence.util.Objects;
import exqudens.persistence.util.Predicates;
import org.junit.Test;

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
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Test2 {

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

        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

}
