package com.eprint.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class EPrintServerApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(EPrintServerApplication.class, args);
    }
}
