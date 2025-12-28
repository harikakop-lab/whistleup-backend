package com.whistleup.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
public class SmsRestClientConfig {

    @Bean
    public RestClient smsCountryRestClient() {
        return RestClient.builder()
                .baseUrl("https://restapi.smscountry.com/v0.1")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Basic OG5OOFIxMW9EREtOSk83T0NUS1U6QzF5a3BucDVVc1UxbzZQamI3ZkprNUV4MUlJMWVJWktPTE95Q1NUWg=="
                )
                .build();
    }
}
