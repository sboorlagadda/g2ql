package org.g2ql.geode;

import graphql.language.BooleanValue;
import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.Region;

public class GeodePutDataFetcher implements DataFetcher {
  private final static Logger logger = LogManager.getLogger(GeodePutDataFetcher.class);

  private Cache cache;
  private String regionName;

  public GeodePutDataFetcher(Cache cache, String regionName) {
    this.cache = cache;
    this.regionName = regionName;
  }

  @Override
  public Object get(DataFetchingEnvironment environment) {
    Object key = environment.getArgument("key");
    Object value = environment.getArgument("value");

    logger.info("GeodeDataFetcher - put - key:" + key + ", value=" + value);
    if (key == null)
      return "{}";
    Region region = cache.getRegion(regionName);
    return region.put(key, value);
  }
}
