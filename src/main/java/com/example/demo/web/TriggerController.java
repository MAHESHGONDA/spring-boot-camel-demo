package com.example.demo.web;

import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/triggers")
public class TriggerController {
    private final ProducerTemplate producerTemplate;
    private final String commonRoute;

    public TriggerController(ProducerTemplate producerTemplate, 
                             @Value("${common.trigger-route}") String commonRoute) {
        this.producerTemplate = producerTemplate;
        this.commonRoute = commonRoute;
    }
    
    @GetMapping("/user-extraction")
    public ResponseEntity userInfoExtraction() {
        producerTemplate.asyncRequestBody(commonRoute, null);
        return ResponseEntity.accepted().build();
    }

}
