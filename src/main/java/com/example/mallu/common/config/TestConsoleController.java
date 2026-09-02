package com.example.mallu.common.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Provides a stable directory entry point for the local API test console. */
@Controller
public class TestConsoleController {

    @GetMapping({"/test-console", "/test-console/"})
    public String testConsoleHome() {
        return "forward:/test-console/index.html";
    }
}