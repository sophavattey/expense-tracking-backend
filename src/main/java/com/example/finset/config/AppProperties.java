package com.example.finset.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
@Getter @Setter
public class AppProperties {

    private String frontendUrl;

    private final Jwt jwt = new Jwt();
    private final Cookie cookie = new Cookie();

    @Getter @Setter
    public static class Jwt {
        private String accessSecret;
        private String refreshSecret;
        private long accessExpirationMs  = 900_000L;       // 15 min
        private long refreshExpirationMs = 2_592_000_000L; // 30 days
    }

    @Getter @Setter
    public static class Cookie {
        private boolean secure = false;
        private String sameSite = "Lax";
    }
}