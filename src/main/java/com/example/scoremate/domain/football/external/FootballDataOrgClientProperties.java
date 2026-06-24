package com.example.scoremate.domain.football.external;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "scoremate.football-data")
public record FootballDataOrgClientProperties(
        String baseUrl,
        String apiToken,
        boolean mockEnabled
) {
    public String resolvedBaseUrl() {
        return baseUrl == null || baseUrl.isBlank() ? "https://api.football-data.org/v4" : baseUrl;
    }
}
