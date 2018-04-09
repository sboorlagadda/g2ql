package org.g2ql.domain;

import org.g2ql.annotation.GeodeGraphQLIgnore;

@GeodeGraphQLIgnore
public class Student {
  private String id;
  private String name;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
