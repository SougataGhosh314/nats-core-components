package com.sougata.natscore.enums;

import lombok.Getter;

@Getter
public enum MDCLoggingEnum {
    CORRELATION_ID("correlationId");
    private final String loggingKey;

    MDCLoggingEnum(String loggingKey) {
        this.loggingKey = loggingKey;
    }
}
