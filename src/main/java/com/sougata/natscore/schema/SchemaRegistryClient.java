package com.sougata.natscore.schema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sougata.natscore.schema.model.SchemaBinding;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Slf4j
@Component
public class SchemaRegistryClient {

    private final RestTemplate restTemplate;

    @Value("${schema.registry.url}")
    private String registryUrl;
    private final ObjectMapper mapper = new ObjectMapper();

    public SchemaRegistryClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void registerSchema(SchemaBinding binding) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<SchemaBinding> entity = new HttpEntity<>(binding, headers);
            restTemplate.postForEntity(registryUrl + "/api/schemas", entity, String.class);
            log.info("✅ Registered schema for topic: {}", binding.getTopic());
        } catch (Exception e) {
            log.error("❌ Failed to register schema for topic {}: {}", binding.getTopic(), e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public Optional<SchemaBinding> fetchSchema(String topic) {
        try {
            ResponseEntity<SchemaBinding> response = restTemplate.getForEntity(registryUrl + "/api/" + topic, SchemaBinding.class);
            return Optional.ofNullable(response.getBody());
        } catch (Exception e) {
            log.warn("⚠️ Could not fetch schema for topic {}: {}", topic, e.getMessage());
            return Optional.empty();
        }
    }
}
