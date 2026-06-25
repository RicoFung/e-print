package com.eprint.admin.module.template.controller;

import com.eprint.admin.module.template.model.PageResult;
import com.eprint.admin.module.template.model.request.TemplateTypeCreateRequest;
import com.eprint.admin.module.template.model.request.TemplateTypeDisableRequest;
import com.eprint.admin.module.template.model.request.TemplateTypeEnableRequest;
import com.eprint.admin.module.template.model.request.TemplateTypeModifyRequest;
import com.eprint.admin.module.template.model.request.TemplateTypeQueryRequest;
import com.eprint.admin.module.template.model.request.TemplateTypeRemoveRequest;
import com.eprint.admin.module.template.service.TemplateTypeService;
import com.eprint.admin.repository.model.entity.TemplateType;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/template-types")
public class TemplateTypeController {

    private final TemplateTypeService templateTypeService;

    public TemplateTypeController(TemplateTypeService templateTypeService) {
        this.templateTypeService = templateTypeService;
    }

    @GetMapping
    public String query(TemplateTypeQueryRequest request, Model model) {
        model.addAttribute("request", request);
        model.addAttribute("page", request.initialPage());
        model.addAttribute("pageSize", request.initialPageSize());
        return "template-type/query";
    }

    @GetMapping("/query")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> query(TemplateTypeQueryRequest request) {
        PageResult<TemplateType> pageResult = templateTypeService.query(request);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("total", pageResult.getTotal());
        body.put("rows", pageResult.getRecords());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/create")
    public String create(Model model) {
        model.addAttribute("request", templateTypeService.createRequest());
        return "template-type/create";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("request") TemplateTypeCreateRequest request,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "template-type/create";
        }
        try {
            templateTypeService.create(request);
        } catch (RuntimeException e) {
            bindingResult.reject("template.type.save.failed", e.getMessage());
            return "template-type/create";
        }
        redirectAttributes.addFlashAttribute("message", "Template type created");
        return "redirect:/admin/template-types";
    }

    @GetMapping("/modify")
    public String modify(TemplateTypeModifyRequest request, Model model) {
        model.addAttribute("request", templateTypeService.getModifyRequest(request));
        return "template-type/modify";
    }

    @PostMapping("/modify")
    public String modify(@Valid @ModelAttribute("request") TemplateTypeModifyRequest request,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "template-type/modify";
        }
        try {
            templateTypeService.modify(request);
        } catch (RuntimeException e) {
            bindingResult.reject("template.type.save.failed", e.getMessage());
            return "template-type/modify";
        }
        redirectAttributes.addFlashAttribute("message", "Template type saved");
        return "redirect:/admin/template-types";
    }

    @PostMapping(value = "/enable", params = "id")
    public String enable(TemplateTypeEnableRequest request, RedirectAttributes redirectAttributes) {
        templateTypeService.enable(request.getId());
        redirectAttributes.addFlashAttribute("message", "Template type enabled");
        return "redirect:/admin/template-types";
    }

    @PostMapping(value = "/disable", params = "id")
    public String disable(TemplateTypeDisableRequest request, RedirectAttributes redirectAttributes) {
        templateTypeService.disable(request.getId());
        redirectAttributes.addFlashAttribute("message", "Template type disabled");
        return "redirect:/admin/template-types";
    }

    @PostMapping(value = "/remove", params = "id")
    public String remove(TemplateTypeRemoveRequest request, RedirectAttributes redirectAttributes) {
        try {
            templateTypeService.remove(request.getId());
            redirectAttributes.addFlashAttribute("message", "Template type deleted");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/template-types";
    }

    @PostMapping(value = "/enable", params = "ids")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> enableSelected(TemplateTypeEnableRequest request) {
        int updated = templateTypeService.enable(request.getIds());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("updated", updated);
        return ResponseEntity.ok(body);
    }

    @PostMapping(value = "/disable", params = "ids")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> disableSelected(TemplateTypeDisableRequest request) {
        int updated = templateTypeService.disable(request.getIds());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("updated", updated);
        return ResponseEntity.ok(body);
    }

    @PostMapping(value = "/remove", params = "ids")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> removeSelected(TemplateTypeRemoveRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        try {
            int removed = templateTypeService.remove(request.getIds());
            body.put("removed", removed);
            return ResponseEntity.ok(body);
        } catch (RuntimeException e) {
            body.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(body);
        }
    }
}
