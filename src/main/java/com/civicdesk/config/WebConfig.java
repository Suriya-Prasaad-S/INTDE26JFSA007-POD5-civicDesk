package com.civicdesk.config;

import com.civicdesk.module.serviceRequest.entity.enums.RequestStatus;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC configuration. Registers converters so enum query parameters accept the same
 * single-letter codes used in JSON bodies/responses (as well as the full name).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        // getAllRequests?status=  accepts e.g. "S" or "Submitted".
        registry.addConverter(new Converter<String, RequestStatus>() {
            @Override
            public RequestStatus convert(String source) {
                return RequestStatus.fromValue(source);
            }
        });
    }
}
