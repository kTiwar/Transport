package com.tms.edi.routing.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(RoutingProperties.class)
public class RoutingModuleConfig {

    @Bean
    public RestTemplate nominatimRestTemplate(RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
        f.setReadTimeout((int) Duration.ofSeconds(20).toMillis());
        return builder.requestFactory(() -> f).build();
    }

    @Bean
    public RestTemplate osrmRestTemplate(RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout((int) Duration.ofSeconds(5).toMillis());
        f.setReadTimeout((int) Duration.ofSeconds(60).toMillis());
        return builder.requestFactory(() -> f).build();
    }
}
