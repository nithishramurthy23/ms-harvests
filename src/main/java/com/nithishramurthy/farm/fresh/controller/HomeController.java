package com.nithishramurthy.farm.fresh.controller;

import org.springframework.web.bind.annotation.*;

/** Simple status endpoint to check that the backend is up. */
@RestController
public class HomeController {

    @GetMapping({"/", "/api/status"})
    public String home() {
        return "Farm Fresh backend is running!";
    }
}


