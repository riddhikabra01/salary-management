package com.acme.salary.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Lets the Angular router handle page URLs such as /employees and /insights,
 * so refreshing the browser on those pages still loads the app.
 * Paths with a file extension (scripts, styles, icons) and everything under /api are left alone.
 */
@Controller
public class SpaController {

    @GetMapping("/{path:[^\\.]*}")
    public String forwardToApp() {
        return "forward:/index.html";
    }
}
