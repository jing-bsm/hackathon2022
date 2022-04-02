package com.foresee.eureka.watcher;

import com.foresee.eureka.watcher.domain.EurekaInstance;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Log4j2
public class PlayGround {
    @SneakyThrows
    public static void main(String[] args) {
        Monitor monitor = new Monitor();
        final List<EurekaInstance> instances = monitor.getInstances();
        OkHttpClient client = new OkHttpClient.Builder().connectTimeout(1, TimeUnit.SECONDS)
                .readTimeout(1, TimeUnit.SECONDS).writeTimeout(1, TimeUnit.SECONDS)
                .callTimeout(10, TimeUnit.SECONDS).build();
        HashSet<String> set = new HashSet<>();
        HashSet<String> all = new HashSet<>();
        instances.forEach(i -> {
            set.add(i.getApp().toLowerCase());
            all.add(i.getApp().toLowerCase());
            Request request = new Request.Builder().url(i.getHealthCheckUrl())
                    .build();
            boolean fail = true;
            try {
                final Response response = client.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null && response.body().string().contains("UP")) {
                    fail = false;
                }
            } catch (IOException e) {
                log.warn("{} {}, {}", i.getApp(), i.getHealthCheckUrl(), e.getMessage());
            }
            if (fail) {
                set.remove(i.getApp().toLowerCase());
//                log.info("{}\t {}", i.getApp(), i.getHealthCheckUrl());
            }
        });
        log.info(set);

        log.info(all);
        all.removeAll(set);
        log.info(all);
    }
}
