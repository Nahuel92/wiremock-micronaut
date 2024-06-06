package app;

import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.client.annotation.Client;

@Client("${user-client.url}")
public interface UserClient {
    @Get("/{id}")
    User findOne(@PathVariable("id") Long userId);
}
