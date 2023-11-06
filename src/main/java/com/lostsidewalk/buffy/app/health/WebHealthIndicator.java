package com.lostsidewalk.buffy.app.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
class WebHealthIndicator implements HealthIndicator {

    @Autowired
    ConcurrentHashMap<String, Integer> errorStatusMap;

    @Override
    public final Health getHealth(boolean includeDetails) {
        return HealthIndicator.super.getHealth(includeDetails);
    }

    @Override
    public final Health health() {
        log.info("Health check invoked");
        int sz = errorStatusMap.size();
        Map<String, String> healthDetails = new HashMap<>(sz);
        errorStatusMap.forEach((e, count) -> {
            String format = String.format("%sCount", e);
            String value = Integer.toString(count);
            healthDetails.put(format, value);
        });
        return new Health.Builder()
                .up()
                .withDetails(healthDetails)
                .build();
    }

    @Override
    public final String toString() {
        return "WebHealthIndicator{" + "errorStatusMap=" + errorStatusMap + '}';
    }
}
