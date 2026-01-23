package com.pe.jm_n8n_jar_clasificado.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@RequiredArgsConstructor
public class RestTemplateConfig {

    private final AppProperties properties;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        int timeoutMs = properties.getTimeoutSeconds() * 1000;
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);

        return new RestTemplate(factory);
    }
}
