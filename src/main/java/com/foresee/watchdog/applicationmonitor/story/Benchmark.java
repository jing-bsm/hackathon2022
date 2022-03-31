package com.foresee.watchdog.applicationmonitor.story;

import com.foresee.api_automation.tests.microservices.benchmark.CategoryClientsTests;
import com.foresee.api_automation.tests.microservices.benchmark.CategoryMappingTests;
import com.foresee.watchdog.applicationmonitor.service.Mornitor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class Benchmark {
    private final Mornitor mornitor;

    @PostConstruct
    public void postConstruct() {
        CategoryClientsTests categoryClientsTests = new CategoryClientsTests();
        mornitor.register("benchmark_get_categories", categoryClientsTests::testGETBenchmarkServiceCategoryClients200_82580735);
        mornitor.register("benchmark_not_legacy", categoryClientsTests::testValidationException_NotLegacyTenant_82590230);
        // in sequence
        CategoryMappingTests mappingTests = new CategoryMappingTests();
        List<Pair<String, Runnable>> sequence = Arrays.asList(
                Pair.of("benchmark_mapping_in_completed", mappingTests::testGetMeasurementsListForTenant_StatusIncomplete_46641296),
                Pair.of("benchmark_mapping_in_compatible", mappingTests::testGetMeasurementsListForTenant_StatusIncompatible_54540956)
        );
        mornitor.register(sequence);
    }
}
