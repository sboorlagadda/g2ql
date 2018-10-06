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

public abstract class GeodeAbstractDataFetcher implements DataFetcher {
  private final static Logger logger = LogManager.getLogger(GeodeAbstractDataFetcher.class);

  private Cache cache;
  private String regionName;
  private Class<?> valueClass;

  public GeodeAbstractDataFetcher(Cache cache, String regionName, Class<?> valueClass) {
    this.cache = cache;
    this.regionName = regionName;
    this.valueClass = valueClass;
  }

  @Override
  public Object get(DataFetchingEnvironment environment) {
    Map<String, Object> arguments = environment.getArguments();

    if (arguments.containsKey("key")) {
      Object key = arguments.get("key");
      Object value = null;
      if (arguments.containsKey("key") && arguments.containsKey("value")) {
        value = environment.getArgument("value");
      } else if (arguments.size() > 0 && valueClass != null
          && arguments.containsKey(valueClass.getSimpleName())) {
        try {
          value = valueClass.newInstance();
          populate(value, (Map<String, Object>) arguments.get(valueClass.getSimpleName()));
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
          e.printStackTrace();
        }
      }
      logger.info("GeodePutDataFetcher - put - key:" + key + ", value=" + value);
      if (value == null) {
        return "{}";
      }
      return doOperation(key, value);
    }
    return "{}";
  }

  protected Region getRegion() {
    return cache.getRegion(regionName);
  }

  protected abstract Object doOperation(Object key, Object value);
}
