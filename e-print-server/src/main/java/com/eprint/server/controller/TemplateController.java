package com.eprint.server.controller;

import com.niko.boot.model.result.NikoResult;
import com.niko.boot.web.controller.BaseRestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/templates")
public class TemplateController extends BaseRestController {

    @GetMapping("/{templateCode}")
    public NikoResult getTemplate(@PathVariable String templateCode) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("templateCode", templateCode);
        data.put("version", "1.0.0");
        data.put("content", "<div class=\"label\"><h3>{{productName}}</h3><p>SKU: {{sku}}</p><p>{{price}}</p><div>{{qrText}}</div></div>");
        return NikoResult.data(data);
    }
}
