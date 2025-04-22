package com.sougata.natscore.dispatcher;

import com.sougata.natscore.config.EventComponentConfig;
import com.sougata.natscore.config.SupplierExecutorConfig;
import com.sougata.natscore.contract.PayloadSupplierFanout;
import com.sougata.natscore.enums.HandlerType;
import com.sougata.natscore.model.PayloadWrapper;
import com.sougata.natscore.monitoring.NatsMetricsRecorder;
import io.nats.client.Connection;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@DependsOn("connection")
public class SupplierFanoutDispatcher extends AbstractDispatcher {
    private final ExecutorService executor;

    public SupplierFanoutDispatcher(Connection connection, EventComponentConfig config, NatsMetricsRecorder metricsRecorder, SupplierExecutorConfig executorConfig) {
        super(connection, metricsRecorder);

        this.executor = Executors.newFixedThreadPool(
                executorConfig.getThreadPoolSize(),
                r -> {
                    Thread t = new Thread(r);
                    t.setName("SupplierFanoutDispatcher-" + UUID.randomUUID());
                    t.setDaemon(true);
                    return t;
                }
        );

        this.writeTopicMap = new HashMap<>();

        config.getComponents().stream()
                .filter(entry -> HandlerType.SUPPLIER_FANOUT.equals(entry.getHandlerType()))
                .flatMap(entry -> entry.getWriteTopics().stream())
                .forEach(binding -> writeTopicMap.put(binding.getMessageType(), binding.getTopicName()));
    }

    public void register(PayloadSupplierFanout handler) {
        log.info("Bean of type PayloadSupplier: {} registered", handler.getClass().getName());
        executor.submit(() -> runFanoutSupplierLoop(handler));
    }

    private void runFanoutSupplierLoop(PayloadSupplierFanout supplier) {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<PayloadWrapper<byte[]>> payloads = supplier.supply();
                if (CollectionUtils.isEmpty(payloads)) {
                    log.info("SupplierFanout: {} returned no payloads. Nothing to dispatch.", payloads.getClass().getName());
                    return;
                }

                payloads.forEach(this::publish);
            } catch (Exception e) {
                log.error("SupplierFanout error ({}):", supplier.getClass().getSimpleName(), e);
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
