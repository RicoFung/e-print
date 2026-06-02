package com.eprint.server.controller;

import com.niko.boot.model.result.NikoResult;
import com.niko.boot.web.controller.BaseRestController;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/templates")
public class TemplateController extends BaseRestController {

    private static final String TEMPLATE_BASE_PATH = "templates/print/";
    private static final String TEMPLATE_SUFFIX = ".html";

    @GetMapping("/{templateCode}")
    public NikoResult getTemplate(@PathVariable String templateCode) throws IOException {
        ClassPathResource resource = new ClassPathResource(TEMPLATE_BASE_PATH + templateCode + TEMPLATE_SUFFIX);
        if (!resource.exists() || !resource.isReadable()) {
            return NikoResult.error("Template not found");
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("templateCode", templateCode);
        data.put("version", "1.0.0");
        data.put("content", StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8));
        return NikoResult.data(data);
    }
}
