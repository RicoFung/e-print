package com.eprint.server.controller;

import com.eprint.server.model.CreatePrintTaskRequest;
import com.eprint.server.model.PrintResultRequest;
import com.eprint.server.service.PrintTaskService;
import com.niko.boot.model.result.NikoResult;
import com.niko.boot.web.controller.BaseRestController;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/print/tasks")
public class PrintTaskController extends BaseRestController {

    private final PrintTaskService printTaskService;

    public PrintTaskController(PrintTaskService printTaskService) {
        this.printTaskService = printTaskService;
    }

    @PostMapping
    public NikoResult create(@Valid @RequestBody CreatePrintTaskRequest request) {
        return NikoResult.data(printTaskService.create(request));
    }

    @GetMapping("/{taskId}")
    public NikoResult get(@PathVariable String taskId) {
        return printTaskService.get(taskId)
                .map(NikoResult::data)
                .orElseGet(() -> NikoResult.error("Print task not found"));
    }

    @GetMapping
    public NikoResult list() {
        return NikoResult.data(printTaskService.list());
    }

    @PostMapping("/{taskId}/result")
    public NikoResult reportResult(
            @PathVariable String taskId,
            @Valid @RequestBody PrintResultRequest request) {
        return printTaskService.reportResult(taskId, request)
                .map(NikoResult::data)
                .orElseGet(() -> NikoResult.error("Print task not found"));
    }
}
