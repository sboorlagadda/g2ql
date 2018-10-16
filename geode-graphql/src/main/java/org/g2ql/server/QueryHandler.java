package org.g2ql.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
      String origin = request.getHeader("Origin");

      logger.info("QueryHandler - handleGraphql:: method - " + method);
      logger.info("QueryHandler - handleGraphql:: path - " + path);
      logger.info("QueryHandler - handleGraphql:: body - " + requestBody);
      logger.info("QueryHandler - handleGraphql:: query string - " + queryString);
      logger.info("QueryHandler - handleGraphql:: query parameter - " + queryAsParameter);
      logger.info("QueryHandler - handleGraphql:: variables parameter - " + variablesAsParameter);
      logger.info("QueryHandler - handleGraphql:: Origin: " + origin);

      if (method.equalsIgnoreCase("OPTIONS")) {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("Access-Control-Allow-Origin", origin);
        response.setHeader("Access-Control-Allow-Methods", "OPTIONS, GET, POST");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Headers", "content-type, accept");
      } else if (method.equalsIgnoreCase("GET")) {
        if (path == null) {
          path = request.getServletPath();
        }
        if (path.contentEquals("/graphql/schema.json")) {
          ExecutionResult result = query(IntrospectionQuery.INTROSPECTION_QUERY);
          returnAsJson(response, result, origin);
        } else {
          if (queryAsParameter != null) {
            final Map<String, Object> variables = new HashMap<>();
            if (variablesAsParameter != null) {
              variables.putAll(deserializeVariables(variablesAsParameter));
            }
            ExecutionResult result = query(queryAsParameter, variables);
            returnAsJson(response, result, origin);
          } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            logger.info(
                "Bad GET request: path was not \"/schema.json\" or no query variable named \"query\" given");
          }
        }
      } else if (method.equalsIgnoreCase("POST")) {
        logger.info("QueryHandler - handleGraphql - serving post request......");
        GraphQLRequest graphQLRequest = mapper.readValue(requestBody, GraphQLRequest.class);
        logger.info("QueryHandler - handleGraphql - query from body:" + graphQLRequest + " received");
        if (graphQLRequest.getOperationName() != null) {
          ExecutionResult result = query(graphQLRequest.getQuery(), graphQLRequest.getVariables(),
              graphQLRequest.getOperationName());
          returnAsJson(response, result, origin);
        } else {
          ExecutionResult result = query(graphQLRequest.getQuery(), graphQLRequest.getVariables());
          returnAsJson(response, result, origin);
        }
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

  private ExecutionResult query(String query) {
    return executor.execute(query);
  }

  private ExecutionResult query(String query, Map<String, Object> variables) {
    return executor.execute(query, variables);
  }

  private ExecutionResult query(String query, Map<String, Object> variables, String operationName) {
    return executor.execute(query, variables, operationName);
  }

  private void returnAsJson(HttpServletResponse response, ExecutionResult executionResult, String origin)
      throws IOException {
    response.setHeader("Access-Control-Allow-Origin", origin);
    response.setHeader("Access-Control-Allow-Methods", "OPTIONS, GET, POST");
    response.setHeader("Access-Control-Allow-Credentials", "true");
    response.setHeader("Access-Control-Allow-Headers", "content-type, accept");
    response.setContentType("application/json");
    response.setStatus(HttpServletResponse.SC_OK);
    response.getWriter().write(mapper.writeValueAsString(executionResult.toSpecification()));
  }

}
