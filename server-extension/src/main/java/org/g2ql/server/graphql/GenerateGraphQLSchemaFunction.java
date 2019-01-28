package org.g2ql.server.graphql;

import graphql.schema.idl.SchemaPrinter;
import org.apache.geode.cache.Cache;
import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionContext;
import org.g2ql.graphql.GraphQLSchemaBuilder;

public class GenerateGraphQLSchemaFunction implements Function {
  public void execute(FunctionContext fc) {
    Cache cache = fc.getCache();
    GraphQLSchemaBuilder schemaBuilder = new GraphQLSchemaBuilder(cache);
    SchemaPrinter printer = new SchemaPrinter();
    fc.getResultSender().lastResult(printer.print(schemaBuilder.build()));
  }

  public String getId() {
    return "generate-schema";
  }
}
