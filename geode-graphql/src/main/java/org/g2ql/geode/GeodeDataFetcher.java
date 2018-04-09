package org.g2ql.geode;

import graphql.language.BooleanValue;
import graphql.language.Field;
import graphql.language.FloatValue;
import graphql.language.IntValue;
import graphql.language.StringValue;
import graphql.language.Value;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.apache.geode.cache.Cache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.query.FunctionDomainException;
import org.apache.geode.cache.query.NameResolutionException;
import org.apache.geode.cache.query.QueryInvocationTargetException;
import org.apache.geode.cache.query.SelectResults;
import org.apache.geode.cache.query.TypeMismatchException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

import static java.util.stream.Collectors.toList;

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
    Field field = environment.getFields().iterator().next();
    if (field.getArguments().size() > 0) {
      String argumentName = field.getArguments().get(0).getName();
      if (argumentName.equalsIgnoreCase("key")) {
        List<Object> keys = field.getArguments().stream().map(a -> a.getValue())
            .map(v -> convertScalarValue(v)).collect(toList());
        logger.info("GeodeDataFetcher - get - keys:" + keys);
        if (keys == null || keys.isEmpty())
          return "{}";
        Region region = cache.getRegion(regionName);
        return region.get(keys.get(0));
      } else {
        List<Object> predicates = field.getArguments().stream().map(a -> a.getValue())
            .map(v -> convertScalarValue(v)).collect(toList());
        logger.info("GeodeCollectionTypeDataFetcher - query - where clause:" + argumentName
            + ", predicate:" + predicates);
        if (predicates.isEmpty())
          return "{}";
        try {
          SelectResults results = (SelectResults) cache.getQueryService()
              .newQuery(query(argumentName)).execute(predicates.get(0));
          if (results.asList().size() > 0)
            return results.asList().get(0);
          return "{}";
        } catch (QueryInvocationTargetException | NameResolutionException | FunctionDomainException
            | TypeMismatchException e) {
          return "{}";
        }
      }
    }
    return "{}";
  }

  protected Object convertScalarValue(Value value) {
    if (value instanceof StringValue)
      return ((StringValue) value).getValue();
    else if (value instanceof IntValue)
      return ((IntValue) value).getValue();
    else if (value instanceof BooleanValue)
      return ((BooleanValue) value).isValue();
    else if (value instanceof FloatValue)
      return ((FloatValue) value).getValue();

    return value.toString();
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
