package org.g2ql.graphql;

import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.g2ql.categories.IntegrationTest;
import org.g2ql.domain.Person;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicResponseHandler;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Category(IntegrationTest.class)
public class GraphqlExecutorIntegrationTest {

  @BeforeClass
  public static void before() {
    ClientCache cache = new ClientCacheFactory().addPoolLocator("127.0.0.1", 10334)
        .set("log-level", "WARN").create();
    Region<String, Person> person = cache
        .<String, Person>createClientRegionFactory(ClientRegionShortcut.PROXY).create("Person");

    Person james = new Person("1", "James", "Gosling", 60, "AWS");
    james.setAddress(james.new Address("1 Pike Street", "Seattle", "USA"));
    james.getFriends().add("2");

    Person joshua = new Person("2", "Joshua", "Bloch", 50, "Google");
    joshua.setAddress(joshua.new Address("1600 Parkway ", "Mountain View", "USA"));
    joshua.getFriends().add("1");

    person.put("1", james);
    person.put("2", joshua);

    Region foo = cache.createClientRegionFactory(ClientRegionShortcut.PROXY).create("Foo");
    foo.put("1", "One");
    foo.put("2", "Two");
  }

  @Test
  public void testGraphQLWithSinglePersonKey() throws IOException {
    String query =
        "{\"query\":\"{\\n  Person(key: \\\"1\\\") {\\n    firstName\\n    age\\n  }\\n}\\n\"}";
    HttpResponse response = Request.Post("http://localhost:3000/graphql")
        .bodyString(query, ContentType.TEXT_PLAIN).execute().returnResponse();
    String responseString = new BasicResponseHandler().handleResponse(response);
    assertThat(responseString)
        .isEqualTo("{\"data\":{\"Person\":{\"firstName\":\"James\",\"age\":60}}}");
  }

  @Test
  public void testGraphQLWithSinglePersonKeyWithAddress() throws IOException {
    String query =
        "{\"query\":\"{\\n  Person(key: \\\"1\\\") {\\n    firstName\\n    age\\n    address {\\n     street\\n   city\\n}\\n}\\n\\n}\"}";
    HttpResponse response = Request.Post("http://localhost:3000/graphql")
        .bodyString(query, ContentType.TEXT_PLAIN).execute().returnResponse();
    String responseString = new BasicResponseHandler().handleResponse(response);
    assertThat(responseString).isEqualTo(
        "{\"data\":{\"Person\":{\"firstName\":\"James\",\"age\":60,\"address\":{\"street\":\"1 Pike Street\",\"city\":\"Seattle\"}}}}");
  }

  @Test
  public void testGraphQLWithMultiplePersonKeys() throws IOException {
    String query =
        "{\"query\":\"{\\n  Persons(key: [\\\"1\\\", \\\"2\\\"]) {\\n    firstName\\n    age\\n  }\\n}\\n\"}";
    HttpResponse response = Request.Post("http://localhost:3000/graphql")
        .bodyString(query, ContentType.TEXT_PLAIN).execute().returnResponse();
    String responseString = new BasicResponseHandler().handleResponse(response);
    assertThat(responseString).isEqualTo(
        "{\"data\":{\"Persons\":[{\"firstName\":\"James\",\"age\":60},{\"firstName\":\"Joshua\",\"age\":50}]}}");
  }

  @Test
  public void testGraphQLWithSinglePersonByFirstName() throws IOException {
    String query =
        "{\"query\":\"{\\n  Person(firstName: \\\"James\\\") {\\n    firstName\\n    age\\n  }\\n}\\n\"}";
    HttpResponse response = Request.Post("http://localhost:3000/graphql")
        .bodyString(query, ContentType.TEXT_PLAIN).execute().returnResponse();
    String responseString = new BasicResponseHandler().handleResponse(response);
    assertThat(responseString)
        .isEqualTo("{\"data\":{\"Person\":{\"firstName\":\"James\",\"age\":60}}}");
  }

  @Test
  public void testGraphQLWithMultiplePersonsByFirstName() throws IOException {
    String query =
        "{\"query\":\"{\\n  Persons(firstName: [\\\"James\\\", \\\"Joshua\\\"]) {\\n    firstName\\n    age\\n  }\\n}\\n\"}";
    HttpResponse response = Request.Post("http://localhost:3000/graphql")
        .bodyString(query, ContentType.TEXT_PLAIN).execute().returnResponse();
    String responseString = new BasicResponseHandler().handleResponse(response);
    assertThat(responseString).isEqualTo(
        "{\"data\":{\"Persons\":[{\"firstName\":\"James\",\"age\":60},{\"firstName\":\"Joshua\",\"age\":50}]}}");
  }

  @Test
  public void testGraphQLWithSingleFooKey() throws IOException {
    String query = "{\"query\":\"{\\nFoo(key : \\\"1\\\")\\n}\"}";
    HttpResponse response = Request.Post("http://localhost:3000/graphql")
        .bodyString(query, ContentType.TEXT_PLAIN).execute().returnResponse();
    String responseString = new BasicResponseHandler().handleResponse(response);
    assertThat(responseString).isEqualTo("{\"data\":{\"Foo\":\"One\"}}");
  }

  @Test
  public void testGraphQLPutFooKeyValue() throws IOException {
    String m =
        "{\"query\":\"mutation PutFoo($key: String, $value: String) {\\n  putFoo(key: $key, value: $value)\\n}\\n\",\"variables\":{\"key\":\"3\",\"value\":\"Three\"},\"operationName\":\"PutFoo\"}";
    HttpResponse response = Request.Post("http://localhost:3000/graphql")
        .bodyString(m, ContentType.TEXT_PLAIN).execute().returnResponse();
    String responseString = new BasicResponseHandler().handleResponse(response);
    assertThat(responseString).isEqualTo("{\"data\":{\"putFoo\":\"Three\"}}");
  }

  @Test
  public void testGraphQLWithMultipleFooKeys() throws IOException {
    String query = "{\"query\":\"{\\nFoos(key : [\\\"1\\\", \\\"2\\\"])\\n}\"}";
    HttpResponse response = Request.Post("http://localhost:3000/graphql")
        .bodyString(query, ContentType.TEXT_PLAIN).execute().returnResponse();
    String responseString = new BasicResponseHandler().handleResponse(response);
    assertThat(responseString).isEqualTo("{\"data\":{\"Foos\":[\"One\",\"Two\"]}}");
  }
}
