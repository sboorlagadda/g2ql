package org.g2ql.graphql;

import graphql.Scalars;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import org.apache.geode.cache.Cache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionAttributes;
import org.apache.geode.cache.query.QueryService;
import org.g2ql.categories.UnitTest;
import org.g2ql.domain.Person;
import org.g2ql.domain.Student;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Category(UnitTest.class)
public class GraphQLSchemaBuilderTest {
  private Cache cache;
  private QueryService queryService;

  @Before
  public void setUp() {
    cache = mock(Cache.class);
    queryService = mock(QueryService.class);

    doReturn(queryService).when(cache).getQueryService();
  }

  @Test
  public void testPerson() {
    Region<String, Person> region = mock(Region.class);
    RegionAttributes<String, Person> regionAttributes = mock(RegionAttributes.class);

    doReturn(Stream.of(region).collect(toSet())).when(cache).rootRegions();
    doReturn("person").when(region).getName();
    doReturn(regionAttributes).when(region).getAttributes();
    doReturn(String.class).when(regionAttributes).getKeyConstraint();
    doReturn(Person.class).when(regionAttributes).getValueConstraint();

    GraphQLSchema schema = new GraphQLSchemaBuilder(cache).build();

    assertThat(schema).isNotNull();

    // two fields for every region
    assertThat(schema.getQueryType().getFieldDefinitions().size()).isEqualTo(2);

    // two arguments
    assertThat(schema.getQueryType().getFieldDefinition("person").getArguments().size())
        .isEqualTo(2);
    assertThat(schema.getQueryType().getFieldDefinition("person").getArgument("key").getType())
        .isEqualTo(Scalars.GraphQLString);
    assertThat(
        schema.getQueryType().getFieldDefinition("person").getArgument("firstName").getType())
            .isEqualTo(Scalars.GraphQLString);

    assertThat(schema.getQueryType().getFieldDefinition("persons").getArguments().size())
        .isEqualTo(2);
    assertThat(schema.getQueryType().getFieldDefinition("persons").getArgument("key").getType())
        .isInstanceOf(GraphQLList.class);
    assertThat(
        schema.getQueryType().getFieldDefinition("persons").getArgument("firstName").getType())
            .isInstanceOf(GraphQLList.class);

    // documentation
    assertThat(schema.getQueryType().getFieldDefinition("person").getDescription())
        .isEqualTo("A person is a person");

    // two types
    assertThat(schema.getQueryType().getFieldDefinition("person").getType())
        .isInstanceOf(GraphQLObjectType.class);
    assertThat(schema.getQueryType().getFieldDefinition("persons").getType())
        .isInstanceOf(GraphQLList.class);

    // embedded types
    GraphQLObjectType personType =
        (GraphQLObjectType) schema.getQueryType().getFieldDefinition("person").getType();
    assertThat(personType.getFieldDefinition("address")).isInstanceOf(GraphQLFieldDefinition.class);
    assertThat(personType.getFieldDefinition("address").getType())
        .isInstanceOf(GraphQLObjectType.class);

    // connection types
    GraphQLList friendsList = (GraphQLList) personType.getFieldDefinition("friends").getType();
    assertThat(friendsList).isInstanceOf(GraphQLList.class);
    assertThat(friendsList.getWrappedType()).isInstanceOf(GraphQLObjectType.class);
    assertThat(friendsList.getWrappedType().getName()).isEqualTo("Person");
  }

  @Test
  public void testIgnoreAnnotationDoesNotCreateAGraphqlObject() {
    Region<String, Student> region = mock(Region.class);
    RegionAttributes<String, Student> regionAttributes = mock(RegionAttributes.class);

    doReturn(Stream.of(region).collect(toSet())).when(cache).rootRegions();
    doReturn("student").when(region).getName();
    doReturn(regionAttributes).when(region).getAttributes();
    doReturn(String.class).when(regionAttributes).getKeyConstraint();
    doReturn(Student.class).when(regionAttributes).getValueConstraint();

    GraphQLSchema schema = new GraphQLSchemaBuilder(cache).build();

    assertThat(schema).isNotNull();

    // there shouldn't be any type created for ignored value classes
    assertThat(schema.getQueryType().getFieldDefinitions().size()).isEqualTo(0);
  }

  @Test
  public void testFoo() {
    Region<String, String> region = mock(Region.class);
    RegionAttributes<String, String> regionAttributes = mock(RegionAttributes.class);

    doReturn(Stream.of(region).collect(toSet())).when(cache).rootRegions();
    doReturn("Foo").when(region).getName();
    doReturn(regionAttributes).when(region).getAttributes();
    doReturn(String.class).when(regionAttributes).getKeyConstraint();
    doReturn(String.class).when(regionAttributes).getValueConstraint();

    GraphQLSchema schema = new GraphQLSchemaBuilder(cache).build();

    assertThat(schema).isNotNull();

    // two fields for every region
    assertThat(schema.getQueryType().getFieldDefinitions().size()).isEqualTo(2);

    // two arguments
    assertThat(schema.getQueryType().getFieldDefinition("Foo").getArguments().size()).isEqualTo(1);
    assertThat(schema.getQueryType().getFieldDefinition("Foo").getArgument("key").getType())
        .isEqualTo(Scalars.GraphQLString);

    assertThat(schema.getQueryType().getFieldDefinition("Foos").getArguments().size()).isEqualTo(1);
    assertThat(schema.getQueryType().getFieldDefinition("Foos").getArgument("key").getType())
        .isInstanceOf(GraphQLList.class);

    // two types
    assertThat(schema.getQueryType().getFieldDefinition("Foo").getType())
        .isInstanceOf(GraphQLScalarType.class);
    assertThat(schema.getQueryType().getFieldDefinition("Foos").getType())
        .isInstanceOf(GraphQLList.class);
  }

  @Test
  public void testFooWithoutExplicitValueClass() {
    Region<String, String> region = mock(Region.class);
    RegionAttributes<String, String> regionAttributes = mock(RegionAttributes.class);

    doReturn(Stream.of(region).collect(toSet())).when(cache).rootRegions();
    doReturn("Foo").when(region).getName();
    doReturn(regionAttributes).when(region).getAttributes();

    GraphQLSchema schema = new GraphQLSchemaBuilder(cache).build();

    assertThat(schema).isNotNull();

    // two fields for every region
    assertThat(schema.getQueryType().getFieldDefinitions().size()).isEqualTo(2);

    // two arguments
    assertThat(schema.getQueryType().getFieldDefinition("Foo").getArguments().size()).isEqualTo(1);
    assertThat(schema.getQueryType().getFieldDefinition("Foo").getArgument("key").getType())
        .isEqualTo(Scalars.GraphQLString);

    assertThat(schema.getQueryType().getFieldDefinition("Foos").getArguments().size()).isEqualTo(1);
    assertThat(schema.getQueryType().getFieldDefinition("Foos").getArgument("key").getType())
        .isInstanceOf(GraphQLList.class);

    // two types
    assertThat(schema.getQueryType().getFieldDefinition("Foo").getType())
        .isInstanceOf(GraphQLScalarType.class);
    assertThat(schema.getQueryType().getFieldDefinition("Foos").getType())
        .isInstanceOf(GraphQLList.class);
  }
}
