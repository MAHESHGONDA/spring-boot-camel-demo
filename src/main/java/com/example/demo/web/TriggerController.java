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
    private final String analyticsTriggerRoute;

    public TriggerController(ProducerTemplate producerTemplate, 
                             @Value("${analytics.trigger-route}") String analyticsTriggerRoute) {
        this.producerTemplate = producerTemplate;
        this.analyticsTriggerRoute = analyticsTriggerRoute;
    }
    
    @GetMapping("/read-analytics")
    public ResponseEntity readAnalytics() {
        producerTemplate.asyncRequestBody(analyticsTriggerRoute, null);
        return ResponseEntity.accepted().build();
    }
    
}
