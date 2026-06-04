package com.eprint.server.module.health.controller;

import com.niko.boot.model.result.NikoResult;
import com.niko.boot.web.controller.BaseRestController;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController(value = "HealthController")
@RequestMapping("/health")
@SecurityRequirements
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
