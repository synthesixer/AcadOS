package com.project.acados.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Basic web controller serving the home / welcome page.
 */
@Controller
public class HomeWebController {

    @GetMapping("/")
    public String index() {
        return "index";
    }
}

