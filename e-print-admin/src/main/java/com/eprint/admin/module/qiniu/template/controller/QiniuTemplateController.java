package com.eprint.admin.module.qiniu.template.controller;

import com.eprint.admin.common.controller.BaseController;
import com.eprint.admin.common.model.page.PageResult;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateCreateRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateDisableRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateEnableRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateModifyRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplatePreviewRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateQueryRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateRemoveRequest;
import com.eprint.admin.module.qiniu.template.service.QiniuTemplateService;
import com.eprint.admin.module.qiniu.template.service.QiniuTemplateTypeService;
import com.eprint.admin.repository.qiniu.model.entity.QiniuTemplate;
import com.eprint.admin.repository.qiniu.model.entity.QiniuTemplateType;
import com.eprint.admin.repository.qiniu.model.result.QiniuTemplateResult;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/qiniu/templates")
public class QiniuTemplateController extends BaseController {

    private static final String RETURN_URL = "/qiniu/templates";

    private final QiniuTemplateService templateService;
    private final QiniuTemplateTypeService templateTypeService;

    public QiniuTemplateController(QiniuTemplateService templateService,
                              QiniuTemplateTypeService templateTypeService) {
        this.templateService = templateService;
        this.templateTypeService = templateTypeService;
    }

    @ModelAttribute("sampleData")
    public String sampleData() {
        return templateService.defaultSampleData();
    }

    @ModelAttribute("objectPrefix")
    public String objectPrefix() {
        return templateService.defaultObjectPrefix();
    }

    @GetMapping("/create")
    public String create(@RequestParam(value = "returnUrl", required = false) String returnUrl, Model model) {
        model.addAttribute("request", templateService.createRequest());
        return templateForm(model, returnUrl, null, "qiniu/template/create");
    }

    @PostMapping(value = "/create", produces = MediaType.TEXT_HTML_VALUE)
    public String create(@Valid @ModelAttribute("request") QiniuTemplateCreateRequest request,
                         BindingResult bindingResult,
                         @RequestParam(value = "returnUrl", required = false) String returnUrl,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return templateForm(model, returnUrl, null, "qiniu/template/create");
        }
        try {
            templateService.create(request);
        } catch (RuntimeException e) {
            bindingResult.reject("template.save.failed", e.getMessage());
            return templateForm(model, returnUrl, null, "qiniu/template/create");
        }
        redirectAttributes.addFlashAttribute("message", "Template created");
        return redirect(returnUrl, RETURN_URL);
    }

    @PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> create(@Valid @ModelAttribute("request") QiniuTemplateCreateRequest request,
                                                      BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("message", bindingErrorMessage(bindingResult)));
        }
        try {
            templateService.create(request);
            return ResponseEntity.ok(Map.of("message", "保存成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", String.valueOf(e.getMessage())));
        }
    }

    @PostMapping(value = "/remove", params = "id")
    public String remove(QiniuTemplateRemoveRequest request, RedirectAttributes redirectAttributes) {
        templateService.remove(request);
        redirectAttributes.addFlashAttribute("message", "Template deleted");
        return "redirect:/qiniu/templates";
    }

    @PostMapping(value = "/remove", params = "ids")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> remove(QiniuTemplateRemoveRequest request) {
        return ResponseEntity.ok(Map.of("removed", templateService.remove(request)));
    }

    @GetMapping("/modify")
    public String modify(QiniuTemplateModifyRequest request,
                         @RequestParam(value = "returnUrl", required = false) String returnUrl,
                         Model model) {
        QiniuTemplateModifyRequest modifyRequest = templateService.getModifyRequest(request);
        model.addAttribute("request", modifyRequest);
        return templateForm(model, returnUrl, modifyRequest.getTemplateTypeId(), "qiniu/template/modify");
    }

    @PostMapping(value = "/modify", produces = MediaType.TEXT_HTML_VALUE)
    public String modify(@Valid @ModelAttribute("request") QiniuTemplateModifyRequest request,
                         BindingResult bindingResult,
                         @RequestParam(value = "returnUrl", required = false) String returnUrl,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return templateForm(model, returnUrl, request.getTemplateTypeId(), "qiniu/template/modify");
        }
        try {
            templateService.modify(request);
        } catch (RuntimeException e) {
            bindingResult.reject("template.save.failed", e.getMessage());
            return templateForm(model, returnUrl, request.getTemplateTypeId(), "qiniu/template/modify");
        }
        redirectAttributes.addFlashAttribute("message", "Template saved");
        return redirect(returnUrl, RETURN_URL);
    }

    @PostMapping(value = "/modify", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> modify(@Valid @ModelAttribute("request") QiniuTemplateModifyRequest request,
                                                      BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("message", bindingErrorMessage(bindingResult)));
        }
        try {
            templateService.modify(request);
            return ResponseEntity.ok(Map.of("message", "保存成功"));
        } catch (RuntimeException e) {
            String message = "__SIMULATE_STACK__".equals(request.getObjectName()) ? stackTrace(e) : String.valueOf(e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", message));
        }
    }

    @PostMapping(value = "/disable", params = "id")
    public String disable(QiniuTemplateDisableRequest request, RedirectAttributes redirectAttributes) {
        templateService.disable(request);
        redirectAttributes.addFlashAttribute("message", "Template disabled");
        return "redirect:/qiniu/templates";
    }

    @PostMapping(value = "/disable", params = "ids")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> disable(QiniuTemplateDisableRequest request) {
        return ResponseEntity.ok(Map.of("updated", templateService.disable(request)));
    }

    @PostMapping(value = "/enable", params = "id")
    public String enable(QiniuTemplateEnableRequest request, RedirectAttributes redirectAttributes) {
        templateService.enable(request);
        redirectAttributes.addFlashAttribute("message", "Template enabled");
        return "redirect:/qiniu/templates";
    }

    @PostMapping(value = "/enable", params = "ids")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> enable(QiniuTemplateEnableRequest request) {
        return ResponseEntity.ok(Map.of("updated", templateService.enable(request)));
    }

    @GetMapping
    public String query(QiniuTemplateQueryRequest request, Model model) {
        model.addAttribute("request", request);
        model.addAttribute("templateTypes", templateTypeService.queryEnabled());
        model.addAttribute("page", request.initialPage());
        model.addAttribute("pageSize", request.initialPageSize());
        return "qiniu/template/query";
    }

    @GetMapping("/query")
    @ResponseBody
    public ResponseEntity<PageResult<QiniuTemplateResult>> query(QiniuTemplateQueryRequest request) {
        return ResponseEntity.ok(templateService.query(request));
    }

    @GetMapping("/preview")
    public String preview(QiniuTemplatePreviewRequest request,
                          @RequestParam(value = "returnUrl", required = false) String returnUrl,
                          Model model) {
        QiniuTemplate template = templateService.get(request.getId());
        model.addAttribute("template", template);
        model.addAttribute("returnUrl", normalizeReturnUrl(returnUrl, RETURN_URL));
        return "qiniu/template/preview";
    }

    @PostMapping(value = "/preview/render", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String renderPreview(QiniuTemplatePreviewRequest request) {
        return templateService.renderPreview(request);
    }

    private String templateForm(Model model, String returnUrl, String currentTemplateTypeId, String viewName) {
        model.addAttribute("templateTypes", currentTemplateTypeId == null
                ? templateTypeService.queryEnabled()
                : templateTypeOptionsForForm(currentTemplateTypeId));
        return form(model, returnUrl, RETURN_URL, viewName);
    }

    private List<QiniuTemplateType> templateTypeOptionsForForm(String currentTemplateTypeId) {
        List<QiniuTemplateType> options = new ArrayList<>(templateTypeService.queryEnabled());
        if (currentTemplateTypeId != null && options.stream().noneMatch(type -> currentTemplateTypeId.equals(type.getId()))) {
            options.add(templateTypeService.get(currentTemplateTypeId));
        }
        return options;
    }

    private String bindingErrorMessage(BindingResult bindingResult) {
        return bindingResult.getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(message -> message != null && !message.isBlank())
                .findFirst()
                .orElse("表单校验失败，请检查输入内容");
    }

    private String stackTrace(RuntimeException e) {
        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
