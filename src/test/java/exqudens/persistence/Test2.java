package exqudens.persistence;

import exqudens.persistence.annotation.WriteOrder;
import exqudens.persistence.model.Item;
import exqudens.persistence.model.Order;
import exqudens.persistence.model.Provider;
import exqudens.persistence.model.User;
import exqudens.persistence.util.Functions;
import exqudens.persistence.util.Objects;
import exqudens.persistence.util.Predicates;
import org.junit.Test;

import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import java.lang.reflect.Field;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

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

            Function<Field, String> fieldJoinTableNameFunction = field -> Arrays.stream(field.getAnnotationsByType(JoinTable.class)).map(JoinTable::name).findFirst().orElse(null);

            System.out.println("---");
            List<Object> nodes1 = Objects
                    .nodes(
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
            System.out.println("Expected: " + (providers.size() + users.size() + orders.size() + items.size() + 2) + " Actual: " + nodes1.size());
            System.out.println("---");
            nodes1.stream().map(o -> o.getClass().isArray() ? Arrays.toString((Object[]) o) : o).forEach(System.out::println);
            System.out.println("---");

            System.out.println("---");
            List<Object> nodes2 = Objects
                    .nodes(
                            Object.class,
                            users.stream().map(Object.class::cast).collect(Collectors.toList()),
                            Functions::fieldClass,
                            fieldJoinTableNameFunction,
                            Functions::getterName,
                            Functions::id,
                            Predicates.fieldPredicate(OneToMany.class, ManyToOne.class, OneToOne.class),
                            Predicates.fieldPredicate(ManyToMany.class),
                            classes
                    );
            System.out.println("Expected: " + (providers.size() + users.size() + orders.size() + items.size() + 2) + " Actual: " + nodes2.size());
            System.out.println("---");
            nodes2.stream().map(o -> o.getClass().isArray() ? Arrays.toString((Object[]) o) : o).forEach(System.out::println);
            System.out.println("---");

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

        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

}
