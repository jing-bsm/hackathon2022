package com.foresee.watchdog.applicationmonitor.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.JsonNodeDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.foresee.watchdog.applicationmonitor.domain.EurekaInstance;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Log4j2
public class EurekaMonitor {
    @Value("${config.all-service:access-management, access-service, acs-discovery, adobe-integration, api-administration, arp-scheduling-service, arp-starter-service, arp-subscription-cxs, association-proxy-service, authorization-service, benchmark-service, calendar-service, case-configuration-services, case-hierarchy-sync, case-management-services, csv-export-service, csv-transformer-service, customer-management, cxmeasure-public-api-adapter, dashboard-data-service, dashboard-management-service, definition-service, engine-service, event-bus-manager, event-tracking-service, fcp-publisher, feedback-admin, feedback-data-loader, feedback-reporting, filter-service, firehose, foresee-services, google-integration, hierarchy-cxmeasure-service, hierarchy-def-upload-activity, hierarchy-definition-service, hierarchy-graph-maker, hierarchy-permissions-sync, hierarchy-service, hierarchy-sync-cxmeasure-service, hierarchy-transform-service, hierarchy-upload-csv-activity, hierarchy-upload-manager-server, hierarchy-upload-workflow, leaderboard-service, master-data-management-service, master-data-upload-activity, mpathy-asset-loader-service, mpathy-asset-server, mpathy-core-service, mpathy-hoover-service, oauth2-server, platform-auditlog-service, platform-communication-service, platform-distributed-tracing, platform-file-service, platform-gateway, platform-scim-service, ppt-export-service, respondents-service, settings-service, styp-service, survey-definition-builder, survey-definition-db-sync, survey-ingestion-ai, survey-ingestion-service, survey-rules-service, ta-public-api-adapter, ta-reporting-beta, text-analytics-admin-beta, user-management, useragent-service}")
    private String allServiceString;

    @Value("${config.health-check-excludes:hierarchy-transform-service, survey-ingestion-ai, useragent-service, mpathy-asset-server, mpathy-core-service, mpathy-asset-loader-service, mpathy-hoover-service, platform-communication-service, platform-distributed-tracing}")
    private String healthCheckExcludesString;
    @Getter
    private PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);


    private final OkHttpClient client = new OkHttpClient.Builder().connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS).writeTimeout(1, TimeUnit.SECONDS).build();

    @Getter
    private final Set<String> downSet = new HashSet<>();
    @Getter
    private Map<String, Set<String>> allServices;
    private Set<String> healthCheckExcludes;

    @PostConstruct
    public void postConstruct() {
        allServices = initSet();
        healthCheckExcludes = Arrays.stream(Objects.requireNonNull(healthCheckExcludesString).split(","))
                .map(String::trim).map(String::toLowerCase).collect(Collectors.toSet());
        Gauge.builder("App-Down", downSet, Set::size)
                .register(registry);
        Gauge.builder("App-Missing", getAllServices(), map -> map.values().stream().filter(Set::isEmpty).count())
                .register(registry);
    }

    @Scheduled(fixedRateString = "${config.schedule.interval:10000}", initialDelay = 5000)
    public void check() {
        final List<EurekaInstance> instances = getInstances();
        log.info("got {} instance", instances.size());
        HashMap<String, Set<String>> currentUpMap = getCurrentUpMap(instances);
        allServices.forEach((name, set) -> {
            if (currentUpMap.containsKey(name)) {
                final Set<String> cSet = currentUpMap.get(name);
                set.removeIf(i -> !cSet.contains(i));
                set.addAll(cSet);
            } else {
                set.clear();
            }
        });

        // all down services
        final Set<String> dSet = instances.stream().filter(EurekaInstance::isDown)
                .map(EurekaInstance::getIpAddr).collect(Collectors.toSet());
        if (dSet.isEmpty()) {
            downSet.clear();
        } else {
            downSet.removeIf(i -> !dSet.contains(i));
            downSet.addAll(dSet);
            log.warn("down for {}", dSet);
        }
    }


    @SneakyThrows
    private List<EurekaInstance> getInstances(String text) {
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.registerModule(new SimpleModule().addDeserializer(
                JsonNode.class,
                new DuplicateToArrayJsonNodeDeserializer()
        ));
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

    private Map<String, Set<String>> initSet() {
        HashMap<String, Set<String>> map = new HashMap<>();
        Arrays.stream(Objects.requireNonNull(allServiceString).split(",")).map(String::trim).map(String::toLowerCase)
                .forEach(n -> map.put(n, new HashSet<>()));
        map.forEach((k, v) -> Gauge.builder("App-Count", v, Set::size)
                .tag("app", k).register(registry));
        return Collections.unmodifiableMap(map);
    }

    private HashMap<String, Set<String>> getCurrentUpMap(List<EurekaInstance> instances) {
        HashMap<String, Set<String>> currentUpMap = new HashMap<>();
        instances.parallelStream().filter(i -> !i.isDown()).forEach(i -> {
            Set<String> set = currentUpMap.get(i.getApp());
            if (set == null) {
                set = new HashSet<>();
            }
            boolean healthUp = healthCheckExcludes.contains(i.getApp().toLowerCase()) || isHealthUp(i.getHealthCheckUrl());
            if (healthUp) {
                set.add(i.getIpAddr());
            }
            currentUpMap.put(i.getApp().toLowerCase(), set);
        });
        return currentUpMap;
    }

    private boolean isHealthUp(String healthUrl) {
        Request request = new Request.Builder().url(healthUrl)
                .build();
        try {
            final Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null && response.body().string().contains("UP")) {
                return true;
            }
        } catch (IOException e) {
            log.warn(e);
        }
        return false;
    }
    // a workaround for old version of xmlMapper
    static class DuplicateToArrayJsonNodeDeserializer extends JsonNodeDeserializer {

        @Override
        protected void _handleDuplicateField(JsonParser p, DeserializationContext ctxt,
                                             JsonNodeFactory nodeFactory, String fieldName, ObjectNode objectNode,
                                             JsonNode oldValue, JsonNode newValue) throws JsonProcessingException {
            ArrayNode node;
            if(oldValue instanceof ArrayNode){
                node = (ArrayNode) oldValue;
                node.add(newValue);
            } else {
                node = nodeFactory.arrayNode();
                node.add(oldValue);
                node.add(newValue);
            }
            objectNode.set(fieldName, node);
        }
    }
}
