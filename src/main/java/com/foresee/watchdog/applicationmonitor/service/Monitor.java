package com.foresee.watchdog.applicationmonitor.service;

import com.foresee.api_automation.object_libraries.api_definition.AccessAPI;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Log4j2
public class Monitor {

    @Value("${config.schedule.job-timeout:30}")
    private long jobTimeout = 30;
    @Value("${config.schedule.sequence-timeout:280}")
    private long sequenceTimeout = 280;

    private final ConcurrentLinkedDeque<Pair<String, Runnable>> randomDeque = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<List<Pair<String, Runnable>>> sequenceDeque = new ConcurrentLinkedDeque<>();

    @Scheduled(fixedRateString = "${config.schedule.interval:60000}", initialDelay = 10000)
    // wait 10s to start, run every 3 minutes
    public void check() {
        log.info("-------- Starting monitor -------------");
        CompletableFuture.runAsync(this::checkRandomList);
        CompletableFuture.runAsync(this::checkSequenceList);
    }

    @Scheduled(fixedRate = 2 * 60 * 60000, initialDelay = 60 * 60000) // wait 1h to start, run every 2 hours
    public void cleanUp() {
        AccessAPI.flushCache();
    }

    private void checkPairTimed(Pair<String, Runnable> pair) {
        try {
            log.info("Checking {}", pair.getFirst());
            final Timer timer = Timer.builder(pair.getFirst()).register(Metrics.globalRegistry);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            timer.record(() -> {
                try {
                    checkPair(pair, executor);
                } catch (InterruptedException e) {
                    log.error("Timeout ?", e);
                    Thread.currentThread().interrupt();
                }
            });
            executor.shutdown();
        } catch (Exception e) {
            log.error("something wrong with random check {}", pair.getFirst(), e);
        }
    }

    private void checkPair(Pair<String, Runnable> pair, ExecutorService executor) throws InterruptedException {
        executor.invokeAll(Collections.singletonList(() -> {
            try {
                pair.getSecond().run();
            } catch (AssertionError assertionError) {
                Counter.builder("FAIL_Tests")
                        .tag("app", pair.getFirst())
                        .register(Metrics.globalRegistry)
                        .increment();
            }
            return null;
        }), jobTimeout, TimeUnit.SECONDS); // Timeout of 10 minutes.
    }

    private void checkRandomList() {
        randomDeque.stream().parallel().forEach(this::checkPairTimed);
    }

    private void checkSequenceList() {
        sequenceDeque.stream().parallel().forEach(list -> {
            try {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.invokeAll(Collections.singletonList(() -> {
                    list.forEach(this::checkPairTimed);
                    return null;
                }), sequenceTimeout, TimeUnit.SECONDS);
                executor.shutdown();
            } catch (Exception e) {
                log.error("Error in full sequence", e);
                Thread.currentThread().interrupt();
            }
        });
    }


    public void register(String name, Runnable checkCase) {
        randomDeque.add(Pair.of(name, checkCase));
    }

    public void register(List<Pair<String, Runnable>> sequence) {
        sequenceDeque.add(sequence);
    }

}
