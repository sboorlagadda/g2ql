package org.g2ql.graphql.server

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.g2ql.server.GraphQLRequest
import spock.lang.Specification

class JacksonDeserializationSpec extends Specification {
  def "request should be deserialized"() {
    given:
    def request = '''
        {
            "query": "{ Person { id firstName } }",
            "variables": null,
            "operationName": null
        }      
        '''

    when:
    ObjectMapper mapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
    GraphQLRequest graphQLRequest = mapper.readValue(request, GraphQLRequest.class)

    then:
    graphQLRequest != null
    graphQLRequest.getQuery() != null

    graphQLRequest.getQuery().equals("{ Person { id firstName } }")
  }

  def "request with only query should be deserialized"() {
    given:
    def request = '''
        {
            "query": "{ Person { id firstName } }"
        }      
        '''

    when:
    ObjectMapper mapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
    GraphQLRequest graphQLRequest = mapper.readValue(request, GraphQLRequest.class)

    then:
    graphQLRequest != null
    graphQLRequest.getQuery() != null

    graphQLRequest.getQuery().equals("{ Person { id firstName } }")
  }
}
