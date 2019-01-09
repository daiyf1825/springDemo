package com.xz.demo;

import com.xz.demo.util.Util;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloWorldController {

    @RequestMapping("/hello")
    public String index(){
        return "Hello World";
    }

    @GetMapping("/download")
    public Boolean download(@RequestParam String filePath) {
        String ext = filePath.substring(filePath.lastIndexOf("/") + 1);
        String saveFile = System.getProperty("user.home") + "/Documents/" + ext;
        return Util.downLoad(filePath, saveFile);
    }
}
