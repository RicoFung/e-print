package com.eprint.server.controller;

import com.niko.boot.model.result.NikoResult;
import com.niko.boot.web.controller.BaseRestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController extends BaseRestController {

    @GetMapping
    public NikoResult health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "e-print-server");
        data.put("status", "UP");
        data.put("time", Instant.now().toString());
        return NikoResult.data(data);
    }
}
