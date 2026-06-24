package com.example.scoremate.global.config;

import com.example.scoremate.domain.football.external.FootballDataOrgClientProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FootballDataOrgClientProperties.class)
public class AppPropertiesConfig {
}
