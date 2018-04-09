package org.g2ql.geode;

import graphql.language.ArrayValue;
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

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
    Field field = environment.getFields().iterator().next();
    if (field.getArguments().size() > 0) {
      String argumentName = field.getArguments().get(0).getName();
      if (argumentName.equalsIgnoreCase("key")) {
        List<Object> keys = new ArrayList<>();
        field.getArguments().stream().forEach(a -> {
          Value v = a.getValue();
          keys.addAll(convertArrayValue(v).collect(toList()));
        });
        logger.info("GeodeCollectionTypeDataFetcher - get - keys:" + keys);
        if (keys.isEmpty())
          return new ArrayList<>();
        Region region = cache.getRegion(regionName);
        return region.getAll(keys).values();
      } else {
        List<Object> predicates = new ArrayList<>();
        field.getArguments().stream().forEach(a -> {
          Value v = a.getValue();
          predicates.addAll(convertArrayValue(v).collect(toList()));
        });
        logger.info("GeodeCollectionTypeDataFetcher - oql - where clause:" + argumentName
            + ", predicate:" + predicates);
        List results = new ArrayList();
        predicates.forEach(p -> {
          try {
            results.addAll(
                (SelectResults) cache.getQueryService().newQuery(query(argumentName)).execute(p));
          } catch (QueryInvocationTargetException | NameResolutionException
              | FunctionDomainException | TypeMismatchException e) {
            // ignore and continue
          }
        });
        return results;
      }
    }
    return new ArrayList<>();
  }

  protected Stream<?> convertArrayValue(Value value) {
    return ((ArrayValue) value).getValues().stream().map(this::convertScalarValue);
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
