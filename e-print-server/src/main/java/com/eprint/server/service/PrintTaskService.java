package com.eprint.server.service;

import com.eprint.server.model.CreatePrintTaskRequest;
import com.eprint.server.model.PrintResultRequest;
import com.eprint.server.model.PrintTask;
import com.eprint.server.model.PrintTaskStatus;
import com.eprint.server.websocket.PrintClientSessionRegistry;
import com.niko.boot.service.BaseService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class PrintTaskService extends BaseService {

    private final ConcurrentMap<String, PrintTask> tasks = new ConcurrentHashMap<>();
    private final PrintClientSessionRegistry sessionRegistry;

    public PrintTaskService(PrintClientSessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    public PrintTask create(CreatePrintTaskRequest request) {
        Instant now = Instant.now();

        PrintTask task = new PrintTask();
        task.setTaskId(UUID.randomUUID().toString());
        task.setClientId(request.getClientId());
        task.setTemplateCode(request.getTemplateCode());
        task.setCopies(request.getCopies());
        task.setData(request.getData());
        task.setStatus(PrintTaskStatus.CREATED);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);

        tasks.put(task.getTaskId(), task);

        if (sessionRegistry.sendPrintTask(task.getClientId(), task)) {
            task.setStatus(PrintTaskStatus.DISPATCHED);
            task.setUpdatedAt(Instant.now());
        }

        return task;
    }

    public Optional<PrintTask> get(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }

    public List<PrintTask> list() {
        List<PrintTask> result = new ArrayList<>(tasks.values());
        result.sort(Comparator.comparing(PrintTask::getCreatedAt).reversed());
        return result;
    }

    public Optional<PrintTask> reportResult(String taskId, PrintResultRequest request) {
        PrintTask task = tasks.get(taskId);
        if (task == null) {
            return Optional.empty();
        }

        if ("SUCCESS".equalsIgnoreCase(request.getStatus())) {
            task.setStatus(PrintTaskStatus.SUCCESS);
        } else {
            task.setStatus(PrintTaskStatus.FAILED);
        }
        task.setResultMessage(request.getMessage());
        task.setUpdatedAt(Instant.now());
        return Optional.of(task);
    }
}
