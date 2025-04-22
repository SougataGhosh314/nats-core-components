package com.sougata.natscore.config;

import com.sougata.natscore.util.NatsSslUtils;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Configuration
@ConfigurationProperties(prefix = "nats")
@Getter
@Setter
public class ConnectionConfig {
    private String url;
    private String credsFile;
    private boolean devMode = false;

    @Bean(destroyMethod = "close")
    public Connection connection() throws Exception {
        Options.Builder builder = new Options.Builder()
                .server(url)
                .connectionTimeout(Duration.ofSeconds(5));

        if (!devMode) {
            log.info("NATS devMode=false → Secure connection (creds + TLS)");

            builder.authHandler(Nats.credentials(credsFile));
            builder.sslContext(NatsSslUtils.createSslContext(
                    "C:/nats-config/certs/ca.pem",
                    null, // No client cert needed for one-way TLS
                    null  // No client key needed for one-way TLS
            ));
        } else {
            log.warn("NATS devMode=true → Connecting without creds or TLS");
        }

        Connection connection = null;
        try {
            connection = Nats.connect(builder.build());
        } catch (IOException | InterruptedException e) {
            log.error("Error establishing NATs connection: {}", ExceptionUtils.getStackTrace(e));
            throw new RuntimeException(e);
        }
        return connection;
    }
}
