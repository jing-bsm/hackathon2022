package com.foresee.watchdog.applicationmonitor.service;

import com.foresee.api_automation.object_libraries.DiscoveryHelper;
import com.foresee.api_automation.object_libraries.api_definition.AccessAPI;
import com.foresee.automation_framework.BaseAutomationClass;
import com.foresee.automation_framework.Config;
import io.restassured.response.Response;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
@RequiredArgsConstructor
@Log4j2
@Getter
public class Lookup {
    private final BaseAutomationClass baseAutomationClass;

    private String token;
    private String secret;
    private String jWTToken;

    public String serviceLocation(String svc) {
        return DiscoveryHelper.GetEurekaServiceIP(svc,
                baseAutomationClass.getParam("eureka"));
    }

    @PostConstruct
    public void initToken() {
        if (jWTToken == null) {
            final Config configuration = baseAutomationClass.configuration;
            Response access = AccessAPI.getAccessToken(configuration.consumerKey(),
                    configuration.consumerSecret(), configuration.defaultUser(), configuration.defaultPass());
            token = access.path("token");
            secret = access.path("secret");
            jWTToken = AccessAPI.getJWTToken();
        }
    }

}
