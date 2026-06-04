package com.eprint.server.module.task.controller;

import com.eprint.server.module.task.model.request.TaskCreateRequest;
import com.eprint.server.module.task.model.request.TaskResultRequest;
import com.eprint.server.module.task.service.TaskService;
import com.niko.boot.model.result.NikoResult;
import com.niko.boot.web.controller.BaseRestController;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController(value = "TaskController")
@RequestMapping("/task")
public class TaskController extends BaseRestController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public NikoResult create(@Valid @RequestBody TaskCreateRequest request) {
        try {
            return NikoResult.data(taskService.create(request));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return NikoResult.error(e.getMessage());
        }
    }

    @GetMapping("/{taskId}")
    public NikoResult get(@PathVariable("taskId") String taskId) {
        return taskService.get(taskId)
                .map(NikoResult::data)
                .orElseGet(() -> NikoResult.error("Print task not found"));
    }

    @GetMapping
    public NikoResult list() {
        return NikoResult.data(taskService.list());
    }

    @PostMapping("/{taskId}/result")
    public NikoResult reportResult(
            @PathVariable("taskId") String taskId,
            @Valid @RequestBody TaskResultRequest request) {
        return taskService.reportResult(taskId, request)
                .map(NikoResult::data)
                .orElseGet(() -> NikoResult.error("Print task not found"));
    }
}
