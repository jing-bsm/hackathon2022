package com.foresee.eureka.watcher;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.foresee.eureka.watcher.domain.EurekaInstance;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Log4j2
public class Monitor {
    @Value("${config.all-service:access-management, access-service, acs-discovery, adobe-integration, api-administration, arp-scheduling-service, arp-starter-service, arp-subscription-cxs, association-proxy-service, authorization-service, benchmark-service, calendar-service, case-configuration-services, case-hierarchy-sync, case-management-services, csv-export-service, csv-transformer-service, customer-management, cxmeasure-public-api-adapter, dashboard-data-service, dashboard-management-service, definition-service, engine-service, event-bus-manager, event-tracking-service, fcp-publisher, feedback-admin, feedback-data-loader, feedback-reporting, filter-service, firehose, foresee-services, google-integration, hierarchy-cxmeasure-service, hierarchy-def-upload-activity, hierarchy-definition-service, hierarchy-graph-maker, hierarchy-permissions-sync, hierarchy-service, hierarchy-sync-cxmeasure-service, hierarchy-transform-service, hierarchy-upload-csv-activity, hierarchy-upload-manager-server, hierarchy-upload-workflow, leaderboard-service, master-data-management-service, master-data-upload-activity, mpathy-asset-loader-service, mpathy-asset-server, mpathy-core-service, mpathy-hoover-service, oauth2-server, platform-auditlog-service, platform-communication-service, platform-distributed-tracing, platform-file-service, platform-gateway, platform-scim-service, ppt-export-service, respondents-service, settings-service, styp-service, survey-definition-builder, survey-definition-db-sync, survey-ingestion-ai, survey-ingestion-service, survey-rules-service, ta-public-api-adapter, ta-reporting-beta, text-analytics-admin-beta, user-management, useragent-service}")
    private String allServiceString;

    @Getter(lazy = true)
    private final Set<String> allServices = initSet();

    private final OkHttpClient client = new OkHttpClient.Builder().connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS).writeTimeout(1, TimeUnit.SECONDS)
            .callTimeout(5, TimeUnit.SECONDS).build();

    private final Set<String> downSet = new HashSet<>();
    private final Set<String> missingSet = new HashSet<>();

    private final Gauge dGauge = Gauge.builder("App-Down", downSet, Set::size)
            .register(Metrics.globalRegistry);
    private final Gauge mGauge = Gauge.builder("App-Missing", missingSet, Set::size)
            .register(Metrics.globalRegistry);

    @Scheduled(fixedRateString = "${config.schedule.interval:10000}", initialDelay = 10000)
    public void check() {
        final List<EurekaInstance> instances = getInstances();
        log.info("got {} instance", instances.size());
        final Set<String> dSet = instances.stream().filter(EurekaInstance::isDown)
                .map(EurekaInstance::getIpAddr).collect(Collectors.toSet());
        if (dSet.isEmpty()) {
            downSet.clear();
        } else {
            downSet.removeIf(i -> !dSet.contains(i));
            downSet.addAll(dSet);
            log.warn("down for {}", dSet);
        }

        HashSet<String> hashset = new HashSet<>(getAllServices());
        instances.stream().map(EurekaInstance::getApp).map(String::toLowerCase).distinct().sorted().forEach(hashset::remove);
        if (hashset.isEmpty()) {
            missingSet.clear();
        } else {
            missingSet.removeIf(i -> !hashset.contains(i));
            missingSet.addAll(hashset);
            log.warn("missing for {}", hashset);
        }
    }


    @SneakyThrows
    private List<EurekaInstance> getInstances(String text) {
        XmlMapper xmlMapper = new XmlMapper();
        final JsonNode treeNode = xmlMapper.readTree(text);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        List<EurekaInstance> instances = new ArrayList<>();
        if (treeNode.at("/application") instanceof ArrayNode) {
            final ArrayNode node = (ArrayNode) treeNode.at("/application");
            for (JsonNode n : node) {
                final JsonNode jsonNode = n.get("instance");
                if (jsonNode instanceof ArrayNode) {
                    final EurekaInstance[] value = objectMapper.treeToValue(jsonNode, EurekaInstance[].class);
                    instances.addAll(Arrays.asList(value));
                } else if (jsonNode instanceof ObjectNode) {
                    instances.add(objectMapper.treeToValue(jsonNode, EurekaInstance.class));
                }
            }
        }
        return instances;
    }

    @SneakyThrows
    private List<EurekaInstance> getInstancesFromUrl(String url) {
        try {
            final Request request = new Request.Builder()
                    .url(url)
                    .build();
            final Response response = client.newCall(request).execute();
            if (response.code() == 200 && response.body() != null) {
                return getInstances(response.body().string());
            }
        } catch (Exception e) {
            log.warn(e);
        }
        return Collections.emptyList();
    }

    List<EurekaInstance> getInstances() {
        List<EurekaInstance> list = getInstancesFromUrl("http://qal-service-discovery-b.foresee.com/acs-discovery/eureka/apps");
        if (list.isEmpty()) {
            list = getInstancesFromUrl("http://qal-service-discovery-a.foresee.com/acs-discovery/eureka/apps");
            if (list.isEmpty()) {
                list = getInstancesFromUrl("http://qal-service-discovery-c.foresee.com/acs-discovery/eureka/apps");
            }
        }
        return list;
    }

    private Set<String> initSet() {
        return Arrays.stream(allServiceString.split(","))
                .map(String::trim).map(String::toLowerCase).collect(Collectors.toSet());
    }
}
