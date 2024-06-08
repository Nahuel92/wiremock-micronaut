# WireMock Micronaut

## Highlights

- Fully declarative `WireMock` setup.
- Support for multiple `WireMockServer` instances - one per HTTP client as recommended in the WireMock documentation
  automatically sets Micronaut environment properties.
- Doesn't pollute the Micronaut application context with extra beans.

## How to install

In your `pom.xml`, simply add the `wiremock-micronaut` dependency:

```xml

<dependency>
  <groupId>io.github.nahuel92</groupId>
  <artifactId>wiremock-micronaut</artifactId>
  <version>1.0.4</version>
  <scope>test</scope>
</dependency>
```

## How to use

Use `@EnableWireMock` with `@ConfigureWireMock` with tests annotated that use `MicronautJunit5Extension`,
like `@MicronautTest`:

```java

@MicronautTest
@EnableWireMock(@ConfigureWireMock(name = "user-service", property = "user-client.url"))
class TodoControllerTests {
  @InjectWireMock("user-service")
  private WireMockServer wiremock;

  @Value("${user-client.url}")
  private String wiremockUrl; // will contain the base URL for this WireMock instance.

  @Test
  void successOnTestingYourSUT() {
    // given
    wiremock.stubFor(...)

      // then
    // execute your subject under test

    // then
    // your assertions...
  }
}
```

- `@EnableWireMock` adds test context customizer and enables `WireMockMicronautExtension`.
- `@ConfigureWireMock` creates a `WireMockServer` and passes the `WireMockServer.baseUrl` to a Micronaut environment
  property with a name given by a property.
- `@InjectWireMock` injects `WireMockServer` instances to your test.

> **Note:** `WireMockServer` instances aren't added as beans to the Micronaut application context. Instead, instances
> are kept in a separate store associated with the application context used by tests.

## Registering WireMock extensions

WireMock extensions can be registered independently with each `@ConfigureWireMock`:

```java
@ConfigureWireMock(name = "...", property = "...", extensions = {...})
```

## Customizing mappings directory

By default, each `WireMockServer` is configured to load mapping files from a classpath directory
`wiremock/{server-name}/mappings`.

It can be changed with setting `stubLocation` on `@ConfigureWireMock`:

```java
@ConfigureWireMock(name = "...", property = "...", stubLocation = "my-stubs")
```

## Credits

This extension was inspired (and based) on
[WireMock Spring Boot](https://github.com/maciejwalkowiak/wiremock-spring-boot) by Maciej Walkowiak and Stefano Cordio.

I encourage you to support them; and, if you appreciate the time and effort I put into making this extension for
Micronaut, please consider to sponsor this project (WIP).

Thank you 😊