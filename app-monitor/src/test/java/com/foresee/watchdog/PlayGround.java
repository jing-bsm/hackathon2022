package com.foresee.watchdog;

import com.foresee.api_automation.object_libraries.DiscoveryHelper;
import com.foresee.api_automation.tests.microservices.hierarchy_service.Dummy;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Log4j2
public class PlayGround {
    @SneakyThrows
    public static void main0(String[] args) {
        Runnable r = () -> {
            try {
                Thread.sleep(6000l);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
        final SimpleMeterRegistry registry = new SimpleMeterRegistry();
        Timer timer = registry.timer("ka");

        ExecutorService executor = Executors.newSingleThreadExecutor();
        log.info("start");
        executor.invokeAll(Arrays.asList(() -> {
            timer.record(r); return null;
        }), 5, TimeUnit.SECONDS); // Timeout of 10 minutes.
        log.info("end 1");
        executor.shutdown();


//        timer.record(r);
        log.info("end");
    }

    public static void main1(String[] args) {
        T t = new T();
        t.ffail();
    }
    public static void main(String[] args) {
        final String eureka = DiscoveryHelper.GetEurekaServiceIPWithPortNo("feedback-reporting".toLowerCase(),
                Dummy.getAuto().getParam("eureka"));
        log.info("------------");
        log.info(eureka);
    }
    static class T{
        @Test
        void ffail(){
            Assert.assertFalse(true);
        }
    }
}
