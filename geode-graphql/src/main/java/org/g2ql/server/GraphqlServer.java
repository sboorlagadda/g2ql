package org.g2ql.server;

import org.apache.geode.cache.Cache;
import org.g2ql.graphql.GraphQLExecutor;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;

public class GraphqlServer {
  final int PORT = 3000;
  Server server = null;

  public GraphqlServer(Cache cache) {
    cache.getLogger().info("GraphqlServer - init!");
    server = new Server(PORT);
    GraphQLExecutor executor = new GraphQLExecutor(cache);
    QueryHandler queryHandler = new QueryHandler(cache, executor);
    cache.getLogger().info("GraphqlServer - queryHandler is initialized!");

    HandlerList handlers = new HandlerList();
    handlers.setHandlers(new Handler[] {queryHandler});
    server.setHandler(handlers);
    cache.getLogger().info("GraphqlServer - handlers are set!");

    try {
      server.start();
      cache.getLogger().info("GraphqlServer - successfully started!");
    } catch (Exception e) {
      cache.getLogger().error("GraphqlServer - Errrrrr while starting graphql", e);
    }
  }
}
