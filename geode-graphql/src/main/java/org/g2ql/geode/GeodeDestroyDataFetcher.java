package org.g2ql.geode;

import java.util.Map;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.Region;

public class GeodeDestroyDataFetcher implements DataFetcher {
  private Cache cache;
  private String regionName;

  public GeodeDestroyDataFetcher(Cache cache, String regionName) {
    this.cache = cache;
    this.regionName = regionName;
  }

  @Override
  public Object get(DataFetchingEnvironment environment) {
    Map<String, Object> arguments = environment.getArguments();

    if (arguments.containsKey("key")) {
      Object key = arguments.get("key");
      Region region = cache.getRegion(regionName);
      return region.destroy(key);
    }
    return "{}";
  }
}
