package com.sougata.natscore.config;

import com.sougata.natscore.enums.HandlerType;
import lombok.Data;

import java.util.List;

@Data
public class EventComponentEntry {
    private HandlerType handlerType;
    private List<TopicBinding> readTopics;
    private List<TopicBinding> writeTopics;
    private String handlerClass;
    private boolean disabled;
}
