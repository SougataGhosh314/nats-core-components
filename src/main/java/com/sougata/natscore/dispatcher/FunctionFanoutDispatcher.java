package com.sougata.natscore.dispatcher;

import com.sougata.natscore.config.EventComponentConfig;
import com.sougata.natscore.config.TopicBinding;
import com.sougata.natscore.contract.PayloadFunctionFanout;
import com.sougata.natscore.enums.HandlerType;
import com.sougata.natscore.model.PayloadWrapper;
import com.sougata.natscore.monitoring.NatsMetricsRecorder;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;

@Slf4j
@Component
@DependsOn("connection")
public class FunctionFanoutDispatcher extends AbstractDispatcher {

    public FunctionFanoutDispatcher(Connection connection, EventComponentConfig config, NatsMetricsRecorder metricsRecorder) {
        super(connection, metricsRecorder);
        this.writeTopicMap = new HashMap<>();

        config.getComponents().stream()
                .filter(entry -> HandlerType.FUNCTION_FANOUT.equals(entry.getHandlerType()))
                .flatMap(entry -> entry.getWriteTopics().stream())
                .forEach(binding -> writeTopicMap.put(binding.getMessageType(), binding.getTopicName()));
    }

    public void register(List<TopicBinding> topics, PayloadFunctionFanout handler) {
        for (TopicBinding binding : topics) {
            Dispatcher dispatcher = connection.createDispatcher(msg -> {
                PayloadWrapper<byte[]> input = extractAndLogIncomingMessage(binding.getMessageType(), binding.getTopicName(), msg);
                List<PayloadWrapper<byte[]>> payloadWrappers = null;
                try {
                    payloadWrappers = handler.process(input);
                    if (CollectionUtils.isEmpty(payloadWrappers)) {
                        log.info("FunctionFanout: {} returned no payloads. Nothing to dispatch.", handler.getClass().getName());
                        return;
                    }
                } catch (Exception e) {
                    log.error("Error while processing message: ", e);
                    metricsRecorder.incrementError(binding.getTopicName());
                }
                for (PayloadWrapper<byte[]> payloadWrapper : payloadWrappers) {
                    publish(payloadWrapper);
                }
            });

            if (StringUtils.isEmpty(binding.getQueueGroup()))
                dispatcher.subscribe(binding.getTopicName());
            else dispatcher.subscribe(binding.getTopicName(), binding.getQueueGroup());
            registerDispatcher(dispatcher);
        }
        log.info("Bean of type PayloadFunction: {} registered", handler.getClass().getName());
    }
}