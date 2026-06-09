package com.eprint.admin.module.template.controller;

import com.eprint.admin.module.template.model.PageResult;
import com.eprint.admin.module.template.model.TemplateForm;
import com.eprint.admin.module.template.model.TemplateType;
import com.eprint.admin.module.template.service.TemplateAdminService;
import com.eprint.admin.repository.model.entity.Template;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/templates")
public class TemplateAdminController {

    private final TemplateAdminService templateAdminService;

    public TemplateAdminController(TemplateAdminService templateAdminService) {
        this.templateAdminService = templateAdminService;
    }

    @GetMapping
    public String list(@RequestParam(value = "templateType", required = false) String templateType,
                       @RequestParam(value = "templateCode", required = false) String templateCode,
                       @RequestParam(value = "status", required = false) Integer status,
                       @RequestParam(value = "page", required = false) Integer page,
                       @RequestParam(value = "pageSize", required = false) Integer pageSize,
                       Model model) {
        model.addAttribute("templateTypes", TemplateType.options());
        model.addAttribute("templateType", templateType);
        model.addAttribute("templateCode", templateCode);
        model.addAttribute("status", status);
        model.addAttribute("pageSize", pageSize == null ? 10 : pageSize);
        return "template/list";
    }

    @GetMapping("/data")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> data(@RequestParam(value = "templateType", required = false) String templateType,
                                                    @RequestParam(value = "templateCode", required = false) String templateCode,
                                                    @RequestParam(value = "status", required = false) Integer status,
                                                    @RequestParam(value = "search", required = false) String search,
                                                    @RequestParam(value = "offset", required = false, defaultValue = "0") Integer offset,
                                                    @RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit) {
        String queryTemplateCode = templateCode == null || templateCode.isBlank() ? search : templateCode;
        int queryLimit = limit == null || limit < 1 ? 10 : limit;
        int page = offset == null || offset < 0 ? 1 : (offset / queryLimit) + 1;
        PageResult<Template> pageResult = templateAdminService.page(templateType, queryTemplateCode, status, page, queryLimit);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("total", pageResult.getTotal());
        body.put("rows", pageResult.getRecords());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("mode", "create");
        model.addAttribute("templateTypes", TemplateType.options());
        model.addAttribute("templateForm", templateAdminService.createForm());
        return "template/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("templateForm") TemplateForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "create");
            model.addAttribute("templateTypes", TemplateType.options());
            return "template/form";
        }
        try {
            templateAdminService.create(form);
        } catch (RuntimeException e) {
            bindingResult.reject("template.save.failed", e.getMessage());
            model.addAttribute("mode", "create");
            model.addAttribute("templateTypes", TemplateType.options());
            return "template/form";
        }
        redirectAttributes.addFlashAttribute("message", "Template created");
        return "redirect:/admin/templates";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") String id, Model model) {
        model.addAttribute("mode", "edit");
        model.addAttribute("templateTypes", TemplateType.options());
        model.addAttribute("templateForm", templateAdminService.getForm(id));
        return "template/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable("id") String id,
                         @Valid @ModelAttribute("templateForm") TemplateForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "edit");
            model.addAttribute("templateTypes", TemplateType.options());
            return "template/form";
        }
        try {
            templateAdminService.update(id, form);
        } catch (RuntimeException e) {
            bindingResult.reject("template.save.failed", e.getMessage());
            model.addAttribute("mode", "edit");
            model.addAttribute("templateTypes", TemplateType.options());
            return "template/form";
        }
        redirectAttributes.addFlashAttribute("message", "Template saved");
        return "redirect:/admin/templates";
    }

    @PostMapping("/{id}/disable")
    public String disable(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        templateAdminService.disable(id);
        redirectAttributes.addFlashAttribute("message", "Template disabled");
        return "redirect:/admin/templates";
    }

    @PostMapping("/{id}/enable")
    public String enable(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        templateAdminService.enable(id);
        redirectAttributes.addFlashAttribute("message", "Template enabled");
        return "redirect:/admin/templates";
    }

    @PostMapping("/disable")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> disableSelected(@RequestParam("ids") List<String> ids) {
        int updated = templateAdminService.disable(ids);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("updated", updated);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/enable")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> enableSelected(@RequestParam("ids") List<String> ids) {
        int updated = templateAdminService.enable(ids);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("updated", updated);
        return ResponseEntity.ok(body);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable("id") String id, RedirectAttributes redirectAttributes) {
        templateAdminService.delete(id);
        redirectAttributes.addFlashAttribute("message", "Template deleted");
        return "redirect:/admin/templates";
    }

    @PostMapping("/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteSelected(@RequestParam("ids") List<String> ids) {
        int deleted = templateAdminService.delete(ids);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("deleted", deleted);
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{id}/preview")
    public String preview(@PathVariable("id") String id, Model model) {
        Template template = templateAdminService.getRequiredTemplate(id);
        model.addAttribute("template", template);
        model.addAttribute("sampleData", templateAdminService.defaultSampleData());
        return "template/preview";
    }

    @GetMapping(value = "/{id}/preview/content", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String previewContent(@PathVariable("id") String id) {
        return templateAdminService.getPreviewContent(id);
    }

    @PostMapping(value = "/{id}/preview/render", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String renderPreview(@PathVariable("id") String id,
                                @RequestParam("sampleData") String sampleData) {
        return templateAdminService.renderPreviewContent(id, sampleData);
    }
}
