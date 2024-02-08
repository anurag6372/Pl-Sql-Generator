package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
public class Controller {
    @Autowired
    public OASJsonGenerator oasJsonGenerator;
    @GetMapping("")
    public Map<String,Map<UUID, Map<String, Object>>> abc(){
        return oasJsonGenerator.generateRetrieveQueryList();
    }
}
