package com.example.homework.config;

import com.example.homework.common.exception.BusinessException;
import io.sentry.SentryOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SentryConfig {

    @Bean
    public SentryOptions.BeforeSendCallback beforeSendCallback() {
        return (event, hint) -> {
            Throwable throwable = event.getThrowable();
            if (throwable instanceof BusinessException) {
                // Suppress expected business exceptions from being reported to Sentry
                return null;
            }
            return event;
        };
    }
}
