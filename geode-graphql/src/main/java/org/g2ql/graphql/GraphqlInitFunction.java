package org.g2ql.graphql;

import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionContext;

public class GraphqlInitFunction implements Function {
  public void execute(FunctionContext fc) {

  }

  public String getId() {
    return "g2ql-init";
  }
}
