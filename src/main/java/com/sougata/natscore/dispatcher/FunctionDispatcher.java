package com.sougata.natscore.dispatcher;

import com.sougata.natscore.config.EventComponentConfig;
import com.sougata.natscore.config.TopicBinding;
import com.sougata.natscore.contract.PayloadFunction;
import com.sougata.natscore.enums.HandlerType;
import com.sougata.natscore.model.PayloadWrapper;
import com.sougata.natscore.monitoring.NatsMetricsRecorder;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

@Slf4j
@Component
@DependsOn("connection")
public class FunctionDispatcher extends AbstractDispatcher {

    public FunctionDispatcher(Connection connection, EventComponentConfig config, NatsMetricsRecorder metricsRecorder) {
        super(connection, metricsRecorder);
        this.writeTopicMap = new HashMap<>();

        config.getComponents().stream()
                .filter(entry -> HandlerType.FUNCTION.equals(entry.getHandlerType()))
                .flatMap(entry -> entry.getWriteTopics().stream())
                .forEach(binding -> writeTopicMap.put(binding.getMessageType(), binding.getTopicName()));
    }

    public void register(List<TopicBinding> topics, PayloadFunction handler) {
        for (TopicBinding binding : topics) {
            Dispatcher dispatcher = connection.createDispatcher(msg -> {
                PayloadWrapper<byte[]> input = extractAndLogIncomingMessage(binding.getMessageType(), binding.getTopicName(), msg);
                PayloadWrapper<byte[]> result = null;
                try {
                    result = handler.process(input);
                } catch (Exception e) {
                    log.error("Error while processing message: ", e);
                    metricsRecorder.incrementError(binding.getTopicName());
                }
                if (result != null) publish(result);
            });

            if (StringUtils.isEmpty(binding.getQueueGroup()))
                dispatcher.subscribe(binding.getTopicName());
            else dispatcher.subscribe(binding.getTopicName(), binding.getQueueGroup());
            registerDispatcher(dispatcher);
        }
        log.info("Bean of type PayloadFunction: {} registered", handler.getClass().getName());
    }
}