package com.whistleup.backend;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "razorpay")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RazorpayConfig {

    private String keyId;
    private String keySecret;
    private String currency;
    private String appName;

//    @Bean
//    public RazorpayClient razorPayClient() {
//        return new RazorpayClient(keyId, keySecret);
//    }
}
