package org.g2ql.graphql;

import static graphql.schema.GraphQLArgument.newArgument;
import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInputObjectType.newInputObject;
import static graphql.schema.GraphQLObjectType.newObject;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import graphql.Scalars;
import graphql.schema.GraphQLArgument;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInputType;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import org.apache.logging.log4j.Logger;
import org.g2ql.annotation.GeodeGraphQLConnection;
import org.g2ql.annotation.GeodeGraphQLDocumentation;
import org.g2ql.annotation.GeodeGraphQLIgnore;
import org.g2ql.geode.GeodeCollectionTypeDataFetcher;
import org.g2ql.geode.GeodeConnectionTypeDataFetcher;
import org.g2ql.geode.GeodeCreateDataFetcher;
import org.g2ql.geode.GeodeDataFetcher;
import org.g2ql.geode.GeodeDestroyDataFetcher;
import org.g2ql.geode.GeodePutDataFetcher;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.RegionAttributes;
import org.apache.geode.cache.query.Index;
import org.apache.geode.internal.logging.LogService;

class GraphQLSchemaBuilder extends GraphQLSchema.Builder {
  private static final Logger logger = LogService.getLogger();
  private final Map<Class<?>, GraphQLType> valueCache = new HashMap<>();
  private Cache cache;

  GraphQLSchemaBuilder(Cache cache) {
    this.cache = cache;
    super.query(getQueryType());
    super.mutation(getMutationType());
  }

  private GraphQLObjectType getQueryType() {
    GraphQLObjectType.Builder queryType = newObject().name("QueryType_Geode")
        .description("All encompassing schema for this Geode Cluster");
    Set<Region<?, ?>> userRegions = cache.rootRegions();

    userRegions.stream().filter(r -> isNotIgnored(r.getAttributes().getValueConstraint()))
        .forEach(region -> queryType.fields(getQueryFieldDefinition(region.getName(),
            region.getAttributes(), cache.getQueryService().getIndexes(region))));

    return queryType.build();
  }

  private GraphQLObjectType getMutationType() {
    GraphQLObjectType.Builder mutationType = newObject().name("MutationType_Geode")
        .description("All encompassing schema for this Geode Cluster");

    Set<Region<?, ?>> userRegions = cache.rootRegions();

    userRegions.stream().filter(r -> isNotIgnored(r.getAttributes().getValueConstraint()))
        .forEach(region -> mutationType
            .fields(getMutationFieldDefinition(region.getName(), region.getAttributes())));

    return mutationType.build();
  }

  private List<GraphQLFieldDefinition> getMutationFieldDefinition(String regionName,
      RegionAttributes<?, ?> regionAttributes) {
    Class<?> valueClass = regionAttributes.getValueConstraint();
    if (valueClass == null) {
      valueClass = String.class;
    }

    String schemaDocumentation = getSchemaDocumentation(regionAttributes.getValueConstraint());
    GraphQLArgument key = getArgument(regionAttributes.getKeyConstraint());

    List<GraphQLFieldDefinition> mutations = new ArrayList<>();
    if (isBasicAttributeType(valueClass)) {
      GraphQLArgument input = getScalarInputArgument(valueClass);
      GraphQLScalarType type = (GraphQLScalarType) getScalarType(valueClass);

      mutations.add(newFieldDefinition().name("create" + regionName)
          .description(schemaDocumentation).type(type).argument(key).argument(input)
          .dataFetcher(new GeodeCreateDataFetcher(cache, regionName, valueClass)).build());
      mutations.add(newFieldDefinition().name("put" + regionName).description(schemaDocumentation)
          .type(type).argument(key).argument(input)
          .dataFetcher(new GeodePutDataFetcher(cache, regionName, valueClass)).build());
      mutations.add(newFieldDefinition().name("destroy" + regionName)
          .description(schemaDocumentation).type(type).argument(key).argument(input)
          .dataFetcher(new GeodeDestroyDataFetcher(cache, regionName)).build());
    } else {
      GraphQLArgument input = getObjectInputArgument(regionName, valueClass);
      GraphQLObjectType type = (GraphQLObjectType) getObjectType(regionName, valueClass);

      mutations.add(newFieldDefinition().name("create" + regionName)
          .description(schemaDocumentation).type(type).argument(key).argument(input)
          .dataFetcher(new GeodeCreateDataFetcher(cache, regionName, valueClass)).build());
      mutations.add(newFieldDefinition().name("put" + regionName).description(schemaDocumentation)
          .type(type).argument(key).argument(input)
          .dataFetcher(new GeodePutDataFetcher(cache, regionName, valueClass)).build());
      mutations.add(newFieldDefinition().name("destroy" + regionName)
          .description(schemaDocumentation).type(type).argument(key).argument(input)
          .dataFetcher(new GeodeDestroyDataFetcher(cache, regionName)).build());
    }
    return mutations;
  }

  private List<GraphQLFieldDefinition> getQueryFieldDefinition(String regionName,
      RegionAttributes<?, ?> regionAttributes, Collection<Index> indexes) {
    Class<?> valueClass = regionAttributes.getValueConstraint();
    if (valueClass == null)
      valueClass = String.class;

    String schemaDocumentation = getSchemaDocumentation(valueClass);

    List<GraphQLFieldDefinition> queries = new ArrayList<>();
    if (isBasicAttributeType(valueClass)) {
      queries.add(newFieldDefinition().name(regionName).description(schemaDocumentation)
          .type((GraphQLScalarType) getScalarType(valueClass))
          .dataFetcher(new GeodeDataFetcher(cache, regionName))
          .argument(getArgument(regionAttributes.getKeyConstraint())).build());

      queries.add(newFieldDefinition().name(regionName + "s").description(schemaDocumentation)
          .type(new GraphQLList(getScalarType(valueClass)))
          .dataFetcher(new GeodeCollectionTypeDataFetcher(cache, regionName))
          .argument(getListArgument(regionAttributes.getKeyConstraint())).build());
    } else {
      List<GraphQLArgument> arguments = new ArrayList<>();
      arguments.add(getArgument(regionAttributes.getKeyConstraint()));

      Set<String> indexedFields =
          indexes.stream().map(Index::getIndexedExpression).collect(toSet());
      logger.info("Indexed fields for region {} are {}", regionName,
          indexedFields.stream().collect(joining(",")));
      // add arguments for each indexed field
      arguments.addAll(Arrays.stream(regionAttributes.getValueConstraint().getDeclaredFields())
          .filter(field -> indexedFields.contains(field.getName()))
          .filter(field -> isBasicAttributeType(field.getType())).map(this::getArgumentForField)
          .collect(toList()));

      queries.add(newFieldDefinition().name(regionName).description(schemaDocumentation)
          .type((GraphQLObjectType) getObjectType(regionName, valueClass))
          .dataFetcher(new GeodeDataFetcher(cache, regionName)).argument(arguments).build());

      List<GraphQLArgument> collectionArguments = new ArrayList<>();
      collectionArguments.add(getListArgument(regionAttributes.getKeyConstraint()));

      // add arguments for each indexed field
      collectionArguments
          .addAll(Arrays.stream(regionAttributes.getValueConstraint().getDeclaredFields())
              .filter(field -> indexedFields.contains(field.getName()))
              .filter(field -> isBasicAttributeType(field.getType()))
              .map(this::getListArgumentForField).collect(toList()));

      queries.add(newFieldDefinition().name(regionName + "s").description(schemaDocumentation)
          .type(new GraphQLList(getObjectType(regionName, regionAttributes.getValueConstraint())))
          .dataFetcher(new GeodeCollectionTypeDataFetcher(cache, regionName))
          .argument(collectionArguments).build());
    }
    return queries;
  }

  private GraphQLType getScalarType(Class<?> valueClass) {
    if (valueCache.containsKey(valueClass))
      return valueCache.get(valueClass);

    GraphQLType valueType = getBasicAttributeType(valueClass);
    valueCache.put(valueClass, valueType);
    return valueType;
  }

  private GraphQLType getObjectType(String regionName, Class<?> valueClass) {
    if (valueCache.containsKey(valueClass))
      return valueCache.get(valueClass);

    GraphQLObjectType.Builder objectType = newObject().name(valueClass.getSimpleName())
        .description(getSchemaDocumentation(valueClass));

    // non connection type
    List<GraphQLFieldDefinition> fields = Arrays.stream(valueClass.getDeclaredFields())
        .filter(f -> !isConnectionType(f)).map(this::getObjectField).collect(toList());

    // connection type
    fields.addAll(Arrays.stream(valueClass.getDeclaredFields()).filter(this::isConnectionType)
        .map(f -> getConnectionType(regionName, f)).collect(toList()));

    GraphQLObjectType answer = objectType.fields(fields).build();
    valueCache.put(valueClass, answer);

    return answer;
  }

  private GraphQLArgument getArgument(Class<?> key) {
    if (key == null)
      key = String.class;
    GraphQLType type = getBasicAttributeType(key);
    return newArgument().name("key") // key is the argument for any type in geode
        .type((GraphQLInputType) type).build();
  }

  private GraphQLArgument getScalarInputArgument(Class<?> value) {
    GraphQLType type = getBasicAttributeType(value);
    return newArgument().name("value") // value is the argument for scalar KV
        .type((GraphQLInputType) type).build();
  }

  private GraphQLArgument getObjectInputArgument(String regionName, Class<?> valueClass) {
    GraphQLInputObjectType.Builder inputType =
        newInputObject().name(valueClass.getSimpleName() + "Input");

    // non connection type
    List<GraphQLInputObjectField> fields = Arrays.stream(valueClass.getDeclaredFields())
        .filter(f -> !isConnectionType(f)).map(this::getInputObjectField).collect(toList());

    // connection type
    // fields.addAll(Arrays.stream(valueClass.getDeclaredFields()).filter(f -> isConnectionType(f))
    // .map(f -> getConnectionInputType(regionName, f)).collect(Collectors.toList()));

    GraphQLInputObjectType answer = inputType.fields(fields).build();
    return newArgument().name(regionName).type(answer).build();
  }

  private GraphQLArgument getListArgument(Class<?> key) {
    if (key == null)
      key = String.class;
    GraphQLType type = getBasicAttributeType(key);
    return newArgument().name("key") // key is the argument for any type in geode
        .type(new GraphQLList(type)).build();
  }

  private GraphQLArgument getArgumentForField(Field field) {
    GraphQLType type = getBasicAttributeType(field.getType());
    return newArgument().name(field.getName()).type((GraphQLInputType) type).build();
  }

  private GraphQLArgument getListArgumentForField(Field field) {
    GraphQLType type = getBasicAttributeType(field.getType());
    return newArgument().name(field.getName()).type(new GraphQLList(type)).build();
  }

  private GraphQLFieldDefinition getObjectField(Field field) {
    try {
      GraphQLType type = getBasicAttributeType(field.getType());
      return newFieldDefinition().name(field.getName()).description(getSchemaDocumentation(field))
          .type((GraphQLOutputType) type).build();
    } catch (UnsupportedOperationException ex) {
      // the field is non-java, so go deep
      GraphQLObjectType embedded = newObject().name(field.getType().getSimpleName())
          .description(getSchemaDocumentation(field))
          .fields(Arrays.stream(field.getType().getDeclaredFields())
              .filter(f -> !f.getName().equalsIgnoreCase("this$0")).map(this::getObjectField)
              .collect(toList()))
          .build();
      return newFieldDefinition().name(field.getName()).type(embedded).build();
    }
  }

  private GraphQLInputObjectField getInputObjectField(Field field) {
    try {
      GraphQLType type = getBasicAttributeType(field.getType());
      return GraphQLInputObjectField.newInputObjectField().name(field.getName())
          .description(getSchemaDocumentation(field)).type((GraphQLInputType) type).build();
    } catch (UnsupportedOperationException ex) {
      // the field is non-java, so go deep
      GraphQLInputType embedded = newInputObject().name(field.getType().getSimpleName() + "Input")
          .description(getSchemaDocumentation(field))
          .fields(Arrays.stream(field.getType().getDeclaredFields())
              .filter(f -> !f.getName().equalsIgnoreCase("this$0")).map(this::getInputObjectField)
              .collect(toList()))
          .build();
      return GraphQLInputObjectField.newInputObjectField().name(field.getName()).type(embedded)
          .build();

    }
  }

  private GraphQLFieldDefinition getConnectionType(String regionName, Field field) {
    String connectionName = getConnectionName(field);
    GraphQLOutputType connectionType = new GraphQLList(new GraphQLTypeReference(connectionName));
    return newFieldDefinition().name(field.getName()).description(getSchemaDocumentation(field))
        .type(connectionType)
        .dataFetcher(new GeodeConnectionTypeDataFetcher(cache, regionName, field.getName()))
        .build();
  }

  private boolean isBasicAttributeType(Class type) {
    try {
      return getBasicAttributeType(type) != null;
    } catch (UnsupportedOperationException e) {
      return false;
    }
  }

  private GraphQLType getBasicAttributeType(Class javaType) {
    if (String.class.isAssignableFrom(javaType))
      return Scalars.GraphQLString;
    else if (UUID.class.isAssignableFrom(javaType))
      return JavaScalars.GraphQLUUID;
    else if (Integer.class.isAssignableFrom(javaType) || int.class.isAssignableFrom(javaType))
      return Scalars.GraphQLInt;
    else if (Short.class.isAssignableFrom(javaType) || short.class.isAssignableFrom(javaType))
      return Scalars.GraphQLShort;
    else if (Float.class.isAssignableFrom(javaType) || float.class.isAssignableFrom(javaType)
        || Double.class.isAssignableFrom(javaType) || double.class.isAssignableFrom(javaType))
      return Scalars.GraphQLFloat;
    else if (Long.class.isAssignableFrom(javaType) || long.class.isAssignableFrom(javaType))
      return Scalars.GraphQLLong;
    else if (Boolean.class.isAssignableFrom(javaType) || boolean.class.isAssignableFrom(javaType))
      return Scalars.GraphQLBoolean;
    else if (Date.class.isAssignableFrom(javaType))
      return JavaScalars.GraphQLDate;
    else if (LocalDateTime.class.isAssignableFrom(javaType))
      return JavaScalars.GraphQLLocalDateTime;
    else if (Instant.class.isAssignableFrom(javaType))
      return JavaScalars.GraphQLInstant;
    else if (LocalDate.class.isAssignableFrom(javaType))
      return JavaScalars.GraphQLLocalDate;
    else if (BigDecimal.class.isAssignableFrom(javaType)) {
      return Scalars.GraphQLBigDecimal;
    }

    throw new UnsupportedOperationException(
        "Class could not be mapped to GraphQL: '" + javaType.getTypeName() + "'");
  }

  private String getConnectionName(AnnotatedElement annotatedElement) {
    if (annotatedElement != null) {
      // GeodeGraphQLConnection connection =
      // annotatedElement.getAnnotation(GeodeGraphQLConnection.class);
      // return connection != null ? connection.value() : null;
      for (Annotation a : annotatedElement.getAnnotations()) {
        if (GeodeGraphQLConnection.class.getName().equalsIgnoreCase(a.annotationType().getName())) {
          return getAnnotationValue(a);
        }
      }
    }
    return null;
  }

  private boolean isConnectionType(AnnotatedElement annotatedElement) {
    if (annotatedElement != null) {
      // GeodeGraphQLConnection connection =
      // annotatedElement.getDeclaredAnnotation(GeodeGraphQLConnection.class);
      // return connection != null;
      for (Annotation a : annotatedElement.getAnnotations()) {
        if (GeodeGraphQLConnection.class.getName().equalsIgnoreCase(a.annotationType().getName())) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isNotIgnored(AnnotatedElement annotatedElement) {
    if (annotatedElement != null) {
      // GeodeGraphQLIgnore ignore =
      // annotatedElement.getDeclaredAnnotation(GeodeGraphQLIgnore.class);
      // return ignore == null;
      for (Annotation a : annotatedElement.getAnnotations()) {
        if (GeodeGraphQLIgnore.class.getName().equalsIgnoreCase(a.annotationType().getName())) {
          return false;
        }
      }
    }
    return true; // for now consider it for regions created with out explicit value class
  }

  private String getSchemaDocumentation(AnnotatedElement annotatedElement) {
    if (annotatedElement != null) {
      // GeodeGraphQLDocumentation schemaDocumentation =
      // annotatedElement.getAnnotation(GeodeGraphQLDocumentation.class);
      // return schemaDocumentation != null ? schemaDocumentation.value() : null;
      for (Annotation a : annotatedElement.getAnnotations()) {
        if (GeodeGraphQLDocumentation.class.getName()
            .equalsIgnoreCase(a.annotationType().getName())) {
          return getAnnotationValue(a);
        }
      }
    }
    return null;
  }

  private String getAnnotationValue(Annotation a) {
    for (Method method : a.annotationType().getDeclaredMethods()) {
      try {
        return (String) method.invoke(a, (Object[]) null);
      } catch (IllegalAccessException | InvocationTargetException e) {
        e.printStackTrace();
      }
    }
    return null;
  }
}
