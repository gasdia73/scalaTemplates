package org.guz.persistence;


import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.UUID;


@Table(name = "user")
public class User {
    @PartitionKey
    @Column(name="id")
    private UUID id;
    @Column(name="name")
    private String name;

    @Column(name="age")
    private Integer age;

    // Default constructor
    public User() {
    }

    // Parameterized constructor
    public User(UUID id, String name, int age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    // toString method for easy printing
    @Override
    public String toString() {
        return "ExampleEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}
