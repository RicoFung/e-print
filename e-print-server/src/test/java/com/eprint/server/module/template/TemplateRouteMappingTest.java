package com.eprint.server.module.template;

import com.eprint.server.module.minio.template.controller.MinioTemplateController;
import com.eprint.server.module.qiniu.template.controller.QiniuTemplateController;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RequestMapping;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class TemplateRouteMappingTest {

    @Test
    void providerControllersUseSeparatedRoutes() {
        assertRoute(MinioTemplateController.class, "/minio/template");
        assertRoute(QiniuTemplateController.class, "/qiniu/template");
    }

    private void assertRoute(Class<?> controllerType, String route) {
        RequestMapping mapping = controllerType.getAnnotation(RequestMapping.class);
        assertArrayEquals(new String[]{route}, mapping.value());
    }
}
