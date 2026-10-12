package com.pooja.farm.fresh.controller;

import org.springframework.web.bind.annotation.*;

@RestController
public class HomeController {

    @GetMapping({"/", "/api/status"})
    public String home() {
        return "Farm Fresh backend is running!";
    }
}


