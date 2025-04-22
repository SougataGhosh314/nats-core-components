package com.sougata.natscore.dispatcher;

import com.sougata.natscore.config.EventComponentConfig;
import com.sougata.natscore.config.SupplierExecutorConfig;
import com.sougata.natscore.contract.PayloadSupplier;
import com.sougata.natscore.enums.HandlerType;
import com.sougata.natscore.model.PayloadWrapper;
import com.sougata.natscore.monitoring.NatsMetricsRecorder;
import io.nats.client.Connection;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@DependsOn("connection")
public class SupplierDispatcher extends AbstractDispatcher {
    private final ExecutorService executor;

    public SupplierDispatcher(Connection connection, EventComponentConfig config, NatsMetricsRecorder metricsRecorder, SupplierExecutorConfig executorConfig) {
        super(connection, metricsRecorder);
        this.executor = Executors.newFixedThreadPool(
                executorConfig.getThreadPoolSize(),
                r -> {
                    Thread t = new Thread(r);
                    t.setName("SupplierDispatcher-" + UUID.randomUUID());
                    t.setDaemon(true);
                    return t;
                }
        );

        this.writeTopicMap = new HashMap<>();

        config.getComponents().stream()
                .filter(entry -> HandlerType.SUPPLIER.equals(entry.getHandlerType()))
                .flatMap(entry -> entry.getWriteTopics().stream())
                .forEach(binding -> writeTopicMap.put(binding.getMessageType(), binding.getTopicName()));
    }

    public void register(PayloadSupplier handler) {
        log.info("Bean of type PayloadSupplier: {} registered", handler.getClass().getName());
        this.executor.submit(() -> runSupplierLoop(handler));
    }

    private void runSupplierLoop(PayloadSupplier handler) {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                PayloadWrapper<byte[]> payload = handler.supply();
                if (payload != null) publish(payload);

            } catch (Exception e) {
                log.error("Error in PayloadSupplier: {}.supply(): ", handler.getClass().getName(), e);
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}