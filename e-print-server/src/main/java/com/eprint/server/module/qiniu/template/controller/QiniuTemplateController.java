package com.eprint.server.module.qiniu.template.controller;

import com.eprint.server.module.qiniu.template.service.QiniuTemplateService;
import com.niko.boot.model.result.NikoResult;
import com.niko.boot.web.controller.BaseRestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "template")
@RestController
@RequestMapping("/qiniu/template")
public class QiniuTemplateController extends BaseRestController {

    @Autowired
    private QiniuTemplateService service;

    @Operation(summary = "Get print template by template code")
    @GetMapping("/{templateCode}")
    public NikoResult getTemplate(@PathVariable("templateCode") String templateCode,
                                  @RequestParam("templateType") String templateType) {
        return service.getByTemplateCode(templateType, templateCode);
    }
}
