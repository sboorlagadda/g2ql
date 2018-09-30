package org.g2ql.server;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import graphql.ExecutionResult;
import graphql.introspection.IntrospectionQuery;
import org.apache.geode.cache.Cache;
import org.g2ql.graphql.GraphQLExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class QueryHandler extends AbstractHandler {
  private final static Logger logger = LogManager.getLogger(QueryHandler.class);

  private GraphQLExecutor executor;
  private Cache cache;
  private static final ObjectMapper mapper =
      new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

  public QueryHandler(Cache cache, GraphQLExecutor executor) {
    this.cache = cache;
    this.executor = executor;
  }

  @Override
  public void handle(String target, Request baseRequest, HttpServletRequest request,
      HttpServletResponse response) {
    if ("/graphql".equals(target)) {
      baseRequest.setHandled(true);
      handleGraphql(request, response);
    }
  }

  private void handleGraphql(HttpServletRequest request, HttpServletResponse response) {
    try {
      String requestBody = requestBody(request);
      String method = request.getMethod();
      String path = request.getPathInfo();
      String queryString = request.getQueryString();
      String queryAsParameter = request.getParameter("query");
      String variablesAsParameter = request.getParameter("variables");

      logger.info("QueryHandler - handleGraphql:: method - " + method);
      logger.info("QueryHandler - handleGraphql:: path - " + path);
      logger.info("QueryHandler - handleGraphql:: body - " + requestBody);
      logger.info("QueryHandler - handleGraphql:: query string - " + queryString);
      logger.info("QueryHandler - handleGraphql:: query parameter - " + queryAsParameter);
      logger.info("QueryHandler - handleGraphql:: variables parameter - " + variablesAsParameter);

      if (method.equalsIgnoreCase("OPTIONS")) {
        logger.info("Options request received....");
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("Access-Control-Allow-Origin", "http://localhost:8080");
        response.setHeader("Access-Control-Allow-Methods", "OPTIONS, GET, POST");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Headers", "content-type, accept");
      } else if (method.equalsIgnoreCase("GET")) {
        if (path == null) {
          path = request.getServletPath();
        }
        if (path.contentEquals("/graphql/schema.json")) {
          query(IntrospectionQuery.INTROSPECTION_QUERY, response);
        } else {
          if (queryAsParameter != null) {
            final Map<String, Object> variables = new HashMap<>();
            if (variablesAsParameter != null) {
              variables.putAll(deserializeVariables(variablesAsParameter));
            }
            query(queryAsParameter, variables, response);
          } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            logger.info(
                "Bad GET request: path was not \"/schema.json\" or no query variable named \"query\" given");
          }
        }
      } else if (method.equalsIgnoreCase("POST")) {
        logger.info("QueryHandler - handleGraphql - serving post request......");
        GraphQLRequest graphQLRequest = mapper.readValue(requestBody, GraphQLRequest.class);
        logger
            .info("QueryHandler - handleGraphql - query from body:" + graphQLRequest + " received");
        if (graphQLRequest.operationName != null) {
          query(graphQLRequest.getQuery(), graphQLRequest.getVariables(),
              graphQLRequest.operationName, response);
        }
        query(graphQLRequest.getQuery(), graphQLRequest.getVariables(), response);
      }
    } catch (IOException ioe) {
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      logger.error("Bad request received with payload.", ioe);
    }
  }

  protected String requestBody(HttpServletRequest request) throws IOException {
    return request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
  }

  private Map<String, Object> deserializeVariables(String variables) {
    try {
      return deserializeVariablesObject(mapper.readValue(variables, Object.class), mapper);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static Map<String, Object> deserializeVariablesObject(Object variables,
      ObjectMapper mapper) {
    if (variables instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> genericVariables = (Map<String, Object>) variables;
      return genericVariables;
    } else if (variables instanceof String) {
      try {
        return mapper.readValue((String) variables, new TypeReference<Map<String, Object>>() {});
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      throw new RuntimeException("variables should be either an object or a string");
    }
  }

  private void query(String query, HttpServletResponse response) throws IOException {
    ExecutionResult executionResult = executor.execute(query);
    returnAsJson(response, executionResult);
  }

  private void query(String query, Map<String, Object> variables, HttpServletResponse response)
      throws IOException {
    ExecutionResult executionResult = executor.execute(query, variables);
    returnAsJson(response, executionResult);
  }

  private void query(String query, Map<String, Object> variables, String operationName,
      HttpServletResponse response) throws IOException {
    ExecutionResult executionResult = executor.execute(query, variables, operationName);
    returnAsJson(response, executionResult);
  }

  private void returnAsJson(HttpServletResponse response, ExecutionResult executionResult)
      throws IOException {
    response.setHeader("Access-Control-Allow-Origin", "http://localhost:8080");
    response.setHeader("Access-Control-Allow-Methods", "OPTIONS, GET, POST");
    response.setHeader("Access-Control-Allow-Credentials", "true");
    response.setHeader("Access-Control-Allow-Headers", "content-type, accept");
    response.setContentType("application/json");
    response.setStatus(HttpServletResponse.SC_OK);
    response.getWriter().write(mapper.writeValueAsString(executionResult.toSpecification()));
  }

  protected static class GraphQLRequest {
    private String query;
    @JsonDeserialize(using = QueryHandler.VariablesDeserializer.class)
    private Map<String, Object> variables = new HashMap<>();
    private String operationName;

    public String getQuery() {
      return query;
    }

    public void setQuery(String query) {
      this.query = query;
    }

    public Map<String, Object> getVariables() {
      return variables;
    }

    public void setVariables(Map<String, Object> variables) {
      this.variables = variables;
    }

    public String getOperationName() {
      return operationName;
    }

    public void setOperationName(String operationName) {
      this.operationName = operationName;
    }

    @Override
    public String toString() {
      return "GraphQLRequest{" + "query='" + query + '\'' + ", variables=" + variables
          + ", operationName='" + operationName + '\'' + '}';
    }
  }

  protected static class VariablesDeserializer extends JsonDeserializer<Map<String, Object>> {
    @Override
    public Map<String, Object> deserialize(JsonParser p, DeserializationContext ctxt)
        throws IOException {
      return deserializeVariablesObject(p.readValueAs(Object.class),
          (ObjectMapper) ctxt.findInjectableValue(ObjectMapper.class.getName(), null, null));
    }
  }
}
