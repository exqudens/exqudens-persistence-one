package exqudens.persistence.model;

import exqudens.persistence.annotation.WriteOrder;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import java.util.List;

@WriteOrder(1)
@Table(name = "provider")
public class Provider {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "label")
    private String label;

    @ManyToMany
    @JoinTable(name = "provider_user")
    private List<User> users;

    public Provider() {
    }

    public Provider(Long id, String label, List<User> users) {
        this.id = id;
        this.label = label;
        this.users = users;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    @Override
    public String toString() {
        return "Provider{" +
                "id=" + id +
                ", label='" + label + '\'' +
                '}';
    }

}
