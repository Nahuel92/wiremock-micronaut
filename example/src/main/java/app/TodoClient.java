package app;

import java.util.List;

import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.annotation.Client;

@Client("${todo-client.url}")
public interface TodoClient {
    @Get("/")
    List<Todo> findAll();
}
