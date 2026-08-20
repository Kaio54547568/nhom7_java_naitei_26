package com.nhom7.coworkingspace.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.otp")
@Getter
@Setter
public class AppOtpProperties {

    private long expirationMinutes = 5;
}
