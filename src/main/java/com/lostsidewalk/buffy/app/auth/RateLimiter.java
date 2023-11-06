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

    final Bucket resolveBucket(String username) {
        Supplier<BucketConfiguration> configSupplier = getConfigSupplierForUser();
        RemoteBucketBuilder<byte[]> builder = buckets.builder();
        if (builder == null) {
            return null;
        } else {
            byte[] bytes = username.getBytes(UTF_8);
            return builder.build(bytes, configSupplier);
        }
    }

    private static Supplier<BucketConfiguration> getConfigSupplierForUser() {
        Duration period = Duration.ofMinutes(1L);
        Refill refill = Refill.intervally(20L, period);
        Bandwidth limit = Bandwidth.classic(20L, refill);
        return () -> (BucketConfiguration.builder()
                .addLimit(limit)
                .build());
    }

    @Override
    public final String toString() {
        return "RateLimiter{" +
                "buckets=" + buckets +
                '}';
    }
}
