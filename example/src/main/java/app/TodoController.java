package app;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

@Controller
@ExecuteOn(TaskExecutors.BLOCKING)
public class TodoController {
    private final UserClient userClient;
    private final TodoClient todoClient;

    public TodoController(final UserClient userClient, final TodoClient todoClient) {
        this.userClient = userClient;
        this.todoClient = todoClient;
    }

    @Get("/")
    List<TodoDTO> todos() {
        return todoClient.findAll()
                .stream()
                .map(todo -> new TodoDTO(todo.id(), todo.title(), userClient.findOne(todo.userId()).name()))
                .toList();
    }

    @Serdeable
    record TodoDTO(Long id, String title, String userName) {
    }
}
