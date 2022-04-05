package com.foresee.watchdog.applicationmonitor.controller;

import com.foresee.watchdog.applicationmonitor.service.QAMonitor;
import com.foresee.watchdog.applicationmonitor.service.EurekaMonitor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.http.MediaType;

@RestController
@RequiredArgsConstructor
public class Controller {
    private final EurekaMonitor eurekaMonitor;
    private final QAMonitor QAMonitor;

    @SneakyThrows
    @RequestMapping("/failed")
    public Set<String> failed() {
        return QAMonitor.listFailed();
    }

    @RequestMapping("info")
    public Map<String, Object> info() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("down", eurekaMonitor.getDownSet());
        map.put("all", eurekaMonitor.getAllServices());
        return map;
    }

    @RequestMapping(value = "eureka-prometheus", produces = MediaType.TEXT_PLAIN_VALUE)
    public String eureka() {
        return eurekaMonitor.getRegistry().scrape();
    }
}
