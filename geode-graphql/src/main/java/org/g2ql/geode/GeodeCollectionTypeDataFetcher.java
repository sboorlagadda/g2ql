package org.g2ql.geode;

import java.util.Collections;
import java.util.List;
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

public class GeodeCollectionTypeDataFetcher implements DataFetcher {
  private final static Logger logger = LogManager.getLogger(GeodeCollectionTypeDataFetcher.class);

  private Cache cache;
  private String regionName;

  public GeodeCollectionTypeDataFetcher(Cache cache, String regionName) {
    this.cache = cache;
    this.regionName = regionName;
  }

  @Override
  public Object get(DataFetchingEnvironment environment) {
    Map<String, Object> arguments = environment.getArguments();
    Field field = environment.getFields().iterator().next();
    if (arguments.size() > 0) {
      String argumentName = field.getArguments().get(0).getName();
      List<Object> predicates = (List<Object>) arguments.get(argumentName);
      if (predicates.isEmpty()) {
        return Collections.EMPTY_LIST;
      }
      if (argumentName.equalsIgnoreCase("key")) {
        logger.info("GeodeCollectionTypeDataFetcher - get - keys:" + predicates);
        Region region = cache.getRegion(regionName);
        return region.getAll(predicates).values();
      } else {
        logger.info("GeodeCollectionTypeDataFetcher - oql - where clause:" + argumentName
            + ", predicate:" + predicates);
        try {
          SelectResults results = (SelectResults) cache.getQueryService()
              .newQuery(query(argumentName, predicates.size())).execute(predicates.toArray());
          if (results.isEmpty()) {
            return Collections.EMPTY_LIST;
          }
          return results.asList();
        } catch (QueryInvocationTargetException | NameResolutionException | FunctionDomainException
            | TypeMismatchException e) {
          return Collections.EMPTY_LIST;
        }
      }
    }
    return Collections.EMPTY_LIST;
  }

  private String query(String field, int args) {
    StringBuilder query = new StringBuilder();
    query.append("SELECT DISTINCT * FROM /");
    query.append(regionName);
    query.append(" x where x.");
    query.append(field);
    query.append(" IN set(");
    for (int i = 1; i <= args; i++) {
      query.append("$" + i);
      if (i < args) {
        query.append(", ");
      }
    }
    query.append(")");
    return query.toString();
  }
}
