package org.g2ql.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class JacksonDeserializationTest {

  @Test
  public void testQueryDeserialization() throws IOException {
    String request =
        "{\"query\":\"{\\n  Person(key: \\\"1\\\") {\\n    id\\n    firstName\\n  }\\n}\\n\",\"variables\":null,\"operationName\":null}";
    ObjectMapper mapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    QueryHandler.GraphQLRequest graphQLRequest =
        mapper.readValue(request, QueryHandler.GraphQLRequest.class);
    assertThat(graphQLRequest).isNotNull();
    assertThat(graphQLRequest.getQuery())
        .isEqualTo("{\n  Person(key: \"1\") {\n    id\n    firstName\n  }\n}\n");
  }
}
