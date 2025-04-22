package com.sougata.natscore.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SupplierExecutorConfig.class)
public class NatsSupplierConfig {}
