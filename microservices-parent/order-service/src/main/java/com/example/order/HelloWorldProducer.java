package com.example.order;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class HelloWorldProducer {

    private static final Logger log = LoggerFactory.getLogger(HelloWorldProducer.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final List<String> SARCASTIC_TEMPLATES = List.of(
            "Ah, mais um dia espetacular às %s... mal posso conter minha empolgação.",
            "Parabéns, você descobriu como chamar um endpoint. São %s, aproveite o momento histórico.",
            "Olá! São %s e, surpreendentemente, ainda estamos publicando mensagens no Kafka.",
            "Que notícia incrível: são %s e nada de extraordinário aconteceu.",
            "Saudações. São %s. Tente conter sua animação."
    );

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Tracer tracer;

    public HelloWorldProducer(KafkaTemplate<String, String> kafkaTemplate, Tracer tracer) {
        this.kafkaTemplate = kafkaTemplate;
        this.tracer = tracer;
    }

    public String publish() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String template = SARCASTIC_TEMPLATES.get(ThreadLocalRandom.current().nextInt(SARCASTIC_TEMPLATES.size()));
        String message = String.format(template, timestamp);

        kafkaTemplate.send("hello-world", message);

        String correlationId = currentTraceId();
        log.info("[KAFKA] topic=hello-world correlationId={} message={}", correlationId, message);

        return message;
    }

    private String currentTraceId() {
        Span span = tracer.currentSpan();
        return span != null ? span.context().traceId() : "N/A";
    }
}
