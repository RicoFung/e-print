package com.eprint.admin.module.qiniu.template.controller;

import com.eprint.admin.common.controller.BaseController;
import com.eprint.admin.common.model.page.PageResult;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateTypeCreateRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateTypeDisableRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateTypeEnableRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateTypeModifyRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateTypeQueryRequest;
import com.eprint.admin.module.qiniu.template.model.request.QiniuTemplateTypeRemoveRequest;
import com.eprint.admin.module.qiniu.template.service.QiniuTemplateTypeService;
import com.eprint.admin.repository.qiniu.model.entity.QiniuTemplateType;
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
@RequestMapping("/qiniu/template-types")
public class QiniuTemplateTypeController extends BaseController {

    private static final String RETURN_URL = "/qiniu/template-types";

    private final QiniuTemplateTypeService templateTypeService;

    public QiniuTemplateTypeController(QiniuTemplateTypeService templateTypeService) {
        this.templateTypeService = templateTypeService;
    }

    @GetMapping("/create")
    public String create(@RequestParam(value = "returnUrl", required = false) String returnUrl, Model model) {
        model.addAttribute("request", templateTypeService.createRequest());
        return form(model, returnUrl, RETURN_URL, "qiniu/template-type/create");
    }

    @PostMapping(value = "/create", produces = MediaType.TEXT_HTML_VALUE)
    public String create(@Valid @ModelAttribute("request") QiniuTemplateTypeCreateRequest request,
                         BindingResult bindingResult,
                         @RequestParam(value = "returnUrl", required = false) String returnUrl,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return form(model, returnUrl, RETURN_URL, "qiniu/template-type/create");
        }
        try {
            templateTypeService.create(request);
        } catch (RuntimeException e) {
            bindingResult.reject("template.type.save.failed", e.getMessage());
            return form(model, returnUrl, RETURN_URL, "qiniu/template-type/create");
        }
        redirectAttributes.addFlashAttribute("message", "Template type created");
        return redirect(returnUrl, RETURN_URL);
    }

    @PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> create(@Valid @ModelAttribute("request") QiniuTemplateTypeCreateRequest request,
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
    public String remove(QiniuTemplateTypeRemoveRequest request, RedirectAttributes redirectAttributes) {
        try {
            templateTypeService.remove(request);
            redirectAttributes.addFlashAttribute("message", "Template type deleted");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/qiniu/template-types";
    }

    @PostMapping(value = "/remove", params = "ids")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> remove(QiniuTemplateTypeRemoveRequest request) {
        try {
            return ResponseEntity.ok(Map.of("removed", templateTypeService.remove(request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", String.valueOf(e.getMessage())));
        }
    }

    @GetMapping("/modify")
    public String modify(QiniuTemplateTypeModifyRequest request,
                         @RequestParam(value = "returnUrl", required = false) String returnUrl,
                         Model model) {
        model.addAttribute("request", templateTypeService.getModifyRequest(request));
        return form(model, returnUrl, RETURN_URL, "qiniu/template-type/modify");
    }

    @PostMapping(value = "/modify", produces = MediaType.TEXT_HTML_VALUE)
    public String modify(@Valid @ModelAttribute("request") QiniuTemplateTypeModifyRequest request,
                         BindingResult bindingResult,
                         @RequestParam(value = "returnUrl", required = false) String returnUrl,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return form(model, returnUrl, RETURN_URL, "qiniu/template-type/modify");
        }
        try {
            templateTypeService.modify(request);
        } catch (RuntimeException e) {
            bindingResult.reject("template.type.save.failed", e.getMessage());
            return form(model, returnUrl, RETURN_URL, "qiniu/template-type/modify");
        }
        redirectAttributes.addFlashAttribute("message", "Template type saved");
        return redirect(returnUrl, RETURN_URL);
    }

    @PostMapping(value = "/modify", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> modify(@Valid @ModelAttribute("request") QiniuTemplateTypeModifyRequest request,
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
    public String disable(QiniuTemplateTypeDisableRequest request, RedirectAttributes redirectAttributes) {
        templateTypeService.disable(request);
        redirectAttributes.addFlashAttribute("message", "Template type disabled");
        return "redirect:/qiniu/template-types";
    }

    @PostMapping(value = "/disable", params = "ids")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> disable(QiniuTemplateTypeDisableRequest request) {
        return ResponseEntity.ok(Map.of("updated", templateTypeService.disable(request)));
    }

    @PostMapping(value = "/enable", params = "id")
    public String enable(QiniuTemplateTypeEnableRequest request, RedirectAttributes redirectAttributes) {
        templateTypeService.enable(request);
        redirectAttributes.addFlashAttribute("message", "Template type enabled");
        return "redirect:/qiniu/template-types";
    }

    @PostMapping(value = "/enable", params = "ids")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> enable(QiniuTemplateTypeEnableRequest request) {
        return ResponseEntity.ok(Map.of("updated", templateTypeService.enable(request)));
    }

    @GetMapping
    public String query(QiniuTemplateTypeQueryRequest request, Model model) {
        model.addAttribute("request", request);
        model.addAttribute("page", request.initialPage());
        model.addAttribute("pageSize", request.initialPageSize());
        return "qiniu/template-type/query";
    }

    @GetMapping("/query")
    @ResponseBody
    public ResponseEntity<PageResult<QiniuTemplateType>> query(QiniuTemplateTypeQueryRequest request) {
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
