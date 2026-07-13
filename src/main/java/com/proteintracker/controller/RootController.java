package com.proteintracker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Controller
public class RootController {

    @GetMapping("/")
    public String root() {
        return "redirect:/index.html";
    }
}

@RestController
class ApiInfoController {
    
    @GetMapping("/api")
    public Map<String, String> api() {
        Map<String, String> info = new HashMap<>();
        info.put("message", "Protein Tracker API v0.1.0");
        info.put("documentation", "Visit / for the web interface");
        return info;
    }
}
