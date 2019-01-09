package com.xz.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @Value("${com.example.name}")
    private String name;

    @RequestMapping("/")
    public String hi() {
        return "hi " + name;
    }


}
