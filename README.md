# G2QL - Geode GraphQL Extension

Despite its name, GraphQL was not meant to be SQL for graph databases. Instead, GraphQL is an API query language developed by Facebook to improve the performance of interactions between a server and a client, such as a web browser or mobile app.

**GraphQL = Projection + Composition** for your APIs.
* **Projection** Client specifies what data fields it needs. Avoiding a predefined output format means that no unneeded data is sent, reducing the response transfer time to the necessary minimum.

* **Composition** Client can fetch multiple resources in a single query. Avoiding multiple back-to-back server requests means significantly less latency to complete loading data on the client.

## Why G2QL?

G2QL is an extension that adds a new query language for your Apache Geode™ or Pivotal GemFire clusters that allows users to build web & mobile applications using any standard GraphQL libraries. G2QL provides an out-of-box experience by defining GraphQL schema through introspection. G2QL can be deployed to any Geode cluster and serves a GraphQL endpoint from an embedded jetty server, just like Geode’s REST endpoint.

G2QL aims to provide an out-of-box solution,
 * For anyone looking to add GraphQL to an existing microservice architecture.
 * For anyone dealing with object graphs.

GraphQL is a solution for reducing API chatter between applications and backend services. It is easy to adapt GraphQL as a layer on top of existing microservices, without realizing that the problems that GraphQL helps to solve between apps and backend now arises between the GraphQL endpoint and backend services. To overcome this, caching is then added to the GraphQL endpoint. Without caching, a GraphQL server is inefficient and fetches a single entity several times from the underlying source. With GraphQL as a query language, Geode can provide an out-of-box solution for anyone adapting GraphQL technology. 

G2QL is also a solution for users who have been grappling with how to deal with object graphs and relationships in Geode. With G2QL, users can use standard GraphQL libraries to query/mutate object graphs, and G2QL takes care of retrieving/storing data to respective Regions holding underlying domain models.

![](assets/graphql.png)

While adding cache to GraphQL server is a natural solution, **G2QL** takes a reverse approach by adding GraphQL to a distributed cache.
 
### G2QL Annotations

Users can use G2QL annotations to control introspection. Assuming user has a region /Person<String, Person>, then annotate the field to let G2QL infer the relationships.

```
class Person {
    String id;
    
    String name;
    
    @GeodeGraphQLConnection(region=“Person”)
    List<String> fiends; //store keys to person objects
}
```
### Roadmap:
 - add cache listener to keep GraphQL schema up-to-date
 - support mutations and subscriptions
 - introspect PDX types
 - introspect oql indexes
 - extend integrated security

### FAQs
- **Why is a server side extension?** Developers can build a micro service reading data through Geode Client APIs, G2QL is more of an out-of-box experience.
- **Why not OQL over developer REST APIs?** Using OQL, users can do projection + composition of multiple region data, but its not intuitive and there is no client libraries. GraphQL has hundreds of libraries in various languages.
- **Why not invoking a function over REST API (to do all the work and return the JSON)?** With G2QL, non-java app developers do not have to write geode functions.

## Getting Started

1. Build a shadow jar using gradle target `shadowJar`

    ```
    $ ./gradlew clean shadowJar
    
    ```
1. Deploy G2QL to any running Geode cacheserver
    ```
    gfsh> deploy geode-graphql-1.0.jar
    gfsh> execute function --id=graphql-init
    ```

### Running unit tests
```
./gradlew test
```
### Running integration tests

```
# downloads and starts locator and cacheserver
./gradlew start

# to run integration tests
./gradlew integrationTest
```

### Built With

* [graphql-java](https://github.com/graphql-java/graphql-java) - Java implementation of GraphQL Spec.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details
