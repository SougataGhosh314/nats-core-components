package com.sougata.natscore.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sougata.natscore.enums.HandlerType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Responsible for loading and validating event component configuration for NATS-based microservices.
 * <p>
 * This class reads the `event-config.json` from the classpath, filters out disabled components,
 * and applies strong validation rules to ensure that topic bindings are conflict-free and behaviorally consistent.
 * </p>
 *
 * <h3>Validation Logic Enforced:</h3>
 * <ul>
 *     <li><b>Each topic can only have one handler type</b> (e.g., only Functions or only Consumers, not both).</li>
 *     <li><b>Handlers of the same type can read from the same topic</b> only if:
 *         <ul>
 *             <li>Each uses a distinct queue group (for parallelism and horizontal scaling).</li>
 *             <li>OR only one handler lacks a queue group (solo consumption).</li>
 *         </ul>
 *     </li>
 *     <li><b>No topic may be both published to and subscribed to</b> within the same application. This avoids the app processing its own messages unintentionally.</li>
 * </ul>
 */
@Slf4j
@Configuration
@Getter
public class EventComponentConfigLoader {

    /**
     * Loads the event component configuration from `event-config.json`,
     * applies validation, and exposes it as a Spring bean.
     *
     * @return the validated EventComponentConfig instance
     * @throws IOException if the config file is missing or malformed
     */
    @Bean
    public EventComponentConfig eventComponentConfig() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("event-config.json")) {
            if (is == null) {
                throw new FileNotFoundException("event-config.json not found in classpath");
            }

            EventComponentConfig config = mapper.readValue(is, EventComponentConfig.class);
            // filter out disabled ones
            config.setComponents(config.getComponents().stream().filter(c -> !c.isDisabled()).collect(Collectors.toList()));

            // do validation on the enabled ones
            validateReadTopicBindings(config);
            validateReadWriteConflicts(config);
            log.info("event-config validation is successful.");
            return config;
        }
    }

    /**
     * Validates the rules surrounding which handlers can subscribe to the same topics.
     * <p>
     * Rules enforced:
     * <ul>
     *     <li>No two handler types (e.g., CONSUMER + FUNCTION) may subscribe to the same topic.</li>
     *     <li>Multiple same-type handlers may subscribe to a topic only if they use distinct queue groups.</li>
     *     <li>Multiple handlers without queue groups (i.e., null group) are disallowed on the same topic.</li>
     * </ul>
     *
     * @param config the event component config to validate
     */
    public void validateReadTopicBindings(EventComponentConfig config) {
        log.info("Validating read topic bindings...");
        Map<String, Map<HandlerType, Set<String>>> topicHandlerMap = new HashMap<>();

        for (EventComponentEntry entry : config.getComponents()) {
            if (entry.getReadTopics() == null) continue;

            for (TopicBinding binding : entry.getReadTopics()) {
                String topic = binding.getTopicName();
                String queueGroup = binding.getQueueGroup(); // can be null
                HandlerType type = entry.getHandlerType();

                topicHandlerMap
                        .computeIfAbsent(topic, t -> new HashMap<>())
                        .computeIfAbsent(type, t -> new HashSet<>())
                        .add(queueGroup);
            }
        }

        for (Map.Entry<String, Map<HandlerType, Set<String>>> entry : topicHandlerMap.entrySet()) {
            String topic = entry.getKey();
            Map<HandlerType, Set<String>> handlersOnTopic = entry.getValue();

            // ❌ Rule: Different handler types on the same topic
            if (handlersOnTopic.size() > 1) {
                throw new IllegalStateException("Topic [" + topic + "] has multiple handler types: " + handlersOnTopic.keySet());
            }

            // ✅ Safe to validate queue group uniqueness
            Set<String> queueGroups = handlersOnTopic.values().iterator().next();
            List<String> nonNullGroups = new ArrayList<>();

            for (String q : queueGroups) {
                if (q == null) {
                    nonNullGroups.add("null");
                } else if (nonNullGroups.contains(q)) {
                    throw new IllegalStateException("Duplicate queue group [" + q + "] on topic [" + topic + "]");
                } else {
                    nonNullGroups.add(q);
                }
            }

            // ❌ Rule: more than one handler with null queueGroup
            long nullCount = queueGroups.stream().filter(Objects::isNull).count();
            if (nullCount > 1) {
                throw new IllegalStateException("Topic [" + topic + "] has multiple handlers without queue groups (null)");
            }
        }
    }

    /**
     * Validates that no topic is configured as both a read and write topic
     * by any component in the same application.
     * <p>
     * This prevents accidental message loops or reprocessing caused by an app
     * emitting to a topic it's also subscribed to.
     *
     * @param config the validated config containing active (non-disabled) components
     */
    private void validateReadWriteConflicts(EventComponentConfig config) {
        log.info("Validating read-write topic conflicts...");
        Set<String> writtenTopics = config.getComponents().stream()
                .flatMap(c -> Optional.ofNullable(c.getWriteTopics()).stream().flatMap(Collection::stream))
                .map(TopicBinding::getTopicName)
                .collect(Collectors.toSet());

        Set<String> readTopics = config.getComponents().stream()
                .flatMap(c -> Optional.ofNullable(c.getReadTopics()).stream().flatMap(Collection::stream))
                .map(TopicBinding::getTopicName)
                .collect(Collectors.toSet());

        Set<String> overlap = new HashSet<>(writtenTopics);
        overlap.retainAll(readTopics);

        if (!overlap.isEmpty()) {
            throw new IllegalStateException(
                    "Invalid config: Topics used for both publishing and subscribing: " + String.join(", ", overlap)
            );
        }
    }
}
