package org.g2ql.geode;

import static org.apache.commons.beanutils.BeanUtils.populate;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

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
  private Class<?> valueClass;

  public GeodePutDataFetcher(Cache cache, String regionName, Class<?> valueClass) {
    this.cache = cache;
    this.regionName = regionName;
    this.valueClass = valueClass;
  }

  @Override
  public Object get(DataFetchingEnvironment environment) {
    Map<String, Object> arguments = environment.getArguments();
    if (arguments.containsKey("key") && arguments.containsKey("value")) {
      Object key = environment.getArgument("key");
      Object value = environment.getArgument("value");

      logger.info("GeodePutDataFetcher - put - key:" + key + ", value=" + value);
      if (key == null)
        return "{}";
      Region region = cache.getRegion(regionName);
      region.put(key, value);
      return value;
    } else if (arguments.size() > 0 && valueClass != null
        && arguments.containsKey(valueClass.getSimpleName())) {
      try {
        Object value = valueClass.newInstance();
        populate(value, (Map<String, Object>) arguments.get(valueClass.getSimpleName()));

        Object key = environment.getArgument("key");
        logger.info("GeodePutDataFetcher - put - key:" + key + ", value=" + value);

        Region region = cache.getRegion(regionName);
        region.put(key, value);
        return value;
      } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
        e.printStackTrace();
      }
    }
    return "{}";
  }
}
