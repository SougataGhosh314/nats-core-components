package com.sougata.natscore.util;

import io.nats.client.impl.Headers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class NatsUtil {

    // Helper method to convert NATS Headers to a Map<String, String>
    public static Map<String, String> headersToMap(Headers headers) {
        Map<String, String> map = new HashMap<>();
        if (headers != null) {
            for (String key : headers.keySet()) {
                // headers.get(key) returns a List<String>, but for simplicity we take the first value
                List<String> values = headers.get(key);
                if (values != null && !values.isEmpty()) {
                    map.put(key, values.get(0));
                }
            }
        }
        return map;
    }

    public static Headers mapToHeaders(Map<String, String> map) {
        if (map == null) return null;
        Headers headers = new Headers();
        map.forEach(headers::add);
        return headers;
    }
}
