package org.g2ql.domain;

import java.io.Serializable;

import org.g2ql.annotation.GeodeGraphQLArgument;
import org.g2ql.annotation.GeodeGraphQLDocumentation;

@GeodeGraphQLDocumentation("A Post is a Post")
public class Post implements Serializable {
  @GeodeGraphQLArgument
  private String author;
  @GeodeGraphQLDocumentation("Description about the image")
  private String title;
  @GeodeGraphQLDocumentation("Url of the image")
  private String contents;

  public Post() {

  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getContents() {
    return contents;
  }

  public void setContents(String contents) {
    this.contents = contents;
  }
}
