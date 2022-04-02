package com.foresee.eureka.watcher;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class Controller {
    private final Monitor monitor;

    @RequestMapping("info")
    public Map<String, Object> info() {
        HashMap<String, Object> map = new HashMap<>();
        map.put("down", monitor.getDownSet());
        map.put("all", monitor.getAllServices());
        return map;
    }
}
