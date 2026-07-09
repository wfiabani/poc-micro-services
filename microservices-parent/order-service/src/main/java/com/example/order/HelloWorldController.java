package com.example.order;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/hello-world")
public class HelloWorldController {

    private final HelloWorldProducer helloWorldProducer;

    public HelloWorldController(HelloWorldProducer helloWorldProducer) {
        this.helloWorldProducer = helloWorldProducer;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> publish() {
        String message = helloWorldProducer.publish();
        return ResponseEntity.ok(Map.of(
                "topic", "hello-world",
                "message", message
        ));
    }
}
