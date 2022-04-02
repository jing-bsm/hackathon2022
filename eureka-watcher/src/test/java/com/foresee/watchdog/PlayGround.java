package com.foresee.watchdog;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.foresee.eureka.watcher.domain.EurekaInstance;
import io.micrometer.core.instrument.util.IOUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
public class PlayGround {
    @SneakyThrows
    public static void main(String[] args) {


        String allString = "access-management, access-service, acs-discovery, adobe-integration, api-administration, arp-scheduling-service, arp-starter-service, arp-subscription-cxs, association-proxy-service, authorization-service, benchmark-service, calendar-service, case-configuration-services, case-hierarchy-sync, case-management-services, csv-export-service, csv-transformer-service, customer-management, cxmeasure-public-api-adapter, dashboard-data-service, dashboard-management-service, definition-service, engine-service, event-bus-manager, event-tracking-service, fcp-publisher, feedback-admin, feedback-data-loader, feedback-reporting, filter-service, firehose, foresee-services, google-integration, hierarchy-cxmeasure-service, hierarchy-def-upload-activity, hierarchy-definition-service, hierarchy-graph-maker, hierarchy-permissions-sync, hierarchy-service, hierarchy-sync-cxmeasure-service, hierarchy-transform-service, hierarchy-upload-csv-activity, hierarchy-upload-manager-server, hierarchy-upload-workflow, leaderboard-service, master-data-management-service, master-data-upload-activity, mpathy-asset-loader-service, mpathy-asset-server, mpathy-core-service, mpathy-hoover-service, oauth2-server, platform-auditlog-service, platform-communication-service, platform-distributed-tracing, platform-file-service, platform-gateway, platform-scim-service, ppt-export-service, respondents-service, settings-service, styp-service, survey-definition-builder, survey-definition-db-sync, survey-ingestion-ai, survey-ingestion-service, survey-rules-service, ta-public-api-adapter, ta-reporting-beta, text-analytics-admin-beta, user-management, useragent-service, xx";
        final Set<String> set = Arrays.stream(allString.split(",")).map(String::trim).map(String::toLowerCase).collect(Collectors.toSet());
        String text = IOUtils.toString(PlayGround.class.getResourceAsStream("/x"),
                StandardCharsets.UTF_8);
//        log.info(text);

        OkHttpClient client = new OkHttpClient();
        final Request request = new Request.Builder()
                .url("http://qal-service-discovery-b.foresee.com/acs-discovery/eureka/apps")
                .build();
        final Response response = client.newCall(request).execute();
        if (response.code() == 200 && response.body() != null) {
            log.info(response.body().string());
            //            instances.forEach(i -> log.info(i.healthCheckUrl));
        }
    }
}
