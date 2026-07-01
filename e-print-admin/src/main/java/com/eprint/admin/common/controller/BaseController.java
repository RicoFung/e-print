package com.eprint.admin.common.controller;

import org.springframework.ui.Model;

public abstract class BaseController {

    protected String form(Model model,
                          String returnUrl,
                          String defaultReturnUrl,
                          String viewName) {
        model.addAttribute("returnUrl", normalizeReturnUrl(returnUrl, defaultReturnUrl));
        return viewName;
    }

    protected String redirect(String returnUrl, String defaultReturnUrl) {
        return "redirect:" + normalizeReturnUrl(returnUrl, defaultReturnUrl);
    }

    protected String normalizeReturnUrl(String returnUrl, String defaultReturnUrl) {
        if (returnUrl == null
                || !(returnUrl.equals(defaultReturnUrl) || returnUrl.startsWith(defaultReturnUrl + "?"))
                || returnUrl.startsWith("//")
                || returnUrl.contains("\r")
                || returnUrl.contains("\n")) {
            return defaultReturnUrl;
        }
        return returnUrl;
    }
}
