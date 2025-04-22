package com.sougata.natscore.model;

import lombok.Getter;

@Getter
public enum PayloadHeader {
    PAYLOAD_TYPE("payloadType"),
    CORRELATION_ID("correlationId"),
    CREATION_TS("creationTs");

    private final String key;

    PayloadHeader(String key) {
        this.key = key;
    }
}
