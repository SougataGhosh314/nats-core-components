package com.sougata.natscore.health;

import io.nats.client.Connection;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class NatsHealthIndicator implements HealthIndicator {

    private final Connection natsConnection;

    public NatsHealthIndicator(Connection natsConnection) {
        this.natsConnection = natsConnection;
    }

    @Override
    public Health health() {
        Health.Builder builder;

        if (natsConnection == null || natsConnection.getStatus() == null) {
            builder = Health.down().withDetail("status", "NATS connection is null or unknown");
        } else if (natsConnection.getStatus() == Connection.Status.CONNECTED) {
            builder = Health.up()
                    .withDetail("status", "CONNECTED")
                    .withDetail("url", natsConnection.getConnectedUrl())
                    .withDetail("serverId", natsConnection.getServerInfo().getServerId());
        } else {
            builder = Health.down()
                    .withDetail("status", natsConnection.getStatus().toString());
        }

        return builder.build();
    }
}

