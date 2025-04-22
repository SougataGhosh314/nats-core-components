package com.sougata.natscore.dispatcher;

import com.sougata.natscore.enums.MDCLoggingEnum;
import com.sougata.natscore.model.PayloadHeader;
import com.sougata.natscore.model.PayloadWrapper;
import com.sougata.natscore.monitoring.NatsMetricsRecorder;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;
import io.nats.client.impl.Headers;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sougata.natscore.util.NatsUtil.headersToMap;

@Slf4j
public abstract class AbstractDispatcher {
    protected final List<Dispatcher> dispatchers = new ArrayList<>();
    protected final Connection connection;
    protected final NatsMetricsRecorder metricsRecorder;
    protected Map<String, String> writeTopicMap = new HashMap<>();

    protected AbstractDispatcher(Connection connection, NatsMetricsRecorder metricsRecorder) {
        this.connection = connection;
        this.metricsRecorder = metricsRecorder;
    }

    protected PayloadWrapper<byte[]> extractAndLogIncomingMessage(String messageType, String topicName, Message msg) {
        String correlationId = msg.getHeaders().getFirst(PayloadHeader.CORRELATION_ID.getKey());

        MDC.put(MDCLoggingEnum.CORRELATION_ID.getLoggingKey(), correlationId);
        logIncomingMessage(topicName, msg.getHeaders());
        metricsRecorder.incrementReceived(topicName);

        return PayloadWrapper.<byte[]>newBuilder()
                .setPayload(msg.getData())
                .setPayloadType(messageType)
                .setCorrelationId(correlationId)
                .setCreationTimestamp(msg.getHeaders().getFirst(PayloadHeader.CREATION_TS.getKey()))
                .build();
    }


    protected void publish(PayloadWrapper<byte[]> payload) {
        try {
            String payloadType = payload.getHeader(PayloadHeader.PAYLOAD_TYPE);
            String topic = writeTopicMap.get(payloadType);

            if (topic != null) {
                MDC.put(MDCLoggingEnum.CORRELATION_ID.getLoggingKey(), payload.getHeader(PayloadHeader.CORRELATION_ID));
                logOutgoingMessage(topic, toHeaders(payload));
                connection.publish(topic, toHeaders(payload), payload.getPayload());
                metricsRecorder.incrementSent(topic);
            }
        } catch (Exception e) {
            log.error("Error while publishing message", e);
        } finally {
            MDC.remove(MDCLoggingEnum.CORRELATION_ID.getLoggingKey());
        }
    }

    protected Headers toHeaders(PayloadWrapper<byte[]> wrapper) {
        Headers headers = new Headers();
        wrapper.getPayloadHeaders().forEach((k, v) -> headers.add(k.getKey(), v));
        return headers;
    }

    private void logIncomingMessage(String topicName, Headers headers) {
        log.debug("Received message on topic: {}", topicName);
        log.trace("Message headers: {}", headersToMap(headers));
    }

    private void logOutgoingMessage(String topicName, Headers headers) {
        log.debug("Sending message on topic: {}", topicName);
        log.trace("Message headers: {}", headersToMap(headers));
    }

    protected void registerDispatcher(Dispatcher dispatcher) {
        this.dispatchers.add(dispatcher);
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down {} dispatcher(s)...", dispatchers.size());
        for (Dispatcher dispatcher : dispatchers) {
            try {
                dispatcher.drain(Duration.ofSeconds(2));
                log.info("Drained dispatcher cleanly.");
            } catch (Exception e) {
                log.warn("Failed to drain dispatcher. Exception: {}", e.getMessage());
            }
        }
    }
}
