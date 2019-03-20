package exqudens.persistence;

import exqudens.persistence.model.Item;
import exqudens.persistence.model.Order;
import exqudens.persistence.model.Provider;
import exqudens.persistence.model.User;
import exqudens.persistence.util.Functions;
import exqudens.persistence.util.Objects;
import exqudens.persistence.util.Predicates;
import org.junit.Test;

import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Test2 {

    @Test
    public void test1() {

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

        System.out.println("---");
        List<Object> nodes1 = Objects
                .nodeMap(
                        Object.class,
                        providers.stream().map(Object.class::cast).collect(Collectors.toList()),
                        Functions::fieldClass,
                        Functions::getterName,
                        Functions::id,
                        Predicates.fieldPredicate(OneToMany.class, ManyToOne.class, OneToOne.class),
                        Predicates.fieldPredicate(ManyToMany.class),
                        Provider.class, User.class, Order.class, Item.class
                );
        System.out.println(nodes1.size());
        System.out.println("---");
        nodes1.stream().forEach(System.out::println);
        System.out.println("---");

        System.out.println("---");
        List<Object> nodes2 = Objects
                .nodeMap(
                        Object.class,
                        users.stream().map(Object.class::cast).collect(Collectors.toList()),
                        Functions::fieldClass,
                        Functions::getterName,
                        Functions::id,
                        Predicates.fieldPredicate(OneToMany.class, ManyToOne.class, OneToOne.class),
                        Predicates.fieldPredicate(ManyToMany.class),
                        Provider.class, User.class, Order.class, Item.class
                );
        System.out.println(nodes2.size());
        System.out.println("---");
        nodes2.stream().forEach(System.out::println);
        System.out.println("---");

        /*System.out.println("---");
        Classes
                .relations(
                        Arrays.asList(User.class, Order.class, Item.class),
                        Arrays.asList(OneToOne.class, OneToMany.class, ManyToOne.class, ManyToMany.class),
                        Collections.emptyList()
                ).forEach(System.out::println);
        System.out.println("---");*/
    }

}
