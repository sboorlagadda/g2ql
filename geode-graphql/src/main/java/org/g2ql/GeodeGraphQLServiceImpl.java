package org.g2ql;

import org.g2ql.server.GraphqlServer;

import org.apache.geode.cache.Cache;
import org.apache.geode.internal.cache.CacheService;
import org.apache.geode.management.internal.beans.CacheServiceMBeanBase;

public class GeodeGraphQLServiceImpl implements GeodeGraphQLService, CacheService {

  private Cache cache;

  @Override
  public void init(Cache cache) {
    this.cache = cache;
    new GraphqlServer(cache);
  }

  @Override
  public Class<? extends CacheService> getInterface() {
    return GeodeGraphQLService.class;
  }

  @Override
  public CacheServiceMBeanBase getMBean() {
    return null;
  }

  @Override
  public Cache getCache() {
    return cache;
  }
}
