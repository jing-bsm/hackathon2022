package com.foresee.watchdog.applicationmonitor.config;

import com.foresee.api_automation.tests.microservices.hierarchy_service.Dummy;
import com.foresee.automation_framework.BaseAutomationClass;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    @ConditionalOnMissingBean
    public BaseAutomationClass getBaseAutomationClass() {
        return Dummy.getAuto();
    }

    @Bean
    @ConditionalOnMissingBean
    public SimpleMeterRegistry getRegistry() {
        return new SimpleMeterRegistry();
    }
}
