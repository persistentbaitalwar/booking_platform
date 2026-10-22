package com.persistent.booking_platform.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    /** Injected everywhere "now" is needed so that time-dependent rules can be tested. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
