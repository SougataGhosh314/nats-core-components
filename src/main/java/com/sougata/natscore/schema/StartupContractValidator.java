package com.sougata.natscore.schema;

import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.sougata.natscore.config.EventComponentConfig;
import com.sougata.natscore.config.TopicBinding;
import com.sougata.natscore.schema.model.SchemaBinding;
import com.sougata.natscore.util.ProtobufUtils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupContractValidator {

    private final SchemaRegistryClient schemaRegistryClient;
    private final EventComponentConfig config;

    @PostConstruct
    public void validateContracts() {
        log.info("Starting contract validation and auto-registration if necessary...");
        Set<String> alreadyRegistered = new HashSet<>();

        Collection<TopicBinding> allBindings = config.getComponents().stream()
                .filter(c -> !c.isDisabled())
                .flatMap(c -> {
                    var reads = c.getReadTopics() == null ? Set.<TopicBinding>of() : c.getReadTopics();
                    var writes = c.getWriteTopics() == null ? Set.<TopicBinding>of() : c.getWriteTopics();
                    return Stream.concat(reads.stream(), writes.stream());
                })
                .toList();

        for (TopicBinding binding : allBindings) {
            validate(binding, alreadyRegistered);
        }

        log.info("✅ Contract validation complete.");
    }

    private void validate(TopicBinding binding, Set<String> alreadyRegistered) {
        String topic = binding.getTopicName();
        String messageType = binding.getMessageType();

        if (alreadyRegistered.contains(topic)) return;
        alreadyRegistered.add(topic);

        try {
            Class<?> clazz = Class.forName(messageType);
            if (!Message.class.isAssignableFrom(clazz)) {
                log.warn("⚠️ Not a valid Protobuf Message: {}", messageType);
                return;
            }

            Message msg = (Message) clazz.getMethod("getDefaultInstance").invoke(null);
            Descriptors.Descriptor descriptor = msg.getDescriptorForType();

            String sha = ProtobufUtils.sha256Hex(descriptor);
            String base64 = Base64.getEncoder().encodeToString(descriptor.toProto().toByteArray());

            SchemaBinding bindingToSend = new SchemaBinding();
            bindingToSend.setTopic(topic);
            bindingToSend.setProtoMessageType(messageType);
            bindingToSend.setDescriptorSha256(sha);
            bindingToSend.setDescriptorBase64(base64);

            schemaRegistryClient.registerSchema(bindingToSend);

            schemaRegistryClient.fetchSchema(topic).ifPresent(remote -> {
                if (!remote.getDescriptorSha256().equals(sha)) {
                    String error = "❌ Contract mismatch for topic: " + topic;
                    log.error(error);
                    throw new IllegalStateException(error);
                }
            });

        } catch (Exception e) {
            log.error("❌ Validation failed for topic {}: {}", topic, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
