package org.g2ql.domain;

import org.g2ql.annotation.GeodeGraphQLArgument;
import org.g2ql.annotation.GeodeGraphQLConnection;
import org.g2ql.annotation.GeodeGraphQLDocumentation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@GeodeGraphQLDocumentation("A person is a person")
public class Person implements Serializable {
  private String id;
  @GeodeGraphQLArgument
  private String firstName;
  private String lastName;
  private int age;
  @GeodeGraphQLDocumentation("Company in which the person is working")
  private String company;
  private Address address;

  @GeodeGraphQLConnection("Person")
  private List<String> friends = new ArrayList<>();// store keys to persons

  public Person() {}

  public Person(String id, String firstName, String lastName, int age, String company) {
    this.id = id;
    this.firstName = firstName;
    this.lastName = lastName;
    this.age = age;
    this.company = company;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public int getAge() {
    return age;
  }

  public void setAge(int age) {
    this.age = age;
  }

  public String getCompany() {
    return company;
  }

  public void setCompany(String company) {
    this.company = company;
  }

  public Address getAddress() {
    return address;
  }

  public void setAddress(Address address) {
    this.address = address;
  }

  public List<String> getFriends() {
    return friends;
  }

  public class Address implements Serializable {
    private String street;
    private String city;
    private String country;

    public Address(String street, String city, String country) {
      this.street = street;
      this.city = city;
      this.country = country;
    }

    public String getStreet() {
      return street;
    }

    public void setStreet(String street) {
      this.street = street;
    }

    public String getCity() {
      return city;
    }

    public void setCity(String city) {
      this.city = city;
    }

    public String getCountry() {
      return country;
    }

    public void setCountry(String country) {
      this.country = country;
    }
  }
}
