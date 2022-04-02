package com.foresee.eureka.watcher.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class EurekaInstance {
    private String hostName;
    private String app;
    private String ipAddr;
    private String status;
    private String healthCheckUrl;

    public boolean isDown() {
        return !"UP".equalsIgnoreCase(status);
    }
}
