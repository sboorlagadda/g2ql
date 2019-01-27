package org.g2ql.serverextension.graphql;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.execute.Function;
import org.apache.geode.cache.execute.FunctionContext;

public class GraphqlInitFunction implements Function {
  public void execute(FunctionContext fc) {
    Cache cache = fc.getCache();
    new GraphqlServer(cache);
    fc.getResultSender().lastResult("g2ql init success!");
  }

  public String getId() {
    return "g2ql-init";
  }
}
