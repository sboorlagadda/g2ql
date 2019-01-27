package org.g2ql.serverextension.graphql;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.g2ql.serverextension.graphql.GraphQLRequest;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class JacksonDeserializationTest {

  @Test
  public void testQueryDeserialization() throws IOException {
    String request =
        "{\"query\":\"{\\n  Person(key: \\\"1\\\") {\\n    id\\n    firstName\\n  }\\n}\\n\",\"variables\":null,\"operationName\":null}";
    ObjectMapper mapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    GraphQLRequest graphQLRequest = mapper.readValue(request, GraphQLRequest.class);
    assertThat(graphQLRequest).isNotNull();
    assertThat(graphQLRequest.getQuery())
        .isEqualTo("{\n  Person(key: \"1\") {\n    id\n    firstName\n  }\n}\n");
  }

  @Test
  public void testMutationDeserialization() throws IOException {
    String request =
        "{\"query\":\"mutation PutFoo($key: String, $value: String) {\\n  putFoo(key: $key, value: $value)\\n}\\n\",\"variables\":{\"key\":\"3\",\"value\":\"Three\"},\"operationName\":\"PutFoo\"}";

    ObjectMapper mapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    GraphQLRequest graphQLRequest = mapper.readValue(request, GraphQLRequest.class);
    assertThat(graphQLRequest).isNotNull();
    assertThat(graphQLRequest.getQuery()).isEqualTo(
        "mutation PutFoo($key: String, $value: String) {\n  putFoo(key: $key, value: $value)\n}\n");
    assertThat(graphQLRequest.getVariables()).containsEntry("key", "3");
    assertThat(graphQLRequest.getVariables()).containsEntry("value", "Three");
    assertThat(graphQLRequest.getOperationName()).isEqualTo("PutFoo");
  }
}
