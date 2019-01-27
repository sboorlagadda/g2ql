package org.g2ql.serverextension.graphql;

import graphql.ExecutionResult;
import org.apache.geode.cache.Cache;
import org.eclipse.jetty.server.Request;
import org.g2ql.graphql.GraphQLExecutor;
import org.g2ql.serverextension.graphql.QueryHandler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;


public class QueryHandlerTest {
  private Cache cache;
  private GraphQLExecutor executor;
  private ExecutionResult executionResult;

  private QueryHandler queryHandler;

  @Before
  public void before() {
    cache = mock(Cache.class);
    executor = mock(GraphQLExecutor.class);
    executionResult = mock(ExecutionResult.class);

    queryHandler = Mockito.spy(new QueryHandler(cache, executor));

    doReturn(executionResult).when(executor).execute(any(), any());
  }

  @Test
  public void testPostWithQueryOnly() throws IOException {
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpServletResponse response = mock(HttpServletResponse.class);

    String body =
        "{\"query\":\"{\\n  Person(key: \\\"1\\\") {\\n    id\\n    firstName\\n  }\\n}\\n\"}";

    doReturn(body).when(queryHandler).requestBody(any());
    doReturn("POST").when(request).getMethod();
    doReturn("/graphql").when(request).getPathInfo();

    doReturn(new HashMap<>()).when(executionResult).toSpecification();
    doReturn(mock(PrintWriter.class)).when(response).getWriter();
    queryHandler.handle("/graphql", mock(Request.class), request, response);
  }
}
