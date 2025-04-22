package com.sougata.natscore.dispatcher;

import com.sougata.natscore.config.TopicBinding;
import com.sougata.natscore.contract.PayloadConsumer;
import com.sougata.natscore.enums.MDCLoggingEnum;
import com.sougata.natscore.model.PayloadWrapper;
import com.sougata.natscore.monitoring.NatsMetricsRecorder;
import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@DependsOn("connection")
public class ConsumerDispatcher extends AbstractDispatcher {

    public ConsumerDispatcher(Connection connection, NatsMetricsRecorder metricsRecorder) {
        super(connection, metricsRecorder);
    }

    public void register(List<TopicBinding> topics, PayloadConsumer handler) {
        for (TopicBinding binding : topics) {
            Dispatcher dispatcher = connection.createDispatcher(msg -> {
                PayloadWrapper<byte[]> input = extractAndLogIncomingMessage(binding.getMessageType(), binding.getTopicName(), msg);

                try {
                    handler.consume(input);
                } catch (Exception e) {
                    log.error("Error while consuming message: ", e);
                    metricsRecorder.incrementError(binding.getTopicName());
                } finally {
                    MDC.remove(MDCLoggingEnum.CORRELATION_ID.getLoggingKey()); // ✅ safer than MDC.clear()
                }
            });

            if (StringUtils.isEmpty(binding.getQueueGroup()))
                dispatcher.subscribe(binding.getTopicName());
            else dispatcher.subscribe(binding.getTopicName(), binding.getQueueGroup());
            registerDispatcher(dispatcher);
        }
        log.info("Bean of type PayloadConsumer: {} registered", handler.getClass().getName());
    }
}