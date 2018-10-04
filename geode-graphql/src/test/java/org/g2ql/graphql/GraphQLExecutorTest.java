package org.g2ql.graphql;

import graphql.ExecutionResult;
import org.apache.geode.cache.Cache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionAttributes;
import org.apache.geode.cache.query.FunctionDomainException;
import org.apache.geode.cache.query.NameResolutionException;
import org.apache.geode.cache.query.Query;
import org.apache.geode.cache.query.QueryInvocationTargetException;
import org.apache.geode.cache.query.QueryService;
import org.apache.geode.cache.query.SelectResults;
import org.apache.geode.cache.query.TypeMismatchException;
import org.apache.geode.cache.query.internal.LinkedResultSet;

import graphql.schema.GraphQLSchema;
import org.g2ql.categories.UnitTest;
import org.g2ql.domain.Person;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Category(UnitTest.class)
public class GraphQLExecutorTest {
  private Cache cache;
  private QueryService queryService;

  private Map<String, Person> personRegionData = new HashMap<>();

  @Before
  public void setUp() {
    cache = mock(Cache.class);
    queryService = mock(QueryService.class);

    doReturn(queryService).when(cache).getQueryService();
    // Person
    Person person1 = new Person("1", "Luke", "Skywalker", 30, "Pivotal");
    person1.setAddress(person1.new Address("1 Pike Street", "Seattle", "USA"));
    person1.getFriends().add("2");
    Person person2 = new Person("2", "James", "Gosling", 60, "AWS");
    personRegionData.put("1", person1);
    personRegionData.put("2", person2);

    Region<String, Person> personRegion = mock(Region.class);
    RegionAttributes<String, Person> personRegionAttributes = mock(RegionAttributes.class);

    doReturn(Stream.of(personRegion).collect(toSet())).when(cache).rootRegions();
    doReturn(personRegion).when(cache).getRegion("Person");
    doReturn("Person").when(personRegion).getName();
    doReturn(personRegionAttributes).when(personRegion).getAttributes();
    doReturn(String.class).when(personRegionAttributes).getKeyConstraint();
    doReturn(Person.class).when(personRegionAttributes).getValueConstraint();

    doReturn(person1).when(personRegion).get("1");
    doReturn(personRegionData).when(personRegion).getAll(Stream.of("1", "2").collect(toList()));

    Map<String, Person> friendsDataForPerson1 = new HashMap<>();
    friendsDataForPerson1.put("2", person2);
    doReturn(friendsDataForPerson1).when(personRegion).getAll(Stream.of("2").collect(toList()));

    doReturn(null).when(personRegion).put(anyString(), any());
  }

  @Test
  public void testPersonWithNoKeysShouldNotReturnAnything() {
    String query = "query personById\n{\nPerson{\nid\nfirstName}\n}";
    GraphQLExecutor executor = new GraphQLExecutor(cache);
    ExecutionResult result = executor.execute(query);
    assertThat(result).isNotNull();
    assertThat(result.getErrors()).isEmpty();
    assertThat(result.getData().toString()).isEqualTo("{Person={id=null, firstName=null}}");
  }

  @Test
  public void testPersonsWithNoKeysShouldNotReturnAnything() {
    String query = "query personsById\n{\nPersons{\nid\nfirstName}\n}";
    GraphQLExecutor executor = new GraphQLExecutor(cache);
    ExecutionResult result = executor.execute(query);
    assertThat(result).isNotNull();
    assertThat(result.getErrors()).isEmpty();
    assertThat(result.getData().toString()).isEqualTo("{Persons=[]}");
  }

  @Test
  public void testPersonWithKey() {
    String query =
        "query personById\n{\nPerson(key: \"1\"){\nid\nfirstName\naddress{\nstreet\ncity\n}\n}\n\n}";
    GraphQLExecutor executor = new GraphQLExecutor(cache);
    ExecutionResult result = executor.execute(query);
    assertThat(result).isNotNull();
    assertThat(result.getErrors()).isEmpty();
    assertThat(result.getData().toString())
        .isEqualTo("{Person={id=1, firstName=Luke, address={street=1 Pike Street, city=Seattle}}}");
  }

  @Test
  public void testPutPerson() {
    String m =
        "mutation CreatePerson($key: String, $person: PersonInput) {\n  putPerson(key: $key, Person:$person) {\n    firstName\n    lastName\n  }\n}\n";

    Map<String, String> person = new HashMap<>();
    person.put("id", "3");
    person.put("firstName", "Elon");
    person.put("lastName", "Mush");

    Map<String, Object> variables = new HashMap<>();
    variables.put("key", "3");
    variables.put("person", person);

    GraphQLExecutor executor = new GraphQLExecutor(cache);
    ExecutionResult result = executor.execute(m, variables, "CreatePerson");
    assertThat(result).isNotNull();
    assertThat(result.getErrors()).isEmpty();
    assertThat(result.getData().toString())
        .isEqualTo("{putPerson={firstName=Elon, lastName=Mush}}");
  }

  @Test
  public void testPersonAndFriendsWithKey() {
    String query =
        "query personsById\n{\nPerson(key: \"1\"){\nid\nfirstName\nfriends{\nfirstName}\n}\n}";
    GraphQLExecutor executor = new GraphQLExecutor(cache);
    ExecutionResult result = executor.execute(query);
    assertThat(result).isNotNull();
    assertThat(result.getErrors()).isEmpty();
    assertThat(result.getData().toString())
        .isEqualTo("{Person={id=1, firstName=Luke, friends=[{firstName=James}]}}");
  }

  @Test
  public void testPersonsWithMultipleKey() {
    String query = "query personsById\n{\nPersons(key: [\"1\", \"2\"]){\nid\nfirstName}\n}";
    GraphQLExecutor executor = new GraphQLExecutor(cache);
    ExecutionResult result = executor.execute(query);
    assertThat(result).isNotNull();
    assertThat(result.getErrors()).isEmpty();
    assertThat(result.getData().toString())
        .isEqualTo("{Persons=[{id=1, firstName=Luke}, {id=2, firstName=James}]}");
  }

  @Test
  public void testPersonByFirstName() throws NameResolutionException, TypeMismatchException,
      QueryInvocationTargetException, FunctionDomainException {
    String expectedOQL = "SELECT DISTINCT * FROM /Person x where x.firstName=$1";

    ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
    Query q = mock(Query.class);
    SelectResults<Person> results = new LinkedResultSet();
    results.add(personRegionData.get("1"));
    doReturn(q).when(queryService).newQuery(expectedOQL);
    doReturn(results).when(q).execute("Luke");

    String query = "query personById\n{\nPerson(firstName: \"Luke\"){\nid\nfirstName}\n}";
    GraphQLExecutor executor = new GraphQLExecutor(cache);
    ExecutionResult result = executor.execute(query);

    verify(queryService).newQuery(argumentCaptor.capture());
    assertThat(argumentCaptor.getValue()).isEqualTo(expectedOQL);

    assertThat(result).isNotNull();
    assertThat(result.getErrors()).isEmpty();
    assertThat(result.getData().toString()).isEqualTo("{Person={id=1, firstName=Luke}}");
  }

  @Test
  public void testPersonsByMultipleFirstNames() throws NameResolutionException,
      TypeMismatchException, QueryInvocationTargetException, FunctionDomainException {
    String expectedOQL = "SELECT DISTINCT * FROM /Person x where x.firstName=$1";

    ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
    Query q = mock(Query.class);
    SelectResults<Person> lukeResult = new LinkedResultSet();
    lukeResult.add(personRegionData.get("1"));
    SelectResults<Person> jamesResult = new LinkedResultSet();
    jamesResult.add(personRegionData.get("2"));
    doReturn(q).when(queryService).newQuery(expectedOQL);
    doReturn(lukeResult).when(q).execute("Luke");
    doReturn(jamesResult).when(q).execute("James");

    String query =
        "query personsById\n{\nPersons(firstName: [\"Luke\", \"James\"]){\nid\nfirstName}\n}";
    GraphQLExecutor executor = new GraphQLExecutor(cache);
    ExecutionResult result = executor.execute(query);

    verify(queryService, times(2)).newQuery(argumentCaptor.capture());
    assertThat(argumentCaptor.getAllValues()).contains(expectedOQL);

    assertThat(result).isNotNull();
    assertThat(result.getErrors()).isEmpty();
    assertThat(result.getData().toString())
        .isEqualTo("{Persons=[{id=1, firstName=Luke}, {id=2, firstName=James}]}");
  }

  @Test
  public void testFoo() {
    Region<String, String> region = mock(Region.class);
    RegionAttributes<String, String> regionAttributes = mock(RegionAttributes.class);

    doReturn(Stream.of(region).collect(toSet())).when(cache).rootRegions();
    doReturn(region).when(cache).getRegion("Foo");
    doReturn("Foo").when(region).getName();
    doReturn(regionAttributes).when(region).getAttributes();
    doReturn(String.class).when(regionAttributes).getKeyConstraint();
    doReturn(String.class).when(regionAttributes).getValueConstraint();

    doReturn("One").when(region).get("1");

    String query = "query fooByKey\n{\nFoo(key : \"1\")\n}";
    GraphQLExecutor executor = new GraphQLExecutor(cache);
    ExecutionResult result = executor.execute(query);
    assertThat(result).isNotNull();
    assertThat(result.getErrors()).isEmpty();
    assertThat(result.getData().toString()).isEqualTo("{Foo=One}");
  }

  @Test
  public void testPutFoo() {
    Region<String, String> region = mock(Region.class);
    RegionAttributes<String, String> regionAttributes = mock(RegionAttributes.class);

    doReturn(Stream.of(region).collect(toSet())).when(cache).rootRegions();
    doReturn(region).when(cache).getRegion("Foo");
    doReturn("Foo").when(region).getName();
    doReturn(regionAttributes).when(region).getAttributes();
    doReturn(String.class).when(regionAttributes).getKeyConstraint();
    doReturn(String.class).when(regionAttributes).getValueConstraint();

    doReturn(null).when(region).put("1", "One");
    doReturn("One").when(region).put("2", "Two");

    String query =
        "mutation PutFoo($key: String, $value: String) {\n  putFoo(key: $key, value: $value)\n}\n";
    Map<String, Object> variables = new HashMap<>();
    variables.put("key", "1");
    variables.put("value", "One");
    GraphQLExecutor executor = new GraphQLExecutor(cache);
    ExecutionResult result = executor.execute(query, variables, "PutFoo");
    assertThat(result).isNotNull();
    assertThat(result.getErrors()).isEmpty();
    assertThat(result.getData().toString()).isEqualTo("{putFoo=One}");

    variables = new HashMap<>();
    variables.put("key", "2");
    variables.put("value", "Two");

    result = executor.execute(query, variables, "PutFoo");
    assertThat(result).isNotNull();
    assertThat(result.getErrors()).isEmpty();
    assertThat(result.getData().toString()).isEqualTo("{putFoo=Two}");
  }

  @Test
  public void testPutFooWithOutExplicitValueClass() {
    Region<String, String> region = mock(Region.class);
    RegionAttributes<String, String> regionAttributes = mock(RegionAttributes.class);

    doReturn(Stream.of(region).collect(toSet())).when(cache).rootRegions();
    doReturn(region).when(cache).getRegion("Foo");
    doReturn("Foo").when(region).getName();
    doReturn(regionAttributes).when(region).getAttributes();

    doReturn(null).when(region).put("1", "One");
    doReturn("One").when(region).put("2", "Two");

    String query =
        "mutation PutFoo($key: String, $value: String) {\n  putFoo(key: $key, value: $value)\n}\n";
    Map<String, Object> variables = new HashMap<>();
    variables.put("key", "1");
    variables.put("value", "One");
    GraphQLExecutor executor = new GraphQLExecutor(cache);
    ExecutionResult result = executor.execute(query, variables, "PutFoo");
    assertThat(result).isNotNull();
    assertThat(result.getErrors()).isEmpty();
    assertThat(result.getData().toString()).isEqualTo("{putFoo=One}");

    variables = new HashMap<>();
    variables.put("key", "2");
    variables.put("value", "Two");

    result = executor.execute(query, variables, "PutFoo");
    assertThat(result).isNotNull();
    assertThat(result.getErrors()).isEmpty();
    assertThat(result.getData().toString()).isEqualTo("{putFoo=Two}");
  }

  @Test
  public void testFoos() {
    Map<String, String> regionData = new HashMap<>();

    Region<String, String> region = mock(Region.class);
    RegionAttributes<String, String> regionAttributes = mock(RegionAttributes.class);

    doReturn(Stream.of(region).collect(toSet())).when(cache).rootRegions();
    doReturn(region).when(cache).getRegion("Foo");
    doReturn("Foo").when(region).getName();
    doReturn(regionAttributes).when(region).getAttributes();
    doReturn(String.class).when(regionAttributes).getKeyConstraint();
    doReturn(String.class).when(regionAttributes).getValueConstraint();

    regionData.put("1", "One");
    regionData.put("2", "Two");
    doReturn(regionData).when(region).getAll(Stream.of("1", "2").collect(toList()));

    String query = "query fooByKey\n{\nFoos(key : [\"1\",\"2\"])\n}";
    GraphQLExecutor executor = new GraphQLExecutor(cache);
    ExecutionResult result = executor.execute(query);
    assertThat(result).isNotNull();
    assertThat(result.getErrors()).isEmpty();
    assertThat(result.getData().toString()).isEqualTo("{Foos=[One, Two]}");
  }

  @Test
  public void testFooWithOutExplicitValueClass() {
    Region<String, String> region = mock(Region.class);
    RegionAttributes<String, String> regionAttributes = mock(RegionAttributes.class);

    doReturn(Stream.of(region).collect(toSet())).when(cache).rootRegions();
    doReturn(region).when(cache).getRegion("Foo");
    doReturn("Foo").when(region).getName();
    doReturn(regionAttributes).when(region).getAttributes();

    doReturn("One").when(region).get("1");

    String query = "query fooByKey\n{\nFoo(key : \"1\")\n}";
    GraphQLExecutor executor = new GraphQLExecutor(cache);
    ExecutionResult result = executor.execute(query);
    assertThat(result).isNotNull();
    assertThat(result.getErrors()).isEmpty();
    assertThat(result.getData().toString()).isEqualTo("{Foo=One}");
  }

  @Test
  public void testFoosWithOutExplicitValueClass() {
    Map<String, String> regionData = new HashMap<>();

    Region<String, String> region = mock(Region.class);
    RegionAttributes<String, String> regionAttributes = mock(RegionAttributes.class);

    doReturn(Stream.of(region).collect(toSet())).when(cache).rootRegions();
    doReturn(region).when(cache).getRegion("Foo");
    doReturn("Foo").when(region).getName();
    doReturn(regionAttributes).when(region).getAttributes();

    regionData.put("1", "One");
    regionData.put("2", "Two");
    doReturn(regionData).when(region).getAll(Stream.of("1", "2").collect(toList()));

    String query = "query fooByKey\n{\nFoos(key : [\"1\",\"2\"])\n}";
    GraphQLExecutor executor = new GraphQLExecutor(cache);
    ExecutionResult result = executor.execute(query);
    assertThat(result).isNotNull();
    assertThat(result.getErrors()).isEmpty();
    assertThat(result.getData().toString()).isEqualTo("{Foos=[One, Two]}");
  }
}
