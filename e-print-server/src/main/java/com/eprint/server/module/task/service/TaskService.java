package com.eprint.server.module.task.service;

import com.eprint.server.module.task.model.Task;
import com.eprint.server.module.task.model.TaskStatus;
import com.eprint.server.module.task.model.request.TaskCreateRequest;
import com.eprint.server.module.task.model.request.TaskResultRequest;
import com.eprint.server.module.template.service.TemplateService;
import com.eprint.server.repository.model.result.TemplateResult;
import com.eprint.server.websocket.PrintClientSessionRegistry;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service(value = "TaskService")
public class TaskService {

    private final ConcurrentMap<String, Task> tasks = new ConcurrentHashMap<>();
    private final PrintClientSessionRegistry sessionRegistry;
    private final TemplateService templateService;

    public TaskService(PrintClientSessionRegistry sessionRegistry, TemplateService templateService) {
        this.sessionRegistry = sessionRegistry;
        this.templateService = templateService;
    }

    public Task create(TaskCreateRequest request) {
        TemplateResult template = templateService.resolveTemplate(request.getTemplateType(), request.getTemplateCode());

        Instant now = Instant.now();

        Task task = new Task();
        task.setTaskId(UUID.randomUUID().toString());
        task.setClientId(request.getClientId());
        task.setTemplateType(template.getTemplateType());
        task.setTemplateCode(template.getTemplateCode());
        task.setCopies(request.getCopies());
        task.setData(request.getData());
        task.setStatus(TaskStatus.CREATED);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);

        tasks.put(task.getTaskId(), task);

        if (sessionRegistry.sendPrintTask(task.getClientId(), task)) {
            task.setStatus(TaskStatus.DISPATCHED);
            task.setUpdatedAt(Instant.now());
        }

        return task;
    }

    public Optional<Task> get(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }

    public List<Task> list() {
        List<Task> result = new ArrayList<>(tasks.values());
        result.sort(Comparator.comparing(Task::getCreatedAt).reversed());
        return result;
    }

    public Optional<Task> reportResult(String taskId, TaskResultRequest request) {
        Task task = tasks.get(taskId);
        if (task == null) {
            return Optional.empty();
        }

        if ("SUCCESS".equalsIgnoreCase(request.getStatus())) {
            task.setStatus(TaskStatus.SUCCESS);
        } else {
            task.setStatus(TaskStatus.FAILED);
        }
        task.setResultMessage(request.getMessage());
        task.setUpdatedAt(Instant.now());
        return Optional.of(task);
    }
}
