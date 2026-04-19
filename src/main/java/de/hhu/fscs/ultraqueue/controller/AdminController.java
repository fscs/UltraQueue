package de.hhu.fscs.ultraqueue.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @GetMapping(value = "/", produces = MediaType.TEXT_PLAIN_VALUE)
    public String login() {
        return "logged in";
    }
}
