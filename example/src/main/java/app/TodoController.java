package app;

import java.util.List;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller
public class TodoController {
    private final UserClient userClient;
    private final TodoClient todoClient;

    public TodoController(UserClient userClient, TodoClient todoClient) {
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

    record TodoDTO(Long id, String title, String userName) {
    }
}
