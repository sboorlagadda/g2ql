package org.g2ql.geode;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.Region;

public class GeodeCreateDataFetcher extends GeodeAbstractDataFetcher {

  public GeodeCreateDataFetcher(Cache cache, String regionName, Class<?> valueClass) {
    super(cache, regionName, valueClass);
  }

  @Override
  protected Object doOperation(Object key, Object value) {
    Region region = getRegion();
    region.create(key, value);
    return value;
  }
}
