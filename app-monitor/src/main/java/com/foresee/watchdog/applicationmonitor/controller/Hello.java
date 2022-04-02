package com.foresee.watchdog.applicationmonitor.controller;

import com.foresee.watchdog.applicationmonitor.service.Monitor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequiredArgsConstructor
@Log4j2
public class Hello {
    private final Monitor monitor;

    @SneakyThrows
    @RequestMapping("/failed")
    public Set<String> failed() {
        return monitor.listFailed();
    }
}
