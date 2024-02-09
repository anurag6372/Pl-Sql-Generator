package com.example.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.UUID;

@RestController
public class Controller {
    @Autowired
    public OASJsonGenerator oasJsonGenerator;
    @GetMapping("")
    public ResponseEntity<Map<String,Object>> abc(){
        return ResponseEntity.ok(oasJsonGenerator.generateRetrieveQueryList());
    }








































//    @PostMapping("/abc")
//    public void test4(HttpServletRequest request){
//        try {
//            InputStream inputStream = request.getInputStream();
//            System.out.println(StreamUtils.copyToString(inputStream, Charset.defaultCharset()));
//
//        } catch (IOException e) {
//            System.out.println(e);
//        }
//    }

}
