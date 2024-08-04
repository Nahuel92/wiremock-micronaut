# WireMock Micronaut

## Highlights

- Fully declarative `WireMock` setup.
- Support for multiple `WireMockServer` instances - one per HTTP client as recommended in the WireMock documentation
  automatically sets Micronaut environment properties.
- Support for gRCP.
- Doesn't pollute the Micronaut application context with extra beans.

## How to install

In your `pom.xml`, simply add the `wiremock-micronaut` dependency:

```xml
<dependency>
  <groupId>io.github.nahuel92</groupId>
  <artifactId>wiremock-micronaut</artifactId>
  <version>1.8.0</version>
  <scope>test</scope>
</dependency>
```

## How to use

Use `@EnableWireMock` with `@ConfigureWireMock` with tests annotated that use `MicronautJunit5Extension`,
like `@MicronautTest`:

```java
@MicronautTest
@EnableWireMock(
        @ConfigureWireMock(
                name = "user-service",
                properties = "user-client.url"
        )
)
class TodoControllerTests {
    @InjectWireMock("user-service")
    private WireMockServer wiremock;

    @Value("${user-client.url}")
    private String wiremockUrl; // will contain the base URL for this WireMock instance.

    @Test
    void yourSUTTest() {
        // given
      wiremock.stubFor(/*Your request*/);

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

### Single Property Injection

The following example shows how to use the *Single Property Injection*, which means each service is bound to an
exclusive `WireMockServer` instance. You get maximum isolation between your services' mocks at the expense of a more
complex test setup.

```java
@MicronautTest
@EnableWireMock({
        @ConfigureWireMock(
                name = "foo-service",
                properties = "app.client-apis.foo.base-path"
        ),
        @ConfigureWireMock(
                name = "bar-service",
                properties = "app.client-apis.bar.base-path"
        ),
        @ConfigureWireMock(
                name = "mojo-service",
                properties = "app.client-apis.mojo.base-path"
        )
})
class YourTest {
    @InjectWireMock("foo-service")
    private WireMockServer fooService;

    @InjectWireMock("bar-service")
    private WireMockServer barService;

    @InjectWireMock("mojo-service")
    private WireMockServer mojoService;

    @Test
    void yourSUTTest() {
        // your test code
    }
}
```

### Multiple Property Injection

The following example shows how to use the *Multiple Property Injection*, which means all services are bound to a shared
`WireMockServer` instance. You give up on isolation between your services' mocks, but you get a less complex test setup.

```java
@MicronautTest
@EnableWireMock(
        @ConfigureWireMock(
                name = "services",
                properties = {
                        "app.client-apis.foo.base-path",
                        "app.client-apis.bar.base-path",
                        "app.client-apis.mojo.base-path"
                }
        )
)
class YourTest {
    @InjectWireMock("services")
    private WireMockServer services;

    @Test
    void yourSUTTest() {
        // your test code
    }
}
```

### Using the `WireMock` client

Usually, you'll configure your tests as follows:

```java
@MicronautTest
@EnableWireMock({
        @ConfigureWireMock(
                name = "todo-client",
                properties = "todo-client.url",
                stubLocation = "custom-location"
        ),
        @ConfigureWireMock(
                name = "user-client",
                properties = "user-client.url"
        )
})
@DisplayName("WireMock server instances must be accessed via injected fields (optional if only one is needed)")
class YourTest {
  @InjectWireMock("todo-service")
  private WireMockServer todoService;

  @InjectWireMock("user-client")
  private WireMockServer userService;

  @Test
  void yourSUTTest() {
    // given
    todoService.stubFor(get("/").willReturn(ok()));
    userService.stubFor(get("/").willReturn(ok()));

    // your test code
  }
}
```

Or, if you need only one server:

```java
@MicronautTest
@EnableWireMock(
        @ConfigureWireMock(
                name = "todo-client",
                properties = "todo-client.url",
                stubLocation = "custom-location"
        )
)
@DisplayName("WireMock server instances must be accessed via injected fields (optional if only one is needed)")
class YourTest {
  @InjectWireMock("todo-service")
  private WireMockServer todoService;

  @Test
  void yourSUTTest() {
    // given
    todoService.stubFor(get("/").willReturn(ok()));

    // your test code
  }
}
```

In the previous situation, when the test only requires exactly one WireMock server instance, we can simplify it a bit.
In this case, the `WireMock` client class can be used to configure your stubs:

```java
@MicronautTest
@EnableWireMock(
        @ConfigureWireMock(
                name = "todo-client",
                properties = "todo-client.url",
                stubLocation = "custom-location"
        )
)
@DisplayName("When exactly one WireMock server instance is configured, it can be accessed statically via the 'WireMock' class")
class YourTest {
  @Test
  void yourSUTTest() {
    // given
    WireMock.stubFor(get("/").willReturn(ok()));

    // your test code
  }
}
```

### Stub location configuration

By default, classpath location is used to get stubs:

```java
@MicronautTest
@EnableWireMock(
        @ConfigureWireMock(
                name = "todo-client",
                properties = "todo-client.url",
                stubLocation = "a-directory-on-the-classpath" // By default, the classpath is used
        )
)
class YourTest {
  @Inject
  private TodoClient todoClient;

  @Test
  @DisplayName("WireMock should use a directory on the classpath as the stub location")
  void yourSUTTest() {
    // when
    final var results = todoClient.findAll();

    //then
    // your test assertions
  }
}
```

But sometimes you may want to use any directory on the file system.
To achieve that, you can override a property called `stubLocationOnClasspath` on the `@ConfigureWireMock`:

```java
@MicronautTest
@EnableWireMock(
        @ConfigureWireMock(
                name = "todo-client",
                properties = "todo-client.url",
                stubLocation = "a-directory-on-the-file-system",
                stubLocationOnClasspath = false
        )
)
class YourTest {
  @Inject
  private TodoClient todoClient;

  @Test
  @DisplayName("WireMock should use a directory on the file system as the stub location")
  void yourSUTTest() {
    // when
    final var results = todoClient.findAll();

    //then
    // your test assertions
  }
}
```

### gRPC

gRPC testing is very similar to previous examples, but:

- It requires extra configuration in your project (not showed here) to compile your `.proto` files.
- It requires a service descriptor file for each `.proto` service you want to test.

In the following example, WireMock is instructed to:

- Internal WireMock server and gRPC service are set with the same name (`GreeterGrpc.SERVICE_NAME`, which comes from the
  generated code from the `.proto` file).
- Load the Grpc extension that enables gRPC support.
- Search for service descriptor files under `src/test/resources/wiremock`.

```java
@MicronautTest
@EnableWireMock({
        @ConfigureWireMock(
                name = GreeterGrpc.SERVICE_NAME,
                portProperty = "my.port",
                properties = "my.server",
                extensionFactories = GrpcExtensionFactory.class,
                stubLocation = "src/test/resources/wiremock"
        )
})
public class GrpcTest {
  @Inject
  private GreeterGrpc.GreeterBlockingStub greeter;

  @InjectWireMock(GreeterGrpc.SERVICE_NAME)
  private WireMockGrpcService greeterGrpcService;

  @Test
  @DisplayName("WireMock should allow configuring single gRPC service per test")
  void successOnTestingWithSingleGrpcService() {
    // given
    createGreeterStub();

    // when
    final var message = greeter.sayHello(HelloRequest.newBuilder().setName("Tom").build());

    // then
    assertThat(message.getMessage()).isEqualTo("Hello Tom!");
  }
}
```

It also supports multiple gRPC and HTTP stubs at the same time, although you may want to stick to simpler tests:

```java
@MicronautTest
@EnableWireMock({
        @ConfigureWireMock(
                name = GreeterGrpc.SERVICE_NAME,
                portProperty = "my.port",
                properties = "my.server",
                extensionFactories = GrpcExtensionFactory.class,
                stubLocation = "src/test/resources/wiremock"
        ),
        @ConfigureWireMock(
                name = Greeter2Grpc.SERVICE_NAME,
                portProperty = "my.port2",
                properties = "my.server2",
                extensionFactories = GrpcExtensionFactory.class,
                stubLocation = "src/test/resources/wiremock2"
        ),
        @ConfigureWireMock(name = "user-client", properties = "user-client.url")
})
class GrpcAndHttpTest {
  @Inject
  private GreeterGrpc.GreeterBlockingStub greeter;

  @Inject
  private Greeter2Grpc.Greeter2BlockingStub greeter2;

  @InjectWireMock(GreeterGrpc.SERVICE_NAME)
  private WireMockGrpcService greeterGrpcService;

  @InjectWireMock(Greeter2Grpc.SERVICE_NAME)
  private WireMockGrpcService greeter2GrpcService;

  @Inject
  private UserClient userClient;

  @Test
  @DisplayName("WireMock should allow configuring multiple gRPC and HTTP services per test")
  void successOnTestingWithGrpc() {
    // given
    greeterGrpcService.stubFor(method("sayHello")
            .withRequestMessage(equalToMessage(HelloRequest.newBuilder().setName("Tom")))
            .willReturn(message(HelloReply.newBuilder().setMessage("Hello Tom!")))
    );
    greeter2GrpcService.stubFor(method("sayHello2")
            .withRequestMessage(equalToMessage(HelloRequest2.newBuilder().setName("Nahuel")))
            .willReturn(message(HelloReply2.newBuilder().setMessage("Hello Nahuel!")))
    );
    final var result = userClient.findOne(1L);

    // when
    final var message = greeter.sayHello(HelloRequest.newBuilder().setName("Tom").build());
    final var message2 = greeter2.sayHello2(HelloRequest2.newBuilder().setName("Nahuel").build());

    // then
    try (final var softly = new AutoCloseableSoftAssertions()) {
      softly.assertThat(message.getMessage()).isEqualTo("Hello Tom!");
      softly.assertThat(message2.getMessage()).isEqualTo("Hello Nahuel!");
      softly.assertThat(result.name()).isEqualTo("Jenna");
    }
  }
}
```

More test examples can be found in the `example` module.

## Registering WireMock extensions

WireMock extensions can be registered independently with each `@ConfigureWireMock`:

```java
@ConfigureWireMock(name = "...", property = "...", extensions = {/*...*/})
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