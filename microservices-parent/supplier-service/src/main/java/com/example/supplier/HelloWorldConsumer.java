package com.example.supplier;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class HelloWorldConsumer {

    private static final Logger log = LoggerFactory.getLogger(HelloWorldConsumer.class);

    private final Tracer tracer;

    public HelloWorldConsumer(Tracer tracer) {
        this.tracer = tracer;
    }

    @KafkaListener(topics = "hello-world", groupId = "supplier-service")
    public void consume(ConsumerRecord<String, String> record) {
        String correlationId = currentTraceId();
        log.info("[KAFKA] topic={} partition={} offset={} correlationId={} message={}",
                record.topic(), record.partition(), record.offset(), correlationId, record.value());
    }

    private String currentTraceId() {
        Span span = tracer.currentSpan();
        return span != null ? span.context().traceId() : "N/A";
    }
}
