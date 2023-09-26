package com.lostsidewalk.buffy.app.auth;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import io.github.bucket4j.redis.jedis.cas.JedisBasedProxyManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
@Service
public class RateLimiter {

    @Autowired
    public JedisBasedProxyManager buckets;

    public Bucket resolveBucket(String username) {
        Supplier<BucketConfiguration> configSupplier = getConfigSupplierForUser();
        RemoteBucketBuilder<byte[]> builder = buckets.builder();
        return builder == null ? null : builder.build(username.getBytes(UTF_8), configSupplier);
    }

    private Supplier<BucketConfiguration> getConfigSupplierForUser() {
        Refill refill = Refill.intervally(20, Duration.ofMinutes(1));
        Bandwidth limit = Bandwidth.classic(20, refill);
        return () -> (BucketConfiguration.builder()
                .addLimit(limit)
                .build());
    }
}
