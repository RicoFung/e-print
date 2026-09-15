package com.eprint.admin.module.minio.template.controller;

import com.eprint.admin.common.controller.BaseController;
import com.eprint.admin.common.model.page.PageResult;
import com.eprint.admin.module.minio.template.model.request.MinioTemplateTypeCreateRequest;
import com.eprint.admin.module.minio.template.model.request.MinioTemplateTypeDisableRequest;
import com.eprint.admin.module.minio.template.model.request.MinioTemplateTypeEnableRequest;
import com.eprint.admin.module.minio.template.model.request.MinioTemplateTypeModifyRequest;
import com.eprint.admin.module.minio.template.model.request.MinioTemplateTypeQueryRequest;
import com.eprint.admin.module.minio.template.model.request.MinioTemplateTypeRemoveRequest;
import com.eprint.admin.module.minio.template.service.MinioTemplateTypeService;
import com.eprint.admin.repository.minio.model.entity.MinioTemplateType;
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

import java.util.Map;

@Controller
@RequestMapping("/minio/template-types")
public class MinioTemplateTypeController extends BaseController {

    private static final String RETURN_URL = "/minio/template-types";

    private final MinioTemplateTypeService templateTypeService;

    public MinioTemplateTypeController(MinioTemplateTypeService templateTypeService) {
        this.templateTypeService = templateTypeService;
    }

    @GetMapping("/create")
    public String create(@RequestParam(value = "returnUrl", required = false) String returnUrl, Model model) {
        model.addAttribute("request", templateTypeService.createRequest());
        return form(model, returnUrl, RETURN_URL, "minio/template-type/create");
    }

    @PostMapping(value = "/create", produces = MediaType.TEXT_HTML_VALUE)
    public String create(@Valid @ModelAttribute("request") MinioTemplateTypeCreateRequest request,
                         BindingResult bindingResult,
                         @RequestParam(value = "returnUrl", required = false) String returnUrl,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return form(model, returnUrl, RETURN_URL, "minio/template-type/create");
        }
        try {
            templateTypeService.create(request);
        } catch (RuntimeException e) {
            bindingResult.reject("template.type.save.failed", e.getMessage());
            return form(model, returnUrl, RETURN_URL, "minio/template-type/create");
        }
        redirectAttributes.addFlashAttribute("message", "Template type created");
        return redirect(returnUrl, RETURN_URL);
    }

    @PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> create(@Valid @ModelAttribute("request") MinioTemplateTypeCreateRequest request,
                                                      BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("message", bindingErrorMessage(bindingResult)));
        }
        try {
            templateTypeService.create(request);
            return ResponseEntity.ok(Map.of("message", "保存成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", String.valueOf(e.getMessage())));
        }
    }

    @PostMapping(value = "/remove", params = "id")
    public String remove(MinioTemplateTypeRemoveRequest request, RedirectAttributes redirectAttributes) {
        try {
            templateTypeService.remove(request);
            redirectAttributes.addFlashAttribute("message", "Template type deleted");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/minio/template-types";
    }

    @PostMapping(value = "/remove", params = "ids")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> remove(MinioTemplateTypeRemoveRequest request) {
        try {
            return ResponseEntity.ok(Map.of("removed", templateTypeService.remove(request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", String.valueOf(e.getMessage())));
        }
    }

    @GetMapping("/modify")
    public String modify(MinioTemplateTypeModifyRequest request,
                         @RequestParam(value = "returnUrl", required = false) String returnUrl,
                         Model model) {
        model.addAttribute("request", templateTypeService.getModifyRequest(request));
        return form(model, returnUrl, RETURN_URL, "minio/template-type/modify");
    }

    @PostMapping(value = "/modify", produces = MediaType.TEXT_HTML_VALUE)
    public String modify(@Valid @ModelAttribute("request") MinioTemplateTypeModifyRequest request,
                         BindingResult bindingResult,
                         @RequestParam(value = "returnUrl", required = false) String returnUrl,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return form(model, returnUrl, RETURN_URL, "minio/template-type/modify");
        }
        try {
            templateTypeService.modify(request);
        } catch (RuntimeException e) {
            bindingResult.reject("template.type.save.failed", e.getMessage());
            return form(model, returnUrl, RETURN_URL, "minio/template-type/modify");
        }
        redirectAttributes.addFlashAttribute("message", "Template type saved");
        return redirect(returnUrl, RETURN_URL);
    }

    @PostMapping(value = "/modify", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> modify(@Valid @ModelAttribute("request") MinioTemplateTypeModifyRequest request,
                                                      BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("message", bindingErrorMessage(bindingResult)));
        }
        try {
            templateTypeService.modify(request);
            return ResponseEntity.ok(Map.of("message", "保存成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", String.valueOf(e.getMessage())));
        }
    }

    @PostMapping(value = "/disable", params = "id")
    public String disable(MinioTemplateTypeDisableRequest request, RedirectAttributes redirectAttributes) {
        templateTypeService.disable(request);
        redirectAttributes.addFlashAttribute("message", "Template type disabled");
        return "redirect:/minio/template-types";
    }

    @PostMapping(value = "/disable", params = "ids")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> disable(MinioTemplateTypeDisableRequest request) {
        return ResponseEntity.ok(Map.of("updated", templateTypeService.disable(request)));
    }

    @PostMapping(value = "/enable", params = "id")
    public String enable(MinioTemplateTypeEnableRequest request, RedirectAttributes redirectAttributes) {
        templateTypeService.enable(request);
        redirectAttributes.addFlashAttribute("message", "Template type enabled");
        return "redirect:/minio/template-types";
    }

    @PostMapping(value = "/enable", params = "ids")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> enable(MinioTemplateTypeEnableRequest request) {
        return ResponseEntity.ok(Map.of("updated", templateTypeService.enable(request)));
    }

    @GetMapping
    public String query(MinioTemplateTypeQueryRequest request, Model model) {
        model.addAttribute("request", request);
        model.addAttribute("page", request.initialPage());
        model.addAttribute("pageSize", request.initialPageSize());
        return "minio/template-type/query";
    }

    @GetMapping("/query")
    @ResponseBody
    public ResponseEntity<PageResult<MinioTemplateType>> query(MinioTemplateTypeQueryRequest request) {
        return ResponseEntity.ok(templateTypeService.query(request));
    }

    private String bindingErrorMessage(BindingResult bindingResult) {
        return bindingResult.getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(message -> message != null && !message.isBlank())
                .findFirst()
                .orElse("表单校验失败，请检查输入内容");
    }

}
