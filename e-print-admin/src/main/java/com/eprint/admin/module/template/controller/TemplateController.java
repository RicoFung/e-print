package com.eprint.admin.module.template.controller;

import com.eprint.admin.module.template.model.PageResult;
import com.eprint.admin.module.template.model.request.TemplateCreateRequest;
import com.eprint.admin.module.template.model.request.TemplateDisableRequest;
import com.eprint.admin.module.template.model.request.TemplateEnableRequest;
import com.eprint.admin.module.template.model.request.TemplateModifyRequest;
import com.eprint.admin.module.template.model.request.TemplatePreviewRequest;
import com.eprint.admin.module.template.model.request.TemplateQueryRequest;
import com.eprint.admin.module.template.model.request.TemplateRemoveRequest;
import com.eprint.admin.module.template.service.TemplateService;
import com.eprint.admin.module.template.service.TemplateTypeService;
import com.eprint.admin.repository.model.entity.Template;
import com.eprint.admin.repository.model.entity.TemplateType;
import com.eprint.admin.repository.model.result.TemplateResult;
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/templates")
public class TemplateController {

    private final TemplateService templateService;
    private final TemplateTypeService templateTypeService;

    public TemplateController(TemplateService templateService,
                              TemplateTypeService templateTypeService) {
        this.templateService = templateService;
        this.templateTypeService = templateTypeService;
    }

    @ModelAttribute("sampleData")
    public String sampleData() {
        return templateService.defaultSampleData();
    }

    @GetMapping
    public String query(TemplateQueryRequest request, Model model) {
        model.addAttribute("request", request);
        model.addAttribute("templateTypes", templateTypeService.queryEnabled());
        model.addAttribute("page", request.initialPage());
        model.addAttribute("pageSize", request.initialPageSize());
        return "template/query";
    }

    @GetMapping("/query")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> query(TemplateQueryRequest request) {
        PageResult<TemplateResult> pageResult = templateService.query(request);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("total", pageResult.getTotal());
        body.put("rows", pageResult.getRecords());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/create")
    public String create(@RequestParam(value = "returnUrl", required = false) String returnUrl, Model model) {
        model.addAttribute("request", templateService.createRequest());
        model.addAttribute("templateTypes", templateTypeService.queryEnabled());
        model.addAttribute("returnUrl", normalizeReturnUrl(returnUrl));
        return "template/create";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("request") TemplateCreateRequest request,
                         BindingResult bindingResult,
                         @RequestParam(value = "returnUrl", required = false) String returnUrl,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        String normalizedReturnUrl = normalizeReturnUrl(returnUrl);
        if (bindingResult.hasErrors()) {
            model.addAttribute("templateTypes", templateTypeService.queryEnabled());
            model.addAttribute("returnUrl", normalizedReturnUrl);
            return "template/create";
        }
        try {
            templateService.create(request);
        } catch (RuntimeException e) {
            bindingResult.reject("template.save.failed", e.getMessage());
            model.addAttribute("templateTypes", templateTypeService.queryEnabled());
            model.addAttribute("returnUrl", normalizedReturnUrl);
            return "template/create";
        }
        redirectAttributes.addFlashAttribute("message", "Template created");
        return "redirect:" + normalizedReturnUrl;
    }

    @GetMapping("/modify")
    public String modify(TemplateModifyRequest request,
                         @RequestParam(value = "returnUrl", required = false) String returnUrl,
                         Model model) {
        TemplateModifyRequest modifyRequest = templateService.getModifyRequest(request);
        model.addAttribute("request", modifyRequest);
        model.addAttribute("templateTypes", templateTypeOptionsForForm(modifyRequest.getTemplateTypeId()));
        model.addAttribute("returnUrl", normalizeReturnUrl(returnUrl));
        return "template/modify";
    }

    @PostMapping("/modify")
    public String modify(@Valid @ModelAttribute("request") TemplateModifyRequest request,
                         BindingResult bindingResult,
                         @RequestParam(value = "returnUrl", required = false) String returnUrl,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        String normalizedReturnUrl = normalizeReturnUrl(returnUrl);
        if (bindingResult.hasErrors()) {
            model.addAttribute("templateTypes", templateTypeOptionsForForm(request.getTemplateTypeId()));
            model.addAttribute("returnUrl", normalizedReturnUrl);
            return "template/modify";
        }
        try {
            templateService.modify(request);
        } catch (RuntimeException e) {
            bindingResult.reject("template.save.failed", e.getMessage());
            model.addAttribute("templateTypes", templateTypeOptionsForForm(request.getTemplateTypeId()));
            model.addAttribute("returnUrl", normalizedReturnUrl);
            return "template/modify";
        }
        redirectAttributes.addFlashAttribute("message", "Template saved");
        return "redirect:" + normalizedReturnUrl;
    }

    @PostMapping(value = "/disable", params = "id")
    public String disable(TemplateDisableRequest request, RedirectAttributes redirectAttributes) {
        templateService.disable(request.getId());
        redirectAttributes.addFlashAttribute("message", "Template disabled");
        return "redirect:/admin/templates";
    }

    @PostMapping(value = "/enable", params = "id")
    public String enable(TemplateEnableRequest request, RedirectAttributes redirectAttributes) {
        templateService.enable(request.getId());
        redirectAttributes.addFlashAttribute("message", "Template enabled");
        return "redirect:/admin/templates";
    }

    @PostMapping(value = "/remove", params = "id")
    public String remove(TemplateRemoveRequest request, RedirectAttributes redirectAttributes) {
        templateService.remove(request.getId());
        redirectAttributes.addFlashAttribute("message", "Template deleted");
        return "redirect:/admin/templates";
    }

    @PostMapping(value = "/disable", params = "ids")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> disableSelected(TemplateDisableRequest request) {
        int updated = templateService.disable(request.getIds());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("updated", updated);
        return ResponseEntity.ok(body);
    }

    @PostMapping(value = "/enable", params = "ids")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> enableSelected(TemplateEnableRequest request) {
        int updated = templateService.enable(request.getIds());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("updated", updated);
        return ResponseEntity.ok(body);
    }

    @PostMapping(value = "/remove", params = "ids")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeSelected(TemplateRemoveRequest request) {
        int removed = templateService.remove(request.getIds());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("removed", removed);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/preview")
    public String preview(TemplatePreviewRequest request,
                          @RequestParam(value = "returnUrl", required = false) String returnUrl,
                          Model model) {
        Template template = templateService.getRequiredTemplate(request.getId());
        model.addAttribute("template", template);
        model.addAttribute("returnUrl", normalizeReturnUrl(returnUrl));
        return "template/preview";
    }

    @PostMapping(value = "/preview/render", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String renderPreview(TemplatePreviewRequest request) {
        return templateService.renderPreview(request);
    }

    private String normalizeReturnUrl(String returnUrl) {
        if (returnUrl == null
                || !(returnUrl.equals("/admin/templates") || returnUrl.startsWith("/admin/templates?"))
                || returnUrl.startsWith("//")
                || returnUrl.contains("\r")
                || returnUrl.contains("\n")) {
            return "/admin/templates";
        }
        return returnUrl;
    }

    private List<TemplateType> templateTypeOptionsForForm(String currentTemplateTypeId) {
        List<TemplateType> options = new ArrayList<>(templateTypeService.queryEnabled());
        if (currentTemplateTypeId != null && options.stream().noneMatch(type -> currentTemplateTypeId.equals(type.getId()))) {
            options.add(templateTypeService.getRequired(currentTemplateTypeId));
        }
        return options;
    }
}
