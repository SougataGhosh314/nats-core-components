package com.sougata.natscore.config;

import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventComponentConfig {
    private List<EventComponentEntry> components;

    @PostConstruct
    public void init() {
        log.info("EventComponentConfig initialized");
    }

}

