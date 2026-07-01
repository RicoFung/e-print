package com.eprint.admin.module.template.controller;

import com.eprint.admin.common.controller.BaseController;
import com.eprint.admin.common.model.page.PageResult;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/admin/template-types")
public class TemplateTypeController extends BaseController {

    private static final String RETURN_URL = "/admin/template-types";

    private final TemplateTypeService templateTypeService;

    public TemplateTypeController(TemplateTypeService templateTypeService) {
        this.templateTypeService = templateTypeService;
    }

    @GetMapping("/create")
    public String create(@RequestParam(value = "returnUrl", required = false) String returnUrl, Model model) {
        model.addAttribute("request", templateTypeService.createRequest());
        return form(model, returnUrl, RETURN_URL, "template-type/create");
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("request") TemplateTypeCreateRequest request,
                         BindingResult bindingResult,
                         @RequestParam(value = "returnUrl", required = false) String returnUrl,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return form(model, returnUrl, RETURN_URL, "template-type/create");
        }
        try {
            templateTypeService.create(request);
        } catch (RuntimeException e) {
            bindingResult.reject("template.type.save.failed", e.getMessage());
            return form(model, returnUrl, RETURN_URL, "template-type/create");
        }
        redirectAttributes.addFlashAttribute("message", "Template type created");
        return redirect(returnUrl, RETURN_URL);
    }

    @PostMapping(value = "/remove", params = "id")
    public String remove(TemplateTypeRemoveRequest request, RedirectAttributes redirectAttributes) {
        try {
            templateTypeService.remove(request);
            redirectAttributes.addFlashAttribute("message", "Template type deleted");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/template-types";
    }

    @PostMapping(value = "/remove", params = "ids")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> remove(TemplateTypeRemoveRequest request) {
        try {
            return ResponseEntity.ok(Map.of("removed", templateTypeService.remove(request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", String.valueOf(e.getMessage())));
        }
    }

    @GetMapping("/modify")
    public String modify(TemplateTypeModifyRequest request,
                         @RequestParam(value = "returnUrl", required = false) String returnUrl,
                         Model model) {
        model.addAttribute("request", templateTypeService.getModifyRequest(request));
        return form(model, returnUrl, RETURN_URL, "template-type/modify");
    }

    @PostMapping("/modify")
    public String modify(@Valid @ModelAttribute("request") TemplateTypeModifyRequest request,
                         BindingResult bindingResult,
                         @RequestParam(value = "returnUrl", required = false) String returnUrl,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return form(model, returnUrl, RETURN_URL, "template-type/modify");
        }
        try {
            templateTypeService.modify(request);
        } catch (RuntimeException e) {
            bindingResult.reject("template.type.save.failed", e.getMessage());
            return form(model, returnUrl, RETURN_URL, "template-type/modify");
        }
        redirectAttributes.addFlashAttribute("message", "Template type saved");
        return redirect(returnUrl, RETURN_URL);
    }

    @PostMapping(value = "/disable", params = "id")
    public String disable(TemplateTypeDisableRequest request, RedirectAttributes redirectAttributes) {
        templateTypeService.disable(request);
        redirectAttributes.addFlashAttribute("message", "Template type disabled");
        return "redirect:/admin/template-types";
    }

    @PostMapping(value = "/disable", params = "ids")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> disable(TemplateTypeDisableRequest request) {
        return ResponseEntity.ok(Map.of("updated", templateTypeService.disable(request)));
    }

    @PostMapping(value = "/enable", params = "id")
    public String enable(TemplateTypeEnableRequest request, RedirectAttributes redirectAttributes) {
        templateTypeService.enable(request);
        redirectAttributes.addFlashAttribute("message", "Template type enabled");
        return "redirect:/admin/template-types";
    }

    @PostMapping(value = "/enable", params = "ids")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> enable(TemplateTypeEnableRequest request) {
        return ResponseEntity.ok(Map.of("updated", templateTypeService.enable(request)));
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
    public ResponseEntity<PageResult<TemplateType>> query(TemplateTypeQueryRequest request) {
        return ResponseEntity.ok(templateTypeService.query(request));
    }

}
