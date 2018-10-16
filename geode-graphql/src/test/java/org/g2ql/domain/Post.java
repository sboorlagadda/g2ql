package org.g2ql.domain;

import java.io.Serializable;

import org.g2ql.annotation.GeodeGraphQLDocumentation;

@GeodeGraphQLDocumentation("A Post is a Post")
public class Post implements Serializable {
  private String author;
  @GeodeGraphQLDocumentation("Title of the post")
  private String title;
  @GeodeGraphQLDocumentation("Contents of the post")
  private String contents;

  public Post() {

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

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }
}
