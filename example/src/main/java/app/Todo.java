package app;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable.Deserializable
public record Todo(Long id, Long userId, String title) {
}
