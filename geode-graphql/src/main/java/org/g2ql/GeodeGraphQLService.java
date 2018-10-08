package org.g2ql;

import org.apache.geode.cache.Cache;
import org.apache.geode.internal.cache.CacheService;

public interface GeodeGraphQLService extends CacheService {
  Cache getCache();
}
