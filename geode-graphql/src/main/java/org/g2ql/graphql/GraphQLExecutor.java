package org.g2ql.graphql;

import static graphql.ExecutionInput.newExecutionInput;

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import org.apache.geode.cache.Cache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * A GraphQL executor capable of constructing a {@link GraphQLSchema} from a Geode Cache
 * {@link Cache}. The executor uses the constructed schema to execute queries directly from the
 * cache.
 */
public class GraphQLExecutor {
  private final static Logger logger = LogManager.getLogger(GraphQLExecutor.class);

  private GraphQL graphQL;
  private GraphQLSchema graphQLSchema;
  private GraphQLSchema.Builder builder;

  private Cache cache;

  protected GraphQLExecutor() {
    createGraphQL();
  }

  /**
   * Creates a read-only GraphQLExecutor using the entities discovered from the given {@link Cache}.
   *
   * @param cache The geode cache from which the regions are extracted as {@link GraphQLSchema}
   *        objects.
   */
  public GraphQLExecutor(Cache cache) {
    this.cache = cache;
    createGraphQL();
  }

  @PostConstruct
  protected synchronized void createGraphQL() {
    if (cache != null) {
      if (builder == null) {
        logger.info("GraphQLExecutor - createGraphQL - creating schema builder");
        this.builder = new GraphQLSchemaBuilder(cache);
      }
      this.graphQLSchema = builder.build();
      this.graphQL = GraphQL.newGraphQL(graphQLSchema).build();
      logger
          .info("GraphQLExecutor - createGraphQL - after schema builder" + this.graphQL.toString());
    }
  }

  /**
   * @return The {@link GraphQLSchema} used by this executor.
   */
  public GraphQLSchema getGraphQLSchema() {
    return graphQLSchema;
  }

  public ExecutionResult execute(String query) {
    return graphQL.execute(query);
  }

  public ExecutionResult execute(String query, Map<String, Object> arguments) {
    if (arguments == null)
      return graphQL.execute(query);
    return graphQL.execute(newExecutionInput().query(query).variables(arguments).build());
  }

  public ExecutionResult execute(String query, Map<String, Object> arguments,
      String operationName) {
    if (arguments == null)
      return graphQL.execute(query);
    return graphQL.execute(
        newExecutionInput().query(query).operationName(operationName).variables(arguments).build());
  }

  /**
   * Gets the builder that was used to create the Schema that this executor is basing its query
   * executions on. The builder can be used to update the executor with the
   * {@link #updateSchema(GraphQLSchema.Builder)} method.
   * 
   * @return An instance of a builder.
   */
  public GraphQLSchema.Builder getBuilder() {
    return builder;
  }

  /**
   * Returns the schema that this executor bases its queries on.
   * 
   * @return An instance of a {@link GraphQLSchema}.
   */
  public GraphQLSchema getSchema() {
    return graphQLSchema;
  }

  /**
   * Uses the given builder to re-create and replace the {@link GraphQLSchema} that this executor
   * uses to execute its queries.
   *
   * @param builder The builder to recreate the current {@link GraphQLSchema} and {@link GraphQL}
   *        instances.
   * @return The same executor but with a new {@link GraphQL} schema.
   */
  public GraphQLExecutor updateSchema(GraphQLSchema.Builder builder) {
    this.builder = builder;
    createGraphQL();
    return this;
  }
}
