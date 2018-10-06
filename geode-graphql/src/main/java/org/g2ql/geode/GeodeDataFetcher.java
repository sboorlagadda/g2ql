package org.g2ql.geode;

import java.util.Map;

import graphql.language.Field;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.query.FunctionDomainException;
import org.apache.geode.cache.query.NameResolutionException;
import org.apache.geode.cache.query.QueryInvocationTargetException;
import org.apache.geode.cache.query.SelectResults;
import org.apache.geode.cache.query.TypeMismatchException;

public class GeodeDataFetcher implements DataFetcher {
  private final static Logger logger = LogManager.getLogger(GeodeDataFetcher.class);

  private Cache cache;
  private String regionName;

  public GeodeDataFetcher(Cache cache, String regionName) {
    this.cache = cache;
    this.regionName = regionName;
  }

  @Override
  public Object get(DataFetchingEnvironment environment) {
    Map<String, Object> arguments = environment.getArguments();
    Field field = environment.getFields().iterator().next();
    if (arguments.size() > 0) {
      String argumentName = field.getArguments().get(0).getName();
      Object predicate = arguments.get(argumentName);
      if (predicate == null) {
        return "{}";
      }
      if (argumentName.equalsIgnoreCase("key")) {
        logger.info("GeodeDataFetcher - get - key:" + predicate);
        Region region = cache.getRegion(regionName);
        return region.get(predicate);
      } else {
        logger.info("GeodeCollectionTypeDataFetcher - query - where clause:" + argumentName
            + ", predicate:" + predicate);
        try {
          SelectResults results = (SelectResults) cache.getQueryService()
              .newQuery(query(argumentName)).execute(predicate);
          if (results.isEmpty()) {
            return "{}";
          }
          return results.asList().get(0);
        } catch (QueryInvocationTargetException | NameResolutionException | FunctionDomainException
            | TypeMismatchException e) {
          return "{}";
        }
      }
    }
    return "{}";
  }

  private String query(String field) {
    StringBuilder query = new StringBuilder();
    query.append("SELECT DISTINCT * FROM /");
    query.append(regionName);
    query.append(" x where x.");
    query.append(field);
    query.append("=$1");

    return query.toString();
  }
}
