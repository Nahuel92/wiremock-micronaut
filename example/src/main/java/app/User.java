package app;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable.Deserializable
public record User(Long id, String name) {
}
