package com.foresee.watchdog.applicationmonitor.controller;

import com.foresee.watchdog.applicationmonitor.service.Mornitor;
import com.foresee.watchdog.applicationmonitor.service.Lookup;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Log4j2
public class Hello {
    private final Lookup lookup;
    private final Mornitor mornitor;

    @SneakyThrows
    @RequestMapping("/")
    public void hell(@RequestParam String service) {
        final String location = lookup.serviceLocation(service);
        log.info(location);
        log.info("got {},{},{}", lookup.getBaseAutomationClass().configuration,
                lookup.getToken(), lookup.getSecret());
        mornitor.check();
    }
}
