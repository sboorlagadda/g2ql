package org.g2ql.geode;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.geode.cache.Cache;
import org.apache.geode.cache.Region;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GeodeConnectionTypeDataFetcher implements DataFetcher {
  private final static Logger logger = LogManager.getLogger(GeodeConnectionTypeDataFetcher.class);

  private Cache cache;
  private String regionName;
  private String fieldName;

  public GeodeConnectionTypeDataFetcher(Cache cache, String regionName, String fieldName) {
    this.cache = cache;
    this.regionName = regionName;
    this.fieldName = fieldName;
  }

  @Override
  public Object get(DataFetchingEnvironment environment) {
    try {
      Object source = environment.getSource();
      Field field = source.getClass().getDeclaredField(fieldName);
      field.setAccessible(true);
      List<?> keys = (List<?>) field.get(source);
      logger.info("GeodeConnectionTypeDataFetcher - get - keys:" + keys);
      if (keys == null || keys.isEmpty())
        return new ArrayList<>();
      Region region = cache.getRegion(regionName);
      Map connections = region.getAll(keys);
      return connections.values();
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
      return new ArrayList<>();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
      return new ArrayList<>();
    }
  }
}
