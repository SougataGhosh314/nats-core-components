package com.sougata.natscore.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class NatsMetricsRecorder {
    @Value("${nats.metrics.enabled:true}")
    private boolean enabled;
    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> counterCache = new ConcurrentHashMap<>();

    public NatsMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    private String counterKey(String name, String topic) {
        return name + ":" + topic;
    }

    private Counter getOrCreateCounter(String name, String topic) {
        return counterCache.computeIfAbsent(counterKey(name, topic), key ->
            Counter.builder(name)
                   .tag("topic", topic)
                   .register(meterRegistry)
        );
    }

    public void incrementSent(String topic) {
        if (enabled) getOrCreateCounter("nats.message.sent", topic).increment();
    }

    public void incrementReceived(String topic) {
        if (enabled) getOrCreateCounter("nats.message.received", topic).increment();
    }

    public void incrementError(String topic) {
        if (enabled) getOrCreateCounter("nats.message.error", topic).increment();
    }
}