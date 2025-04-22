package com.sougata.natscore.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "nats.supplier")
public class SupplierExecutorConfig {
    private int threadPoolSize = Runtime.getRuntime().availableProcessors() * 2; // default fallback
}
