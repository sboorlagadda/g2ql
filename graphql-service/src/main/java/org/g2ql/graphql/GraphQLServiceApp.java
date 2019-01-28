package org.g2ql.graphql;

import graphql.language.FieldDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.language.TypeName;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.TypeRuntimeWiring;
import org.apache.geode.cache.Cache;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.execute.FunctionService;
import org.apache.geode.cache.execute.ResultCollector;
import org.g2ql.geode.GeodeCollectionTypeDataFetcher;
import org.g2ql.geode.GeodeDataFetcher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

import static org.apache.geode.cache.client.ClientRegionShortcut.PROXY;

@SpringBootApplication
public class GraphQLServiceApp {

  public static void main(String[] args) {
    SpringApplication.run(GraphQLServiceApp.class, args);
  }

  @Bean
  GraphQLSchema schema() {
    ClientCache cache = new ClientCacheFactory().addPoolLocator("127.0.0.1", 10334)
        .set("log-level", "WARN").create();

    ResultCollector<String, List<String>> resultCollector =
        FunctionService.onServers(cache.getDefaultPool()).execute("generate-schema");

    List<String> schemaResult = resultCollector.getResult();

    SchemaParser parser = new SchemaParser();
    TypeDefinitionRegistry registry = parser.parse(schemaResult.get(0));
    SchemaGenerator generator = new SchemaGenerator();

    ObjectTypeDefinition query = (ObjectTypeDefinition) registry.getType("QueryType_Geode").get();

    // Initialize client cache
    initClientCache(cache, query.getFieldDefinitions());

    TypeRuntimeWiring.Builder queryTypeRuntimeWiring =
        queryTypeRuntimeWiring(cache, query.getFieldDefinitions());

    RuntimeWiring runtimeWiring =
        RuntimeWiring.newRuntimeWiring().type(queryTypeRuntimeWiring.build()).build();

    return generator.makeExecutableSchema(registry, runtimeWiring);
  }

  void initClientCache(ClientCache cache, List<FieldDefinition> fields) {
    fields.stream().filter(fieldDefinition -> fieldDefinition.getType() instanceof TypeName)
        .forEach(fieldDefinition -> cache.createClientRegionFactory(PROXY)
            .create(fieldDefinition.getName()));
  }

  TypeRuntimeWiring.Builder queryTypeRuntimeWiring(ClientCache cache,
      List<FieldDefinition> fields) {
    TypeRuntimeWiring.Builder queryTypeRuntimeWiring =
        TypeRuntimeWiring.newTypeWiring("QueryType_Geode");
    for (FieldDefinition field : fields) {
      String fieldName = field.getName();
      if (field.getType() instanceof TypeName) {
        queryTypeRuntimeWiring.dataFetcher(field.getName(),
            new GeodeDataFetcher((Cache) cache, fieldName));
      } else {
        String regionName = fieldName.substring(0, fieldName.length() - 1);
        queryTypeRuntimeWiring.dataFetcher(field.getName(),
            new GeodeCollectionTypeDataFetcher((Cache) cache, regionName));
      }
    }

    return queryTypeRuntimeWiring;
  }
}

