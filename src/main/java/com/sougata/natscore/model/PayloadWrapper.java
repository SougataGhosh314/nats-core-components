package com.sougata.natscore.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PayloadWrapper<T> {
    private final T payload;
    private final Map<PayloadHeader, String> payloadHeaders;

    private PayloadWrapper(T payload, Map<PayloadHeader, String> payloadHeaders) {
        this.payload = payload;
        this.payloadHeaders = payloadHeaders;
    }

    public T getPayload() {
        return this.payload;
    }

    public Map<PayloadHeader, String> getPayloadHeaders() {
        return this.payloadHeaders;
    }

    public String getHeader(PayloadHeader header) {
        return this.payloadHeaders.get(header);
    }

    public static <T> Builder<T> newBuilder() {
        return new Builder<>();
    }

    public Builder<T> toBuilder() {
        return new Builder<>(this);
    }

    public static class Builder<T> {
        private static final Set<PayloadHeader> STRICTLY_REQUIRED_HEADERS = Set.of(
                PayloadHeader.PAYLOAD_TYPE
        );

        private T payload;
        private final Map<PayloadHeader, String> headers = new EnumMap<>(PayloadHeader.class);

        public Builder() {}

        public Builder(PayloadWrapper<T> wrapper) {
            this.payload = wrapper.payload;
            this.headers.putAll(wrapper.payloadHeaders);
        }

        public Builder<T> setPayload(T payload) {
            this.payload = payload;
            return this;
        }

        public Builder<T> setPayloadType(String payloadType) {
            headers.put(PayloadHeader.PAYLOAD_TYPE, payloadType);
            return this;
        }

        public Builder<T> setCorrelationId(String correlationId) {
            headers.put(PayloadHeader.CORRELATION_ID, correlationId);
            return this;
        }

        public Builder<T> setCreationTimestamp(String creationTs) {
            headers.put(PayloadHeader.CREATION_TS, creationTs);
            return this;
        }

        public Builder<T> addHeader(PayloadHeader key, String value) {
            headers.put(key, value);
            return this;
        }

        public PayloadWrapper<T> build() {
            if (payload == null) {
                throw new IllegalStateException("Payload must be set.");
            }

            // Validate strictly required headers
            for (PayloadHeader required : STRICTLY_REQUIRED_HEADERS) {
                if (headers.get(required) == null || headers.get(required).isBlank()) {
                    throw new IllegalStateException("Missing required header: " + required.name());
                }
            }

            // Auto-fill optional-but-required headers
            headers.putIfAbsent(PayloadHeader.CORRELATION_ID, UUID.randomUUID().toString());
            headers.putIfAbsent(PayloadHeader.CREATION_TS, String.valueOf(System.currentTimeMillis()));

            return new PayloadWrapper<>(payload, new EnumMap<>(headers));
        }
    }
}